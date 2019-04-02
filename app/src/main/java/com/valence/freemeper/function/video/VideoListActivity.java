package com.valence.freemeper.function.video;

import android.app.ProgressDialog;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.valence.freemeper.R;
import com.valence.freemeper.base.BaseActivity;
import com.valence.freemeper.base.IFindView;
import com.valence.freemeper.database.DatabaseHelper;
import com.valence.freemeper.holder.BottomToolHolder;
import com.valence.freemeper.tool.AppContext;
import com.valence.freemeper.tool.CommonMethod;
import com.valence.freemeper.tool.GlideApp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static android.provider.MediaStore.Video.Thumbnails.MINI_KIND;
import static com.valence.freemeper.database.DatabaseHelper.FILE_ID;
import static com.valence.freemeper.database.DatabaseHelper.FILE_PATH;
import static com.valence.freemeper.database.DatabaseHelper.FILE_THUMB_PATH;
import static com.valence.freemeper.tool.AppContext.THUMB_FILE_END;

public class VideoListActivity extends BaseActivity implements IFindView, View.OnClickListener {

    private static final String TAG = "VideoListActivity";
    public static final String VIDEO_LIST_KEY = "video_list";
    public static final String VIDEO_LIST_INDEX_KEY = "list_index";

    public static final int VIDEO_LIST_VIEW_REQUEST = 20001;
    public static final int VIDEO_LIST_VIEW_RESPONSE_N = 20002;
    public static final int VIDEO_LIST_VIEW_RESPONSE_M = 20003;

    private RecyclerView videoListView;
    private ArrayList<VideoBucket> videoList;
    private VideoListAdapter adapter;
    private TextView headText;
    private ImageView back;
    private boolean selectMode;
    private FrameLayout bottomView;
    public TextView all;
    public TextView none;
    public TextView backSel;
    public TextView rename;
    public TextView delete;
    private ArrayList<VideoBucket> selectItems;
    private LocalVideoHelper helper;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_list);

        findView();
        setVariate();
        setListener();
        loadVideoList();
        getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(value = Lifecycle.Event.ON_DESTROY)
            public void destroy() {
                if (helper != null) helper.clear();
            }
        });
    }

    private void loadVideoList() {
        dialog = ProgressDialog.show(this, null, getString(R.string.loading));
        helper = LocalVideoHelper.getInstance(this);
        helper.setListLoader(list -> {
            back.postDelayed(() -> {
                if (dialog != null) dialog.dismiss();
            }, 200);
            if (videoList == null) videoList = new ArrayList<>();
            else videoList.clear();
            if (list != null) videoList.addAll(list);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        });
        helper.execute(false);
    }

    @Override
    public void setVariate() {
        selectMode = false;

        videoList = new ArrayList<>();
        selectItems = new ArrayList<>();
        adapter = new VideoListAdapter(this, R.layout.free_video_list_items);
        videoListView.setLayoutManager(new LinearLayoutManager(this));
        videoListView.setAdapter(adapter);

        headText.setText(R.string.video);
    }

    @Override
    public void findView() {
        headText = findViewById(R.id.free_tool_tip);
        back = findViewById(R.id.free_tool_left_img);
        bottomView = findViewById(R.id.free_bottom_tool_layout);
        videoListView = findViewById(R.id.videoRecyclerList);

        all = findViewById(R.id.free_bottom_select_all);
        none = findViewById(R.id.free_bottom_all_unSelect);
        backSel = findViewById(R.id.free_bottom_select_back);
        rename = findViewById(R.id.free_bottom_rename);
        delete = findViewById(R.id.free_bottom_delete);
    }

    @Override
    public void setListener() {
        back.setOnClickListener(this);
        all.setOnClickListener(this);
        none.setOnClickListener(this);
        backSel.setOnClickListener(this);
        rename.setOnClickListener(this);
        delete.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.free_tool_left_img:
                onBackPressed();
                break;
            case R.id.free_bottom_select_all:
                selectItems.clear();
                selectItems.addAll(videoList);
                all.setVisibility(View.GONE);
                none.setVisibility(View.VISIBLE);
                updateList();
                break;
            case R.id.free_bottom_all_unSelect:
                selectItems.clear();
                none.setVisibility(View.GONE);
                all.setVisibility(View.VISIBLE);
                updateList();
                break;
            case R.id.free_bottom_select_back:
                doBackSelect();
                break;
            case R.id.free_bottom_rename:
                AppContext.showToast("Rename");
                break;
            case R.id.free_bottom_delete:
                doDelete();
                adapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }

    private void doBackSelect() {
        ArrayList<VideoBucket> temp = new ArrayList<>();
        for (VideoBucket bucket : videoList) {
            if (selectItems.contains(bucket)) continue;
            temp.add(bucket);
        }
        selectItems.clear();
        selectItems.addAll(temp);
        updateList();
    }

    private void updateList() {
        adapter.notifyDataSetChanged();
        headText.setText(getString(R.string.has_select_list, selectItems.size()));
    }

    private void doDelete() {
        AppContext.showToast(("Choose " + selectItems.size() + " to Delete"));
        if (!selectItems.isEmpty()) onBackPressed();
    }

    @Override
    public void onBackPressed() {
        if (selectMode) {
            selectMode = false;
            setBottomToolVisibility(false);
            selectItems.clear();
            adapter.notifyDataSetChanged();
            headText.setText(R.string.video);
            all.setVisibility(View.VISIBLE);
            none.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    public class VideoListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener, View.OnLongClickListener {

        private LayoutInflater inflater;
        private int itemLayoutResId;

        VideoListAdapter(Context context, int itemLayout) {
            inflater = LayoutInflater.from(context);
            itemLayoutResId = itemLayout;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                View view = inflater.inflate(itemLayoutResId, parent, false);
                view.setOnClickListener(this);
                view.setOnLongClickListener(this);
                return new VideoListHolder(view);
            } else if (viewType == 1) {
                return new BottomToolHolder(inflater.inflate(R.layout.layout_bottom_tool_size, parent, false));
            } else {
                // 事实上这里是错误的, 暂时处理成没有内容的布局
                // TODO...
                return new BottomToolHolder(inflater.inflate(R.layout.layout_bottom_tool_size, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder1, int position) {
            if (holder1 instanceof VideoListHolder) {
                VideoBucket bucket = videoList.get(position);
                if (bucket == null) {
                    Log.e(TAG, "This Video Bucket is Null!_No." + position);
                    return;
                }
                VideoListHolder holder = (VideoListHolder) holder1;
                if (TextUtils.isEmpty(bucket.getCoverPath())) {
                    VideoItem item = bucket.getVideoList().get(0);
                    String thumbPath = item.getThumbnailPath();
                    if (TextUtils.isEmpty(thumbPath)) {
                        if (holder.disposable != null) holder.disposable.dispose();
                        holder.disposable = Single.fromCallable(() -> {
                            Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(item.getVideoPath(), MINI_KIND);
                            if (bitmap == null) throw new IOException("Get Video BitMap Error");
                            String filePath = AppContext.getThumbDirVideo() + item.getVideoName() + THUMB_FILE_END;
                            if (CommonMethod.saveBitmap2JPG(bitmap, filePath)) {
                                ContentValues c = new ContentValues();
                                c.put(FILE_ID, item.getVideoId());
                                c.put(FILE_PATH, item.getVideoPath());
                                c.put(FILE_THUMB_PATH, filePath);
                                DatabaseHelper.insert(getApplicationContext(), c);
                                item.setThumbnailPath(filePath);
                                bucket.setCoverPath(filePath);
                            }
                            return bitmap;
                        })
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(bitmap -> {
                                    holder.coverImage.setImageBitmap(bitmap);
                                    holder.coverImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                }, throwable -> {
                                    Log.e(TAG, "Get Video BitMap Error");
                                    throwable.printStackTrace();
                                    holder.coverImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                                });
                    } else {
                        bucket.setCoverPath(thumbPath);
                        holder.coverImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        GlideApp.with(VideoListActivity.this)
                                .load(new File(thumbPath))
                                .error(getDrawable(R.mipmap.free_pic_error))
                                .into(holder.coverImage);
                    }
                } else {
                    holder.coverImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    GlideApp.with(VideoListActivity.this)
                            .load(new File(bucket.getCoverPath()))
                            .error(getDrawable(R.mipmap.free_pic_error))
                            .into(holder.coverImage);
                }
                if (selectMode) {
                    holder.ahead.setVisibility(View.GONE);
                    holder.select.setVisibility(View.VISIBLE);
                    holder.select.setChecked(selectItems.contains(bucket));
//                    headText.setText(R.string.has_select_list,);
                } else {
//                    bucket.setSelect(false);
                    holder.select.setVisibility(View.GONE);
                    holder.ahead.setVisibility(View.VISIBLE);
                }
                // holder.coverImage.setImageURI(Uri.fromFile(new File(bucket.coverPath)));
                holder.bucketName.setText(bucket.getBucketName());
                holder.bucketPath.setText(bucket.getBucketPath());
                holder.bucketCount.setText(String.valueOf(bucket.count));
                holder.itemView.setTag(holder);
            } else if (holder1 instanceof BottomToolHolder) {
                BottomToolHolder holder = (BottomToolHolder) holder1;
                holder.itemView.setTag(holder);
            } else {
                // 什么也不做
                // TODO...
                Log.e(TAG, "Holder Type Error!");
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == getItemCount() - 1) return 1;
            return super.getItemViewType(position);
        }

        @Override
        public int getItemCount() {
            return videoList == null ? 1 : videoList.size() + 1;
        }

        @Override
        public void onClick(View v) {
            VideoListHolder holder = (VideoListHolder) v.getTag();
            int position = holder.getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) {
                return;
            }
            VideoBucket bucket = videoList.get(position);
            if (!selectMode) {
                Intent intent = new Intent(VideoListActivity.this, VideoActivity.class);
                intent.putExtra(VIDEO_LIST_KEY, bucket);
                intent.putExtra(VIDEO_LIST_INDEX_KEY, position);
                v.postDelayed(() -> startActivityForResult(intent, VIDEO_LIST_VIEW_REQUEST), 100);
            } else {
                boolean state = !holder.select.isChecked();
                holder.select.setChecked(state);
                if (state) selectItems.add(bucket);
                else selectItems.remove(bucket);
                headText.setText(getString(R.string.has_select_list, selectItems.size()));
            }
        }

        @Override
        public boolean onLongClick(View v) {
            VideoListHolder holder = (VideoListHolder) v.getTag();
            int position = holder.getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return false;
            if (selectMode) {
                // updateListData(holder, position);
                // 这里不需要上面的update语句, 因为longClick返回false后, 会回调onClick短按事件
                return false;
            }
            selectMode = true;
            setBottomToolVisibility(true);
            selectItems.clear();
            selectItems.add(videoList.get(position));
            adapter.notifyDataSetChanged();
            headText.setText(getString(R.string.has_select_list, selectItems.size()));
            return true;
        }
    }

    private void setBottomToolVisibility(boolean visibale) {
        bottomView.setVisibility(visibale ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;
        if (requestCode == VIDEO_LIST_VIEW_REQUEST) {
            if (resultCode == VIDEO_LIST_VIEW_RESPONSE_M) {
                int idx = data.getIntExtra(VIDEO_LIST_INDEX_KEY, -1);
                if (idx < 0) return;
                VideoBucket bucket = (VideoBucket) data.getSerializableExtra(VIDEO_LIST_KEY);
                ArrayList<VideoItem> list = bucket.getVideoList();
                if (list == null) return;
                videoList.set(idx, bucket);
                adapter.notifyItemChanged(idx);
            }
        }
    }

    class VideoListHolder extends RecyclerView.ViewHolder {

        private ImageView coverImage;
        private TextView bucketName;
        private TextView bucketPath;
        private TextView bucketCount;
        private ImageView ahead;
        private CheckBox select;
        private Disposable disposable;

        VideoListHolder(View itemView) {
            super(itemView);
            coverImage = itemView.findViewById(R.id.free_videoList_cover);
            bucketName = itemView.findViewById(R.id.free_videoList_dirname);
            bucketPath = itemView.findViewById(R.id.free_videoList_dirpath);
            bucketCount = itemView.findViewById(R.id.free_videoList_num);
            ahead = itemView.findViewById(R.id.free_videoList_ahead);
            select = itemView.findViewById(R.id.free_videoList_check);
        }
    }

//    class Holder {
//        public TextView all;
//        public TextView none;
//        public TextView back;
//        public TextView rename;
//        public TextView delete;
//
//        public Holder(View itemView) {
//            all = itemView.findViewById(R.id.free_bottom_select_all);
//            none = itemView.findViewById(R.id.free_bottom_all_unSelect);
//            back = itemView.findViewById(R.id.free_bottom_select_back);
//            rename = itemView.findViewById(R.id.free_bottom_rename);
//            delete = itemView.findViewById(R.id.free_bottom_delete);
//        }
//
//        public void clear() {
//            all = null;
//            none = null;
//            back = null;
//            rename = null;
//            delete = null;
//        }
//    }
}
