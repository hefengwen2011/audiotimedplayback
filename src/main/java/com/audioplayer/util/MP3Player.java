package com.audioplayer.util;

import javazoom.jl.player.Player;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MP3Player {
    private Player player;
    private Thread playerThread;
    private String currentPlaying;
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);

    public void play(String filePath) {
        // 如果当前正在播放，先停止
        stop();
        
        playerThread = new Thread(() -> {
            try {
                FileInputStream fis = new FileInputStream(filePath);
                player = new Player(fis);
                currentPlaying = filePath;
                isPlaying.set(true);
                player.play();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isPlaying.set(false);
            }
        });
        
        playerThread.start();
    }

    public void stop() {
        if (player != null) {
            player.close();
            isPlaying.set(false);
        }
        if (playerThread != null && playerThread.isAlive()) {
            playerThread.interrupt();
        }
        currentPlaying = null;
    }

    public boolean isPlaying() {
        return isPlaying.get();
    }

    public String getCurrentPlaying() {
        return currentPlaying;
    }
} 