package com.audioplayer.model;

import lombok.Data;

@Data
public class AudioFile {
    private Long id;
    private String fileName;
    private String filePath;
    private String scheduledTime;
    
    // 构造函数、getter和setter方法
} 