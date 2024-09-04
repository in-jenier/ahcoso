package com.coraducci.ahcoso.utils;

import android.os.Environment;

import java.io.File;

public final class Preferences {
    public static final String GLOBAL_LANGUAGE = "global.language";
    public static final String LOG_ENABLE = "global.LOG";
    public static final String DIRECTORY_PICTURES = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + File.separator;
    public static final String MEDIA_PROJECTION_GRANTED = "mediaProjection.permissionGranted";

}
