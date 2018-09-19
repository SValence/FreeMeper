package com.valence.freemeper.function.video;

import java.io.Serializable;
import java.util.ArrayList;

public class VideoBucket implements Serializable {
    public int count = 0;
    private String bucketId;
    private String bucketName;
    private String bucketPath;
    private String coverPath;
    public ArrayList<VideoItem> videoList;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getBucketId() {
        return bucketId;
    }

    public void setBucketId(String buckrtId) {
        this.bucketId = buckrtId;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getCoverPath() {
        return coverPath;
    }

    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
    }

    public String getBucketPath() {
        return bucketPath;
    }

    public void setBucketPath(String bucketPath) {
        this.bucketPath = bucketPath;
    }

    public ArrayList<VideoItem> getVideoList() {
        return videoList;
    }
}
