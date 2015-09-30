package com.example.vklyovan.dailyselfie;

import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * TODO
 */
public class StorageUtil {

    private static String TAG = "StorageUtil";

    private static String SELFIES_FOLDER_NAME = "Selfies";
    private static String THUMBNAIL_FOLDER_NAME = ".thumbnail";

    private static File mSelfiePath;
    private static File mThumbnailPath;

    /**
     * Determine correct place for photos to be stored. Try to get external storage
     * if possible. If some needed folder does not exist, create it.
     *
     * @return created and ready to use path to directory with selfies
     */
    public static File getSelfiePath() {

        if (mSelfiePath != null) {
            return mSelfiePath;
        }

        // Check external media availability
        if (!Environment.MEDIA_MOUNTED.equals(
                Environment.getExternalStorageState())) {
            Log.e(TAG, "Media is not mounted");
            return null;
        }

        // Get public external DCIM path
        File dcimPath = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM);
        if (!dcimPath.exists()) {
            Log.i(TAG, "DCIM directory is not existed in " + dcimPath.getAbsolutePath());
            if (dcimPath.mkdir()) {
                Log.i(TAG, "DCIM directory successfully created.");
            } else {
                Log.e(TAG, "DCIM directory cannot be created.");
                return null;
            }
        }

        // Get selfie path
        File selfiePath = new File(dcimPath, SELFIES_FOLDER_NAME);
        if (!selfiePath.exists()) {
            Log.i(TAG, "Selfie directory is not existed in " + selfiePath.getAbsolutePath());
            if (selfiePath.mkdir()) {
                Log.i(TAG, "Selfie directory successfully created");
            } else {
                Log.e(TAG, "Selfie directory cannot be created");
                return null;
            }
        }

        mSelfiePath = selfiePath;
        return mSelfiePath;
    }

    public static File getThumbnailSelfiePath() {

        if (mThumbnailPath != null) {
            return mThumbnailPath;
        }

        File selfiePath = getSelfiePath();
        if (selfiePath == null) {
            Log.e(TAG, "Returning null due to null selfiePath");
            return null;
        }

        File thumbnailPath = new File(selfiePath, THUMBNAIL_FOLDER_NAME);
        if (!thumbnailPath.exists()) {
            Log.i(TAG, "Thumbnail directory is not existed in " + thumbnailPath.getAbsolutePath());
            if (thumbnailPath.mkdir()) {
                Log.i(TAG, "Thumbnail directory successfully created");
            } else {
                Log.e(TAG, "Thumbnail directory cannot be created");
                return null;
            }
        }

        mThumbnailPath = thumbnailPath;
        return mThumbnailPath;
    }
}
