package com.example.vklyovan.dailyselfie;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
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

    private File mPublicSelfiePath;
    private File mPrivateScaledSelfiePath;

    private String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Creating activity...");
        setContentView(R.layout.activity_selfie_list);

        mPublicSelfiePath = new File(
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DCIM), SELFIES_FOLDER_NAME);

        mPrivateScaledSelfiePath = getExternalFilesDir(
                Environment.DIRECTORY_PICTURES);

        Log.i(TAG, "mPublicSelfiePath = " + mPublicSelfiePath);
        Log.i(TAG, "mPrivateScaledSelfiePath = " + mPrivateScaledSelfiePath);

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
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            // Add picture to gallery application
            galleryAddPic();

            // Hide notification about making selfie
            NotificationManager notifyMgr =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notifyMgr.cancel(NOTIFICATION_GO_TO_APP_ID);
            Log.i(TAG, "Take photo notification cancelled");

            // Reset alarm
            setNotificationAlarm();

            // TODO decode mCurrentImage to private app folder with small size
            // Create AsyncTask for this and then update list
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

        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        Log.i(TAG, "Current image path = " + image);

        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        Log.i(TAG, "Sending ACTION_MEDIA_SCANNER_SCAN_FILE");
        sendBroadcast(mediaScanIntent);
    }

    private void fullSyncScaledImages() {
        CreateScaledImagesTask task = new CreateScaledImagesTask(
                mPublicSelfiePath, mPrivateScaledSelfiePath, 100, 100);
        task.execute(mPublicSelfiePath.list());
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
}
