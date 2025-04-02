package com.audioplayer.controller;

import com.audioplayer.model.AudioFile;
import com.audioplayer.service.AudioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audio")
public class AudioController {
    
    @Autowired
    private AudioService audioService;
    
    @PostMapping("/schedule")
    public ResponseEntity<?> scheduleAudio(@RequestParam("file") MultipartFile file,
                                         @RequestParam("scheduleTime") String scheduleTime) {
        try {
            AudioFile audioFile = audioService.scheduleAudio(file, scheduleTime);
            return ResponseEntity.ok(audioFile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @GetMapping("/playlist")
    public List<AudioFile> getPlaylist() {
        return audioService.getScheduledAudios();
    }
    
    @PostMapping("/play/{id}")
    public ResponseEntity<?> playAudio(@PathVariable Long id) {
        try {
            AudioFile audioFile = audioService.getScheduledAudios().stream()
                .filter(audio -> audio.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("音频文件未找到"));
                
            audioService.playAudio(audioFile);
            return ResponseEntity.ok(Map.of("message", "开始播放"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/stop")
    public ResponseEntity<?> stopAudio() {
        audioService.stopAudio();
        return ResponseEntity.ok(Map.of("message", "停止播放"));
    }
    
    @GetMapping("/status")
    public ResponseEntity<?> getPlayStatus() {
        return ResponseEntity.ok(Map.of("playing", audioService.isPlaying()));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAudio(@PathVariable Long id) {
        try {
            audioService.deleteAudio(id);
            return ResponseEntity.ok(Map.of("message", "音频文件已删除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/stream/{id}")
    public ResponseEntity<Resource> streamAudio(@PathVariable Long id) {
        try {
            AudioFile audioFile = audioService.findById(id);
            if (audioFile == null) {
                return ResponseEntity.notFound().build();
            }

            Path path = Paths.get(audioFile.getFilePath());
            Resource resource = new FileSystemResource(path);

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + audioFile.getFileName() + "\"")
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 