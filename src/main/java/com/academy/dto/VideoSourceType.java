package com.academy.dto;

public enum VideoSourceType {
    LOCAL,      // Stored on server filesystem
    YOUTUBE,    // YouTube video (embed/watch URL)
    GOOGLE_DRIVE, // Google Drive hosted
    S3,         // Amazon S3 bucket
    EXTERNAL    // Any other external URL
}
