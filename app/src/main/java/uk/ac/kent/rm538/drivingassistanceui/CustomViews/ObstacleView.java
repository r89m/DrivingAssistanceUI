package uk.ac.kent.rm538.drivingassistanceui.CustomViews;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Created by Richard on 24/01/2016.
 */
public class ObstacleView extends View {

    private List<DrawnObstacle> obstacles;
//    private Bitmap bufferBitmap;
    private int width;
    private int height;

    public ObstacleView(Context context) {
        this(context, null);
    }

    public ObstacleView(Context context, AttributeSet attrs){

        this(context, attrs, 0);
    }

    public ObstacleView(Context context, AttributeSet attrs, int defaultStyle){

        super(context, attrs, defaultStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {

//        if(bufferBitmap != null) {
//            canvas.drawBitmap(bufferBitmap, 0f, 0f, null);
//        }
        if(obstacles != null) {
            for (DrawnObstacle o : obstacles) {
                o.draw(canvas, width, height);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int wSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int hSpecSize = MeasureSpec.getSize(heightMeasureSpec);

        Timber.i("onMeasure - Width: %05d; Height: %05d", wSpecSize, hSpecSize);

        width = wSpecSize;
        height = hSpecSize;

//        bufferBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
//        Canvas c = new Canvas(bufferBitmap);
//        c.drawColor(Color.RED);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        this.width = w;
        this.height = h;

        super.onSizeChanged(w, h, oldw, oldh);
    }

    public void addObstacles(int startAngle, int endAngle, int distance, int xOffset, int yOffset, int colour){

        if(obstacles == null){
            obstacles = new ArrayList<>();
        }

        obstacles.add(new DrawnObstacle(startAngle, endAngle, distance, xOffset, yOffset, colour));

        //redrawBuffer();
        //invalidate();
    }

    public void drawObstacles(){

        invalidate();
    }

    public void redrawBuffer(){
//
//        if(bufferBitmap != null){
//            Canvas bufferCanvas = new Canvas(bufferBitmap);
//
//            if(obstacles != null) {
//                for (DrawnObstacle obstacle : obstacles) {
//                    obstacle.draw(bufferCanvas, width, height);
//                }
//            }
//        }
    }

    public void clearObstacles(){

        if(obstacles != null) {
            obstacles.clear();
            //redrawBuffer();
            invalidate();
        }
    }

    private class DrawnObstacle{

        private int distance;
        private int arcStartAngle;
        private int arcEndAngle;
        private int xOffset;
        private int yOffset;
        private int colour;

        public DrawnObstacle(int arcStartAngle, int arcEndAngle, int distance, int xOffset, int yOffset, int colour){

            this.distance = distance;
            this.arcStartAngle = Math.min(arcStartAngle, arcEndAngle);
            this.arcEndAngle = Math.max(arcStartAngle, arcEndAngle);
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.colour = colour;
        }

        public void draw(Canvas canvas, int width, int height){

            float center = width / 2;
            float dimensions = distance;
            float halfDimensions = dimensions / 2;
            float leftOffset = center - halfDimensions;
            float topOffset = 150 - halfDimensions;
            int arcSweep = arcEndAngle - arcStartAngle;

            RectF enclosingRect = new RectF(0, 0, dimensions, dimensions);

            if(arcStartAngle >= -22 && arcStartAngle <= 180){
                //yOffset = -(int)halfDimensions + 150;
            }

            enclosingRect.offset(leftOffset, topOffset);
            enclosingRect.offset(xOffset, yOffset);

            canvas.drawArc(enclosingRect, arcStartAngle, arcSweep, false, getPaintWithColour(colour));
        }

        private Paint getPaintWithColour(int colour){

            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setDither(true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(30);
            paint.setColor(colour);
            return paint;
        }
    }
}
