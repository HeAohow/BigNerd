package com.heao.photogallery;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.collection.LruCache;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends VisibleFragment {
    // RecyclerView滑动到底部时的回调函数
    public interface OnBottomCallBack {
        boolean isOnBottom();

        void onBottom();
    }

    private static final String TAG = "PhotoGalleryFragment";
    // 图片缓存
    private static final int CACHE_MAX_NUM = 100;
    public static final LruCache<String, Bitmap> mCache = new LruCache<>(CACHE_MAX_NUM);

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<Integer> mThumbnailDownloader;
    private GridLayoutManager mLayoutManager;

    /**
     * 工具类 在后台异步获取图片的元信息
     */
    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {
        private List<GalleryItem> mGalleryItems;
        private String mQuery;

        public FetchItemsTask(List<GalleryItem> items, String query) {
            mGalleryItems = items;
            mQuery = query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {
            if (mQuery == null) {
                // 默认查询
                return new FlickrFetchr().fetchRecentPhotos();
            } else {
                // 根据用户输入SearchView的内容查询
                return new FlickrFetchr().searchPhotos(mQuery);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            super.onPostExecute(items);
            // 将所有新元素加至末尾
            mGalleryItems.addAll(items);
            setAdapter();
        }
    }

    /**
     * RecyclerView Holder
     */
    private class PhotoHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        private ImageView mImageView;
        private GalleryItem mGalleryItem;

        public PhotoHolder(@NonNull View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.item_image_view);
            mImageView.setOnClickListener(this);
        }

        public void bindDrawable(Drawable drawable) {
            mImageView.setImageDrawable(drawable);
        }

        public void bindGalleryItem(GalleryItem galleryItem) {
            mGalleryItem = galleryItem;
        }

        @Override
        public void onClick(View view) {
            Intent i = PhotoPageActivity.newIntent(getActivity(), mGalleryItem.getPhotoPageUri());
            startActivity(i);
        }
    }

    /**
     * RecyclerView Adapter
     */
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
            holder.bindGalleryItem(galleryItem);
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
            // TODO 这一块代码可以优化 借鉴别人的解决方案
            if (mItems.size() < 10) {
                return;
            }
            // 缓存当前显示界面的前十个和后十个item
            int start = mLayoutManager.findFirstVisibleItemPosition();
            int end = mLayoutManager.findLastVisibleItemPosition();
            start = Math.max(start - 10, 0);
            end = Math.min(end + 10, mItems.size() - 1);
            Log.i(TAG, "First: " + start + " and Last: " + end);

            for (int i = 0; i < 10; i++) {
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
            // 创建新的异步任务并将获取的结果添加到mList中
            updateItems();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 保证旋转时fragment状态不被销毁
        setRetainInstance(true);
        setHasOptionsMenu(true);
        updateItems();

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
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "QueryTextSubmit: " + query);
                QueryPreferences.setStoredQuery(getActivity(), query);
                updateUI();
                updateItems();
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "QueryTextChangeL " + newText);
                return false;
            }
        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_item_clear) {
            // 清除当前搜索的图片 并执行默认搜索
            QueryPreferences.setStoredQuery(getActivity(), null);
            updateUI();
            updateItems();
            return true;
        } else if (id == R.id.menu_item_toggle_polling) {
            // PollService 开关实现
            boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
            PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
            // 更新 menu item 的显示
            getActivity().invalidateOptionsMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 执行后台任务 获取相关搜索内容的 GalleryItem
     */
    private void updateItems() {
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(mItems, query).execute();
    }

    /**
     * 根据 mItems 更新 RecyclerView
     */
    private void updateUI() {
        if (mPhotoRecyclerView != null) {
            mItems.clear();
            mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
//            mItems = new ArrayList<>();
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 销毁后台线程
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    /**
     * 初始化 或 更新RecyclerView视图
     */
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
