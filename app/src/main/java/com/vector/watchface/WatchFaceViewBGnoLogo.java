package com.vector.watchface;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import androidx.core.content.ContextCompat;

import java.util.Calendar;

public class WatchFaceViewBGnoLogo extends View {
    private static final String TAG = "WatchFaceView";

    private Bitmap backgroundBlack;      // 原始背景(黑底 + Moto logo)
    private Bitmap backgroundBlue;       // “蓝底 + Moto logo”的替换图
    private Bitmap hourHand, minuteHand;
    private Paint progressPaint;
    private boolean invertColors = false;
    private boolean screenResized = false;
    private boolean isFirstDraw = true;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private RectF progressRect;
    // 专门用来给背景图加滤镜的画笔
    private Paint recolorPaint;

    public WatchFaceViewBGnoLogo(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "Constructor() called");
        init();
    }

    private void init() {
        Log.d(TAG, "init() called");

        // 加载图片资源
        backgroundBlack = BitmapFactory.decodeResource(getResources(), R.drawable.background);
        hourHand = BitmapFactory.decodeResource(getResources(), R.drawable.hour_hand);
        minuteHand = BitmapFactory.decodeResource(getResources(), R.drawable.minute_hand);
        //一次性生成 “蓝底” 图像
        //    这样后续切换时, 直接 drawBitmap(backgroundBlue) 即可
        backgroundBlue = createBlueBackgroundFromBlack(backgroundBlack);

        // 进度条画笔
        progressPaint = new Paint();
        progressPaint.setAntiAlias(true);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(dpToPx(getContext(), 16));
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        // 用于染色背景的画笔
        recolorPaint = new Paint();
        recolorPaint.setAntiAlias(true);

        startClock();
    }

    private void startClock() {
        Log.d(TAG, "startClock() called");

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                int second = calendar.get(Calendar.SECOND);
                // 每分钟(秒==0)切换颜色
                if (second == 0) {
                    invertColors = !invertColors;
                    Log.d(TAG, "startClock() - invertColors changed to: " + invertColors);
                }

                invalidate(); // 触发重绘，确保颜色变化
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    // 第3点修正：在 onSizeChanged() 中更新 progressRect
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int dialRadius = backgroundBlack.getWidth() / 2;
        int progressOffset = dpToPx(getContext(), 48);

        // 以 View 的中心为圆心，计算秒环区域
        progressRect = new RectF(
                w / 2f - dialRadius + progressOffset,
                h / 2f - dialRadius + progressOffset,
                w / 2f + dialRadius - progressOffset,
                h / 2f + dialRadius - progressOffset
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        screenResized = false;

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

//        // 绘制背景
//        canvas.drawBitmap(background,
//                centerX - background.getWidth() / 2,
//                centerY - background.getHeight() / 2,
//                null);
        //根据 invertColors 选择要绘制的背景
        Bitmap currentBg = invertColors ? backgroundBlue : backgroundBlack;
        // 绘制背景
        if (currentBg != null) {
            canvas.drawBitmap(currentBg,
                    centerX - currentBg.getWidth() / 2f,
                    centerY - currentBg.getHeight() / 2f,
                    null
            );
        }


//        // 根据 invertColors 判定是否需要给背景加蓝色滤镜
//        if (invertColors) {
//            // 蓝底(背景被染蓝)，秒环黑色
//            PorterDuffColorFilter blueFilter =
//                    new PorterDuffColorFilter(ContextCompat.getColor(getContext(), R.color.progress_blue), PorterDuff.Mode.SRC_ATOP);
//            recolorPaint.setColorFilter(blueFilter);
//
//            // 用带滤镜的画笔绘制背景
//            canvas.drawBitmap(background,
//                    centerX - background.getWidth() / 2,
//                    centerY - background.getHeight() / 2,
//                    recolorPaint);
//
//        } else {
//            // 保持原图(黑底)，秒环蓝色
//            recolorPaint.setColorFilter(null);
//            canvas.drawBitmap(background,
//                    centerX - background.getWidth() / 2,
//                    centerY - background.getHeight() / 2,
//                    recolorPaint);
//        }
        // 获取当前时间
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);

        float hourAngle = (hour % 12 + minute / 60f) * 30;
        float minuteAngle = (minute + second / 60f) * 6;

        int hourPivotOffset = hourHand.getHeight() - dpToPx(getContext(), 7);
        int minutePivotOffset = minuteHand.getHeight() - dpToPx(getContext(), 7);

        // 旋转并绘制时针
        canvas.save();
        canvas.rotate(hourAngle, centerX, centerY);
        canvas.drawBitmap(hourHand,
                centerX - hourHand.getWidth() / 2,
                centerY - hourPivotOffset,
                null);
        canvas.restore();

        // 旋转并绘制分针
        canvas.save();
        canvas.rotate(minuteAngle, centerX, centerY);
        canvas.drawBitmap(minuteHand,
                centerX - minuteHand.getWidth() / 2,
                centerY - minutePivotOffset,
                null);
        canvas.restore();

        // 绘制秒环
        int offset = 2;
        progressPaint.setColor(invertColors
                ? Color.BLACK
                : ContextCompat.getColor(getContext(), R.color.progress_blue));
        float sweepAngle = (second / 60f) * 360 - offset;
        canvas.drawArc(progressRect, -90 + offset, sweepAngle, false, progressPaint);
    }

    // 第4点修正：在自定义 View 中保存 & 恢复 invertColors
    @Override
    protected Parcelable onSaveInstanceState() {
        // 先保存父类的状态
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.invertColors = this.invertColors;
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        // 如果不是我们自定义的 SavedState，就直接交给父类处理
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        // 恢复父类状态
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        // 恢复自定义字段
        this.invertColors = ss.invertColors;
    }

    static class SavedState extends BaseSavedState {
        boolean invertColors;

        SavedState(Parcelable superState) {
            super(superState);
        }

        // 从 Parcel 中读取
        private SavedState(Parcel in) {
            super(in);
            this.invertColors = in.readByte() != 0; // 1 = true, 0 = false
        }

        // 写入 Parcel
        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeByte((byte) (invertColors ? 1 : 0));
        }

        // 必须要有 CREATOR
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    /**
     * 遍历原图像素，如果是纯黑(R=G=B=0)，则替换为蓝色（可根据需求改色）
     * 返回一张新的 Bitmap。
     */
    private Bitmap createBlueBackgroundFromBlack(Bitmap originalBlackBg) {
        Bitmap mutableCopy = originalBlackBg.copy(Bitmap.Config.ARGB_8888, true);
        int w = mutableCopy.getWidth();
        int h = mutableCopy.getHeight();

        int tolerance = 10;

        // 假设图是正方形，圆心在 (w/2, h/2)，半径为 w/2
        float centerX = w / 2f;
        float centerY = h / 2f;
        float radius = w / 2f;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double distSq = (x - centerX) * (x - centerX)
                        + (y - centerY) * (y - centerY);
                // 判断是否在圆内
                if (distSq <= radius * radius) {
                    int pixel = mutableCopy.getPixel(x, y);
                    int red   = Color.red(pixel);
                    int green = Color.green(pixel);
                    int blue  = Color.blue(pixel);

                    // 若是接近纯黑，则替换为蓝色
                    if (red < tolerance && green < tolerance && blue < tolerance) {
                        mutableCopy.setPixel(x, y, ContextCompat.getColor(getContext(), R.color.progress_blue));
                    }
                }
                // else 圆外部分不动，仍保持原黑色（或你想改为透明也可以）
            }
        }
        return mutableCopy;
    }
    private int dpToPx(Context context, float dp) {
        int px = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
        Log.d(TAG, "dpToPx() called - " + dp + "dp = " + px + "px");
        return px;
    }
}
