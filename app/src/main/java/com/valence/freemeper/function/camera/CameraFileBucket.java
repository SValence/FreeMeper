package com.valence.freemeper.function.camera;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

public class CameraFileBucket implements Serializable {
    private File[] files;
    private HashMap<String, String> videoPathMap;

    public void setFiles(File[] files) {
        this.files = files;
    }

    public File[] getFiles() {
        return files;
    }

    public HashMap<String, String> getVideoPathMap() {
        return videoPathMap;
    }

    public void setVideoPathMap(HashMap<String, String> videoPathMap) {
        this.videoPathMap = videoPathMap;
    }
}
