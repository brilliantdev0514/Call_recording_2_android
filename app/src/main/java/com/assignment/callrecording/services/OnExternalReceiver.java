package com.assignment.callrecording.services;

import android.content.Context;
import android.content.Intent;

public class OnExternalReceiver extends com.github.axet.androidlibrary.services.OnExternalReceiver {
    @Override
    public void onBootReceived(Context context) {
        super.onBootReceived(context);
        RecordingService.startIfEnabled(context);
    }
}
