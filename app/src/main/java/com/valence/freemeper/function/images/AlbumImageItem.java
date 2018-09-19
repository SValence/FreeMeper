package com.valence.freemeper.function.images;

import java.io.Serializable;

@SuppressWarnings("serial")
public class AlbumImageItem implements Serializable {

	private String imageId;
	private String imagePath;
	private String thumbnailPath;

	public String getImageId() {
		return imageId;
	}

	public void setImageId(String imageId) {
		this.imageId = imageId;
	}

	public String getImagePath() {
		return imagePath;
	}

	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}

	public String getThumbnailPath() {
		return thumbnailPath;
	}

	public void setThumbnailPath(String thumbnailPath) {
		this.thumbnailPath = thumbnailPath;
	}
}
