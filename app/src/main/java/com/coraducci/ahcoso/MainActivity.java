package com.coraducci.ahcoso;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.CamcorderProfile;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowMetrics;
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
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private Context CONTEXT;
    private final String TAG = getClass().getSimpleName();
    private ToastOrLog tol;

    //private static final String pathImages = Preferences.DIRECTORY_PICTURES;

    public ImageView ivImage;

    public PreviewView previewView;

    boolean boundService = false;

    private final int REQUEST_CODE_MEDIA_PROJECTION = 112;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaRecorder mMediaRecorder;
    private MediaProjectionManager mProjectionManager;
    private boolean permissionGranted = false;
    private int mVideoDensity;
    private int mVideoWidth;
    private int mVideoHeight;

    private static final String STATE_RESULT_CODE = "result_code";
    private static final String STATE_RESULT_DATA = "result_data";

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
            extra = BroadcastHelper.ACTIVITY_MAIN_EXTRA_START_RECORDING;
            if (intent.hasExtra(extra)) {
                tol.Print(TAG, Costants.BROADCAST_RECEIVER_PREFIX +extra, false, true);
                prepareRecording();
                startRecording();
            }
        }
    };

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CONTEXT = getApplicationContext();

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

        //mDisplayMetrics = new DisplayMetrics();
        //getWindowManager().getDefaultDisplay().getRealMetrics(mDisplayMetrics);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        mVideoDensity = displayMetrics.densityDpi;
        if(displayMetrics.widthPixels == 1080){
            mVideoWidth = 1080;
            mVideoHeight = 1920;
        }else{
            mVideoWidth = 1920;
            mVideoHeight = 1080;
        }
        tol.Print(TAG, "widthPixels:"+mVideoWidth, false, true);
        tol.Print(TAG, "heightPixels:"+mVideoHeight, false, true);
        tol.Print(TAG, "densityDpi:"+mVideoDensity, false, true);
        mProjectionManager = (MediaProjectionManager) getSystemService (Context.MEDIA_PROJECTION_SERVICE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(CONTEXT, CameraService.class), serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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

        checkPermissionsStep34();
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
        stopRecording();

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

    @SuppressLint("NewApi")
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

        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                tol.Print(TAG, "nr:"+mr, false, true);
                tol.Print(TAG, "what:"+what, false, true);
                tol.Print(TAG, "extra:"+extra, false, true);
            }
        });
        mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                tol.Print(TAG, "nr:"+mr, false, true);
                tol.Print(TAG, "what:"+what, false, true);
                tol.Print(TAG, "extra:"+extra, false, true);
                if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED){
                    //stopRecording();
                    prepareRecording();
                    startRecording();
                    /*
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            SendBroadcastTo sendBroadcastTo = new SendBroadcastTo(CONTEXT, BroadcastHelper.ACTIVITY_MAIN);
                            sendBroadcastTo.addExtra(new BroadcastHelper(BroadcastHelper.ACTIVITY_MAIN_EXTRA_START_RECORDING, Types.BOOLEAN, true));
                            sendBroadcastTo.send();
                        }
                    }, 1000);
                    */
                }
            }
        });
        mMediaRecorder.setOutputFile(filePath);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setMaxDuration(10000);
        mMediaRecorder.setVideoSize(mVideoWidth, mVideoHeight);
        //mMediaRecorder.setVideoFrameRate(5);
        //mMediaRecorder.setVideoEncodingBitRate(512 * 1000);

        try {
            mMediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startRecording() {
        tol.Print(TAG, "startRecording", false, true);
        getVirtualDisplay();
        mMediaRecorder.start();
    }
    private void stopRecording() {
        tol.Print(TAG, "stopRecording", false, true);
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
        }
        cleanVirtualDisplay();
        if (mMediaProjection != null) {
            mMediaProjection.stop();
        }
    }
    private void checkPermissionsStep34(){
        tol.Print(TAG, "checkPermissions_34", false, true);
        if(!permissionGranted) {
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_MEDIA_PROJECTION);
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        tol.Print(TAG, "requestCode:" + requestCode, false, true);
        tol.Print(TAG, "resultCode:" + resultCode, false, true);
        if (requestCode == REQUEST_CODE_MEDIA_PROJECTION) {
            if (resultCode == Activity.RESULT_OK) {
                permissionGranted = true;
                mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
                prepareRecording();
                startRecording();
            }
        }
    }
    private void getVirtualDisplay() {
        cleanVirtualDisplay();
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG,
                mVideoWidth,
                mVideoHeight,
                mVideoDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null, null);

    }
    private void cleanVirtualDisplay(){
        if(mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }
    }
}