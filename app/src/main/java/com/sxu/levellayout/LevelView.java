package com.sxu.levellayout;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/*******************************************************************************
 * FileName: LevelView
 *
 * Description: Level Choose View
 *
 * Author: Freeman
 *
 * Version: v1.0
 *
 * Date: 16/7/10
 *******************************************************************************/
public class LevelView extends View {

    private int stepCount;
    private int defaultPos;
    private int scrollDuration;
    private Drawable thumb;
    private float thumbWidth;
    private float thumbHeight;
    private float lineWidth;
    private int lineColor;
    private float pointRadius;
    private int pointColor;

    private float stepLength;
    private float startPos;
    private float startX;
    private float offset = 0;
    private int oldPosition = 0;
    private OnLevelChangeListener listener;

    /**
     * the default range of clicked
     */
    private final int DEFAULT_CLICK_RANGE = 45;

    public LevelView(Context context) {
        super(context);
    }

    public LevelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.LevelLayout);
        thumb = array.getDrawable(R.styleable.LevelLayout_thumb);
        thumbWidth = array.getDimension(R.styleable.LevelLayout_thumbWidth, 80);
        thumbHeight = array.getDimension(R.styleable.LevelLayout_thumbHeight, 80);
        stepCount = array.getInteger(R.styleable.LevelLayout_stepCount, 3);
        defaultPos = array.getInteger(R.styleable.LevelLayout_defaultPos, 0);
        scrollDuration = array.getInteger(R.styleable.LevelLayout_scrollDuration, 400);
        lineWidth = array.getDimension(R.styleable.LevelLayout_lineWidth, 9);
        lineColor = array.getColor(R.styleable.LevelLayout_lineColor, Color.parseColor("#eeeeee"));
        pointRadius = array.getDimension(R.styleable.LevelLayout_pointRadius, 15);
        pointColor = array.getColor(R.styleable.LevelLayout_pointColor, Color.parseColor("#eeeeee"));

        array.recycle();
    }

    public LevelView(Context context, AttributeSet attrs, int theme) {
        super(context, attrs, theme);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int height = getHeight() - getPaddingTop() - getPaddingBottom();
        int startX = getPaddingLeft();
        // 设置线条的画笔
        Paint linePaint = new Paint();
        linePaint.setStrokeWidth(lineWidth);
        linePaint.setColor(lineColor);
        linePaint.setAntiAlias(true);
        // 设置点的画笔
        Paint pointPaint = new Paint();
        pointPaint.setStrokeWidth(pointRadius);
        pointPaint.setColor(pointColor);
        pointPaint.setAntiAlias(true);
        // 获取每段横线的长度
        int centerHeight = height / 2;
        stepLength = (getWidth() - getPaddingLeft() - getPaddingRight() - thumbWidth) * 1.0f / stepCount;
        if (thumb != null) {
            final Bitmap bitmap = Bitmap.createScaledBitmap(drawableToBitmap(thumb), (int) thumbWidth, (int) thumbHeight, true);
            float bitmapX = startX += bitmap.getWidth() / 2;
            canvas.drawLine(startX, centerHeight, startX + stepLength*stepCount, centerHeight, linePaint);
            for (int i = 0; i < stepCount; i++) {
                if (defaultPos == i) {
                    bitmapX = startX - bitmap.getWidth() / 2 + offset;
                }
                canvas.drawCircle(startX, centerHeight, pointRadius, pointPaint);
                startX += stepLength;

            }
            canvas.drawCircle(startX, centerHeight, pointRadius, pointPaint);
            if (defaultPos == stepCount) {
                bitmapX = startX - bitmap.getWidth() / 2 + offset;
            }
            canvas.drawBitmap(bitmap, bitmapX, (getHeight() - thumbHeight) / 2, pointPaint);
        }
    }

    /**
     * Convert drawable to bitmap.
     * @param drawable
     * @return
     */
    public Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) {
            return null;
        }

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        try {
            Bitmap bitmap;
            if (drawable instanceof ColorDrawable) {
                bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.RGB_565);
            } else {
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.RGB_565);
            }
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                oldPosition = defaultPos;
                startPos = event.getX();
                startX = event.getX();
                defaultPos = (int)((startPos - getPaddingLeft()) / stepLength);
                listener.onStartTrackingTouch();
                break;
            case MotionEvent.ACTION_MOVE:
                // avoid thumb scrolls to extra area
                if (event.getX() > 0 && event.getX() < getWidth()-thumbWidth/2) {
                    offset += (event.getX() - startX);
                    startX = event.getX();
                    Log.i("out", "X===" + event.getX() + " offset==" + offset);
                    if (Math.abs(offset) > DEFAULT_CLICK_RANGE) {
                        listener.onLevelChanged((int) (event.getX() - thumbWidth / 2 - getPaddingLeft()));
                        invalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                // get latest position of thumb
                defaultPos = (int) ((event.getX() - getPaddingLeft()) / stepLength);
                // avoid thumb scrolls to extra area
                if (event.getX() > 0 && event.getX() < getWidth()) {
                    // get the distance of up and down
                    offset = event.getX() - startPos;
                    if (Math.abs(offset) < DEFAULT_CLICK_RANGE) { // thumb's click event
                        offset = (event.getX() - getPaddingLeft()) % stepLength;
                        if (offset > stepLength / 2) {
                            defaultPos++;
                        }
                        final float distance = (oldPosition - defaultPos) * stepLength;
                        if (distance != 0) {
                            ValueAnimator animator = ValueAnimator.ofFloat(distance, 0).setDuration(scrollDuration);
                            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    offset = (float) animation.getAnimatedValue();
                                    invalidate();
                                }
                            });
                            animator.start();
                        } else {
                            listener.onLevelClick();
                        }
                    } else if (offset > DEFAULT_CLICK_RANGE) { // scroll from left to right
                        offset = offset % stepLength;
                        if (offset > stepLength / 2 && defaultPos < stepCount) {
                            defaultPos++;
                        }
                    } else { // scroll from right to left
                        offset = offset % stepLength;
                        if (Math.abs(offset) < stepLength / 2 && defaultPos < stepCount) {
                            defaultPos++;
                        }
                    }
                    listener.onStopTrackingTouch();
                }
                offset = 0;
                invalidate();
                break;
            default:
                break;
        }

        return true;
    }

    public float getStepLength() {
        return stepLength;
    }

    public void setStepLength(float stepLength) {
        this.stepLength = stepLength;
    }

    public void setThumb(Drawable thumb) {
        if (thumb == null) {
            throw new RuntimeException("thumb can't be null");
        }

        this.thumb = thumb;
        invalidate();
    }

    public void setStepCount(int stepCount) {
        this.stepCount = stepCount;
    }

    public int getStepCount() {
        return stepCount;
    }

    public void setDefaultPos(int defaultPos) {
        if (defaultPos > stepCount) {
            throw new RuntimeException("defaultPos out of stepCount's range");
        }
        this.defaultPos = defaultPos;
    }

    /**
     * Get the default postion of thumb
     * @return
     */
    public int getDefaultPos() {
        return defaultPos;
    }

    public int getScrollDuration() {
        return scrollDuration;
    }

    public void setScrollDuration(int scrollDuration) {
        this.scrollDuration = scrollDuration;
    }

    public Drawable getThumb() {
        return thumb;
    }

    public float getThumbWidth() {
        return thumbWidth;
    }

    public void setThumbWidth(float thumbWidth) {
        this.thumbWidth = thumbWidth;
    }

    public float getThumbHeight() {
        return thumbHeight;
    }

    public void setThumbHeight(float thumbHeight) {
        this.thumbHeight = thumbHeight;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    public int getLineColor() {
        return lineColor;
    }

    public void setLineColor(int lineColor) {
        this.lineColor = lineColor;
    }

    public float getPointRadius() {
        return pointRadius;
    }

    public void setPointRadius(float pointRadius) {
        this.pointRadius = pointRadius;
    }

    public int getPointColor() {
        return pointColor;
    }

    public void setPointColor(int pointColor) {
        this.pointColor = pointColor;
    }

    /**
     * Listen the scrolling of thumb
     * @param listener
     */
    public void setOnLevelChangeListener(OnLevelChangeListener listener) {
        this.listener = listener;
    }

    public interface OnLevelChangeListener {
        /**
         * It is called when thumb scrolling.
         * @param progress means current position
         */
        void onLevelChanged(int progress);

        /**
         * It is called when thumb is pressed.
         */
        void onStartTrackingTouch();

        /**
         * It is called when thumb is up.
         */
        void onStopTrackingTouch();

        /**
         * It is called when thumb is clicked.
         */
        void onLevelClick();
    }
}