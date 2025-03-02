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
    // 原图
    private Bitmap backgroundBlack, backgroundBlue;
    private Bitmap hourHand, minuteHand;

    // 缩放后
    private Bitmap scaledBlack, scaledBlue;
    private Bitmap scaledHourHand, scaledMinuteHand;
    // 画笔
    private Paint progressPaint;
    private float currentScale = 1f; // 默认值为1f
    // 是否翻转颜色：false -> 黑底蓝环, true -> 蓝底黑环
    private boolean invertColors = false;

    // 用于定时刷新
    private final Handler handler = new Handler(Looper.getMainLooper());

    // 用于绘制秒环的区域
    private RectF progressRect;

    public WatchFaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // 允许系统保存 / 恢复 View 状态（如 invertColors）
        setSaveEnabled(true);

        init();
    }

    private void init() {
        // 1) 加载原始背景 & 指针
        backgroundBlack = BitmapFactory.decodeResource(getResources(), R.drawable.background);
        hourHand = BitmapFactory.decodeResource(getResources(), R.drawable.hour_hand);
        minuteHand = BitmapFactory.decodeResource(getResources(), R.drawable.minute_hand);

        // 2) 生成蓝底图（把表盘圆内的黑色像素替换为蓝色）
        backgroundBlue = createBlueBackgroundFromBlack(backgroundBlack);

        // 3) 用于绘制秒环的画笔
        progressPaint = new Paint();
        progressPaint.setAntiAlias(true);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(dpToPx(getContext(), 12));
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        // 每秒刷新一次（若到 0 秒翻转 invertColors）
        startClock();
    }

    private void startClock() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                int second = calendar.get(Calendar.SECOND);

                // 每分钟 (second == 0) 切换一次颜色
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
        // 1) 先让系统测量一次
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // 2) 得到系统测量后的宽度
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // 1) 计算一个全局 scale：以背景图的原尺寸(black)为基准
        //    让它等比缩放到不超过 (w, h)
        float scale = Math.min(
                (float) w / backgroundBlack.getWidth(),
                (float) h / backgroundBlack.getHeight()
        );
        currentScale = scale;
        // 2) 用同一个 scale 缩放 black / blue 背景
        scaledBlack = scaleBitmap(backgroundBlack, scale);
        scaledBlue = scaleBitmap(backgroundBlue, scale);

        // 3) 再用同一个 scale 缩放指针
        scaledHourHand = scaleBitmap(hourHand, scale);
        scaledMinuteHand = scaleBitmap(minuteHand, scale);

        // 4) 计算秒环区域
        //    因为表盘已经缩放成 scaledBlack，故表盘半径 = scaledBlack.getWidth() / 2
        int dialRadius = scaledBlack.getWidth() / 2;
        int progressOffset = dpToPx(getContext(), 30);

        progressRect = new RectF(
                w / 2f - dialRadius + progressOffset,
                h / 2f - dialRadius + progressOffset,
                w / 2f + dialRadius - progressOffset,
                h / 2f + dialRadius - progressOffset
        );
    }


    /**
     * 在这里不再处理屏幕方向或大小变化，也不缩放背景图。
     * 只要画面只在固定方向 (如 portrait) 使用即可。
     */
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // 1) 绘制背景
        Bitmap currentBg = invertColors ? scaledBlue : scaledBlack;
        if (currentBg != null) {
            canvas.drawBitmap(
                    currentBg,
                    centerX - currentBg.getWidth() / 2f,
                    centerY - currentBg.getHeight() / 2f,
                    null
            );
        }

        // 2) 计算时间，得到旋转角度
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);

        float hourAngle = (hour % 12 + minute / 60f) * 30f;
        float minuteAngle = (minute + second / 60f) * 6f;

        // 3) 绘制时针、分针，注意 pivotOffset 用“缩放后指针”的高度
        if (scaledHourHand != null && scaledMinuteHand != null) {
            // 原图底到 pivot 是 7px
            float offsetInScaledPx = dpToPx(getContext(), 7)*currentScale;
            float hourPivotOffset = (scaledHourHand.getHeight() - offsetInScaledPx);
            float minutePivotOffset = (scaledMinuteHand.getHeight() - offsetInScaledPx);

            // 时针
            canvas.save();
            canvas.rotate(hourAngle, centerX, centerY);
            canvas.drawBitmap(
                    scaledHourHand,
                    centerX - scaledHourHand.getWidth() / 2f,
                    centerY - hourPivotOffset,
                    null
            );
            canvas.restore();

            // 分针
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

        // 4) 绘制秒环
        progressPaint.setColor(
                invertColors
                        ? Color.BLACK
                        : ContextCompat.getColor(getContext(), R.color.progress_blue)
        );
        float sweepAngle = (second / 60f) * 360f;
        canvas.drawArc(progressRect, -90f, sweepAngle, false, progressPaint);
    }

    /**
     * 将原始 Bitmap 等比缩放到指定比例 scale。
     * 如果 scale=1f，则返回与原图尺寸相同的副本（或可以根据需求直接返回原图）。
     */
    private Bitmap scaleBitmap(Bitmap original, float scale) {
        if (original == null) return null;

        // 若 scale 接近1，可以选择返回原图，也可以返回新 Bitmap
        // 这里为了统一，全部都执行一次 createBitmap
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        return Bitmap.createBitmap(
                original,
                0, 0,
                original.getWidth(),
                original.getHeight(),
                matrix,
                true // bilinear过滤，更平滑
        );
    }


    /**
     * 把表盘圆内的黑色像素变为蓝色，生成 backgroundBlue。
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

    private int dpToPx(Context context, float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }
}
