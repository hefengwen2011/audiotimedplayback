package com.audioplayer.util;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public interface AudioPlayer {
    void play(String filePath) throws Exception;
    void stop();
    boolean isPlaying();
    String getCurrentPlaying();
}

class AudioPlayerImpl implements AudioPlayer {
    private Clip clip;
    private String currentPlaying;

    public void play(String filePath) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        // 如果当前有音频在播放，先停止
        stop();

        File audioFile = new File(filePath);
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
        clip = AudioSystem.getClip();
        clip.open(audioStream);
        clip.start();
        currentPlaying = filePath;
    }

    public void stop() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.close();
        }
    }

    public String getCurrentPlaying() {
        return currentPlaying;
    }

    public boolean isPlaying() {
        return clip != null && clip.isRunning();
    }
}
