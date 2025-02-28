package com.vector.watchface;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import java.util.Calendar;

public class WatchFaceView extends View {
    private Bitmap background, hourHand, minuteHand;
    private Paint progressPaint;
    private int seconds;
    private boolean invertColors = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public WatchFaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 加载图片资源
        background = BitmapFactory.decodeResource(getResources(), R.drawable.background);
        hourHand = BitmapFactory.decodeResource(getResources(), R.drawable.hour_hand);
        minuteHand = BitmapFactory.decodeResource(getResources(), R.drawable.minute_hand);

        // 进度条画笔
        progressPaint = new Paint();
        progressPaint.setAntiAlias(true);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(12);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        startClock();
    }

    private void startClock() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                seconds = calendar.get(Calendar.SECOND);

                if (seconds == 0) {
                    invertColors = !invertColors;
                }

                invalidate();
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int radius = Math.min(centerX, centerY);

        // 1. 绘制背景
        canvas.drawBitmap(background, centerX - background.getWidth() / 2, centerY - background.getHeight() / 2, null);

        // 2. 绘制秒针进度条（环绕表盘）
        progressPaint.setColor(invertColors ? Color.BLACK : Color.BLUE);
        RectF rect = new RectF(centerX - radius + 20, centerY - radius + 20, centerX + radius - 20, centerY + radius - 20);
        float sweepAngle = (seconds / 60f) * 360;
        canvas.drawArc(rect, -90, sweepAngle, false, progressPaint);

        // 3. 获取当前时间
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);

        float hourAngle = (hour % 12 + minute / 60f) * 30;
        float minuteAngle = (minute + second / 60f) * 6;

        // **使用 dpToPx() 计算旋转中心的 Y 偏移量**
        int hourPivotOffset = hourHand.getHeight() - dpToPx(getContext(), 7);
        int minutePivotOffset = minuteHand.getHeight() - dpToPx(getContext(), 7);

        // 4. 旋转并绘制时针（底部第 7 dp 作为旋转点）
        canvas.save();
        canvas.rotate(hourAngle, centerX, centerY);
        canvas.drawBitmap(hourHand, centerX - hourHand.getWidth() / 2, centerY - hourPivotOffset, null);
        canvas.restore();

        // 5. 旋转并绘制分针（底部第 7 dp 作为旋转点）
        canvas.save();
        canvas.rotate(minuteAngle, centerX, centerY);
        canvas.drawBitmap(minuteHand, centerX - minuteHand.getWidth() / 2, centerY - minutePivotOffset, null);
        canvas.restore();
    }

    /**
     * 将 dp 转换为 px，确保 UI 适配所有屏幕密度
     */
    private int dpToPx(Context context, float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }
}
