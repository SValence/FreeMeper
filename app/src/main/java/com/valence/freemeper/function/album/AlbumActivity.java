package com.valence.freemeper.function.album;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.valence.freemeper.function.images.AlbumImageItem;
import com.valence.freemeper.function.images.ImageActivity;
import com.valence.freemeper.holder.BottomToolHolder;
import com.valence.freemeper.tool.AppContext;
import com.valence.freemeper.tool.GlideApp;

import java.io.File;
import java.util.ArrayList;

/**
 * @author Valence
 * @version 1.0
 * @since 2017/12/10
 */

public class AlbumActivity extends BaseActivity implements IFindView, View.OnClickListener {

    private static final String TAG = "AlbumActivity";
    public static final String ALBUM_LIST_KEY = "album_list";
    public static final String ALBUM_LIST_INDEX_KEY = "list_index";

    public static final int ALBUM_LIST_VIEW_REQUEST = 21001;
    public static final int ALBUM_LIST_VIEW_RESPONSE_N = 21002;
    public static final int ALBUM_LIST_VIEW_RESPONSE_M = 21003;

    private RecyclerView imageListView;
    private TextView headText;
    private AlbumAdapter adapter;
    private ArrayList<AlbumImageBucket> albumList;
    private ArrayList<AlbumImageBucket> selectItems;
    private ImageView back;
    private boolean selectMode;
    private AlbumHelper helper;
    private FrameLayout bottomLayout;
    public TextView all;
    public TextView none;
    public TextView backSel;
    public TextView rename;
    public TextView delete;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        findView();
        setListener();
        setVariate();
        loadAlbumData();
    }

    @Override
    public void setVariate() {
        selectMode = false;
        headText.setText(R.string.free_album);

        albumList = new ArrayList<>();
        selectItems = new ArrayList<>();
        adapter = new AlbumAdapter(this, R.layout.free_album_items);
        imageListView.setLayoutManager(new LinearLayoutManager(this));
        imageListView.setAdapter(adapter);

        dialog = ProgressDialog.show(this, null, getString(R.string.loading));
        helper = AlbumHelper.getInstance(this);
    }

    @Override
    public void findView() {
        imageListView = findViewById(R.id.freeAlbumList);
        back = findViewById(R.id.free_tool_left_img);
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

    private void loadAlbumData() {
        helper.setGetAlbumList(list -> {
            back.postDelayed(() -> {
                if (dialog != null) dialog.dismiss();
            }, 200);
            if (list == null) albumList = new ArrayList<>();
            else albumList.addAll(list);
            if (adapter != null) adapter.notifyDataSetChanged();
        });
        helper.execute(false);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.free_tool_left_img:
                onBackPressed();
                break;
            case R.id.free_bottom_select_all:
                selectItems.clear();
                selectItems.addAll(albumList);
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
        ArrayList<AlbumImageBucket> temp = new ArrayList<>();
        for (AlbumImageBucket bucket : albumList) {
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
        if (!selectMode) {
            super.onBackPressed();
        } else {
            selectMode = false;
            setBottomToolVisibility(false);
            selectItems.clear();
            adapter.notifyDataSetChanged();
            headText.setText(R.string.free_album);
            all.setVisibility(View.VISIBLE);
            none.setVisibility(View.GONE);
        }
    }

    class AlbumAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener, View.OnLongClickListener {
        private int itemResId;
        private LayoutInflater inflater;

        AlbumAdapter(Context context, int itemResId) {
            this.itemResId = itemResId;
            this.inflater = LayoutInflater.from(context);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                View view = inflater.inflate(itemResId, parent, false);
                view.setOnClickListener(this);
                view.setOnLongClickListener(this);
                return new AlbumViewHolder(view);
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
            if (holder1 instanceof AlbumViewHolder) {
                AlbumImageBucket bucket = albumList.get(position);
                if (bucket == null) {
                    Log.e(TAG, "This Album Bucket is Null!_No." + position);
                    return;
                }
                AlbumViewHolder holder = (AlbumViewHolder) holder1;
                if (selectMode) {
                    holder.ahead.setVisibility(View.GONE);
                    holder.select.setVisibility(View.VISIBLE);
                    holder.select.setChecked(selectItems.contains(bucket));
                } else {
                    holder.ahead.setVisibility(View.VISIBLE);
                    holder.select.setVisibility(View.GONE);
                }
                if (TextUtils.isEmpty(bucket.getBucketCoverPath())) {
                    AlbumImageItem item = bucket.imageList.get(0);
                    String thumbPath = item.getThumbnailPath();
                    if (TextUtils.isEmpty(thumbPath))
                        bucket.setBucketCoverPath(item.getImagePath());
                }
                GlideApp.with(AlbumActivity.this)
                        .load(new File(bucket.getBucketCoverPath()))
                        .error(getDrawable(R.mipmap.free_pic_error))
                        .centerCrop()
                        .into(holder.coverImage);
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
        public int getItemCount() {
            return albumList == null ? 1 : albumList.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == getItemCount() - 1) return 1;
            return super.getItemViewType(position);
        }

        @Override
        public void onClick(View v) {
            AlbumViewHolder holder = (AlbumViewHolder) v.getTag();
            int position = holder.getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;
            AlbumImageBucket bucket = albumList.get(position);
            if (selectMode) {
                boolean state = !holder.select.isChecked();
                holder.select.setChecked(state);
                if (state) selectItems.add(bucket);
                else selectItems.remove(bucket);
                headText.setText(getString(R.string.has_select_list, selectItems.size()));
            } else {
                Intent intent = new Intent(AlbumActivity.this, ImageActivity.class);
                intent.putExtra(ALBUM_LIST_KEY, bucket);
                intent.putExtra(ALBUM_LIST_INDEX_KEY, position);
                v.postDelayed(() -> startActivityForResult(intent, ALBUM_LIST_VIEW_REQUEST), 100);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            AlbumViewHolder holder = (AlbumViewHolder) v.getTag();
            int position = holder.getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return false;
            if (selectMode) {
                // 这里不需要上面的update语句, 因为longClick返回false后, 会回调onClick短按事件
                return false;
            }
            selectMode = true;
            setBottomToolVisibility(true);
            selectItems.add(albumList.get(position));
            adapter.notifyDataSetChanged();
            headText.setText(getString(R.string.has_select_list, selectItems.size()));
            return true;
        }
    }

    private void setBottomToolVisibility(boolean b) {
        bottomLayout.setVisibility(b ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;
        if (requestCode == ALBUM_LIST_VIEW_REQUEST) {
            if (resultCode == ALBUM_LIST_VIEW_RESPONSE_M) {
                int idx = data.getIntExtra(ALBUM_LIST_INDEX_KEY, -1);
                if (idx < 0) return;
                AlbumImageBucket bucket = (AlbumImageBucket) data.getSerializableExtra(ALBUM_LIST_KEY);
                ArrayList<AlbumImageItem> list = bucket.getImageList();
                if (list == null) return;
                albumList.set(idx, bucket);
                adapter.notifyItemChanged(idx);
            }
        }
    }

    class AlbumViewHolder extends RecyclerView.ViewHolder {

        private ImageView coverImage;
        private TextView bucketName;
        private TextView bucketPath;
        private TextView bucketCount;
        private ImageView ahead;
        private CheckBox select;

        AlbumViewHolder(View itemView) {
            super(itemView);
            coverImage = itemView.findViewById(R.id.free_imageList_cover);
            bucketName = itemView.findViewById(R.id.free_imageList_dirname);
            bucketPath = itemView.findViewById(R.id.free_imageList_dirpath);
            bucketCount = itemView.findViewById(R.id.free_imageList_num);
            ahead = itemView.findViewById(R.id.free_album_ahead);
            select = itemView.findViewById(R.id.free_album_check);
        }
    }
}
