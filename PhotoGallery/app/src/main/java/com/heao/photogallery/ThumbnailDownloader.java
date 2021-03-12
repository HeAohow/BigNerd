package com.heao.photogallery;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThumbnailDownloader<T> extends HandlerThread {
    public static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;
    private Handler mRequestHandler;
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();

    public Boolean mHasQuit = false;

    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T obj, Bitmap bitmap);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        mThumbnailDownloadListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T obj = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(obj));
                    handleRequest(obj);
                }
            }
        };
    }

    private void handleRequest(final T obj) {
        try {
            final String url = mRequestMap.get(obj);
            if (url == null) {
                return;
            }
            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            // 加入缓存
            PhotoGalleryFragment.mCache.put(url, bitmap);
            Log.i(TAG, "Bitmap created and added to cache.");

            // 利用引用的主线程中的Handler更新UI
            mResponseHandler.post(() -> {
                if (mRequestMap.get(obj) != url || mHasQuit) {
                    return;
                }
                mRequestMap.remove(obj);
                mThumbnailDownloadListener.onThumbnailDownloaded(obj, bitmap);
            });
        } catch (IOException e) {
            Log.e(TAG, "Error downloading image", e);
        }
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    public void queueThumbnail(T obj, String url) {
        Log.i(TAG, "Got a URL: " + url);
        if (url == null) {
            mRequestMap.remove(obj);
        } else {
            mRequestMap.put(obj, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, obj).sendToTarget();
        }
    }

    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestMap.clear();
    }
}
