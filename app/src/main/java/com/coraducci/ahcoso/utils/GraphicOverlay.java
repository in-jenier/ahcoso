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

    private static Canvas canvas = null;
    private static Bitmap bitmap = null;

    public GraphicOverlay() {

    }

    public void setCanvas(Bitmap bmp) {
        canvas = new Canvas(bmp);
    }
    public void setCanvas(int width, int height) {
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
    }
    public Bitmap getCanvas() {
        return bitmap;
    }
    public void drawFacesBoxes(List<Face> faces){
        Paint penRect = new Paint();
        penRect.setColor(Color.RED);
        penRect.setStyle(Paint.Style.FILL);
        penRect.setStrokeWidth(2f);

        for(Face face : faces){
            canvas.drawRect(face.getBoundingBox(), penRect);
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
    public void empty(){
        canvas.drawColor(Color.TRANSPARENT);
    }
    private Float invertImage(Float x){
        return x;
    }
}
