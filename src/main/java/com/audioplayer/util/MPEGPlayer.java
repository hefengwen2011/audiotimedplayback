package com.audioplayer.util;

import org.springframework.stereotype.Component;
import javax.sound.sampled.*;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MPEGPlayer implements AudioPlayer {
    private Clip clip;
    private String currentPlaying;
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private Thread playerThread;

    @Override
    public void play(String filePath) throws Exception {
        stop();
        
        playerThread = new Thread(() -> {
            try {
                // 使用BufferedInputStream来改善性能
                BufferedInputStream bufferedIn = new BufferedInputStream(
                    new FileInputStream(filePath)
                );
                
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
                
                // 获取音频格式
                AudioFormat baseFormat = audioStream.getFormat();
                
                // 转换为PCM格式
                AudioFormat decodedFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,  // 编码技术
                    baseFormat.getSampleRate(),       // 采样率
                    16,                               // 采样位数
                    baseFormat.getChannels(),         // 声道数
                    baseFormat.getChannels() * 2,     // 帧大小
                    baseFormat.getSampleRate(),       // 帧率
                    false                             // 大端序
                );
                
                // 转换音频流
                AudioInputStream decodedStream = AudioSystem.getAudioInputStream(
                    decodedFormat, 
                    audioStream
                );
                
                // 获取数据行信息
                DataLine.Info info = new DataLine.Info(
                    Clip.class, 
                    decodedFormat
                );
                
                // 确保系统支持该数据行
                if (!AudioSystem.isLineSupported(info)) {
                    throw new LineUnavailableException("不支持的音频格式");
                }
                
                // 获取并打开数据行
                clip = (Clip) AudioSystem.getLine(info);
                clip.open(decodedStream);
                
                currentPlaying = filePath;
                isPlaying.set(true);
                
                // 添加播放完成监听器
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        isPlaying.set(false);
                        clip.close();
                        try {
                            decodedStream.close();
                            audioStream.close();
                            bufferedIn.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                
                // 开始播放
                clip.start();
                
                // 等待播放完成
                while (isPlaying.get()) {
                    Thread.sleep(100);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                isPlaying.set(false);
            }
        });
        
        playerThread.start();
    }

    @Override
    public void stop() {
        if (clip != null) {
            clip.stop();
            clip.close();
            isPlaying.set(false);
        }
        if (playerThread != null && playerThread.isAlive()) {
            playerThread.interrupt();
            try {
                playerThread.join(1000); // 等待线程结束
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        currentPlaying = null;
    }

    @Override
    public boolean isPlaying() {
        return isPlaying.get();
    }

    @Override
    public String getCurrentPlaying() {
        return currentPlaying;
    }
} 