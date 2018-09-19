package com.valence.freemeper.function.album;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore.Images.Media;
import android.provider.MediaStore.Images.Thumbnails;
import android.text.TextUtils;
import android.util.Log;

import com.valence.freemeper.function.images.AlbumImageItem;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class AlbumHelper extends AsyncTask<Object, Object, Object> {

    private final String TAG = getClass().getSimpleName();
    private Context context;
    private ContentResolver cr;
    private HashMap<String, String> thumbnailList = new HashMap<>();
    private List<HashMap<String, String>> albumList = new ArrayList<>();
    private HashMap<String, AlbumImageBucket> bucketList = new HashMap<>();
    private GetAlbumList getAlbumList;

    private AlbumHelper() {
    }

    public static AlbumHelper getInstance(Context context) {
        AlbumHelper helper = new AlbumHelper();
        helper.init(context);
        return helper;
    }

    /**
     * @param context ..
     */
    public void init(Context context) {
        if (this.context == null) {
            this.context = context;
            cr = context.getContentResolver();
        }
    }

    private void getThumbnail() {
        String[] projection = {Thumbnails._ID, Thumbnails.IMAGE_ID,
                Thumbnails.DATA};
        Cursor cursor1 = Thumbnails.queryMiniThumbnails(cr,
                Thumbnails.EXTERNAL_CONTENT_URI, Thumbnails.MINI_KIND,
                projection);
        getThumbnailColumnData(cursor1);
        cursor1.close();
    }

    private void getThumbnailColumnData(Cursor cur) {
        if (cur.moveToFirst()) {
            int image_id;
            String image_path;
            int image_idColumn = cur.getColumnIndex(Thumbnails.IMAGE_ID);
            int dataColumn = cur.getColumnIndex(Thumbnails.DATA);
            do {
                image_id = cur.getInt(image_idColumn);
                image_path = cur.getString(dataColumn);
                thumbnailList.put("" + image_id, image_path);
            } while (cur.moveToNext());
        }
    }

    private boolean hasBuildImagesBucketList = false;

    private void buildImagesBucketList() {
        getThumbnail();
        String columns[] = new String[]{Media._ID, Media.BUCKET_ID,
                Media.PICASA_ID, Media.DATA, Media.DISPLAY_NAME, Media.TITLE,
                Media.SIZE, Media.BUCKET_DISPLAY_NAME};
        Cursor cur = cr.query(Media.EXTERNAL_CONTENT_URI, columns, null, null,
                Media.DATE_MODIFIED + " desc");
        if (cur.moveToFirst()) {
            int photoIDIndex = cur.getColumnIndexOrThrow(Media._ID);
            int photoPathIndex = cur.getColumnIndexOrThrow(Media.DATA);
            int bucketDisplayNameIndex = cur
                    .getColumnIndexOrThrow(Media.BUCKET_DISPLAY_NAME);
            int bucketIdIndex = cur.getColumnIndexOrThrow(Media.BUCKET_ID);

            /**
             * Description:这里增加了一个判断：判断照片的名字是否合法，例如.jpg .png
             * 图片名字是不合法的，直接过滤
             */
            do {
//                Log.e("path:", cur.getString(photoPathIndex));
                /**
                 * 需要注意的是: 使用以下截取名字的方式时(第二个 if 语句中的取名代码), 如果文件没有后缀, 即名称中没有 **.** 类型的命名方式, 获取脚标时会出错.
                 * 所以, 需要进行代码修改. 修改如下:
                 * 在判断之前, 先判断文件所处路径中(包括文件本身名称)是否含有 ".", "/", 这两个标点符号, 如果没有, 就直接跳过
                 */

                // 这个 if 判断语句是后来加的
                if (!(cur.getString(photoPathIndex).contains("/") && cur.getString(photoPathIndex).contains("."))) {
                    continue;
                }

                if (cur.getString(photoPathIndex)
                        .substring(
                                cur.getString(photoPathIndex).lastIndexOf("/") + 1,
                                cur.getString(photoPathIndex).lastIndexOf("."))
                        .replaceAll(" ", "").length() <= 0) {
                    Log.e(TAG, "出现了异常图片的地址：cur.getString(photoPathIndex)="
                            + cur.getString(photoPathIndex));
                    Log.e(TAG,
                            "出现了异常图片的地址：cur.getString(photoPathIndex).substring="
                                    + cur.getString(photoPathIndex).substring(
                                    cur.getString(photoPathIndex)
                                            .lastIndexOf("/") + 1,
                                    cur.getString(photoPathIndex)
                                            .lastIndexOf(".")));
                } else {
                    String _id = cur.getString(photoIDIndex);
                    String path = cur.getString(photoPathIndex);
                    String bucketName = cur.getString(bucketDisplayNameIndex);
                    String bucketId = cur.getString(bucketIdIndex);

                    File file = new File(path);
                    if (!file.exists()) {
                        Log.e(TAG, "IMAGE FILE NOT EXIST: " + path);
                        continue;
                    }

                    String thumbPath = thumbnailList.get(_id);
                    AlbumImageBucket bucket = bucketList.get(bucketId);
                    if (bucket == null) {
                        bucket = new AlbumImageBucket();
                        bucketList.put(bucketId, bucket);
                        bucket.imageList = new ArrayList<>();
                        bucket.setBucketCoverPath(thumbPath == null ? path : thumbPath);
                        bucket.setBucketPath(new File(path).getParent());
                        if (TextUtils.equals(bucket.getBucketPath(), Environment.getExternalStorageDirectory().getAbsolutePath()))
                            bucketName = "根目录";
                        bucket.setBucketName(bucketName);
                    }
                    bucket.count++;
                    AlbumImageItem imageItem = new AlbumImageItem();
                    imageItem.setImageId(_id);
                    imageItem.setImagePath(path);
                    imageItem.setThumbnailPath(thumbPath == null ? path : thumbPath);
                    bucket.imageList.add(imageItem);
                }
            } while (cur.moveToNext());
        }
        cur.close();
        hasBuildImagesBucketList = true;
    }

    /**
     * 得到图片
     *
     * @param refresh .
     * @return
     */
    public List<AlbumImageBucket> getImagesBucketList(boolean refresh) {
        if (refresh || (!refresh && !hasBuildImagesBucketList)) {
            buildImagesBucketList();
        }
        List<AlbumImageBucket> tmpList = new ArrayList<AlbumImageBucket>();
        Iterator<Entry<String, AlbumImageBucket>> itr = bucketList.entrySet()
                .iterator();
        while (itr.hasNext()) {
            Entry<String, AlbumImageBucket> entry = itr
                    .next();
            tmpList.add(entry.getValue());
        }
        return tmpList;
    }

    public void destroyList() {
        thumbnailList.clear();
        thumbnailList = null;
        albumList.clear();
        albumList = null;
        bucketList.clear();
        bucketList = null;
    }

    public void setGetAlbumList(GetAlbumList getAlbumList) {
        this.getAlbumList = getAlbumList;
    }

    public interface GetAlbumList {
        public void getAlbumList(List<AlbumImageBucket> list);
    }

    @Override
    protected Object doInBackground(Object... params) {
        return getImagesBucketList((Boolean) (params[0]));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onPostExecute(Object result) {
        super.onPostExecute(result);
        getAlbumList.getAlbumList((List<AlbumImageBucket>) result);
    }

}
