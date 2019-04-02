package com.valence.freemeper.function.camera;

import android.content.Context;
import android.content.Intent;
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
import com.valence.freemeper.holder.BottomToolHolder;
import com.valence.freemeper.holder.EmptyViewHolder;
import com.valence.freemeper.tool.AppContext;
import com.valence.freemeper.tool.GlideApp;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public class CameraFileActivity extends BaseActivity2 implements View.OnClickListener {

    private TextView headText;
    private ImageView back;
    private RecyclerView mRecyclerView;
    private FrameLayout bottomView;

    public TextView all;
    public TextView none;
    public TextView backSel;
    public TextView rename;
    public TextView delete;

    private CameraFileAdapter adapter;
    private File[] fileArray;
    private HashMap<String, String> videoPathMap;
    private final int column = 4;
    private boolean selectMode;
    private ArrayList<File> selectItems;
    private CompositeDisposable mComDis = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 与 VideoActivity 共用一个布局
        setContentView(R.layout.activity_video);
        findView();
        initData();
        setListener();
    }

    @Override
    public void findView() {
        headText = findViewById(R.id.free_tool_tip);
        back = findViewById(R.id.free_tool_left_img);
        mRecyclerView = findViewById(R.id.free_video_list);

        bottomView = findViewById(R.id.free_bottom_tool_layout);
        all = findViewById(R.id.free_bottom_select_all);
        none = findViewById(R.id.free_bottom_all_unSelect);
        backSel = findViewById(R.id.free_bottom_select_back);
        rename = findViewById(R.id.free_bottom_rename);
        delete = findViewById(R.id.free_bottom_delete);
    }

    @Override
    public void initData() {
        selectMode = false;
        selectItems = new ArrayList<>();

        headText.setText(R.string.camera_album);
        CameraFileBucket bucket = (CameraFileBucket) Objects.requireNonNull(getIntent().getExtras()).getSerializable("camera_file_bucket");
        if (bucket != null) {
            fileArray = bucket.getFiles();
            videoPathMap = bucket.getVideoPathMap();
        } else fileArray = null;
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, column));
        adapter = new CameraFileAdapter(this, R.layout.free_camera_file_item);
        mRecyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void setListener() {
        back.setOnClickListener(v -> onBackPressed());

        all.setOnClickListener(this);
        none.setOnClickListener(this);
        backSel.setOnClickListener(this);
        rename.setOnClickListener(this);
        delete.setOnClickListener(this);
    }

    @Override
    public void onBackPressed() {
        if (!selectMode)
            super.onBackPressed();
        else {
            selectMode = false;
            headText.setText(R.string.camera_album);
            bottomView.setVisibility(View.GONE);
            all.setVisibility(View.VISIBLE);
            none.setVisibility(View.GONE);
            selectItems.clear();
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.free_bottom_select_all:
                selectItems.clear();
                selectItems.addAll(Arrays.asList(fileArray));
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

    private void updateList() {
        adapter.notifyDataSetChanged();
        headText.setText(getString(R.string.has_select_list, selectItems.size()));
    }

    private void doBackSelect() {
        ArrayList<File> temp = new ArrayList<>();
        for (File item : fileArray) {
            if (selectItems.contains(item)) continue;
            temp.add(item);
        }
        selectItems.clear();
        selectItems.addAll(temp);
        updateList();
    }

    private void doDelete() {
        AppContext.showToast(("Choose " + selectItems.size() + " to Delete"));
        if (!selectItems.isEmpty()) onBackPressed();
    }

    public class CameraFileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener, View.OnLongClickListener {

        private LayoutInflater inflater;
        private int itemLayoutResId;

        CameraFileAdapter(Context context, int itemLayout) {
            inflater = LayoutInflater.from(context);
            itemLayoutResId = itemLayout;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0 || viewType == 3) {
                View view = inflater.inflate(itemLayoutResId, parent, false);
                view.setOnClickListener(this);
                view.setOnLongClickListener(this);
                return new CameraFileHolder(view);
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
            if (holder1 instanceof CameraFileHolder) {
                CameraFileHolder holder = (CameraFileHolder) holder1;
                File file = fileArray[position];
                String path = file.getAbsolutePath();
//                Timber.e("CameraFileActivity %s" + path);

                // 需要注意的是, 这里返回的都是图片的路径, 所以都是.jpg结尾, 为了区分, 在最后加上0(即:".jpg0"结尾)代表视频, .jpg代表图片
                if (path.endsWith(".jpg0")) {
                    holder.videoSign.setVisibility(View.VISIBLE);
                    String realPath = path.substring(0, path.length() - 1);
                    GlideApp.with(CameraFileActivity.this)
                            .load(new File(realPath))
                            .error(getDrawable(R.mipmap.free_pic_error))
                            .into(holder.thumbView);
                } else if (path.endsWith(".jpg")) {
                    holder.videoSign.setVisibility(View.GONE);
                    GlideApp.with(CameraFileActivity.this)
                            .load(file)
                            .error(getDrawable(R.mipmap.free_pic_error))
                            .into(holder.thumbView);
                } else {
                    holder.videoSign.setVisibility(View.GONE);
                    Timber.e("Illegal File,Neither \".mp4\" nor \".jpg\":\n%s", path);
                    return;
                }
                if (selectMode) {
                    holder.select.setVisibility(View.VISIBLE);
                    holder.select.setChecked(selectItems.contains(file));
                } else {
                    holder.select.setVisibility(View.GONE);
                }

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
            else if (position >= (fileArray == null ? 0 : fileArray.length)) return 2;
            else {
                if (fileArray == null || fileArray[position] == null) return 2;
                if (fileArray[position].getAbsolutePath().endsWith(".jpg")) return 0;
                if (fileArray[position].getAbsolutePath().endsWith(".mp4")) return 3;
            }
            return super.getItemViewType(position);
        }

        @Override
        public int getItemCount() {
            if (fileArray == null) return 1;
            if (fileArray.length % column == 0) return fileArray.length + 1;
            return fileArray.length + column - (fileArray.length % column) + 1;
        }

        @Override
        public void onClick(View v) {
            CameraFileHolder holder = (CameraFileHolder) v.getTag();
            int position = holder.getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) {
                return;
            }
            File file = fileArray[position];
            if (file == null) return;
            if (selectMode) {
                boolean state = !holder.select.isChecked();
                holder.select.setChecked(state);
                if (state) selectItems.add(file);
                else selectItems.remove(file);
                headText.setText(getString(R.string.has_select_list, selectItems.size()));
            } else {
                String thumbPath = file.getAbsolutePath();
                if (TextUtils.isEmpty(thumbPath) || videoPathMap == null) return;
                File targetFile;

                // 需要注意的是, 这里返回的都是图片的路径, 所以都是.jpg结尾, 为了区分, 在最后加上0(即:".jpg0"结尾)代表视频, .jpg代表图片
                if (thumbPath.endsWith(".jpg0")) {
                    targetFile = new File(videoPathMap.get(thumbPath));
                } else if (thumbPath.endsWith(".jpg")) {
                    targetFile = file;       // 图片的实际路径就是传进来的thumbPath
                } else {
                    Timber.e("Illegal File Path: %s", thumbPath);
                    return;
                }
                if (!targetFile.exists()) {
                    Timber.e("File Does not Exist! %s", targetFile.getAbsolutePath());
                    AppContext.showToast("File not Exist!");
                    return;
                }
                Intent intent = new Intent(Intent.ACTION_VIEW);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    if (targetFile.getAbsolutePath().endsWith(".mp4"))
                        intent.setDataAndType(Uri.fromFile(targetFile), "video/*");
                    else if (targetFile.getAbsolutePath().endsWith(".jpg"))
                        intent.setDataAndType(Uri.fromFile(targetFile), "image/*");
                } else {
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    Uri contentUri = FileProvider.getUriForFile(CameraFileActivity.this, BuildConfig.APPLICATION_ID + ".fileProvider", targetFile);
                    if (targetFile.getAbsolutePath().endsWith(".mp4"))
                        intent.setDataAndType(contentUri, "video/*");
                    else if (targetFile.getAbsolutePath().endsWith(".jpg"))
                        intent.setDataAndType(contentUri, "image/*");
                }
                Timber.i("Open Camera File: %s", targetFile.getAbsolutePath());
                startActivity(intent);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            CameraFileHolder holder = (CameraFileHolder) v.getTag();
            int position = holder.getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return false;
            if (selectMode) {
                // longClick返回false后, 会回调onClick短按事件
                return false;
            } else {
                selectMode = true;
                selectItems.clear();
                selectItems.add(fileArray[position]);
                adapter.notifyDataSetChanged();
                headText.setText(getString(R.string.has_select_list, selectItems.size()));
                bottomView.setVisibility(View.VISIBLE);
                return true;
            }
        }
    }

    public class CameraFileHolder extends RecyclerView.ViewHolder {

        private ImageView thumbView;
        private ImageView videoSign;
        private CheckBox select;

        CameraFileHolder(View itemView) {
            super(itemView);
            thumbView = itemView.findViewById(R.id.free_camera_file_items_img);
            videoSign = itemView.findViewById(R.id.free_camera_file_video_sign);
            select = itemView.findViewById(R.id.free_camera_file_item_check);
        }
    }
}
