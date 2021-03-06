package com.valence.freemeper.function.video;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.valence.freemeper.R;
import com.valence.freemeper.database.DatabaseHelper;
import com.valence.freemeper.tool.AppContext;
import com.valence.freemeper.tool.CommonMethod;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

import static com.valence.freemeper.database.DatabaseHelper.FILE_ID;
import static com.valence.freemeper.database.DatabaseHelper.FILE_PATH;
import static com.valence.freemeper.database.DatabaseHelper.FILE_THUMB_PATH;
import static com.valence.freemeper.database.DatabaseHelper.THUMB_TABLE;

public class LocalVideoHelper extends AsyncTask<Object, Object, Object> {

    @SuppressLint("StaticFieldLeak")
    private Context context;
    private HashMap<String, VideoBucket> bucketList = new HashMap<>();
    private HashMap<String, String> thumbnailList = new HashMap<>();
    private final HashMap<String, String> thumbDatabase = new HashMap<>();
    private final ArrayList<ContentValues> thumbNeedInsertDatabase = new ArrayList<>();
    private boolean hasBuildVideoBucketList = false;
    private ContentResolver contentResolver;
    private LoadVideoList listLoader;
    private Thread dataThread;

    private void init(Context context) {
        if (this.context == null) {
            this.context = context;
            contentResolver = context.getContentResolver();
        }
    }

    public void setListLoader(LoadVideoList listLoader) {
        this.listLoader = listLoader;
    }

    public static LocalVideoHelper getInstance(Context context) {
        LocalVideoHelper helper = new LocalVideoHelper();
        helper.init(context);
        return helper;
    }

    @Override
    protected Object doInBackground(Object[] params) {
        return getVideoBucketList((Boolean) (params[0]));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onPostExecute(Object list) {
        super.onPostExecute(list);
        listLoader.onListLoaded((ArrayList<VideoBucket>) list);
    }

    /**
     * 得到图片
     *
     * @param refresh ...
     * @return ...
     */
    private List<VideoBucket> getVideoBucketList(boolean refresh) {
        if (refresh || !hasBuildVideoBucketList) {
            buildVideoBucketList();
        }
        List<VideoBucket> tmpList = new ArrayList<>();
        for (Map.Entry<String, VideoBucket> entry : bucketList.entrySet()) {
            tmpList.add(entry.getValue());
        }
        return tmpList;
    }

    private void buildVideoBucketList() {
        loadFromDatabase();
        getThumbnail();
        String columns[] = new String[]{MediaStore.Video.Media._ID, MediaStore.Video.Media.BUCKET_ID,
                MediaStore.Video.Media.DURATION, MediaStore.Video.Media.DATA, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.SIZE, MediaStore.Video.Media.BUCKET_DISPLAY_NAME};
        Cursor cur = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, columns, null, null,
                MediaStore.Video.Media.DATE_MODIFIED + " desc");
        if (cur == null) {
            Timber.e("Query Video List Error! —— cursor is null!");
            return;
        }
        if (cur.moveToFirst()) {
            int videoIDIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int videoPathIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            int bucketDisplayNameIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
            int bucketIdIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID);
            int durationIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
            int nameIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);

            /*
              Description:这里增加了一个判断：判断照片的名字是否合法，例如.jpg .png
              图片名字是不合法的，直接过滤
             */
            HashMap<String, BucketVideoNum> noList = new HashMap<>();
            do {
                if (!(cur.getString(videoPathIndex).contains("/") && cur.getString(videoPathIndex).contains("."))
                        || cur.getString(videoPathIndex).substring(
                        cur.getString(videoPathIndex).lastIndexOf("/") + 1,
                        cur.getString(videoPathIndex).lastIndexOf("."))
                        .replaceAll(" ", "").length() <= 0) {
                    Timber.e("出现了异常图片的地址：cur.getString(videoPathIndex)=%s", cur.getString(videoPathIndex));
                    Timber.e("出现了异常图片的地址：cur.getString(videoPathIndex).substring=%s", cur.getString(videoPathIndex).substring(
                            cur.getString(videoPathIndex).lastIndexOf("/") + 1,
                            cur.getString(videoPathIndex).lastIndexOf(".")));
                    continue;
                }
                String _id = cur.getString(videoIDIndex);
                String path = cur.getString(videoPathIndex);
                String bucketName = cur.getString(bucketDisplayNameIndex);
                String bucketId = cur.getString(bucketIdIndex);
                long duration = cur.getLong(durationIndex);
                String name = cur.getString(nameIndex);

                File file = new File(path);
                if (!file.exists()) {
                    Timber.e("VIDEO FILE NOT EXIST: %s", path);
                    continue;
                }

                loadFromThumbnailList(_id, path, bucketName, bucketId, duration, name, noList);
            } while (cur.moveToNext());
            if (!noList.isEmpty()) {
                if (dataThread.isAlive()) {
                    try {
                        dataThread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        dataThread = null;
                    }
                }
                // 数据库中存在系统没保存缩略图的文件缩略图文件信息, 去加载这些信息
                for (Map.Entry<String, BucketVideoNum> entry : noList.entrySet()) {
                    String _id = entry.getKey();
                    BucketVideoNum bv = entry.getValue();
                    String thumb = thumbDatabase.get(_id);
                    VideoBucket bucket = bv.bucket;
                    int index = bv.videoIndex;

                    String thumbPath = AppContext.getThumbDirVideo() + bucket.videoList.get(index).getVideoName() + AppContext.THUMB_FILE_END;
                    File thumbFile = new File(thumbPath);
                    if (thumb == null && thumbFile.exists()) {
                        // 数据库中没有这个视频的缩略图信息, 但是视频的缩略图文件存在,
                        String videoPath = bucket.videoList.get(index).getVideoPath();
                        ContentValues c = new ContentValues();
                        c.put(FILE_ID, _id);
                        c.put(FILE_PATH, videoPath);
                        c.put(FILE_THUMB_PATH, thumbPath);
                        thumbNeedInsertDatabase.add(c);
                        bucket.videoList.get(index).setThumbnailPath(thumbPath);
                        // 不需要重新put, 上一步代码已经修改了其内部对象的值
                        // bucketList.put(bucket.getBucketId(), bucket);
                    } else if (thumb != null) {
                        File f = new File(thumb);
                        // 如果这个缩略图文件不存在, 直接跳过该文件（可能被手动删除了）
                        if (!f.exists()) {
                            Timber.e("Thumb File is Not Exist!——%s", thumb);
                            continue;
                        }
                        bucket.videoList.get(index).setThumbnailPath(thumb);
                    }
                }
                insertThumbDatabase();
            }
        }
        cur.close();
        hasBuildVideoBucketList = true;
    }

    private void loadFromDatabase() {
        if (dataThread == null) {
            dataThread = new Thread(() -> {
                DatabaseHelper.initDatabase(context);
                SQLiteDatabase db = AppContext.getInstance().db;
                String sql = "SELECT * FROM " + THUMB_TABLE;
                Cursor cursor = DatabaseHelper.query(db, sql);
                if (cursor == null || !cursor.moveToFirst()) {
                    Timber.e("QUERY DATABASE ERROR: %s", sql);
                    dataThread.interrupt();
                    DatabaseHelper.unInitDatabase();
                    return;
                }
                synchronized (thumbDatabase) {
                    do {
                        String filePath = cursor.getString(cursor.getColumnIndex(FILE_PATH));
                        if (!new File(filePath).exists()) continue;
                        String thumbPath = cursor.getString(cursor.getColumnIndex(FILE_THUMB_PATH));
                        String id = cursor.getString(cursor.getColumnIndex(FILE_ID));
                        thumbDatabase.put(id, thumbPath);
                    } while (cursor.moveToNext());
                    cursor.close();
                }
                DatabaseHelper.unInitDatabase();
            });
        }
        dataThread.start();
    }

    private void loadFromThumbnailList(String _id, String path, String bucketName, String bucketId, long duration, String name, HashMap<String, BucketVideoNum> noList) {
        /*
         * 需要注意的是: 使用以下截取名字的方式时(第二个 if 语句中的取名代码), 如果文件没有后缀, 即名称中没有 **.** 类型的命名方式, 获取脚标时会出错.
         * 所以, 需要进行代码修改. 修改如下:
         * 在判断之前, 先判断文件所处路径中(包括文件本身名称)是否含有 ".", "/", 这两个标点符号, 如果没有, 就直接跳过
         */
        VideoBucket bucket = bucketList.get(bucketId);
        String thumbPath = thumbnailList.get(_id);
        if (bucket == null) {
            bucket = new VideoBucket();
            bucket.videoList = new ArrayList<>();
            bucket.setBucketId(bucketId);
            bucket.setBucketPath(new File(path).getParent());
            if (TextUtils.equals(bucket.getBucketPath(), Environment.getExternalStorageDirectory().getAbsolutePath()))
                bucketName = context.getString(R.string.root_dir);
            bucket.setBucketName(bucketName);
            bucket.setCoverPath(thumbPath);
            bucketList.put(bucketId, bucket);
        }
        bucket.count++;
        VideoItem videoItem = new VideoItem();
        videoItem.setVideoId(_id);
        videoItem.setVideoPath(path);
        videoItem.setThumbnailPath(thumbPath);
        videoItem.setDuration(CommonMethod.longTime2String(duration));
        videoItem.setVideoName(name);
        if (TextUtils.isEmpty(bucket.getCoverPath()) && !TextUtils.isEmpty(thumbPath))
            bucket.setCoverPath(thumbPath);
        bucket.videoList.add(videoItem);
        BucketVideoNum bv = new BucketVideoNum(bucket, bucket.videoList.size() - 1);
        if (thumbPath == null) noList.put(_id, bv);
    }

    private void insertThumbDatabase() {
        if (!thumbNeedInsertDatabase.isEmpty()) {
            int size = thumbNeedInsertDatabase.size();
            ContentValues[] cs = thumbNeedInsertDatabase.toArray(new ContentValues[size]);
            DatabaseHelper.inserts(context, cs);
        }
    }

    private void getThumbnail() {
        String[] projection = {MediaStore.Video.Thumbnails._ID, MediaStore.Video.Thumbnails.VIDEO_ID,
                MediaStore.Video.Thumbnails.DATA};
        Cursor cursor1 = contentResolver.query(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, projection,
                null, null, null);
        if (cursor1 != null) {
            getThumbnailColumnData(cursor1);
            cursor1.close();
        }
    }

    private void getThumbnailColumnData(Cursor cur) {
        if (cur.moveToFirst()) {
            int video_id;
            String video_path;
            int video_idColumn = cur.getColumnIndex(MediaStore.Video.Thumbnails.VIDEO_ID);
            int dataColumn = cur.getColumnIndex(MediaStore.Video.Thumbnails.DATA);
            do {
                video_id = cur.getInt(video_idColumn);
                video_path = cur.getString(dataColumn);
                thumbnailList.put("" + video_id, video_path);
            } while (cur.moveToNext());
        }
    }

    public void clear() {
        bucketList.clear();
        thumbnailList.clear();
        thumbDatabase.clear();
        this.contentResolver = null;
        this.listLoader = null;
        this.context = null;
    }

    public interface LoadVideoList {
        void onListLoaded(ArrayList<VideoBucket> list);
    }
}
