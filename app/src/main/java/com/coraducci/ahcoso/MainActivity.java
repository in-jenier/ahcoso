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
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Preview;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.coraducci.ahcoso.utils.BroadcastHelper;
import com.coraducci.ahcoso.utils.Costants;
import com.coraducci.ahcoso.utils.Now;
import com.coraducci.ahcoso.utils.Preferences;
import com.coraducci.ahcoso.utils.SendBroadcastTo;
import com.coraducci.ahcoso.utils.ServiceHelpder;
import com.coraducci.ahcoso.utils.ToastOrLog;
import com.coraducci.ahcoso.utils.Types;

import java.io.File;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private Context CONTEXT;
    private final String TAG = getClass().getSimpleName();
    private ToastOrLog tol;

    //private static final String pathImages = Preferences.DIRECTORY_PICTURES;

    public ImageView ivImage;

    public PreviewView previewView;

    boolean boundService = false;

    private static final int CAST_PERMISSION_CODE = 22;
    private DisplayMetrics mDisplayMetrics;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaRecorder mMediaRecorder;
    private MediaProjectionManager mProjectionManager;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @SuppressLint("RestrictedApi")
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            tol.Print(TAG, "onServiceConnected", false, true);
            CameraService.CameraServiceBinder binderService = (CameraService.CameraServiceBinder) service;
            CameraService cameraService = binderService.getService();

            cameraService.setImageView(ivImage);
            cameraService.setSurfaceProvider(previewView.getSurfaceProvider());
            previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);


            tol.Print(TAG, "img:"+ivImage.getWidth()+"x"+ivImage.getHeight(), false, true);
            tol.Print(TAG, "prv:"+previewView.getWidth()+"x"+previewView.getHeight(), false, true);
            tol.Print(TAG, "bmp:"+cameraService.getWidth()+"x"+cameraService.getHeight(), false, true);
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
        previewView = findViewById(R.id.previewView);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        Objects.requireNonNull(getSupportActionBar()).hide();

        mProjectionManager = (MediaProjectionManager) getSystemService (Context.MEDIA_PROJECTION_SERVICE);
        mDisplayMetrics = new DisplayMetrics();

        getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);

        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                tol.Print(TAG, "nr:"+mr, false, true);
                tol.Print(TAG, "what:"+what, false, true);
                tol.Print(TAG, "extra:"+extra, false, true);
            }
        });

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

        prepareRecording();
        startRecording();
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
        stopRecording();
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

    private void prepareRecording() {

        String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath() + File.separator;
        tol.Print(TAG, "path:"+directory, false, true);
        /*
        final File folder = new File(directory);
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdir();
        }
        */
        String videoName = new Now().MomentWithDate().replace(":", "")+".mp4";
        String filePath = directory + videoName;
        tol.Print(TAG, "file:"+filePath, false, true);

        int width = mDisplayMetrics.widthPixels;
        int height = mDisplayMetrics.heightPixels;
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        //mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(width, height);
        mMediaRecorder.setOutputFile(filePath);

        try {
            mMediaRecorder.prepare();
            mMediaRecorder.getSurface();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startRecording() {
        tol.Print(TAG, "startRecording", false, true);
        mMediaRecorder.start();
    }
    private void stopRecording() {
        tol.Print(TAG, "stopRecording", false, true);
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != CAST_PERMISSION_CODE) {
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Screen Cast Permission Denied :(", Toast.LENGTH_SHORT).show();
            return;
        }
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        // TODO Register a callback that will listen onStop and release & prepare the recorder for next recording
        // mMediaProjection.registerCallback(callback, null);
        mVirtualDisplay = getVirtualDisplay();
        mMediaRecorder.start();
    }
    private VirtualDisplay getVirtualDisplay() {
        int screenDensity = mDisplayMetrics.densityDpi;
        int width = mDisplayMetrics.widthPixels;
        int height = mDisplayMetrics.heightPixels;

        return mMediaProjection.createVirtualDisplay(this.getClass().getSimpleName(),
                width, height, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null, null);
    }
}