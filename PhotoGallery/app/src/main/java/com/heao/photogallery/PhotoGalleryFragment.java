package com.heao.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends Fragment {
    private interface OnBottomCallBack {
        boolean isOnBottom();

        void onBottom();
    }

    public static final LruCache<String, Bitmap> mCache = new LruCache<>(100);
    private static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<Integer> mThumbnailDownloader;
    private GridLayoutManager mLayoutManager;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    /**
     * 工具类 在后台异步获取图片的元信息
     */
    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {
        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {
            return new FlickrFetchr().fetchItems();
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            super.onPostExecute(items);
            // 将所有新元素加至末尾
            mItems.addAll(items);
            setAdapter();
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        private ImageView mImageView;

        public PhotoHolder(@NonNull View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.item_image_view);
        }

        public void bindDrawable(Drawable drawable) {
            mImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @NonNull
        @Override
        public PhotoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.list_item_gallery, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoHolder holder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            String url = galleryItem.getUrl();
            Drawable placeholder;
            Bitmap bitmap;

            bitmap = mCache.get(url);
            if (bitmap == null) {
                // 无cache则先使用占位符
                placeholder = getResources().getDrawable(R.drawable.bill_up_close);
                // 将图片下载请求加入等待队列，交给Handler处理
                mThumbnailDownloader.queueThumbnail(position, galleryItem.getUrl());
            } else {
                // 使用缓存
                placeholder = new BitmapDrawable(getResources(), bitmap);
            }
            holder.bindDrawable(placeholder);
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    /**
     * 屏幕滚动监听器，当屏幕滚动到底部时，向后台异步请求新图片
     */
    private class OnBottomListener extends RecyclerView.OnScrollListener
            implements OnBottomCallBack {
        RecyclerView mView;

        public OnBottomListener(RecyclerView view) {
            mView = view;
        }

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (isOnBottom()) {
                Toast.makeText(getActivity(), "Loading next page...", Toast.LENGTH_SHORT).show();
                onBottom();
            }

            // 缓存当前显示界面的前十个和后十个item
            int start = mLayoutManager.findFirstVisibleItemPosition();
            int end = mLayoutManager.findLastVisibleItemPosition();
            start = Math.max(start - 10, 0);
            end = Math.min(end + 10, mItems.size() - 1);
            Log.i(TAG, "First: " + start + " and Last: " + end);

            for(int i = 0; i < 10; i++) {
                int position1 = start + i;
                String url1 = mItems.get(position1).getUrl();
                int position2 = end - i;
                String url2 = mItems.get(position2).getUrl();
                if (mCache.get(url1) == null) {
                    mThumbnailDownloader.queueThumbnail(position1, url1);
                }
                if (mCache.get(url2) == null) {
                    mThumbnailDownloader.queueThumbnail(position2, url2);
                }
            }
        }

        @Override
        public boolean isOnBottom() {
            if (mView == null) {
                return false;
            }
            return mView.computeVerticalScrollExtent() + mView.computeVerticalScrollOffset()
                    >= mView.computeVerticalScrollRange();
        }

        @Override
        public void onBottom() {
            Log.i(TAG, "Recycler view is dragged to bottom.");
            // 创建新的异步任务并将获取的结果添加到mList中
            new FetchItemsTask().execute();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 保证旋转时fragment状态不被销毁
        setRetainInstance(true);
        new FetchItemsTask().execute();

        // 在异步任务之后启动Handler，防止出现线程冲突
        // 主线程Handler
        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        // 监听器，图片下载完之后及时更新UI
        mThumbnailDownloader.setThumbnailDownloadListener((obj, bitmap) -> {
            mPhotoRecyclerView.getAdapter().notifyItemChanged(obj);
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = view.findViewById(R.id.photo_recycler_view);
        // 通过layoutManager获取当前显示在屏幕上的item的position
        mLayoutManager = new GridLayoutManager(getActivity(), 3);
        mPhotoRecyclerView.setLayoutManager(mLayoutManager);
        // 滑动监听器，滑至底部时加载下一页
        OnBottomListener listener = new OnBottomListener(mPhotoRecyclerView);
        mPhotoRecyclerView.addOnScrollListener(listener);

        setAdapter();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    private void setAdapter() {
        if (isAdded()) {
            // 确保fragment已被添加给相关的activity
            if (mPhotoRecyclerView.getAdapter() == null) {
                mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
            } else {
                mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
            }
        }
    }
}
