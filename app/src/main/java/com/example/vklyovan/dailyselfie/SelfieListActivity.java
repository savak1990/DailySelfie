package com.example.vklyovan.dailyselfie;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * @author Viacheslav Klovan
 *
 * This activity is a main entry point to the Daily Selfie application. It's
 * main purpose is to display the list of already made selfies in a ListView.
 * Also the user interface should provide the ActionBar button that allow us
 * to create new selfie.
 *
 * Also this activity should start the alarm, that will display notification
 * in notification bar about necessity to make new selfie every two minutes
 */
public class SelfieListActivity extends Activity {

    private static String TAG = "SelfieListActivity";

    private static String SELFIES_FOLDER_NAME = "Selfies";

    private static int REQUEST_IMAGE_CAPTURE = 1;

    public static int NOTIFICATION_GO_TO_APP_ID = 10;
    public static int NOTIFICATION_ALARM_ID = 100;

    private static int TWO_MINUTES = 15 * 1000; //TODO 2*60*1000;

    private File mPublicSelfiePath = new File(
            Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM), SELFIES_FOLDER_NAME);

    private String mCurrentPhotoName;

    private ListView mSelfieListView;
    private SelfieViewArrayAdapter mSelfieListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Creating activity...");
        setContentView(R.layout.activity_selfie_list);
        setSelfieList();
        setNotificationAlarm();
        Log.i(TAG, "Created.");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_selfie_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        Log.i(TAG, "Option selected: " + item.getTitle());

        switch (id) {
            case R.id.action_take_photo:
                startPhotoCapture();
                return true;
            case R.id.action_settings:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE) {

            if (resultCode == RESULT_OK) {
                // Add picture to gallery application
                galleryAddPic();

                // Hide notification about making selfie
                NotificationManager notifyMgr =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notifyMgr.cancel(NOTIFICATION_GO_TO_APP_ID);
                Log.i(TAG, "Take photo notification cancelled");

                // Reset alarm
                setNotificationAlarm();

                // Add new image to list view
                mSelfieListAdapter.insert(
                        new File(mPublicSelfiePath, mCurrentPhotoName), 0);
                mCurrentPhotoName = null;
            } else {

                // Remove temporary file
                File temp = new File(mPublicSelfiePath, mCurrentPhotoName);
                if (temp.delete()) {
                    Log.i(TAG, "Temporary file successfully removed");
                } else {
                    Log.e(TAG, "Temporary file failed to remove");
                }
                mCurrentPhotoName = null;
            }
        }
    }

    private void startPhotoCapture() {
        // Start activity for capturing a selfie
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            // Create photo file
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                Log.e(TAG, "IOException occurred while getting the file path for new selfie");
                e.printStackTrace();
            }

            if (photoFile != null) {
                takePictureIntent.putExtra(
                        MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);

                Log.i(TAG, "Successfully start take photo activity");
            } else {
                Log.e(TAG, "Failed to start take photo activity");
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat(
                "yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "SELFIE_" + timeStamp;

        if (!mPublicSelfiePath.exists()) {
            if (mPublicSelfiePath.mkdir()) {
                Log.i(TAG, "Public storage directory successfully created");
            } else {
                Log.e(TAG, "Public storage directory failed to create");
            }
        }

        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                mPublicSelfiePath);

        mCurrentPhotoName = image.getName();

        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mPublicSelfiePath, mCurrentPhotoName);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        Log.i(TAG, "Sending ACTION_MEDIA_SCANNER_SCAN_FILE");
        sendBroadcast(mediaScanIntent);
    }

    private void setNotificationAlarm() {

        Intent notificationIntent = new Intent(
                getApplicationContext(), NotificationAlarmReceiver.class);

        PendingIntent pendingNotificationIntent = PendingIntent.getBroadcast(
                getApplicationContext(),
                NOTIFICATION_ALARM_ID,
                notificationIntent, 0);

        AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmMgr.setRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + TWO_MINUTES,
                TWO_MINUTES,
                pendingNotificationIntent);

        Log.i(TAG, "Notification alarm has been set");
    }

    private boolean setSelfieList() {
        mSelfieListView = (ListView) findViewById(R.id.selfieListView);
        mSelfieListAdapter = new SelfieViewArrayAdapter(this);
        mSelfieListView.setAdapter(mSelfieListAdapter);

        File[] fileList = mPublicSelfiePath.listFiles();
        if (fileList == null) return false;

        // Sort values by date modified
        Arrays.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                return (int) (rhs.lastModified() - lhs.lastModified());
            }
        });

        // Prepare and set list adapter
        mSelfieListAdapter.attachFileList(fileList);
        return true;
    }

    private static class SelfieViewArrayAdapter extends ArrayAdapter<File> {

        private final Activity mActivity;

        private ArrayList<Bitmap> mSelfieBitmaps = new ArrayList<>();
        private ArrayList<String> mSelfieDates = new ArrayList<>();

        private SelfieViewArrayAdapter(Activity activity) {
            super(activity, R.layout.selfie_view);
            mActivity = activity;
        }

        private void attachFileList(File[] files) {
            initSelfiesInfo(files);
            notifyDataSetChanged();
        }

        @Override
        public void add(File imageFile) {
            mSelfieDates.add(getImageDate(imageFile));
            mSelfieBitmaps.add(getScaledBitmap(imageFile));
            notifyDataSetChanged();
        }

        @Override
        public void insert(File imageFile, int index) {
            Log.i(TAG, "Inserting new photo");
            mSelfieDates.add(index, getImageDate(imageFile));
            mSelfieBitmaps.add(index, getScaledBitmap(imageFile));
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mSelfieDates.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;

            // Reuse views
            if (rowView == null) {
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
            String strSelfieDate = mSelfieDates.get(position);
            Bitmap selfieBitmap = mSelfieBitmaps.get(position);

            holder.text.setText(strSelfieDate);
            holder.image.setImageBitmap(selfieBitmap);

            return rowView;
        }

        private void initSelfiesInfo(File[] files) {
            for (int i = 0; i < files.length; ++i) {

                // Remove file if it was temporary file
                if (files[i].length() <= 0) {
                    if (files[i].delete()) {
                        Log.i(TAG, "Removed forgotten temporary file");
                    } else {
                        Log.e(TAG, "Unable to remove forgotten temporary file");
                    }
                    continue;
                }

                mSelfieBitmaps.add(getScaledBitmap(files[i]));
                mSelfieDates.add(getImageDate(files[i]));
            }
        }

        private Bitmap getScaledBitmap(File imageFile) {
            // TODO
            // 1) Probably need to be done in the other thread
            // 2) Look how to scale image taking scale factor into accout
            // 3) We don't need to hardcode image size
            Bitmap selfieBitmap = BitmapFactory.decodeFile(
                    imageFile.getAbsolutePath());
            return ThumbnailUtils.extractThumbnail(selfieBitmap, 100, 100);
        }

        private String getImageDate(File imageFile) {
            Date date = new Date(imageFile.lastModified());
            SimpleDateFormat selfieDateFormat =
                    new SimpleDateFormat("yyyy/MM/dd - hh:mm:ss", Locale.US);
            return selfieDateFormat.format(date);
        }

        private static class ViewHolder {
            private TextView text;
            private ImageView image;
        }
    }
}
