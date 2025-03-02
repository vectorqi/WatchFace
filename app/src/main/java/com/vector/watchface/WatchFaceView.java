package com.vector.watchface;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.Calendar;

public class WatchFaceView extends View {
    // Original bitmap
    private Bitmap backgroundBlack, backgroundBlue;
    private Bitmap hourHand, minuteHand;

    // After scale
    private Bitmap scaledBlack, scaledBlue;
    private Bitmap scaledHourHand, scaledMinuteHand;

    private Paint progressPaint;
    private float currentScale = 1f;
    // Flag for inverting the color：false -> black background with blue ring, true -> blue background with black ring
    private boolean invertColors = false;

    // For rerefreshing the view.
    private final Handler handler = new Handler(Looper.getMainLooper());

    // For drawing the second ring.
    private RectF progressRect;

    public WatchFaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSaveEnabled(true);
        init();
    }

    /**
     * Initialize the view.
     */
    private void init() {
        // 1) Load the original background and watch hands.
        backgroundBlack = BitmapFactory.decodeResource(getResources(), R.drawable.background);
        hourHand = BitmapFactory.decodeResource(getResources(), R.drawable.hour_hand);
        minuteHand = BitmapFactory.decodeResource(getResources(), R.drawable.minute_hand);

        // 2) create a blue background from the black background.
        backgroundBlue = createBlueBackgroundFromBlack(backgroundBlack);

        // 3) Create the paint for the second ring.
        progressPaint = new Paint();
        progressPaint.setAntiAlias(true);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(dpToPx(getContext(), 12));
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        // Refresh every second, switch the color when reached 0 second.
        startClock();
    }

    /**
     * Start the clock and update the view.
     */
    private void startClock() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                int second = calendar.get(Calendar.SECOND);

                // Switch colors every minute
                if (second == 0) {
                    invertColors = !invertColors;
                }

                invalidate();
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 1) Let the system measure the width and height of the view
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // 2) Get the width and height of the view
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // 1) Calculate and get the scale for the background
        float scale = Math.min(
                (float) w / backgroundBlack.getWidth(),
                (float) h / backgroundBlack.getHeight()
        );
        currentScale = scale;

        // Recycle the old bitmaps
        recycleBitmap(scaledBlack);
        recycleBitmap(scaledBlue);
        recycleBitmap(scaledHourHand);
        recycleBitmap(scaledMinuteHand);

        // 2) Use the same scale to scale the background
        scaledBlack = scaleBitmap(backgroundBlack, scale);
        scaledBlue = scaleBitmap(backgroundBlue, scale);

        // 3) Use the same scale to scale the watch face pointers
        scaledHourHand = scaleBitmap(hourHand, scale);
        scaledMinuteHand = scaleBitmap(minuteHand, scale);

        // 4) Calculate the progress rect for the second hand
        int dialRadius = scaledBlack.getWidth() / 2;
        int progressOffset = dpToPx(getContext(), 30);

        progressRect = new RectF(
                w / 2f - dialRadius + progressOffset,
                h / 2f - dialRadius + progressOffset,
                w / 2f + dialRadius - progressOffset,
                h / 2f + dialRadius - progressOffset
        );
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // 1) Draw background
        Bitmap currentBg = invertColors ? scaledBlue : scaledBlack;
        if (currentBg != null) {
            canvas.drawBitmap(
                    currentBg,
                    centerX - currentBg.getWidth() / 2f,
                    centerY - currentBg.getHeight() / 2f,
                    null
            );
        }

        // 2) Calulate the time go get the rotation angle
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);

        float hourAngle = (hour % 12 + minute / 60f) * 30f;
        float minuteAngle = (minute + second / 60f) * 6f;

        // 3) Draw the hourhand and the minutehand
        if (scaledHourHand != null && scaledMinuteHand != null) {
            // The distance from the bottom of the dial to the pivot is 7px
            float offsetInScaledPx = dpToPx(getContext(), 7)*currentScale;
            float hourPivotOffset = (scaledHourHand.getHeight() - offsetInScaledPx);
            float minutePivotOffset = (scaledMinuteHand.getHeight() - offsetInScaledPx);

            // Hour Hand
            canvas.save();
            canvas.rotate(hourAngle, centerX, centerY);
            canvas.drawBitmap(
                    scaledHourHand,
                    centerX - scaledHourHand.getWidth() / 2f,
                    centerY - hourPivotOffset,
                    null
            );
            canvas.restore();

            // Minute Hand
            canvas.save();
            canvas.rotate(minuteAngle, centerX, centerY);
            canvas.drawBitmap(
                    scaledMinuteHand,
                    centerX - scaledMinuteHand.getWidth() / 2f,
                    centerY - minutePivotOffset,
                    null
            );
            canvas.restore();
        }

        // 4) Draw the second hand
        progressPaint.setColor(
                invertColors
                        ? Color.BLACK
                        : ContextCompat.getColor(getContext(), R.color.progress_blue)
        );
        float sweepAngle = (second / 60f) * 360f;
        canvas.drawArc(progressRect, -90f, sweepAngle, false, progressPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        recycleBitmap(backgroundBlack);
        recycleBitmap(backgroundBlue);
        recycleBitmap(hourHand);
        recycleBitmap(minuteHand);
        recycleBitmap(scaledHourHand);
        recycleBitmap(scaledMinuteHand);
        recycleBitmap(scaledBlack);
        recycleBitmap(scaledBlue);
    }

    /**
     * Scale the the given Bitmap.
     */
    private Bitmap scaleBitmap(Bitmap original, float scale) {
        if (original == null) return null;
        if (scale == 1f) return original;
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        return Bitmap.createBitmap(
                original,
                0, 0,
                original.getWidth(),
                original.getHeight(),
                matrix,
                true
        );
    }


    /**
     * Change the black background bitmap to blue, return the new bitmap.
     */
    private Bitmap createBlueBackgroundFromBlack(Bitmap originalBlackBg) {
        Bitmap mutableCopy = originalBlackBg.copy(Bitmap.Config.ARGB_8888, true);
        int w = mutableCopy.getWidth();
        int h = mutableCopy.getHeight();
        int tolerance = 10;

        float centerX = w / 2f;
        float centerY = h / 2f;
        float radius = w / 2f;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double distSq = (x - centerX) * (x - centerX)
                        + (y - centerY) * (y - centerY);

                if (distSq <= radius * radius) {
                    int pixel = mutableCopy.getPixel(x, y);
                    int red   = Color.red(pixel);
                    int green = Color.green(pixel);
                    int blue  = Color.blue(pixel);

                    if (red < tolerance && green < tolerance && blue < tolerance) {
                        mutableCopy.setPixel(x, y,
                                ContextCompat.getColor(getContext(), R.color.progress_blue)
                        );
                    }
                }
            }
        }
        return mutableCopy;
    }

    private void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    private int dpToPx(Context context, float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }
}
