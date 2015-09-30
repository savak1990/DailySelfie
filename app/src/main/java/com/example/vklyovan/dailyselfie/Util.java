package com.example.vklyovan.dailyselfie;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;

public class Util {

    private static final String TAG = "Util";

    public static final int NOTIFICATION_ALARM_ID = 100;

    private static int TWO_MINUTES = 2*60*1000;

    public static void startNotificationAlarm(Context ctx) {
        Log.i(TAG, "(Re)Starting notification alarm");

        Intent notificationIntent = new Intent(
                ctx, NotificationAlarmReceiver.class);

        PendingIntent pendingNotificationIntent = PendingIntent.getBroadcast(
                ctx,
                NOTIFICATION_ALARM_ID,
                notificationIntent, 0);

        AlarmManager alarmMgr = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.setRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + TWO_MINUTES,
                TWO_MINUTES,
                pendingNotificationIntent);
    }
}
