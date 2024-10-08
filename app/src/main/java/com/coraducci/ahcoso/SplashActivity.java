package com.coraducci.ahcoso;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.coraducci.ahcoso.utils.ServiceHelpder;
import com.coraducci.ahcoso.utils.ToastOrLog;

import java.sql.Time;
import java.util.Timer;
import java.util.TimerTask;

public class SplashActivity extends AppCompatActivity {

    private Context CONTEXT;
    private final String TAG = getClass().getSimpleName();
    private ToastOrLog tol;

    private final int REQUEST_CODE_CAMERA = 106;
    private final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 107;
    private final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 108;
    private final int REQUEST_CODE_MANAGE_EXTERNAL_STORAGE = 109;
    private final int REQUEST_CODE_POST_NOTIFICATIONS = 111;

    private Boolean permissionToCamera = false;
    private Boolean permissionToWriteExternalStorage = true;
    private Boolean permissionToReadExternalStorage = true;
    private Boolean permissionToManageExternalStorage = false;
    private Boolean permissionToPostNotification = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        CONTEXT = getApplicationContext();
        tol = new ToastOrLog(CONTEXT);
        tol.Print(TAG, "onCreate", false, true);

        checkPermissions();
    }


    private void checkPermissions(){
        tol.Print(TAG, "checkPermissions", false, true);
        if (ContextCompat.checkSelfPermission(CONTEXT, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA);
        } else {
            permissionToCamera = true;
            checkPermissionsStep1();
        }
    }
    private void checkPermissionsStep1(){
        tol.Print(TAG, "checkPermissionsStep1", false, true);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(CONTEXT, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionToWriteExternalStorage = false;
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
            }else {
                if (ContextCompat.checkSelfPermission(CONTEXT, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissionToReadExternalStorage = false;
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_READ_EXTERNAL_STORAGE);
                }else {
                    checkPermissionsStep3_1();
                }
            }
        }else{
            checkPermissionsStep3_1();
        }
    }
    private void checkPermissionsStep3_1(){
        tol.Print(TAG, "checkPermissionsStep3_1", false, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(CONTEXT, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionToPostNotification = false;
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_POST_NOTIFICATIONS);
            }else{
                AvviaServizio();
            }
        }else {
            tol.Print(TAG, "READY", true, true);
            openMainActivity();
        }
    }
    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        tol.Print(TAG, "requestCode:"+requestCode + "->" + grantResults[0], false, true);
        if (requestCode == REQUEST_CODE_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionToCamera = true;
            }else{
                checkPermissions();
            }
        }
        if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionToWriteExternalStorage = true;
            }else{
                checkPermissions();
            }
        }
        if (requestCode == REQUEST_CODE_MANAGE_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionToManageExternalStorage = true;
            }else{
                checkPermissions();
            }
        }
        if (requestCode == REQUEST_CODE_READ_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionToReadExternalStorage = true;
            }else{
                checkPermissions();
            }
        }
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionToPostNotification = true;
            }else{
                checkPermissions();
            }
        }
        if (permissionToCamera
                && permissionToWriteExternalStorage
                && permissionToManageExternalStorage
                && permissionToReadExternalStorage
                && permissionToPostNotification){
            tol.Print(TAG, "permissionsGranted", true, true);
            checkPermissions();
        }else{
            checkPermissions();
        }
    }
    private void AvviaServizio(){
        if (!ServiceHelpder.InRunning(CONTEXT, CameraService.class)) {
            try {
                Intent intent = new Intent(CONTEXT, CameraService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    CONTEXT.startForegroundService(intent);
                } else {
                    CONTEXT.startService(intent);
                }
                tol.Print(TAG, "serviceStarted", false, true);
            } catch (Exception e) {
                tol.Print(TAG, "serviceNotStarted:" + e.toString(), false, true);
            }
        }
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                openMainActivity();
            }
        }, 5000);
    }

    private void openMainActivity(){

        //tol.Print(TAG, "openMainActivity", true, true);
        finish();
        startActivity(new Intent(CONTEXT, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        );
    }

}