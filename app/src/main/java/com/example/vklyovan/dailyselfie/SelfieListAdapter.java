package com.example.vklyovan.dailyselfie;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

class SelfieListAdapter extends ArrayAdapter<File> {

    private static final String TAG = "SelfieListAdapter";

    private final Activity mActivity;

    private ArrayList<SelfieItem> mSelfieItems = new ArrayList<>();

    public SelfieListAdapter(Activity activity, File selfieDir) {
        super(activity, R.layout.selfie_view);
        mActivity = activity;

        // Init selfie lists from file directory
        init(selfieDir);
    }

    public File getSelfieFile(int pos) {
        return mSelfieItems.get(pos).file;
    }

    public void selectSelfieItem(int i, boolean selected) {
        mSelfieItems.get(i).selected = selected;
    }

    public void removeSelected() {
        new SelfieRemoveTask(this).execute(mSelfieItems.toArray(new SelfieItem[mSelfieItems.size()]));
    }

    @Override
    public void add(File imageFile) {
        Log.i(TAG, "Adding a new photo");
        new SelfieItemsPrepareTask(mActivity, this).execute(imageFile);
    }

    @Override
    public void insert(File imageFile, int index) {
        Log.i(TAG, "Inserting a new photo");
        new SelfieItemsPrepareTask(mActivity, this, 0).execute(imageFile);
    }

    @Override
    public int getCount() {
        synchronized (mSelfieItems) {
            return mSelfieItems.size();
        }
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View rowView = convertView;

        // Reuse views
        if (rowView == null) {
            Log.v(TAG, "Creating new view...");
            LayoutInflater inflater = mActivity.getLayoutInflater();
            rowView = inflater.inflate(R.layout.selfie_view, null);

            // configure view holder
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.index = position;
            viewHolder.text = (TextView) rowView.findViewById(R.id.selfieTextView);
            viewHolder.image = (ImageView) rowView.findViewById(R.id.selfieImageView);
            viewHolder.selected = (CheckBox) rowView.findViewById(R.id.selectedItemCheckBox);
            viewHolder.selected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    if (viewHolder.index < mSelfieItems.size()) {
                        mSelfieItems.get(viewHolder.index).selected = isChecked;
                        Log.i(TAG, "Selfie item at " + viewHolder.index + " pos is "
                                + (isChecked ? "selected" : "unselected"));
                    }
                }
            });
            rowView.setTag(viewHolder);
        }

        // Fill data
        ViewHolder holder = (ViewHolder) rowView.getTag();

        String strSelfieDate = mSelfieItems.get(position).strDate;
        Bitmap selfieBitmap = mSelfieItems.get(position).scaledImg;

        Log.v(TAG, "Filling adapter item with date " + strSelfieDate
                + " with image of size (" + selfieBitmap.getWidth()
                + ", " + selfieBitmap.getHeight() + ")");

        holder.text.setText(strSelfieDate);
        holder.image.setImageBitmap(selfieBitmap);
        holder.selected.setChecked(mSelfieItems.get(position).selected);
        holder.index = position;

        return rowView;
    }

    private void init(File selfieDir) {
        Log.i(TAG, "Initializing array adapter");

        if (!selfieDir.isDirectory()) {
            Log.e(TAG, "Selfie path is not a directory");
            return;
        }

        File[] files = selfieDir.listFiles();
        new SelfieItemsPrepareTask(mActivity, this).execute(files);

        Log.i(TAG, "Adapter initialization finished");
    }

    private static class ViewHolder {
        private int index;
        private TextView text;
        private ImageView image;
        private CheckBox selected;
    }

    private static class SelfieItem {
        private File file;
        private File thumbnailFile;
        private String strDate;
        private Bitmap scaledImg;
        private boolean selected;
    }

    private static class SelfieRemoveTask extends AsyncTask<SelfieItem, SelfieItem, Void> {

        private WeakReference<SelfieListAdapter> mAdapter;

        public SelfieRemoveTask(SelfieListAdapter adapter) {
            mAdapter = new WeakReference<>(adapter);
        }

        @Override
        protected Void doInBackground(SelfieItem... items) {
            for (int i = 0; i < items.length; ++i) {
                if (isCancelled()) {
                    break;
                }

                SelfieItem item = items[i];
                if (item.selected) {
                    if (item.file.delete()) {
                        Log.i(TAG, item.file.getAbsolutePath() + " removed");
                    } else {
                        Log.e(TAG, item.file.getAbsolutePath() + " was not removed");
                    }

                    if (item.thumbnailFile.delete()) {
                        Log.i(TAG, item.thumbnailFile.getAbsolutePath() + " removed");
                    } else {
                        Log.e(TAG, item.thumbnailFile.getAbsolutePath() + " was not removed");
                    }

                    publishProgress(item);
                }
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(SelfieItem... items) {
            if (items.length < 1) {
                Log.e(TAG, "Empty items array in progress update");
            }
            SelfieItem item = items[0];

            SelfieListAdapter adapter = mAdapter.get();
            if (adapter != null) {
                adapter.mSelfieItems.remove(item);
                adapter.notifyDataSetChanged();
            } else {
                cancel(true);
            }
        }
    }

    private static class SelfieItemsPrepareTask extends AsyncTask<File, SelfieItem, Void> {

        private int mScaledW, mScaledH;
        private WeakReference<SelfieListAdapter> mAdapter;
        private int mIndex = -1;

        public SelfieItemsPrepareTask(Context ctx, SelfieListAdapter adapter) {
            mAdapter = new WeakReference<>(adapter);

            // Get target width and height from resources in pixels
            mScaledW = (int) ctx.getResources().getDimension(
                    R.dimen.selfie_image_width);
            mScaledH = (int) ctx.getResources().getDimension(
                    R.dimen.selfie_image_height);
        }

        public SelfieItemsPrepareTask(Context ctx, SelfieListAdapter adapter, int index) {
            this(ctx, adapter);
            mIndex = index;
        }

        @Override
        protected Void doInBackground(File... files) {
            if (files.length > 1) {
                Arrays.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(File lhs, File rhs) {
                        return (int) (rhs.lastModified() - lhs.lastModified());
                    }
                });

                Log.i(TAG, "Filenames have been sorted by date");
            }

            for (File file : files) {

                if (isCancelled()) {
                    break;
                }

                if (!validate(file)) {
                    Log.i(TAG, file.getName() + " is skipping");
                    continue;
                }

                SelfieItem item = createSelfieListItem(file);
                publishProgress(item);
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(SelfieItem... items) {
            if (items.length < 1) {
                return;
            }

            SelfieItem item = items[0];
            if (item == null) {
                return;
            }

            SelfieListAdapter adapter = mAdapter.get();
            if (adapter != null) {
                if (mIndex == -1) {
                    adapter.mSelfieItems.add(item);
                } else {
                    adapter.mSelfieItems.add(mIndex, item);
                }
                adapter.notifyDataSetChanged();
            } else {
                cancel(true);
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.i(TAG, "Task finished");
        }

        private SelfieItem createSelfieListItem(File file) {

            Log.v(TAG, file.getName() + " is processing to create list item");

            SelfieItem item = new SelfieItem();

            item.file = file;
            item.thumbnailFile = new File(
                    StorageUtil.getThumbnailSelfiePath(), item.file.getName());

            if (!item.thumbnailFile.exists()) {
                // Creating thumbnail file
                item.scaledImg = createAndSaveScaledImage(item.file, item.thumbnailFile);
            } else {
                // Get scaledImg from file
                item.scaledImg = BitmapFactory.decodeFile(
                        item.thumbnailFile.getAbsolutePath());

                if (item.scaledImg.getWidth() != mScaledW
                        || item.scaledImg.getHeight() != mScaledH) {

                    Log.i(TAG, "Scaled image size - (" + item.scaledImg.getWidth() + ", "
                            + item.scaledImg.getHeight() + ") while required is (" + mScaledW
                            + ", " + mScaledH + ")");
                    item.scaledImg = createAndSaveScaledImage(item.file, item.thumbnailFile);
                }

                Log.i(TAG, "Scaled image successfully retrieved from file "
                        + item.thumbnailFile.getAbsolutePath());
            }

            // Format date
            Date date = new Date(file.lastModified());
            SimpleDateFormat selfieDateFormat =
                    new SimpleDateFormat("yyyy/MM/dd - hh:mm:ss", Locale.US);
            item.strDate = selfieDateFormat.format(date);
            Log.v(TAG, file.getName() + " date is " + item.strDate);

            return item;

        }

        private Bitmap createAndSaveScaledImage(File inputFile, File outputFile) {
            Bitmap selfieBitmap = BitmapFactory.decodeFile(
                    inputFile.getAbsolutePath());

            // Save scaled img to file
            Bitmap scaledImg = ThumbnailUtils.extractThumbnail(selfieBitmap, mScaledW, mScaledH);

            Log.v(TAG, inputFile.getName() + " scaling to height "
                    + mScaledH + " and width " + mScaledW);

            if (outputFile == null) {
                Log.e(TAG, "Scaled image was not save to the file");

                return scaledImg;
            }

            try {
                if (!outputFile.exists()) {
                    if (!outputFile.createNewFile()) {
                        Log.e(TAG, "Failed to create new file " + outputFile.getAbsolutePath());

                        return scaledImg;
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Creating new thumbnail file failed ("
                        + e.getMessage() + "): "
                        + outputFile.getAbsolutePath());
                return scaledImg;
            }

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(outputFile);
                scaledImg.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                fos.flush();
                fos.close();
                Log.i(TAG, "Scaled image successfully saved in " + outputFile.getAbsolutePath());
            } catch (FileNotFoundException e) {
                Log.e(TAG, "File not found exception: " + outputFile.getAbsolutePath());
                if (fos != null) {
                    try {fos.close();} catch(IOException fnfe) {
                        Log.e(TAG, "Failed to close stream");
                    }
                }
                return scaledImg;
            } catch (IOException e) {
                Log.e(TAG, "IOException during saving file: " + outputFile.getAbsolutePath());
                try {fos.close();} catch(IOException ioe) {
                    Log.e(TAG, "Failed to close stream");
                }
                return scaledImg;
            }

            return scaledImg;
        }

        private boolean validate(File file) {
            // Check if not a directory
            if (file.isDirectory()) {
                Log.i(TAG, file.getName() + " is directory");
                return false;
            }

            // Remove file if it was temporary file
            if (file.length() == 0) {
                Log.i(TAG, file.getName() + " is temporary file. Deleting...");
                if (!file.delete()) {
                    Log.e(TAG, file.getName() + " is not removed due to unknown error");
                }
                return false;
            }

            // Check whether is image file
            if (!isImageFile(file)) {
                Log.i(TAG, file.getName() + " is not an image");
                return false;
            }

            Log.v(TAG, file.getName() + " is correct image");
            return true;
        }

        private boolean isImageFile(File filename) {
            String extension = "";
            int i = filename.getName().lastIndexOf(".");
            if (i > 0) {
                extension = filename.getName().substring(i+1);
            }
            if (extension.equals("jpg")) {
                return true;
            }
            return false;
        }
    }
}
