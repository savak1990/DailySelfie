package com.example.vklyovan.dailyselfie;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class SelfieListFragment extends Fragment {

    private static String TAG = "SelfieListFragment";

    private SelfieListActivity mSelfieListActivity;

    private SelfieViewArrayAdapter mSelfieListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Creating...");

        // TODO remove link to activity. Could lead to memory leak.
        mSelfieListActivity = (SelfieListActivity) getActivity();
        mSelfieListAdapter = new SelfieViewArrayAdapter(
                mSelfieListActivity,
                mSelfieListActivity.getPublicSelfiePath());
        setRetainInstance(true);

        Log.i(TAG, "Created.");
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "Creating  view...");

        View view = inflater.inflate(R.layout.fragment_selfie_list, container);
        GridView selfieGridView = (GridView) view.findViewById(R.id.selfieGridView);
        selfieGridView.setAdapter(mSelfieListAdapter);

        Log.i(TAG, "Created.");
        return view;
    }

    public void addSelfie(File file) {
        mSelfieListAdapter.insert(file, 0);
    }

    private static class SelfieViewArrayAdapter extends ArrayAdapter<File> {

        private final Activity mActivity;

        // Target scaled image width and height
        private int mScaledW;
        private int mScaledH;

        private ArrayList<SelfieItem> mSelfieItems = new ArrayList<>();

        private SelfieViewArrayAdapter(Activity activity, File selfieDir) {
            super(activity, R.layout.selfie_view);
            mActivity = activity;

            // Get target width and height from resources in pixels
            mScaledW = (int) mActivity.getResources().getDimension(
                    R.dimen.selfie_image_width);
            mScaledH = (int) mActivity.getResources().getDimension(
                    R.dimen.selfie_image_height);

            // Init selfie lists from file directory
            init(selfieDir);
        }

        @Override
        public void add(File imageFile) {
            mSelfieItems.add(createSelfieListItem(imageFile));
            notifyDataSetChanged();
        }

        @Override
        public void insert(File imageFile, int index) {
            Log.i(TAG, "Inserting new photo");
            mSelfieItems.add(index, createSelfieListItem(imageFile));
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mSelfieItems.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;

            // Reuse views
            if (rowView == null) {
                Log.v(TAG, "Creating new view...");
                LayoutInflater inflater = mActivity.getLayoutInflater();
                rowView = inflater.inflate(R.layout.selfie_view, null);

                // configure view holder
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.text = (TextView) rowView.findViewById(R.id.selfieTextView);
                viewHolder.image = (ImageView) rowView.findViewById(R.id.selfieImageView);
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

            return rowView;
        }

        private void init(File selfieDir) {
            Log.i(TAG, "Initializing array adapter");

            if (!selfieDir.isDirectory()) {
                Log.e(TAG, "Selfie path is not a directory");
                return;
            }

            File[] files = selfieDir.listFiles();

            // Sort values by date modified
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File lhs, File rhs) {
                    return (int) (rhs.lastModified() - lhs.lastModified());
                }
            });

            Log.i(TAG, "Filenames have been sorted by date");

            for (File file : files) {
                if (!validate(file)) {
                    Log.i(TAG, file.getName() + " is skipping");
                    continue;
                }
                mSelfieItems.add(createSelfieListItem(file));
            }

            Log.i(TAG, "Adapter initialization finished");
        }

        private SelfieItem createSelfieListItem(File file) {

            Log.v(TAG, file.getName() + " is processing to create list item");

            SelfieItem item = new SelfieItem();

            item.file = file;

            // Get thumbnail image
            // TODO
            // 1) Probably need to be done in the other thread
            // 2) Look how to scale image taking scale factor into account
            Bitmap selfieBitmap = BitmapFactory.decodeFile(
                    file.getAbsolutePath());

            Log.v(TAG, file.getName() + " scaling to height "
                    + mScaledH + " and width " + mScaledW);

            item.scaledImg = ThumbnailUtils.extractThumbnail(selfieBitmap, mScaledW, mScaledH);

            // Format date
            Date date = new Date(file.lastModified());
            SimpleDateFormat selfieDateFormat =
                    new SimpleDateFormat("yyyy/MM/dd - hh:mm:ss", Locale.US);
            item.strDate = selfieDateFormat.format(date);
            Log.v(TAG, file.getName() + " date is " + item.strDate);

            return item;

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
            // TODO
            return true;
        }

        private static class ViewHolder {
            private TextView text;
            private ImageView image;
        }

        private static class SelfieItem {
            private File file;
            private String strDate;
            private Bitmap scaledImg;
        }
    }
}
