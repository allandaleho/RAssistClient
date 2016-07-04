package com.ramco.rassistclient.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.RunnableFuture;

/**
 * Created by 12041 on 5/23/2016.
 */
public class DrawingView extends View {
    //drawing path
    private Path drawPath;
    //drawing and canvas paint
    private Paint drawPaint, canvasPaint;
    //initial color
    private int paintColor = 0xBDB76B00;
    //canvas
    private Canvas drawCanvas;
    //canvas bitmap
    private Bitmap canvasBitmap;

    private List<String> coord = new ArrayList<String>();

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            checkTimer();
        }
    };
    private boolean drawTriggered = false;
    private boolean isTriggeredAgain = false;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();
    }

    public void startNew(){
        drawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        invalidate();
    }

    private void setupDrawing() {
        //get drawing area setup for interaction
        drawPath = new Path();
        drawPaint = new Paint();
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(20);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        canvasPaint = new Paint(Paint.DITHER_FLAG);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        //view given size
        super.onSizeChanged(w, h, oldw, oldh);

        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //draw view

        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        canvas.drawPath(drawPath, drawPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //detect user touch
        /*float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                drawPath.moveTo(touchX, touchY);
                coord.add("{x:" + touchX + ",y:" + touchY + "}");
                break;
            case MotionEvent.ACTION_MOVE:
                drawPath.lineTo(touchX, touchY);
                coord.add("{x:" + touchX + ",y:" + touchY + "}");
                break;
            case MotionEvent.ACTION_UP:
                drawCanvas.drawPath(drawPath, drawPaint);
                drawPath.reset();
                break;
            default:
                return false;
        }

        invalidate();*/
        return true;
    }

    public void startDraw(String coord) {
        float touchX;
        float touchY;
        StringTokenizer st = new StringTokenizer(coord, ";");

        startNew();

        int cnt = 0;
        while (st.hasMoreElements()) {
            String coordSet = st.nextToken();
            StringTokenizer st2 = new StringTokenizer(coordSet, ",");
            String strX = st2.nextToken();
            String strY = st2.nextToken();
            touchX = Float.parseFloat(strX.substring(strX.indexOf(":") + 1));
            touchY = Float.parseFloat(strY.substring(strY.indexOf(":") + 1));
            Log.d("TEST", touchX + "/" + touchY);
            if (cnt == 0) {
                cnt++;
                drawPath.moveTo(touchX, touchY);
                invalidate();
            } else {
                drawPath.lineTo(touchX, touchY);
                invalidate();
            }
        }

        //drawCanvas.drawPath(drawPath, drawPaint);
        //drawPath.reset();
        //invalidate();

        if (!drawTriggered) {
            drawTriggered = true;
            handler.postDelayed(this.runnable, 300);
        } else {
            isTriggeredAgain = true;
        }
    }

    private void checkTimer() {
        if (isTriggeredAgain) {
            isTriggeredAgain = false;
            handler.postDelayed(this.runnable, 300);
        } else {
            drawTriggered = false;
            startNew();
        }
    }

}
