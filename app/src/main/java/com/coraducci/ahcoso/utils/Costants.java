package com.coraducci.ahcoso.utils;

import com.coraducci.ahcoso.R;

public class Costants {
    public static final String BUCKET = "AYoS.preferences";
    public static final int SERVICE_ID_CAMERA = 101;
    public static final String CHANNEL_ID_CAMERA = "com.coraducci.ahcoso.camera";;
    public static String CHAR_SPACE = " ";
    public static String CHAR_NULL = "";

    public static class Camera{
        public static final String[] SAVE_BY_TAG_OBJECT = {"Fashion good", "Person good"};
        public static final Integer[] SAVE_BY_TAG_LABELER = {46, 51, 58,66, 73, 149, 197, 209, 216, 249, 283, 390};
        public static final float SUBJECT_CONFIDENCE_GAP = 0.5f;
        public static final int TEXT_TARGA_LENGTH = 7;
    }
;
    public static final int IMAGE_REKOGNITION_DETECTION_ORDER_FACES = 1;
    public static final int IMAGE_REKOGNITION_DETECTION_ORDER_TEXT = 2;
    public static final int IMAGE_REKOGNITION_DETECTION_ORDER_COMPLETE = 3;

    public static final String INPUT_LANGUAGE_DEFAULT = "it";

    public static final String EXTRA_STRING_VALUE_TRUE = "true";
    public static final String EXTRA_STRING_VALUE_FALSE = "false";
    public static final boolean EXTRA_BOOLEAN_VALUE_TRUE = true;
    public static final boolean EXTRA_BOOLEAN_VALUE_FALSE = false;
    public static final String BROADCAST_RECEIVER_PREFIX = "BR->";

    public static final int NOTIFY_SERVICE_ID = 101;//it's the same for minor services of app
    public static final String NOTIFY_CHANNEL_ID = "it.havok.ahcoso";
    public static final String NOTIFY_CHANNEL_NAME = "AYOS";
    public static final String NOTIFY_CHANNEL_DESCRIPTION = "Notifies from the services of AYOS";
    public static final int STANDARD_NOTIFY_ICON_CAMERA_IDLE = R.drawable.ic_stat_filter_4;
    public static final int STANDARD_NOTIFY_ICON_CAMERA_RUN = R.drawable.ic_stat_looks_4;
}
