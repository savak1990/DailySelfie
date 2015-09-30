package com.example.vklyovan.dailyselfie;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

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
public class SelfieListActivity extends Activity
        implements SelfieListFragment.OnSelfieListFragmentListener {

    private static String TAG = "SelfieListActivity";

    private static int REQUEST_IMAGE_CAPTURE = 1;

    public static int NOTIFICATION_GO_TO_APP_ID = 10;

    private SelfieListFragment mSelfieListFragment;
    private SelfieViewFragment mSelfieViewFragment;

    private File mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Creating activity...");

        setContentView(R.layout.activity_selfie_list);


        mSelfieListFragment = (SelfieListFragment) getFragmentManager().findFragmentByTag(
                SelfieListFragment.TAG);

        // Create selfie list fragment
        if (mSelfieListFragment == null) {
            mSelfieListFragment = new SelfieListFragment();

            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, mSelfieListFragment, SelfieListFragment.TAG)
                    .commit();
        }

        Util.startNotificationAlarm(getApplicationContext());

        Log.i(TAG, "Activity created.");
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
                Log.i(TAG, "Take photo action clicked");
                startPhotoCapture();
                return true;
            case R.id.action_select_all:
                Log.i(TAG, "Select all action clicked");
                boolean isSelect = item.getTitle() ==
                        getResources().getString(R.string.action_select_all);
                mSelfieListFragment.onSelectAll(isSelect);
                if (isSelect) {
                    item.setTitle(getResources().getString(R.string.action_deselect_all));
                } else {
                    item.setTitle(getResources().getString(R.string.action_select_all));
                }
                return true;
            case R.id.action_remove_selected:
                Log.i(TAG, "Remove all action clicked");
                maybePopBackStack();
                mSelfieListFragment.onRemoveSelected();
                return true;
            default:
                Log.e(TAG, "Unsupported action clicked");
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        maybePopBackStack();

        if (requestCode == REQUEST_IMAGE_CAPTURE) {

            Log.i(TAG, "Take photo activity returned with code " + resultCode);

            if (resultCode == RESULT_OK) {

                // Add new image to list view
                if (StorageUtil.getSelfiePath() != null && mCurrentPhotoPath != null) {

                    addImageToGallery();

                    hideTakeSelfieNotification();

                    Util.startNotificationAlarm(getApplicationContext());

                    mSelfieListFragment.addSelfie(mCurrentPhotoPath);
                } else {
                    Log.e(TAG, "Result was RESULT_OK, but file was not saved in storage.");

                    Toast.makeText(getApplicationContext(),
                            "Image cannot be saved.", Toast.LENGTH_LONG).show();
                }

            } else {

                // Remove temporary file
                if (StorageUtil.getSelfiePath() != null
                        && mCurrentPhotoPath != null
                        && mCurrentPhotoPath.exists()) {
                    if (mCurrentPhotoPath.delete()) {
                        Log.i(TAG, "Temporary file successfully removed");
                    } else {
                        Log.e(TAG, "Temporary file failed to remove");
                    }
                } else {
                    Log.e(TAG, "Unable to remove temporary file");
                }
                mCurrentPhotoPath = null;
            }
        }
    }

    @Override
    public void onSelfieItemClicked(File selfieFile) {

        Log.i(TAG, "Starting selfie view activity for " + selfieFile.getAbsolutePath());

        mSelfieViewFragment = SelfieViewFragment.newInstance(selfieFile);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, mSelfieViewFragment, SelfieViewFragment.TAG)
                .addToBackStack(null)
                .commit();
    }

    private void maybePopBackStack() {
        if (mSelfieViewFragment != null && mSelfieViewFragment.isVisible()) {
            getFragmentManager().popBackStackImmediate();
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
                takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", 1);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);

                Log.i(TAG, "Successfully start image capture activity");
            } else {
                Log.e(TAG, "Failed to start image capture activity");
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat(
                "yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "SELFIE_" + timeStamp;

        mCurrentPhotoPath = null;
        if (StorageUtil.getSelfiePath() != null) {
            mCurrentPhotoPath = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    StorageUtil.getSelfiePath());
        } else {
            Log.e(TAG, "Temporary file was not created due to null selfie path");
        }

        return mCurrentPhotoPath;
    }

    private void addImageToGallery() {

        if (mCurrentPhotoPath != null) {
            Log.i(TAG, "Adding new image to gallery app...");
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(mCurrentPhotoPath);
            mediaScanIntent.setData(contentUri);
            sendBroadcast(mediaScanIntent);
        } else {
            Log.e(TAG, "Adding to gallery broadcast was not send. Current photo path is null");
        }
    }

    private void hideTakeSelfieNotification() {
        Log.i(TAG, "Hiding take photo notification...");

        NotificationManager notifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notifyMgr.cancel(NOTIFICATION_GO_TO_APP_ID);
    }
}
