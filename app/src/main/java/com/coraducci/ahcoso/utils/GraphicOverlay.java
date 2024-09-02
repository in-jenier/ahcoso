package com.coraducci.ahcoso.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.List;

public class GraphicOverlay {

    private Canvas canvas;

    public GraphicOverlay() {

    }

    public void setCanvas(Bitmap bitmap) {
        canvas = new Canvas(bitmap);
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
    public void drawBoxes(List<Rect> rects){
        Paint penRect = new Paint();
        penRect.setColor(Color.RED);
        penRect.setStyle(Paint.Style.FILL);

        for(Rect rect : rects){
            canvas.drawRect(rect, penRect);
        }
    }
    public void drawPolygonWithBlur(List<PoseLandmark> points) {
        // line at minimum...
        if (points.size() < 2) {
            return;
        }

        // paint
        Paint polyPaint = new Paint();
        polyPaint.setColor(Color.BLUE);
        polyPaint.setStyle(Paint.Style.FILL);

        // path
        Path polyPath = new Path();
        polyPath.moveTo(points.get(0).getPosition().x, points.get(0).getPosition().y);
        for (int i = 1; i < points.size(); i++) {
            polyPath.lineTo(points.get(i).getPosition().x, points.get(i).getPosition().y);
        }
        polyPath.lineTo(points.get(0).getPosition().x, points.get(0).getPosition().y);

        // draw
        canvas.drawPath(polyPath, polyPaint);
    }
    public void empty(){
        canvas.drawColor(Color.BLACK);
    }
    private Float invertImage(Float x){
        return x;
    }
}
