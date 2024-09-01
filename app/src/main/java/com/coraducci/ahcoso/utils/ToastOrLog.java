package com.coraducci.ahcoso.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class ToastOrLog {
    private Context CONTEXT;
    public boolean enable = true;

    public ToastOrLog(Context context) {
        CONTEXT = context;
    }

    public void Print(String TAG, String msg, Boolean toast, Boolean log) {
        if(enable) {
            if (toast) {
                Toast(msg);
            }
            if (log) {
                d(TAG, msg);
            }
        }
    }
    public void Toast(String msg){
        Toast.makeText(CONTEXT, msg, Toast.LENGTH_LONG).show();
    }
    public static void v(String TAG, String m){
        Log.v(TAG,  m);
    }
    public static void i(String TAG, String m){
        Log.i(TAG,  m);
    }
    public static void d(String TAG, String m){
        Log.d(TAG,  m);
    }
    public static void e(String TAG, String m, Exception e){
        Log.e(TAG,  m + e);
    }
    public static void e(String TAG, String m){
        Log.e(TAG,  m);
    }
    public static void w(String TAG, Exception e){
        Log.w(TAG,  e);
    }
    public static void w(String TAG, String e){
        Log.w(TAG,  e);
    }
    public static void w(String TAG, String m, Exception e){
        Log.w(TAG,  e);
    }
}
