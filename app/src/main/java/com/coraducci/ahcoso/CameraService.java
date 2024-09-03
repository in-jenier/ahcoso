package com.coraducci.ahcoso;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ExperimentalLensFacing;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionFilter;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleService;
import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.coraducci.ahcoso.utils.BroadcastHelper;
import com.coraducci.ahcoso.utils.CommonNotificationSystem;
import com.coraducci.ahcoso.utils.Costants;
import com.coraducci.ahcoso.utils.GraphicOverlay;
import com.coraducci.ahcoso.utils.Messages;
import com.coraducci.ahcoso.utils.Now;
import com.coraducci.ahcoso.utils.Preferences;
import com.coraducci.ahcoso.utils.SendBroadcastTo;
import com.coraducci.ahcoso.utils.ToastOrLog;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.MlKit;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CameraService  extends LifecycleService {

    private Context CONTEXT;
    private final String TAG = getClass().getSimpleName();

    private final int SERVICE_ID = Costants.SERVICE_ID_CAMERA;
    private final String CHANNEL_ID = Costants.CHANNEL_ID_CAMERA;
    private final String CHANNEL_NAME = "Camera";
    private final String CHANNEL_DESCRIPTION = "";
    private final int NOTIFY_NUMBER = 1;
    private CommonNotificationSystem cns;

    private WorkManager workManager;

    private ToastOrLog tol;
    private Camera camera;
    private CameraManager cameraManager;
    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;
    private ImageCapture imageCapture;
    private static ImageProxy imageProxy;
    private Bitmap bitmap;
    private CameraSelector cameraSelector;

    private LifecycleOwner lifecycleOwner;

    private boolean mainActivityInUse = false;

    private int screenOrientation = 0;
    private int previousScreeOrientation = 0;
    private GraphicOverlay graphicOverlay;

    private Preview preview;

    private ImageView ivOnCameraActivity;

    private int mlAnalizerCurrentStep = Costants.IMAGE_REKOGNITION_DETECTION_ORDER_FACES;
    private final float ML_POSEDETECTOR_GAP = 0.95f;

    private boolean mlFaceDetector = true;
    private boolean mlTextDetector = true;

    private boolean mlFaceCompleted = false;
    private boolean mlTextCompleted = false;

    private final IBinder binder = new CameraServiceBinder();

    private Executor executor;

    public class CameraServiceBinder extends Binder {
        public CameraService getService() {
            // Return this instance of LocalService so clients can call public methods.
            return CameraService.this;
        }
    }
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @SuppressLint("NewApi")
        @Override
        public void onReceive(Context context, Intent intent) {
            String extra = BroadcastHelper.ACTIVITY_MAIN_EXTRA_IN_USE;
            if (intent.hasExtra(extra)) {
                mainActivityInUse = intent.getBooleanExtra(extra, Costants.EXTRA_BOOLEAN_VALUE_FALSE);
                tol.Print(TAG, Costants.BROADCAST_RECEIVER_PREFIX +extra, false, true);
            }
            extra = BroadcastHelper.SERVICE_CAMERA_EXTRA_CAPTURE_START;
            if (intent.hasExtra(extra)) {
                tol.Print(TAG, Costants.BROADCAST_RECEIVER_PREFIX +extra, false, true);
            }
        }
    };

    @SuppressLint("NewApi")
    @Override
    public void onCreate() {
        super.onCreate();

        CONTEXT = getApplicationContext();

        SharedPreferences sharedPreferences = CONTEXT.getSharedPreferences(Costants.BUCKET, Context.MODE_MULTI_PROCESS);

        tol = new ToastOrLog(CONTEXT);
        //tol.enable = sharedPreferences.getBoolean(Preferences.LOG_ENABLE, false);
        tol.Print(TAG, "onCreate", false, true);

        cns = new CommonNotificationSystem(CONTEXT);
        cns.sameChannel = true;
        cns.customIcons = false;
        cns.setParams(NOTIFY_NUMBER, SERVICE_ID, CHANNEL_ID, CHANNEL_NAME, CHANNEL_DESCRIPTION);

        Messages.setCulture(CONTEXT, Locale.forLanguageTag(sharedPreferences.getString(Preferences.GLOBAL_LANGUAGE, Costants.INPUT_LANGUAGE_DEFAULT)));

        WorkManager.initialize(CONTEXT, new Configuration.Builder().build());

        lifecycleOwner = this;
        MlKit.initialize(CONTEXT);

        String message;
        Intent cameraIntent = new Intent(CONTEXT, this.getClass());
        switch (Build.VERSION.SDK_INT) {
            case Build.VERSION_CODES.M://Android 6
            case Build.VERSION_CODES.N://Android 7
                message = "foregroundService_Android_M";
                startService(cameraIntent);
                break;
            case Build.VERSION_CODES.O://Android 8
                message = "foregroundService_Android_O";
                startForeground(SERVICE_ID, cns.Android_O());
                break;
            case Build.VERSION_CODES.P://Android 9
                message = "foregroundService_Android_P";
                startForeground(SERVICE_ID, cns.Android_P());
                break;
            case Build.VERSION_CODES.Q://Android 10
            case Build.VERSION_CODES.R://Android 11
            case Build.VERSION_CODES.S://Android 12
            case Build.VERSION_CODES.TIRAMISU://Android 13
            case Build.VERSION_CODES.UPSIDE_DOWN_CAKE://Android 14
                message = "foregroundServiceForMicrophone";
                startForeground(cns.SERVICE_ID, cns.Android_P(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST);
                break;
            default:
                message = "classicService";
                startService(cameraIntent);
        }
        tol.Print(TAG, message, false, true);
    }

    @SuppressLint("MissingSuperCall")
    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        return binder;
    }

    /** Methods for clients. */
    public void setImageView(ImageView imageView){
        ivOnCameraActivity = imageView;
    }
    /** Methods for clients. */
    /*
    public void setPreviewView(PreviewView preview){
        previewView = preview;
    }
    */
    public void setSurfaceProvider(Preview.SurfaceProvider provider){
        preview.setSurfaceProvider(provider);
    }
    public int getWidth(){
        return bitmap.getWidth();
    }
    public int getHeight(){
        return bitmap.getHeight();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

        tol.Print(TAG, "onDestroy", false, true);

        unregisterReceiver(broadcastReceiver);

        if (imageProxy != null) {
            imageProxy.close();
        }
        unbindCamera();
    }
    @SuppressLint({"NewApi", "UnspecifiedRegisterReceiverFlag"})
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        tol.Print(TAG, "onStartCommand", false, true);

        cns.MakeNotification(CHANNEL_NAME, CHANNEL_DESCRIPTION, true, true, true);

        OrientationEventListener orientationEventListener = new OrientationEventListener(CONTEXT, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int i) {
                screenOrientation = i;
                int needToRotate = 0;
                if (screenOrientation > 315 || screenOrientation <= 45) {
                    needToRotate = 0;
                }
                if (screenOrientation > 45 && screenOrientation <= 135) {
                    needToRotate = 90;
                }
                if (screenOrientation > 135 && screenOrientation <= 225) {
                    needToRotate = 180;
                }
                if (screenOrientation > 225 && screenOrientation <= 315) {
                    needToRotate = 270;
                }
                if(needToRotate != previousScreeOrientation) {
                    screenOrientation = needToRotate;
                }
                previousScreeOrientation = needToRotate;
            }
        };
        if(orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BroadcastHelper.SERVICE_CAMERA);
        registerReceiver(broadcastReceiver, intentFilter);

        graphicOverlay = new GraphicOverlay();

        cameraManager = (CameraManager) CONTEXT.getSystemService(Context.CAMERA_SERVICE);

        executor = Executors.newSingleThreadExecutor();

        startCamera();

        return START_REDELIVER_INTENT;
    }

    private void unbindCamera(){
        cameraProvider.unbindAll();
    }

    @SuppressLint("NewApi")
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(CONTEXT);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                tol.Print(TAG, "ProcessCameraProvider->addListener->run:"+cameraProvider, false, true);

                bindCamera();
                streamAnalysis();

            } catch (ExecutionException | InterruptedException e) {
                tol.Print(TAG, "ProcessCameraProvider->addListener->run->error:" + e, false, true);
            }
        }, ContextCompat.getMainExecutor(CONTEXT));
    }

    @OptIn(markerClass = ExperimentalLensFacing.class)
    @SuppressLint({"NewApi", "RestrictedApi"})
    public void bindCamera() {
        preview = new Preview.Builder()
                .build();

        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        imageCapture = new ImageCapture.Builder()
                .setCaptureType(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE)
                .build();

        imageAnalysis = new ImageAnalysis.Builder()
                .setResolutionSelector(new ResolutionSelector.Builder()
                        .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                        .build())
                .build();
        try {
            unbindCamera();
            String bindType = "bindToLifecycle";
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis, imageCapture);

            tol.Print(TAG, bindType, false, true);
        } catch (IllegalArgumentException e) {
            tol.Print(TAG, "bindPreview->error:" + e, false, true);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public void streamAnalysis() {
        //tol.Print(TAG, "streamAnalysis", false, true);
        imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
            @OptIn(markerClass = ExperimentalGetImage.class)
            @Override
            public void analyze(@NonNull ImageProxy proxy) {
                if (proxy.getImage() != null) {
                    mlFaceCompleted = false;
                    mlTextCompleted = false;
                    //ATTENZIONE rotation E' SEMPRE 90
                    int rotation = proxy.getImageInfo().getRotationDegrees();
                    bitmap = proxy.toBitmap();
                    if (rotation > 0) {
                        Matrix matrix = new Matrix();
                        matrix.preRotate(rotation);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    }
                    rotationImageByOrientation();
                    graphicOverlay.setCanvas(bitmap.getWidth(), bitmap.getHeight());
                    graphicOverlay.empty();
                    imageProxy = proxy;
                    InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
                    //ML_Manager();
                    ML_FaceDetector(inputImage);
                    ML_TextDetector(inputImage);
                }
            }
        });
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    @SuppressLint("NewApi")
    private void ML_Manager(InputImage inputImage) {
        //tol.Print(TAG, "mlAnalizerCurrentStep:" + mlAnalizerCurrentStep, false, true);
        switch (mlAnalizerCurrentStep) {
            case Costants.IMAGE_REKOGNITION_DETECTION_ORDER_FACES:
                mlAnalizerCurrentStep = Costants.IMAGE_REKOGNITION_DETECTION_ORDER_TEXT;
                if (mlFaceDetector) {
                    ML_FaceDetector(inputImage);
                } else {
                    ML_Manager(inputImage);
                }
                break;
            case Costants.IMAGE_REKOGNITION_DETECTION_ORDER_TEXT:
                mlAnalizerCurrentStep = Costants.IMAGE_REKOGNITION_DETECTION_ORDER_COMPLETE;
                if (mlTextDetector) {
                    ML_TextDetector(inputImage);
                }else{
                    ML_Manager(inputImage);
                }
                break;
            case Costants.IMAGE_REKOGNITION_DETECTION_ORDER_COMPLETE:
                mlAnalizerCurrentStep = Costants.IMAGE_REKOGNITION_DETECTION_ORDER_FACES;
                if(mainActivityInUse) {
                    ivOnCameraActivity.setImageBitmap(bitmap);
                }
                if(imageProxy != null) {
                    imageProxy.close();
                }
                streamAnalysis();
                break;
        }
    }

    @SuppressLint("NewApi")
    private void ML_FaceDetector(InputImage inputImage) {
        /*
        FACE DETECTOR
        */
        //tol.Print(TAG, "faceDetector", false, true);
        FaceDetectorOptions faceDetectorOptions = new FaceDetectorOptions.Builder()
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                //.setExecutor(executor)
                .build();
        FaceDetection.getClient(faceDetectorOptions)
                .process(inputImage)
                .addOnSuccessListener(
                        faces -> {
                            if (!faces.isEmpty()) {
                                graphicOverlay.drawFacesBoxes(faces);
                            }
                        })
                .addOnCompleteListener(task -> {
                    mlFaceCompleted = true;
                    waitMLCompleted();
                });
    }

    @SuppressLint("NewApi")
    private void ML_TextDetector(InputImage inputImage) {
        /*
        SEGMENTATION DETECTOR
        */
        //tol.Print(TAG, "textDetector", false, true);
        TextRecognizerOptions textRecognizerOptions = new TextRecognizerOptions.Builder()
                //.setExecutor(executor)
                .build();
        TextRecognition.getClient(textRecognizerOptions)
                .process(inputImage)
                .addOnSuccessListener(
                        result -> {
                            String resultText = result.getText();
                            String targa = resultText.replace(Costants.CHAR_SPACE, Costants.CHAR_NULL).trim();
                            boolean found = false;
                            List<Rect> rectList = new ArrayList<>();
                            for (Text.TextBlock block : result.getTextBlocks()) {
                                for (Text.Line line : block.getLines()) {
                                    for (Text.Element element : line.getElements()) {
                                        String elementText = element.getText();
                                        targa = elementText.replace(Costants.CHAR_SPACE, Costants.CHAR_NULL).trim();
                                        if(targa.length() == Costants.Camera.TEXT_TARGA_LENGTH) {
                                            found = true;
                                            rectList.add(element.getBoundingBox());
                                            //tol.Print(TAG, "targa:"+targa, false, true);
                                        }
                                        for (Text.Symbol symbol : element.getSymbols()) {
                                            String symbolText = symbol.getText();
                                            targa = symbolText.replace(Costants.CHAR_SPACE, Costants.CHAR_NULL).trim();
                                            if(targa.length() == Costants.Camera.TEXT_TARGA_LENGTH) {
                                                found = true;
                                                rectList.add(symbol.getBoundingBox());
                                                //tol.Print(TAG, "targa:"+targa, false, true);
                                            }
                                        }
                                    }
                                }
                            }
                            if(found){
                                graphicOverlay.drawBoxes(rectList);
                            }
                        })
                .addOnCompleteListener(
                        task -> {
                            mlTextCompleted = true;
                            waitMLCompleted();
                        });
    }


    @SuppressLint("NewApi")
    private void waitMLCompleted() {
        //tol.Print(TAG, "waitMLCompleted", false, true);
        if (mlFaceCompleted && mlTextCompleted) {
            /*
            if (mlFaceDetector) {
                mlFaceCompleted = false;
            }
            if (mlTextDetector) {
                mlTextCompleted = false;
            }
            */
            if(mainActivityInUse) {
                ivOnCameraActivity.setImageBitmap(graphicOverlay.getCanvas());
            }
            imageProxy.close();
            streamAnalysis();
        }
        //ML_Manager();
    }

    @SuppressLint("NewApi")
    private void OperationsOnImage() {
        //to save the strema
        String fileName = "";
        fileName += (new Now().MomentWithDate().replace(":", ""))
                + ".jpg";

        File file = new File(Preferences.DIRECTORY_PICTURES + fileName);
        if (!file.exists()) {
            String message = "file:" + fileName;
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)) {
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    message += "=>saved";
                }
            } catch (IOException e) {
                message += "->" + e;
            }
            tol.Print(TAG, message, false, true);
        }
    }

    private void rotationImageByOrientation() {
        float needToRotate = 0f;
        if (screenOrientation > 315 || screenOrientation <= 45) {
            needToRotate = 0f;
        }
        if (screenOrientation > 45 && screenOrientation <= 135) {
            needToRotate = 90f;
        }
        if (screenOrientation > 135 && screenOrientation <= 225) {
            needToRotate = 180f;
        }
        if (screenOrientation > 225 && screenOrientation <= 315) {
            needToRotate = 270f;
        }
        if (needToRotate > 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(needToRotate);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
    }

    private void sendBroadcastToActivity(String activity, List<BroadcastHelper> broadcastTriplets){
        SendBroadcastTo sendBroadcastTo = new SendBroadcastTo(CONTEXT, activity);
        for(BroadcastHelper broadcastTriplet : broadcastTriplets) {
            sendBroadcastTo.addExtra(broadcastTriplet);
        }
        sendBroadcastTo.send();
    }
}