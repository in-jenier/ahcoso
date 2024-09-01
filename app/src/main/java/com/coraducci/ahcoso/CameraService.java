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
import android.hardware.camera2.CameraManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ExperimentalLensFacing;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleService;
import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.coraducci.ahcoso.utils.BoxWithLabel;
import com.coraducci.ahcoso.utils.BroadcastHelper;
import com.coraducci.ahcoso.utils.CommonNotificationSystem;
import com.coraducci.ahcoso.utils.Costants;
import com.coraducci.ahcoso.utils.GraphicOverlay;
import com.coraducci.ahcoso.utils.LabelsList;
import com.coraducci.ahcoso.utils.Messages;
import com.coraducci.ahcoso.utils.Now;
import com.coraducci.ahcoso.utils.Preferences;
import com.coraducci.ahcoso.utils.SendBroadcastTo;
import com.coraducci.ahcoso.utils.ToastOrLog;
import com.coraducci.ahcoso.utils.Types;
import com.coraducci.ahcoso.utils.mPoint;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.MlKit;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions;
import com.google.mlkit.vision.segmentation.subject.Subject;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

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
    private InputImage inputImage;
    private Bitmap bitmap;
    private CameraSelector cameraSelector;

    private LifecycleOwner lifecycleOwner;

    private boolean mainActivityInUse = false;

    private int screenOrientation = 0;
    private GraphicOverlay graphicOverlay;

    private Preview preview;

    private ImageView ivOnCameraActivity;

    private int mlAnalizerCurrentStep = Costants.IMAGE_REKOGNITION_DETECTION_ORDER_OBJECT;
    private final float ML_POSEDETECTOR_GAP = 0.95f;
    private boolean mlObjectDetector = false;
    private boolean mlLabelerDetector = false;
    private boolean mlFaceDetector = true;
    private boolean mlPoseDetector = true;
    private boolean mlSegmentationDetector = false;
    private boolean mlTextDetector = true;
    private boolean mlSegmentationMultipleSubject = false;

    private boolean mlObjectCompleted = false;
    private boolean mlLabelerCompleted = false;
    private boolean mlFaceCompleted = false;
    private boolean mlPoseCompleted = false;
    private boolean mlSegmentationCompleted = false;
    private boolean mlTextCompleted = false;

    private ArrayList labelsToSaveForObjects;
    private ArrayList labelsToSaveForLabeler;

    private final IBinder binder = new CameraServiceBinder();


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

        labelsToSaveForObjects = new ArrayList(Arrays.asList(Costants.Camera.SAVE_BY_TAG_OBJECT));
        labelsToSaveForLabeler = new ArrayList(Arrays.asList(Costants.Camera.SAVE_BY_TAG_LABELER));

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

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BroadcastHelper.SERVICE_CAMERA);
        registerReceiver(broadcastReceiver, intentFilter);

        graphicOverlay = new GraphicOverlay();

        cameraManager = (CameraManager) CONTEXT.getSystemService(Context.CAMERA_SERVICE);

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
        preview = new Preview.Builder().build();

        int cameraType = CameraSelector.LENS_FACING_BACK;

        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(cameraType)
                .build();

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build();

        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .setResolutionSelector(new ResolutionSelector.Builder()
                        .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
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
        imageAnalysis.setAnalyzer(CONTEXT.getMainExecutor(), new ImageAnalysis.Analyzer() {
            @OptIn(markerClass = ExperimentalGetImage.class)
            @Override
            public void analyze(@NonNull ImageProxy proxy) {
                if (proxy.getImage() != null) {
                    //ATTENZIONE rotation E' SEMPRE 90
                    int rotation = proxy.getImageInfo().getRotationDegrees();
                    bitmap = proxy.toBitmap();
                    if (rotation > 0) {
                        Matrix matrix = new Matrix();
                        matrix.preRotate(rotation);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    }
                    rotationImageByOrientation();
                    graphicOverlay.setCanvas(bitmap);
                    imageProxy = proxy;
                    inputImage = InputImage.fromBitmap(bitmap, 0);
                    ML_Manager();
                }
            }
        });
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    @SuppressLint("NewApi")
    private void ML_Manager() {
        //tol.Print(TAG, "mlAnalizerCurrentStep:" + mlAnalizerCurrentStep, false, true);
        switch (mlAnalizerCurrentStep) {
            case Costants.IMAGE_REKOGNITION_DETECTION_ORDER_OBJECT:
                mlAnalizerCurrentStep = Costants.IMAGE_REKOGNITION_DETECTION_ORDER_LABELER;
                if (mlObjectDetector) {
                    ML_ObjectDetector();
                } else {
                    ML_Manager();
                }
                break;
            case Costants.IMAGE_REKOGNITION_DETECTION_ORDER_LABELER:
                mlAnalizerCurrentStep = Costants.IMAGE_REKOGNITION_DETECTION_ORDER_FACES;
                if (mlLabelerDetector) {
                    ML_LabelerDetector();
                } else {
                    ML_Manager();
                }
                break;
            case Costants.IMAGE_REKOGNITION_DETECTION_ORDER_FACES:
                mlAnalizerCurrentStep = Costants.IMAGE_REKOGNITION_DETECTION_ORDER_POSE;
                if (mlFaceDetector) {
                    ML_FaceDetector();
                } else {
                    ML_Manager();
                }
                break;
            case Costants.IMAGE_REKOGNITION_DETECTION_ORDER_POSE:
                mlAnalizerCurrentStep = Costants.IMAGE_REKOGNITION_DETECTION_ORDER_SEGMENTATION;
                if (mlPoseDetector) {
                    ML_PoseDetector();
                }else{
                    ML_Manager();
                }
                break;
            case Costants.IMAGE_REKOGNITION_DETECTION_ORDER_SEGMENTATION:
                mlAnalizerCurrentStep = Costants.IMAGE_REKOGNITION_DETECTION_ORDER_TEXT;
                if (mlSegmentationDetector) {
                    ML_SegmentationDetector();
                }else{
                    ML_Manager();
                }
                break;
            case Costants.IMAGE_REKOGNITION_DETECTION_ORDER_TEXT:
                mlAnalizerCurrentStep = Costants.IMAGE_REKOGNITION_DETECTION_ORDER_COMPLETE;
                if (mlTextDetector) {
                    ML_TextDetector();
                }else{
                    ML_Manager();
                }
                break;
            case Costants.IMAGE_REKOGNITION_DETECTION_ORDER_COMPLETE:
                mlAnalizerCurrentStep = Costants.IMAGE_REKOGNITION_DETECTION_ORDER_OBJECT;
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
    private void ML_ObjectDetector() {
        /*
        OBJECT DETECTOR
        */
        //tol.Print(TAG, "objectDetector:enabled", false, true);
        mlObjectCompleted = false;
        ObjectDetectorOptions objectDetectorOptions = new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build();
        ObjectDetection.getClient(objectDetectorOptions)
                .process(inputImage)
                .addOnSuccessListener(
                        detectedObjects -> {
                            //tol.Print(TAG, "objectDetector->onSuccess", false, true);
                            boolean imageOfInterest = false;
                            if (!detectedObjects.isEmpty()) {
                                List<BoxWithLabel> boxes = new ArrayList<>();
                                for (DetectedObject object : detectedObjects) {
                                    Integer trackingId = object.getTrackingId();
                                    StringBuilder labels = new StringBuilder(String.valueOf(trackingId));
                                    /*
                                    Rect boundingBox = object.getBoundingBox();
                                    tol.Print(TAG, "object:rect:" + boundingBox + "/trackingID:" + trackingId, false, true);
                                    */
                                    if (!object.getLabels().isEmpty()) {
                                        labels.append(": ");
                                        for (DetectedObject.Label label : object.getLabels()) {
                                            if(labelsToSaveForObjects.contains(label.getText())){
                                                imageOfInterest = true;
                                            }
                                            labels.append(label.getText());
                                        }
                                    }
                                    boxes.add(new BoxWithLabel(object.getBoundingBox(), labels.toString()));
                                }
                                if (!boxes.isEmpty()) {
                                    if (imageOfInterest || mainActivityInUse) {
                                        graphicOverlay.drawObjectsBoxes(boxes);
                                    }
                                }
                            }
                        })
                .addOnCompleteListener(
                        task -> {
                            mlObjectCompleted = true;
                            waitMLCompleted();
                        });
    }

    @SuppressLint("NewApi")
    private void ML_LabelerDetector() {
        /*
        IMAGE LABELER
        */
        //tol.Print(TAG, "labelerDetector:enabled", false, true);
        mlLabelerCompleted = false;
        ImageLabelerOptions imageLabelerOptions = new ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.7f)
                .build();
        ImageLabeling.getClient(imageLabelerOptions)
                .process(inputImage)
                .addOnSuccessListener(labels -> {
                    //tol.Print(TAG, "imageLabeler->onSuccess", false, true);
                    StringBuilder message = new StringBuilder();
                    if (!labels.isEmpty()) {
                        boolean imageOfInterest = false;
                        List<LabelsList> labelsList = new ArrayList<>();
                        for (ImageLabel imageLabel : labels) {
                            String label = imageLabel.getText();
                            labelsList.add(new LabelsList(label, imageLabel.getIndex()));
                        }

                        if (!labelsList.isEmpty()) {
                            if(imageOfInterest || mainActivityInUse){
                                graphicOverlay.drawLabelsList(labelsList);
                            }
                        }
                    }
                })
                .addOnCompleteListener(task -> {
                    mlLabelerCompleted = true;
                    waitMLCompleted();
                });
    }

    @SuppressLint("NewApi")
    private void ML_FaceDetector() {
        /*
        FACE DETECTOR
        */
        //tol.Print(TAG, "faceDetector:enabled", false, true);
        mlFaceCompleted = false;
        FaceDetectorOptions faceDetectorOptions = new FaceDetectorOptions.Builder()
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .enableTracking()
                .build();
        FaceDetection.getClient(faceDetectorOptions)
                .process(inputImage)
                .addOnSuccessListener(
                        faces -> {
                            //tol.Print(TAG, "faceDetector->onSuccess:" + faces.size(), false, true);
                            String message = "";
                            if (!faces.isEmpty()) {
                                boolean imageOfInterest = false;
                                /*
                                for (Face face : faces) {
                                    Rect bounds = face.getBoundingBox();
                                    float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                                    float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

                                    // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                                    // nose available):
                                    if (face.getLandmark(FaceLandmark.LEFT_EAR) != null) {
                                        FaceLandmark leftEar = face.getLandmark(FaceLandmark.LEFT_EAR);
                                        if (leftEar != null) {
                                            PointF leftEarPos = leftEar.getPosition();
                                        }
                                    }

                                    // If contour detection was enabled:
                                    if (face.getContour(FaceContour.LEFT_EYE) != null) {
                                        List<PointF> leftEyeContour =
                                                face.getContour(FaceContour.LEFT_EYE).getPoints();
                                    }
                                    if (face.getContour(FaceContour.UPPER_LIP_BOTTOM) != null) {
                                        List<PointF> upperLipBottomContour =
                                                face.getContour(FaceContour.UPPER_LIP_BOTTOM).getPoints();
                                    }

                                    // If classification was enabled:
                                    if (face.getSmilingProbability() != null) {
                                        float smileProb = face.getSmilingProbability();
                                    }
                                    if (face.getRightEyeOpenProbability() != null) {
                                        float rightEyeOpenProb = face.getRightEyeOpenProbability();
                                    }

                                    // If face tracking was enabled:
                                    if (face.getTrackingId() != null) {
                                        int id = face.getTrackingId();
                                    }
                                }
                                */
                                graphicOverlay.drawFacesBoxes(faces);
                            }
                        })
                .addOnCompleteListener(task -> {
                    mlFaceCompleted = true;
                    waitMLCompleted();
                });
    }

    @SuppressLint("NewApi")
    private void ML_PoseDetector() {
        /*
        POSE DETECTOR
        */
        //tol.Print(TAG, "poseDetector:enabled", false, true);
        mlPoseCompleted = false;
        PoseDetectorOptions poseDetectorOptions = new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build();
        PoseDetector poseDetector = PoseDetection.getClient(poseDetectorOptions);
        poseDetector.process(inputImage)
                .addOnSuccessListener(
                        pose -> {
                            String message = "";
                            //tol.Print(TAG, "poseDetector->onSuccess", false, true);
                            if (!pose.getAllPoseLandmarks().isEmpty()) {
                                PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
                                PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
                                PoseLandmark leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW);
                                PoseLandmark rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW);
                                PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
                                PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
                                PoseLandmark leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP);
                                PoseLandmark rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP);
                                PoseLandmark leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE);
                                PoseLandmark rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE);
                                PoseLandmark leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE);
                                PoseLandmark rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE);

                                if (LandmarkInFrame(leftShoulder)
                                        && LandmarkInFrame(rightShoulder)
                                        && LandmarkInFrame(leftHip)
                                        && LandmarkInFrame(rightHip)
                                        && LandmarkInFrame(leftKnee)
                                        && LandmarkInFrame(rightKnee)
                                        && LandmarkInFrame(leftElbow)
                                        && LandmarkInFrame(rightElbow)
                                        && LandmarkInFrame(leftWrist)
                                        && LandmarkInFrame(rightWrist)
                                        && LandmarkInFrame(leftAnkle)
                                        && LandmarkInFrame(rightAnkle)) {
                                    graphicOverlay.drawPoseDetected(pose);
                                }
                            }
                        })
                .addOnCompleteListener(task -> {
                    mlPoseCompleted = true;
                    waitMLCompleted();
                });
    }

    @SuppressLint("NewApi")
    private void ML_SegmentationDetector() {
        /*
        SEGMENTATION DETECTOR
        */
        //tol.Print(TAG, "segmentationDetector:enabled", false, true);
        mlSegmentationCompleted = false;
        SubjectSegmenterOptions options;
        if(mlSegmentationMultipleSubject) {
            options = new SubjectSegmenterOptions.Builder()
                    .enableForegroundBitmap()
                    .enableMultipleSubjects(new SubjectSegmenterOptions.SubjectResultOptions.Builder()
                            .enableSubjectBitmap()
                            .build())
                    .build();
        }else{
            options = new SubjectSegmenterOptions.Builder()
                    .enableForegroundBitmap()
                    .build();
        }
        SubjectSegmentation.getClient(options)
                .process(inputImage)
                .addOnSuccessListener(
                        result -> {
                            if(mlSegmentationMultipleSubject) {
                                graphicOverlay.empty();
                                List<Subject> subjects = result.getSubjects();
                                //tol.Print(TAG, "subjects:"+subjects.size(), false, true);
                                for (Subject subject : subjects) {
                                    graphicOverlay.drawSubject(subject.getBitmap(), subject.getStartX(), subject.getStartY());
                                    //tol.Print(TAG, "-> "+subject.getWidth()+" x "+subject.getHeight()+" - "+subject.getStartX()+":"+subject.getStartY(), false, true);
                                    /*FloatBuffer floatBuffer = subject.getConfidenceMask();
                                    assert floatBuffer != null;
                                    int height = bitmap.getHeight();
                                    int width = bitmap.getWidth();
                                    for (int y = 0; y < height; y++) {
                                        for (int x = 0; x < width; x++) {
                                            if (floatBuffer.get() > Costants.Camera.SUBJECT_CONFIDENCE_GAP) {
                                                points.add(new mPoint(x, y));
                                            }
                                        }
                                    }*/
                                }
                            }else{
                                List<mPoint> points = new ArrayList<>();
                                FloatBuffer floatBuffer = result.getForegroundConfidenceMask();
                                assert floatBuffer != null;
                                int height = bitmap.getHeight();
                                int width = bitmap.getWidth();
                                for (int y = 0; y < height; y++) {
                                    for (int x = 0; x < width; x++) {
                                        if (floatBuffer.get() > Costants.Camera.SUBJECT_CONFIDENCE_GAP) {
                                            points.add(new mPoint(x, y));
                                        }
                                    }
                                }
                                if(!points.isEmpty()) {
                                    graphicOverlay.drawSubject(points);
                                }
                            }
                        })
                .addOnCompleteListener(
                        task -> {
                            mlSegmentationCompleted = true;
                            waitMLCompleted();
                        });
    }

    @SuppressLint("NewApi")
    private void ML_TextDetector() {
        /*
        SEGMENTATION DETECTOR
        */
        mlTextCompleted = false;
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(inputImage)
                .addOnSuccessListener(
                        result -> {
                            String resultText = result.getText();
                            if(!result.getTextBlocks().isEmpty() || !resultText.isEmpty()){
                                /*
                                for (Text.TextBlock block : result.getTextBlocks()) {
                                    String blockText = block.getText();
                                    Point[] blockCornerPoints = block.getCornerPoints();
                                    Rect blockFrame = block.getBoundingBox();
                                    for (Text.Line line : block.getLines()) {
                                        String lineText = line.getText();
                                        Point[] lineCornerPoints = line.getCornerPoints();
                                        Rect lineFrame = line.getBoundingBox();
                                        for (Text.Element element : line.getElements()) {
                                            String elementText = element.getText();
                                            Point[] elementCornerPoints = element.getCornerPoints();
                                            Rect elementFrame = element.getBoundingBox();
                                            for (Text.Symbol symbol : element.getSymbols()) {
                                                String symbolText = symbol.getText();
                                                Point[] symbolCornerPoints = symbol.getCornerPoints();
                                                Rect symbolFrame = symbol.getBoundingBox();
                                            }
                                        }
                                    }
                                }
                                */
                                String targa = resultText.replace(Costants.CHAR_SPACE, Costants.CHAR_NULL).trim();
                                if(targa.length() == Costants.Camera.TEXT_TARGA_LENGTH) {
                                    graphicOverlay.drawLabel(targa, 1);
                                }
                            }
                        })
                .addOnCompleteListener(
                        task -> {
                            mlTextCompleted = true;
                            waitMLCompleted();
                        });
    }

    private boolean LandmarkInFrame(PoseLandmark typePose) {
        boolean inFrameLikelihood = false;
        if (typePose.getInFrameLikelihood() > ML_POSEDETECTOR_GAP) {
            inFrameLikelihood = true;
        }
        //tol.Print(TAG, "poseDetector->LandmarkInFrame->"+typePose.getLandmarkType() +":" + probability, false, true);
        return inFrameLikelihood;
    }


    @SuppressLint("NewApi")
    private void waitMLCompleted() {
        if (mlFaceCompleted
                && mlLabelerCompleted
                && mlObjectCompleted
                && mlPoseCompleted
                && mlSegmentationCompleted
                && mlTextCompleted) {
                if (mlObjectDetector) {
                    mlObjectCompleted = false;
                }
                if (mlLabelerDetector) {
                    mlLabelerCompleted = false;
                }
                if (mlFaceDetector) {
                    mlFaceCompleted = false;
                }
                if (mlPoseDetector) {
                    mlPoseCompleted = false;
                }
                if (mlSegmentationDetector) {
                    mlSegmentationCompleted = false;
                }
                if (mlTextDetector) {
                    mlTextCompleted = false;
                }
                OperationsOnImage();
        }
        ML_Manager();
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
            //tol.Print(TAG, message, false, true);
        }
        if(!mainActivityInUse) {
            unbindCamera();
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