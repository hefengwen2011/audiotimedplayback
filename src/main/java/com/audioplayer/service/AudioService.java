package com.audioplayer.service;

import com.audioplayer.model.AudioFile;
import com.audioplayer.util.MPEGPlayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class AudioService {
    
    @Value("${audio.upload-dir}")
    private String uploadDir;
    
    @Autowired
    private MPEGPlayer mpegPlayer;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final List<AudioFile> scheduledAudios = new ArrayList<>();
    private Long currentId = 1L;
    
    public AudioFile scheduleAudio(MultipartFile file, String scheduleTime) throws IOException {
        // 验证文件类型
        if (!isSupportedAudioFile(file)) {
            throw new IllegalArgumentException("不支持的音频格式！仅支持MP3/MPEG格式");
        }
        
        // 确保上传目录存在
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String fileName = System.currentTimeMillis() + "_" + originalFilename;
        Path filePath = uploadPath.resolve(fileName);
        
        // 保存文件
        Files.copy(file.getInputStream(), filePath);
        
        // 创建音频文件记录
        AudioFile audioFile = new AudioFile();
        audioFile.setId(currentId++);
        audioFile.setFileName(originalFilename);
        audioFile.setFilePath(filePath.toString());
        audioFile.setScheduledTime(scheduleTime);
        
        // 计算延迟时间
        LocalDateTime scheduledDateTime = LocalDateTime.parse(scheduleTime);
        long delay = ChronoUnit.MILLIS.between(LocalDateTime.now(), scheduledDateTime);
        
        // 设置定时任务
        scheduler.schedule(() -> {
            try {
                playAudio(audioFile);
            } catch (Exception e) {
                System.err.println("播放音频失败: " + e.getMessage());
            }
        }, delay, TimeUnit.MILLISECONDS);
        
        scheduledAudios.add(audioFile);
        return audioFile;
    }
    
    private boolean isSupportedAudioFile(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            if (fileName == null) {
                return false;
            }
            
            // 检查文件扩展名
            String extension = fileName.toLowerCase();
            return extension.endsWith(".mp3") || 
                   extension.endsWith(".mpeg") || 
                   extension.endsWith(".mpg");
                   
        } catch (Exception e) {
            return false;
        }
    }
    
    public void playAudio(AudioFile audioFile) throws Exception {
        mpegPlayer.play(audioFile.getFilePath());
    }
    
    public void stopAudio() {
        mpegPlayer.stop();
    }
    
    public List<AudioFile> getScheduledAudios() {
        return scheduledAudios;
    }
    
    public boolean isPlaying() {
        return mpegPlayer.isPlaying();
    }
    
    public AudioFile findById(Long id) {
        return scheduledAudios.stream()
                .filter(audio -> audio.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
    
    public void deleteAudio(Long id) throws IOException {
        AudioFile audioFile = findById(id);
        if (audioFile == null) {
            throw new IllegalArgumentException("未找到指定的音频文件");
        }
        
        // 如果文件正在播放，先停止播放
        if (mpegPlayer.getCurrentPlaying() != null && 
            mpegPlayer.getCurrentPlaying().equals(audioFile.getFilePath())) {
            mpegPlayer.stop();
        }
        
        // 删除物理文件
        Path filePath = Paths.get(audioFile.getFilePath());
        Files.deleteIfExists(filePath);
        
        // 从列表中移除
        scheduledAudios.removeIf(audio -> audio.getId().equals(id));
    }
} 