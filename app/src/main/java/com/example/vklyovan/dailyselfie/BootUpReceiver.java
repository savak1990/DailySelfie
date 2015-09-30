package com.example.vklyovan.dailyselfie;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootUpReceiver extends BroadcastReceiver {

    private static final String TAG = "BootUpReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i(TAG, "Device finished booting up.");

        Util.startNotificationAlarm(context);
    }
}
