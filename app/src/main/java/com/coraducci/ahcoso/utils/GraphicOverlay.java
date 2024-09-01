package com.coraducci.ahcoso.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.List;

public class GraphicOverlay {

    private Canvas canvas;

    private Paint objectBox,
            objectLabel,
            subjectPoint,
            background;

    private Paint posePoint,
            poseLeft,
            poseRight,
            poseUnion;

    private PoseLandmark leftShoulder,
            rightShoulder,
            leftElbow,
            rightElbow,
            leftWrist,
            rightWrist,
            leftHip,
            rightHip,
            leftKnee,
            rightKnee,
            leftAnkle,
            rightAnkle,
            leftPinky,
            rightPinky,
            leftIndex,
            rightIndex,
            leftThumb,
            rightThumb,
            leftHeel,
            rightHeel,
            leftFootIndex,
            rightFootIndex;

    Paint penLabel;

    public GraphicOverlay() {
        objectBox = new Paint();
        objectBox.setColor(Color.RED);
        objectBox.setStyle(Paint.Style.FILL_AND_STROKE);
        objectBox.setAlpha(20);
        objectBox.setStrokeWidth(4f);

        objectLabel = new Paint();
        objectLabel.setColor(Color.YELLOW);
        objectLabel.setStyle(Paint.Style.FILL_AND_STROKE);
        objectLabel.setTextSize(36f);

        posePoint = new Paint();
        posePoint.setColor(Color.BLUE);
        posePoint.setStrokeWidth(3f);
        poseLeft = new Paint();
        poseLeft.setStrokeWidth(3f);
        poseLeft.setColor(Color.GREEN);
        poseRight = new Paint();
        poseRight.setStrokeWidth(3f);
        poseRight.setColor(Color.YELLOW);
        poseUnion = new Paint();
        poseUnion.setStrokeWidth(3f);
        poseUnion.setColor(Color.BLUE);

        subjectPoint = new Paint();
        subjectPoint.setAlpha(20);
        subjectPoint.setColor(Color.RED);

        background = new Paint();
        background.setColor(Color.TRANSPARENT);
    }

    public void setCanvas(Bitmap bitmap) {
        canvas = new Canvas(bitmap);
    }
    public void inPause(){
        canvas.drawText("PAUSE", 10, 10, objectLabel);
    }

    public void drawObjectsBoxes(List<BoxWithLabel> boxes){

        for(BoxWithLabel boxWithLabel : boxes){
            canvas.drawRect(boxWithLabel.rect, objectBox);

            Rect labelSize = new Rect(0, 0,0,0);
            objectLabel.getTextBounds(boxWithLabel.label, 0, boxWithLabel.label.length(), labelSize);

            float fontSize = objectLabel.getTextSize() * boxWithLabel.rect.width() / labelSize.width();
            if(fontSize < objectLabel.getTextSize()){
                objectLabel.setTextSize(objectLabel.getTextSize());
            }

            canvas.drawText(boxWithLabel.label, boxWithLabel.rect.left + 10, boxWithLabel.rect.top + labelSize.height() + 10, objectLabel);
        }
    }
    public void drawLabel(String text, int index){
        canvas.drawText(text, 20, 40 * index, objectLabel);
    }
    public void drawLabelsList(List<LabelsList> labels){
        int index = 1;
        for(LabelsList labelsList : labels){
            drawLabel(labelsList.label, index);
            index++;
        }
    }
    public void drawFacesBoxes(List<Face> faces){
        Paint penRect = new Paint();
        penRect.setColor(Color.RED);
        penRect.setStyle(Paint.Style.STROKE);
        penRect.setStrokeWidth(2f);

        Paint penPoint = new Paint();
        penPoint.setColor(Color.BLUE);
        penPoint.setStrokeWidth(3f);

        for(Face face : faces){
            canvas.drawRect(face.getBoundingBox(), penRect);

            for(FaceLandmark landmark: face.getAllLandmarks()) {
                canvas.drawPoint(landmark.getPosition().x, landmark.getPosition().y, penPoint);
            }
        }
    }
    public void drawPoseDetected(Pose pose){
        for(PoseLandmark landmark : pose.getAllPoseLandmarks()){
            canvas.drawPoint(landmark.getPosition().x, landmark.getPosition().y, posePoint);
        }

        leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
        rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
        leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW);
        rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW);
        leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
        rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
        leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP);
        rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP);
        leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE);
        rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE);
        leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE);
        rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE);
        leftPinky = pose.getPoseLandmark(PoseLandmark.LEFT_PINKY);
        rightPinky = pose.getPoseLandmark(PoseLandmark.RIGHT_PINKY);
        leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX);
        rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX);
        leftThumb = pose.getPoseLandmark(PoseLandmark.LEFT_THUMB);
        rightThumb = pose.getPoseLandmark(PoseLandmark.RIGHT_THUMB);
        leftHeel = pose.getPoseLandmark(PoseLandmark.LEFT_HEEL);
        rightHeel = pose.getPoseLandmark(PoseLandmark.RIGHT_HEEL);
        leftFootIndex = pose.getPoseLandmark(PoseLandmark.LEFT_FOOT_INDEX);
        rightFootIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_FOOT_INDEX);
        /*
        PoseLandmark nose = pose.getPoseLandmark(PoseLandmark.NOSE);
        PoseLandmark leftEyeInner = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_INNER);
        PoseLandmark leftEye = pose.getPoseLandmark(PoseLandmark.LEFT_EYE);
        PoseLandmark leftEyeOuter = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_OUTER);
        PoseLandmark rightEyeInner = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_INNER);
        PoseLandmark rightEye = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE);
        PoseLandmark rightEyeOuter = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_OUTER);
        PoseLandmark leftEar = pose.getPoseLandmark(PoseLandmark.LEFT_EAR);
        PoseLandmark rightEar = pose.getPoseLandmark(PoseLandmark.RIGHT_EAR);
        PoseLandmark leftMouth = pose.getPoseLandmark(PoseLandmark.LEFT_MOUTH);
        PoseLandmark rightMouth = pose.getPoseLandmark(PoseLandmark.RIGHT_MOUTH);
        */

        //Left body
        canvas.drawLine(invertImage(leftShoulder.getPosition().x), leftShoulder.getPosition().y,invertImage(leftElbow.getPosition().x),leftElbow.getPosition().y, poseLeft);
        canvas.drawLine(invertImage(leftElbow.getPosition().x),leftElbow.getPosition().y,invertImage(leftWrist.getPosition().x),leftWrist.getPosition().y, poseLeft);
        canvas.drawLine(invertImage(leftShoulder.getPosition().x), leftShoulder.getPosition().y,invertImage(leftHip.getPosition().x),leftHip.getPosition().y, poseLeft);
        canvas.drawLine(invertImage(leftHip.getPosition().x),leftHip.getPosition().y,invertImage(leftKnee.getPosition().x),leftKnee.getPosition().y, poseLeft);
        canvas.drawLine(invertImage(leftKnee.getPosition().x),leftKnee.getPosition().y,invertImage(leftAnkle.getPosition().x),leftAnkle.getPosition().y, poseLeft);
        canvas.drawLine(invertImage(leftWrist.getPosition().x),leftWrist.getPosition().y,invertImage(leftThumb.getPosition().x),leftThumb.getPosition().y, poseLeft);
        canvas.drawLine(invertImage(leftWrist.getPosition().x),leftWrist.getPosition().y,invertImage(leftPinky.getPosition().x),leftPinky.getPosition().y, poseLeft);
        canvas.drawLine(invertImage(leftWrist.getPosition().x),leftWrist.getPosition().y,invertImage(leftIndex.getPosition().x),leftIndex.getPosition().y, poseLeft);
        canvas.drawLine(invertImage(leftIndex.getPosition().x),leftIndex.getPosition().y,invertImage(leftPinky.getPosition().x),leftPinky.getPosition().y, poseLeft);
        canvas.drawLine(invertImage(leftAnkle.getPosition().x),leftAnkle.getPosition().y,invertImage(leftHeel.getPosition().x),leftHeel.getPosition().y, poseLeft);
        canvas.drawLine(invertImage(leftHeel.getPosition().x),leftHeel.getPosition().y,invertImage(leftFootIndex.getPosition().x),leftFootIndex.getPosition().y, poseLeft);

        //Right body
        canvas.drawLine(invertImage(rightShoulder.getPosition().x),rightShoulder.getPosition().y,invertImage(rightElbow.getPosition().x),rightElbow.getPosition().y, poseRight);
        canvas.drawLine(invertImage(rightElbow.getPosition().x),rightElbow.getPosition().y,invertImage(rightWrist.getPosition().x),rightWrist.getPosition().y, poseRight);
        canvas.drawLine(invertImage(rightShoulder.getPosition().x),rightShoulder.getPosition().y,invertImage(rightHip.getPosition().x),rightHip.getPosition().y, poseRight);
        canvas.drawLine(invertImage(rightHip.getPosition().x),rightHip.getPosition().y,invertImage(rightKnee.getPosition().x),rightKnee.getPosition().y, poseRight);
        canvas.drawLine(invertImage(rightKnee.getPosition().x),rightKnee.getPosition().y,invertImage(rightAnkle.getPosition().x),rightAnkle.getPosition().y, poseRight);
        canvas.drawLine(invertImage(rightWrist.getPosition().x),rightWrist.getPosition().y,invertImage(rightThumb.getPosition().x),rightThumb.getPosition().y, poseRight);
        canvas.drawLine(invertImage(rightWrist.getPosition().x),rightWrist.getPosition().y,invertImage(rightPinky.getPosition().x),rightPinky.getPosition().y, poseRight);
        canvas.drawLine(invertImage(rightWrist.getPosition().x),rightWrist.getPosition().y,invertImage(rightIndex.getPosition().x),rightIndex.getPosition().y, poseRight);
        canvas.drawLine(invertImage(rightIndex.getPosition().x),rightIndex.getPosition().y,invertImage(rightPinky.getPosition().x),rightPinky.getPosition().y, poseRight);
        canvas.drawLine(invertImage(rightAnkle.getPosition().x),rightAnkle.getPosition().y,invertImage(rightHeel.getPosition().x),rightHeel.getPosition().y, poseRight);
        canvas.drawLine(invertImage(rightHeel.getPosition().x),rightHeel.getPosition().y,invertImage(rightFootIndex.getPosition().x),rightFootIndex.getPosition().y, poseRight);

        //Union Left-Right
        canvas.drawLine(invertImage(leftShoulder.getPosition().x), leftShoulder.getPosition().y,invertImage(rightShoulder.getPosition().x),rightShoulder.getPosition().y, poseUnion);
        canvas.drawLine(invertImage(leftHip.getPosition().x),leftHip.getPosition().y,invertImage(rightHip.getPosition().x),rightHip.getPosition().y, poseUnion);
    }
    public void drawSubject(List<mPoint> points){
        for(mPoint point: points) {
            canvas.drawPoint(point.getX(), point.getY(), subjectPoint);
        }
    }
    public void drawSubject(Bitmap bitmap, int startX, int startY){
        canvas.drawBitmap(bitmap, startX, startY, subjectPoint);
    }
    public void empty(){
        canvas.drawColor(Color.BLACK);
    }
    private Float invertImage(Float x){
        return x;
    }
}
