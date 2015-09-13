package com.example.vklyovan.dailyselfie;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * This task takes array of String filenames. For each filename it checks whether
 * the scaled image in a private application directory exists. If not, task
 * creates the scaled image and update list view with the results. The return
 * value is Integer indicates the number of scaled images.
 *
 * TODO Probably this file need to be removed
 */
public class CreateScaledImagesTask extends AsyncTask<String, Void, Integer> {

    private static String TAG = "CreateScaledImageTask";

    private File mPathFrom;
    private File mPathTo;

    private int mTargetImageWidth;
    private int mTargetImageHeight;

    public CreateScaledImagesTask(File pathFrom, File pathTo,
                                  int resultImageWidth, int resultImageHeight) {
        // TODO Check input params

        mPathFrom = pathFrom;
        mPathTo = pathTo;
        mTargetImageWidth = resultImageWidth;
        mTargetImageHeight = resultImageHeight;
    }

    @Override
    protected Integer doInBackground(String... filenames) {
        Log.i(TAG, "Task started");

        Integer scaledCount = 0;

        String[] filenamesTo = mPathTo.list();

        // For each image check whether the small image is exists.
        // If it doesn't exist scale image and save there with the same
        // name
        for (String filename : filenames) {

            // If image is presented, nothing to do with this image
            if (Arrays.binarySearch(filenamesTo, filename) >= 0) continue;

            // Otherwise scale image and save with the same name
            String srcPhotoPath = new File(mPathFrom, filename).getAbsolutePath();
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(srcPhotoPath);
            int srcPhotoW = bmOptions.outWidth;
            int srcPhotoH = bmOptions.outHeight;

            // Determine the scale factor
            int scaleFactor = Math.min(
                    srcPhotoH / mTargetImageHeight,
                    srcPhotoW / mTargetImageWidth);

            // Scale the image
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            Bitmap resultPhoto = BitmapFactory.decodeFile(srcPhotoPath, bmOptions);

            // Save image to file
            File targetPhotoFile = new File(mPathTo, filename);
            FileOutputStream out = null;
            try {

                out = new FileOutputStream(targetPhotoFile.getAbsolutePath());
                resultPhoto.compress(Bitmap.CompressFormat.JPEG, 100, out);

            } catch (IOException e) {
                Log.e(TAG, "Failed to save image in file");
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) out.close();
                } catch(IOException e) {
                    Log.e(TAG, "Failed to close output stream");
                    e.printStackTrace();
                }
            }

            ++scaledCount;
        }

        Log.i(TAG, "Task finished");
        return scaledCount;
    }
}
