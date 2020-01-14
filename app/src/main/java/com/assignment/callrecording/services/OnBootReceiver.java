package com.assignment.callrecording.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.github.axet.androidlibrary.preferences.OptimizationPreferenceCompat;
import com.assignment.callrecording.app.CallApplication;

public class OnBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        OptimizationPreferenceCompat.setPrefTime(context, CallApplication.PREFERENCE_BOOT, System.currentTimeMillis());
        RecordingService.startIfEnabled(context);
    }
}
