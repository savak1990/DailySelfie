package com.example.vklyovan.dailyselfie;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Date;

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
public class SelfieListActivity extends ActionBarActivity {

    private static String TAG = "SelfieListActivity";

    private static int NOTIFICATION_ALARM_ID = 100;

    private static int TWO_MINUTES = 15 * 1000; //TODO 2*60*1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "Creating activity...");

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_selfie_list);

        maybeSetNotificationAlarm();

        Log.i(TAG, "Created.");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_selfie_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void maybeSetNotificationAlarm() {

        Log.i(TAG, "Setting notification alarm...");

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
    }
}
