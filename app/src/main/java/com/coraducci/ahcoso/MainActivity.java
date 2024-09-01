package com.coraducci.ahcoso;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Preview;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.coraducci.ahcoso.utils.BroadcastHelper;
import com.coraducci.ahcoso.utils.Costants;
import com.coraducci.ahcoso.utils.Preferences;
import com.coraducci.ahcoso.utils.SendBroadcastTo;
import com.coraducci.ahcoso.utils.ServiceHelpder;
import com.coraducci.ahcoso.utils.ToastOrLog;
import com.coraducci.ahcoso.utils.Types;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private Context CONTEXT;
    private final String TAG = getClass().getSimpleName();
    private ToastOrLog tol;

    private static final String pathImages = Preferences.DIRECTORY_PICTURES;

    public ImageView ivImage;

    CameraService cameraService;
    boolean boundService = false;


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @SuppressLint("RestrictedApi")
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            tol.Print(TAG, "onServiceConnected", false, true);
            CameraService.CameraServiceBinder binderService = (CameraService.CameraServiceBinder) service;
            cameraService = binderService.getService();

            cameraService.setImageView(ivImage);
            boundService = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            boundService = false;
        }
    };

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @SuppressLint("NewApi")
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            String extra = BroadcastHelper.ACTIVITY_MAIN_EXTRA_SERVICE_AVAILABLE;
            if (intent.hasExtra(extra)) {
                tol.Print(TAG, Costants.BROADCAST_RECEIVER_PREFIX +extra, false, true);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CONTEXT = getApplicationContext();

        SharedPreferences sharedPreferences = CONTEXT.getSharedPreferences(Costants.BUCKET, Context.MODE_PRIVATE);

        tol = new ToastOrLog(CONTEXT);
        //tol.enable = sharedPreferences.getBoolean(Preferences.LOG_ENABLE, false);
        tol.Print(TAG, "onCreate", false, true);

        ivImage = findViewById(R.id.imageViewImage);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        Objects.requireNonNull(getSupportActionBar()).hide();

    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(CONTEXT, CameraService.class), serviceConnection, BIND_AUTO_CREATE);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();

        tol.Print(TAG, "onResume", false, true);

        SharedPreferences sharedPreferences = CONTEXT.getSharedPreferences(Costants.BUCKET, Context.MODE_PRIVATE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BroadcastHelper.ACTIVITY_MAIN);
        registerReceiver(broadcastReceiver, intentFilter);

        sendActivityInUse(Costants.EXTRA_BOOLEAN_VALUE_TRUE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        tol.Print(TAG, "onPause", false, true);

        if(boundService) {
            unbindService(serviceConnection);
            boundService = false;
        }

        if(broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }

        sendActivityInUse(Costants.EXTRA_BOOLEAN_VALUE_FALSE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tol.Print(TAG, "onDestroy", false, true);
    }
    private void setImageOnView(Bitmap bitmap){
        ivImage.setImageBitmap(bitmap);
        tol.Print(TAG, "imageByBitmap", false, true);
    }
    private void sendActivityInUse(boolean inUse){
        SendBroadcastTo sendBroadcastTo = new SendBroadcastTo(CONTEXT, BroadcastHelper.SERVICE_CAMERA);
        sendBroadcastTo.addExtra(new BroadcastHelper(BroadcastHelper.ACTIVITY_MAIN_EXTRA_IN_USE, Types.BOOLEAN, inUse));
        sendBroadcastTo.send();
    }


}