package com.coraducci.ahcoso.utils;

import java.time.LocalDateTime;

public class BroadcastHelper {
    public String extra = "";
    public int type = 0;
    public String valueString = "";
    public Boolean valueBoolean = false;
    public Integer valueInteger = 0;
    public Float valueFloat = 0.0f;
    public Double valueDouble = 0.0;
    public Long valueLong = 0L;
    public LocalDateTime dataTime;
    public byte[] valueByteArray;

    public static final String GLOBAL_EXTRA_SCREEN_ORIENTATION = "3";
    public static final String GLOBAL_EXTRA_CUSTOM_NOTIFY_ICONS = "2";
    public static final String GLOBAL_EXTRA_SCREEN_ROTATION = "6";

    public static final String SERVICE_CAMERA = "100";//"remoteToCamera"

    public static final String SERVICE_CAMERA_EXTRA_CAPTURE_START = "101";
    public static final String ACTIVITY_MAIN_EXTRA_SERVICE_AVAILABLE = "102";


    public static final String ACTIVITY_MAIN = "1000";//"activityMain"
    public static final String ACTIVITY_MAIN_EXTRA_IN_USE = "1001";
    public static final String ACTIVITY_MAIN_EXTRA_START_RECORDING = "1002";

    public BroadcastHelper(String extra, int type, String value) {
        this.extra = extra;
        this.type = type;
        this.valueString = value;
    }

    public BroadcastHelper(String extra, int type, boolean value) {
        this.extra = extra;
        this.type = type;
        this.valueBoolean = value;
    }

    public BroadcastHelper(String extra, int type, int value) {
        this.extra = extra;
        this.type = type;
        this.valueInteger = value;
    }

    public BroadcastHelper(String extra, int type, long value) {
        this.extra = extra;
        this.type = type;
        this.valueLong = value;
    }

    public BroadcastHelper(String extra, int type, float value) {
        this.extra = extra;
        this.type = type;
        this.valueFloat = value;
    }

    public BroadcastHelper(String extra, int type, double value) {
        this.extra = extra;
        this.type = type;
        this.valueDouble = value;
    }

    public BroadcastHelper(String extra, int type, byte[] value) {
        this.extra = extra;
        this.type = type;
        this.valueByteArray = value;
    }

    public BroadcastHelper(String extra, int type, LocalDateTime value) {
        this.extra = extra;
        this.type = type;
        this.dataTime = value;
    }
}
