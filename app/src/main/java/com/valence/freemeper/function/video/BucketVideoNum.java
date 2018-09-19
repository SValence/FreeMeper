package com.valence.freemeper.function.video;

import java.io.Serializable;

public class BucketVideoNum implements Serializable {
    public VideoBucket bucket;
    public int videoIndex;

    public BucketVideoNum(VideoBucket bucket, int videoIndex) {
        this.bucket = bucket;
        this.videoIndex = videoIndex;
    }
}
