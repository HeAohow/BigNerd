package com.heao.photogallery;

import androidx.annotation.NonNull;

/**
 * 存储图片的原信息
 * 需要通过图片URL下载图片内容
 */
public class GalleryItem {
    private String mCaption;
    private String mId;
    private String mUrl;

    @NonNull
    @Override
    public String toString() {
        return mCaption;
    }

    public String getCaption() {
        return mCaption;
    }

    public void setCaption(String caption) {
        mCaption = caption;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }
}
