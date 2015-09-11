package com.example.vklyovan.dailyselfie;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

/**
 * The purpose of this notification alarm is to add notification about
 * necessity of making new selfie if such notification is not already existed
 * When the notification is pressed android teleports you to SelfieListActivity
 */
public class NotificationAlarmReceiver extends BroadcastReceiver {

    private static String TAG = "NotificationAlarmReceiver";

    public static int NOTIFICATION_GO_TO_APP_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Alarm notification received. Updating notification.");

        Intent goToAppIntent = new Intent(context, SelfieListActivity.class);
        PendingIntent pendingGoToAppIntent = PendingIntent.getActivity(
                context, 0, goToAppIntent, PendingIntent.FLAG_ONE_SHOT);

        // TODO resources, create custom view
        NotificationCompat.Builder notificationBuilder
                = new NotificationCompat.Builder(context)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentTitle("Selfie time")
                .setContentText("It's time to take selfie")
                .setContentIntent(pendingGoToAppIntent)
                .setAutoCancel(true);

        NotificationManager notificationMgr = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationMgr.notify(NOTIFICATION_GO_TO_APP_ID, notificationBuilder.build());
    }
}
