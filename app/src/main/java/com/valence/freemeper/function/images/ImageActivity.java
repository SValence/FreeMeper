package com.valence.freemeper.function.images;

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.valence.freemeper.BuildConfig;
import com.valence.freemeper.R;
import com.valence.freemeper.base.BaseActivity;
import com.valence.freemeper.base.IFindView;
import com.valence.freemeper.function.album.AlbumImageBucket;
import com.valence.freemeper.holder.BottomToolHolder;
import com.valence.freemeper.holder.EmptyViewHolder;
import com.valence.freemeper.tool.AppContext;
import com.valence.freemeper.tool.GlideApp;

import java.io.File;
import java.util.ArrayList;

import static com.valence.freemeper.function.album.AlbumActivity.ALBUM_LIST_KEY;
import static com.valence.freemeper.function.album.AlbumActivity.ALBUM_LIST_VIEW_RESPONSE_N;

public class ImageActivity extends BaseActivity implements IFindView, View.OnClickListener {

    private static final String TAG = "ImageActivity";
    private AlbumItemsAdapter adapter;
    private ImageView back;
    private RecyclerView imageViews;
    private TextView headText;
    private final int column = 4;
    private ArrayList<AlbumImageItem> imageList;
    private ArrayList<AlbumImageItem> selectItems;
    private AlbumImageBucket albumImageBucket;

    private boolean selectMode;
    private FrameLayout bottomLayout;
    public TextView all;
    public TextView none;
    public TextView backSel;
    public TextView rename;
    public TextView delete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_images);
        findView();
        setVariate();
        setListener();
    }

    @Override
    public void setVariate() {
        imageList = new ArrayList<>();
        selectItems = new ArrayList<>();
        Intent intent = getIntent();
        albumImageBucket = (AlbumImageBucket) intent.getSerializableExtra(ALBUM_LIST_KEY);
        if (albumImageBucket == null) return;
        else imageList.addAll(albumImageBucket.getImageList());
        adapter = new AlbumItemsAdapter(this, R.layout.free_image_items);
        imageViews.setLayoutManager(new GridLayoutManager(this, column));
        imageViews.setAdapter(adapter);
        headText.setText(albumImageBucket.getBucketName());
    }

    @Override
    public void findView() {
        back = findViewById(R.id.free_tool_left_img);
        imageViews = findViewById(R.id.free_album_images);
        headText = findViewById(R.id.free_tool_tip);

        bottomLayout = findViewById(R.id.free_bottom_tool_layout);
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
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.free_tool_left_img:
                onBackPressed();
                break;
            case R.id.free_bottom_select_all:
                selectItems.clear();
                selectItems.addAll(imageList);
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
        ArrayList<AlbumImageItem> temp = new ArrayList<>();
        for (AlbumImageItem item : imageList) {
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
        if(!selectMode) {
            setResult(ALBUM_LIST_VIEW_RESPONSE_N);
            super.onBackPressed();
        } else {
            selectMode = false;
            bottomLayout.setVisibility(View.GONE);
            all.setVisibility(View.VISIBLE);
            none.setVisibility(View.GONE);
            headText.setText(albumImageBucket.getBucketName());
            selectItems.clear();
            adapter.notifyDataSetChanged();
        }
    }

    class AlbumItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener, View.OnLongClickListener {

        private int itemLayoutResId;
        private LayoutInflater inflater;

        AlbumItemsAdapter(Context context, int itemLayoutResId) {
            inflater = LayoutInflater.from(context);
            this.itemLayoutResId = itemLayoutResId;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                View view = inflater.inflate(itemLayoutResId, parent, false);
                view.setOnClickListener(this);
                view.setOnLongClickListener(this);
                return new ImageViewHolder(view);
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
            if (holder1 instanceof ImageViewHolder) {
                AlbumImageItem item = imageList.get(position);
                if (item == null) return;
                Log.e(TAG, "pos:" + position + "----ImagePath:" + item.getImagePath() + "----ThumbnailPath:" + item.getThumbnailPath());
                ImageViewHolder holder = (ImageViewHolder) holder1;
                GlideApp.with(ImageActivity.this)
                        .load(new File(TextUtils.isEmpty(item.getThumbnailPath()) ? item.getImagePath() : item.getImagePath()))
                        .centerCrop()
                        .error(R.mipmap.free_pic_error)
                        .into(holder.image);
                if (selectMode) {
                    holder.select.setVisibility(View.VISIBLE);
                    holder.select.setChecked(selectItems.contains(item));
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
                Log.e(TAG, "Holder Type Error!");
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == getItemCount() - 1) return 1;
            else if (position >= (imageList == null ? 0 : imageList.size())) return 2;
            return super.getItemViewType(position);
        }

        @Override
        public int getItemCount() {
            if (imageList == null) return 1;
            if (imageList.size() % column == 0) return imageList.size() + 1;
            return imageList.size() + column - (imageList.size() % column) + 1;
        }

        @Override
        public void onClick(View v) {
            ImageViewHolder holder = (ImageViewHolder) v.getTag();
            int position = holder.getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;
            AlbumImageItem item = imageList.get(position);
            if (!selectMode) {
                String path = TextUtils.isEmpty(item.getThumbnailPath()) ? item.getImagePath() : item.getThumbnailPath();
                v.postDelayed(() -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    File picFile = new File(path);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        intent.setDataAndType(Uri.fromFile(picFile), "image/*");
                    } else {
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        Uri contentUri = FileProvider.getUriForFile(ImageActivity.this, BuildConfig.APPLICATION_ID + ".fileProvider", picFile);
                        intent.setDataAndType(contentUri, "image/*");
                    }
                    startActivity(intent);
                }, 100);
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
            ImageViewHolder holder = (ImageViewHolder) v.getTag();
            int position = holder.getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return false;
            if (selectMode) {
                // longClick返回false后, 会回调onClick短按事件
                return false;
            } else {
                selectMode = true;
                selectItems.clear();
                selectItems.add(imageList.get(position));
                adapter.notifyDataSetChanged();
                headText.setText(getString(R.string.has_select_list, selectItems.size()));
                bottomLayout.setVisibility(View.VISIBLE);
                return true;
            }
        }
    }

    class ImageViewHolder extends RecyclerView.ViewHolder {

        private ImageView image;
        private CheckBox select;

        ImageViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image_list_items);
            select = itemView.findViewById(R.id.free_image_item_check);
        }
    }
}
