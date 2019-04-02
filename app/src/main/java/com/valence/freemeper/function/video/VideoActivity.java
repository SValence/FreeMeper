package com.valence.freemeper.function.video;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.valence.freemeper.BuildConfig;
import com.valence.freemeper.R;
import com.valence.freemeper.base.BaseActivity2;
import com.valence.freemeper.database.DatabaseHelper;
import com.valence.freemeper.holder.BottomToolHolder;
import com.valence.freemeper.holder.EmptyViewHolder;
import com.valence.freemeper.tool.AppContext;
import com.valence.freemeper.tool.CommonMethod;
import com.valence.freemeper.tool.GlideApp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.provider.MediaStore.Video.Thumbnails.MINI_KIND;
import static com.valence.freemeper.database.DatabaseHelper.FILE_ID;
import static com.valence.freemeper.database.DatabaseHelper.FILE_PATH;
import static com.valence.freemeper.database.DatabaseHelper.FILE_THUMB_PATH;
import static com.valence.freemeper.function.video.VideoListActivity.VIDEO_LIST_INDEX_KEY;
import static com.valence.freemeper.function.video.VideoListActivity.VIDEO_LIST_KEY;
import static com.valence.freemeper.function.video.VideoListActivity.VIDEO_LIST_VIEW_RESPONSE_M;
import static com.valence.freemeper.function.video.VideoListActivity.VIDEO_LIST_VIEW_RESPONSE_N;

public class VideoActivity extends BaseActivity2 implements View.OnClickListener {

    private TextView headText;
    private ImageView back;
    private ArrayList<VideoItem> videoList;
    private ArrayList<VideoItem> selectItems;
    private VideoBucket bucket;
    private RecyclerView videoRecyclerView;
    private VideoAdapter adapter;
    private boolean thumbCreate;
    private int video_index;
    private final int column = 3;

    private FrameLayout bottomView;
    public TextView all;
    public TextView none;
    public TextView backSel;
    public TextView rename;
    public TextView delete;
    private boolean selectMode;

    private CompositeDisposable mComDis = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        findView();
        initData();
        setListener();
    }

    @Override
    protected void onDestroy() {
        mComDis.clear();
        super.onDestroy();
    }

    @Override
    public void initData() {
        thumbCreate = false;
        selectMode = false;
        bucket = (VideoBucket) getIntent().getSerializableExtra(VIDEO_LIST_KEY);
        video_index = getIntent().getIntExtra(VIDEO_LIST_INDEX_KEY, -1);
        // 如果index是-1说明有错
        if (bucket == null) {
            videoList = new ArrayList<>();
            Timber.e("Video Bucket is Null!");
            return;
        }
        videoList = bucket.getVideoList();
        videoList = videoList == null ? new ArrayList<>() : videoList;
        selectItems = new ArrayList<>();

        headText.setText(bucket.getBucketName());

        videoRecyclerView.setLayoutManager(new GridLayoutManager(this, column));
        adapter = new VideoAdapter(this, R.layout.free_video_items);
        videoRecyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void findView() {
        headText = findViewById(R.id.free_tool_tip);
        back = findViewById(R.id.free_tool_left_img);
        videoRecyclerView = findViewById(R.id.free_video_list);

        bottomView = findViewById(R.id.free_bottom_tool_layout);
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
        ArrayList<VideoItem> temp = new ArrayList<>();
        for (VideoItem item : videoList) {
            if (selectItems.contains(item)) continue;
            temp.add(item);
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
        if (!selectMode) {
            if (!thumbCreate) setResult(VIDEO_LIST_VIEW_RESPONSE_N);
            else {
                Intent intent = new Intent();
                intent.putExtra(VIDEO_LIST_KEY, bucket);
                intent.putExtra(VIDEO_LIST_INDEX_KEY, video_index);
                setResult(VIDEO_LIST_VIEW_RESPONSE_M, intent);
            }
            super.onBackPressed();
        } else {
            selectMode = false;
            bottomView.setVisibility(View.GONE);
            all.setVisibility(View.VISIBLE);
            none.setVisibility(View.GONE);
            headText.setText(bucket.getBucketName());
            selectItems.clear();
            adapter.notifyDataSetChanged();
        }
    }

    public class VideoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener, View.OnLongClickListener {

        private LayoutInflater inflater;
        private int itemLayoutResId;

        VideoAdapter(Context context, int itemLayout) {
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
                return new VideoHolder(view);
            } else if (viewType == 1) {
                return new BottomToolHolder(inflater.inflate(R.layout.layout_bottom_tool_size, parent, false));
            } else if (viewType == 2) {
                return new EmptyViewHolder(inflater.inflate(R.layout.layout_empty, parent, false));
            } else {
                // 事实上这里是错误的, 暂时处理成没有内容的布局
                // TODO...
                return new BottomToolHolder(inflater.inflate(R.layout.layout_bottom_tool_size, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder1, int position) {
            if (holder1 instanceof VideoHolder) {
                VideoHolder holder = (VideoHolder) holder1;
                VideoItem item = videoList.get(position);
                if (item == null) return;
                Timber.i("pos:" + position + "----VideoPath:" + item.getVideoPath() + "----ThumbnailPath:" + item.getThumbnailPath());
                if (TextUtils.isEmpty(item.getThumbnailPath())) {
                    if (holder.disposable != null) {
                        mComDis.remove(holder.disposable);
                        holder.disposable.dispose();
                    }
                    holder.disposable = Single.fromCallable(() -> {
                        thumbCreate = true;
                        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(item.getVideoPath(), MINI_KIND);
                        if (bitmap == null) throw new IOException("Get Video BitMap Error");
                        String filePath = AppContext.getThumbDirVideo() + item.getVideoName() + "_thumb.jpg";
                        if (CommonMethod.saveBitmap2JPG(bitmap, filePath)) {
                            ContentValues c = new ContentValues();
                            c.put(FILE_ID, item.getVideoId());
                            c.put(FILE_PATH, item.getVideoPath());
                            c.put(FILE_THUMB_PATH, filePath);
                            DatabaseHelper.insert(getApplicationContext(), c);
                            item.setThumbnailPath(filePath);
                        }
                        return bitmap;
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(bitmap -> {
                                holder.videoPreview.setImageBitmap(bitmap);
                                holder.videoPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            }, throwable -> {
                                Timber.e("Get Video BitMap Error");
                                throwable.printStackTrace();
                                holder.videoPreview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                                holder.videoPreview.setImageResource(R.mipmap.free_pic_error);
                            });
                    mComDis.add(holder.disposable);
                } else {
                    holder.videoPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    GlideApp.with(VideoActivity.this)
                            .load(new File(item.getThumbnailPath()))
                            .error(getDrawable(R.mipmap.free_pic_error))
                            .into(holder.videoPreview);
                }
                if (selectMode) {
                    holder.select.setVisibility(View.VISIBLE);
                    holder.select.setChecked(selectItems.contains(item));
                } else {
                    holder.select.setVisibility(View.GONE);
                }
                holder.videoDuration.setText(String.valueOf(item.getDuration()));
                holder.videoName.setText(item.getVideoName());
                holder.itemView.setTag(holder);
            } else if (holder1 instanceof BottomToolHolder) {
                BottomToolHolder holder = (BottomToolHolder) holder1;
                holder.itemView.setTag(holder);
            } else if (holder1 instanceof EmptyViewHolder) {
                EmptyViewHolder holder = (EmptyViewHolder) holder1;
                holder.itemView.setTag(holder);
            } else {
                // 什么也不做
                // TODO...
                Timber.e("Holder Type Error!");
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == getItemCount() - 1) return 1;
            else if (position >= (videoList == null ? 0 : videoList.size())) return 2;
            return super.getItemViewType(position);
        }

        @Override
        public int getItemCount() {
            if (videoList == null) return 1;
            if (videoList.size() % column == 0) return videoList.size() + 1;
            return videoList.size() + column - (videoList.size() % column) + 1;
        }

        @Override
        public void onClick(View v) {
            VideoHolder holder = (VideoHolder) v.getTag();
            int position = holder.getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) {
                return;
            }
            VideoItem item = videoList.get(position);
            if (!selectMode) {
                File videoFile = new File(item.getVideoPath());
                Intent intent = new Intent(Intent.ACTION_VIEW);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    intent.setDataAndType(Uri.fromFile(videoFile), "video/*");
                } else {
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    Uri contentUri = FileProvider.getUriForFile(VideoActivity.this, BuildConfig.APPLICATION_ID + ".fileProvider", videoFile);
                    intent.setDataAndType(contentUri, "video/*");
                }
                startActivity(intent);
            } else {
                boolean state = !holder.select.isChecked();
                holder.select.setChecked(state);
                if (state) selectItems.add(item);
                else selectItems.remove(item);
                headText.setText(getString(R.string.has_select_list, selectItems.size()));
            }
        }

        @Override
        public boolean onLongClick(View v) {
            VideoHolder holder = (VideoHolder) v.getTag();
            int position = holder.getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return false;
            if (selectMode) {
                // longClick返回false后, 会回调onClick短按事件
                return false;
            } else {
                selectMode = true;
                selectItems.clear();
                selectItems.add(videoList.get(position));
                adapter.notifyDataSetChanged();
                headText.setText(getString(R.string.has_select_list, selectItems.size()));
                bottomView.setVisibility(View.VISIBLE);
                return true;
            }
        }
    }

    class VideoHolder extends RecyclerView.ViewHolder {

        private ImageView videoPreview;
        private TextView videoName;
        private TextView videoDuration;
        private CheckBox select;
        private Disposable disposable;

        VideoHolder(View itemView) {
            super(itemView);
            videoPreview = itemView.findViewById(R.id.free_video_list_items_img);
            videoName = itemView.findViewById(R.id.free_video_name);
            videoDuration = itemView.findViewById(R.id.free_video_duration);
            select = itemView.findViewById(R.id.free_video_item_check);
        }
    }
}
