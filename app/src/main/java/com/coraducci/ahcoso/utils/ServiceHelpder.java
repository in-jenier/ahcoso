package com.coraducci.ahcoso.utils;

import android.app.ActivityManager;
import android.content.Context;

import androidx.annotation.NonNull;

public class ServiceHelpder {
    public static boolean InRunning(@NonNull Context CONTEXT, Class<?> serviceClass) {
        boolean serviceRunning = false;
        ActivityManager manager = (ActivityManager) CONTEXT.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo runningServiceInfo : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(runningServiceInfo.service.getClassName())) {
                serviceRunning = true;
            }
        }
        return serviceRunning;
    }
}
