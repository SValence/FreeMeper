package com.valence.freemeper.function.album;

import com.valence.freemeper.function.images.AlbumImageItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class AlbumImageBucket implements Serializable {

    public int count = 0;
    private String bucketName;
    private String bucketPath;
    private String bucketCoverPath;
    public ArrayList<AlbumImageItem> imageList;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public ArrayList<AlbumImageItem> getImageList() {
        return imageList;
    }

    public void setImageList(ArrayList<AlbumImageItem> imageList) {
        this.imageList = imageList;
    }

    public String getBucketPath() {
        return bucketPath;
    }

    public void setBucketPath(String bucketPath) {
        this.bucketPath = bucketPath;
    }

    public String getBucketCoverPath() {
        return bucketCoverPath;
    }

    public void setBucketCoverPath(String bucketCoverPath) {
        this.bucketCoverPath = bucketCoverPath;
    }
}
