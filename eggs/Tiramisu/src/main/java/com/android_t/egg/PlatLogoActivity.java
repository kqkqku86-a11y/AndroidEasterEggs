package com.android_t.egg;

import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.AnalogClock;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.util.Calendar;

public class PlatLogoActivity extends Activity {
    private static final String TAG = "PlatLogoActivity";
    private static final String S_EGG_UNLOCK_SETTING = "egg_mode_s";

    private SettableAnalogClock mClock;
    private ImageView mLogo;
    private BubblesDrawable mBg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setNavigationBarColor(0);
        getWindow().setStatusBarColor(0);

        final ActionBar ab = getActionBar();
        if (ab != null) ab.hide();

        final FrameLayout layout = new FrameLayout(this);

        mClock = new SettableAnalogClock(this);
        // Memastikan jam bisa menerima sentuhan
        mClock.setClickable(true);
        mClock.setFocusable(true);

        final DisplayMetrics dm = getResources().getDisplayMetrics();
        final float dp = dm.density;
        final int minSide = Math.min(dm.widthPixels, dm.heightPixels);
        final int widgetSize = (int) (minSide * 0.75);
        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(widgetSize, widgetSize);
        lp.gravity = Gravity.CENTER;
        layout.addView(mClock, lp);

        mLogo = new ImageView(this);
        mLogo.setVisibility(View.GONE);
        mLogo.setImageDrawable(createDrawable());
        layout.addView(mLogo, lp);

        mBg = new BubblesDrawable();
        mBg.setLevel(0);
        mBg.avoid = widgetSize / 2;
        mBg.padding = 0.5f * dp;
        mBg.minR = 1 * dp;
        layout.setBackground(mBg);

        setContentView(layout);
    }

    private Drawable createDrawable() {
        Drawable logo = ContextCompat.getDrawable(this, R.drawable.t_platlogo);
        if (logo != null) return logo;

        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(0xFF1A6ECC);
        circle.setSize(256, 256);
        return circle;
    }

    private void launchNextStage(boolean locked) {
        mClock.animate()
                .alpha(0f).scaleX(0.5f).scaleY(0.5f)
                .withEndAction(() -> mClock.setVisibility(View.GONE))
                .start();
        mLogo.setAlpha(0f);
        mLogo.setScaleX(0.5f);
        mLogo.setScaleY(0.5f);
        mLogo.setVisibility(View.VISIBLE);
        mLogo.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setInterpolator(new OvershootInterpolator())
                .start();
        mLogo.postDelayed(() -> {
            final ObjectAnimator anim = ObjectAnimator.ofInt(mBg, "level", 0, 10000);
            anim.setInterpolator(new DecelerateInterpolator(1f));
            anim.start();
        }, 500);

        final ContentResolver cr = getContentResolver();
        try {
            if (getPackageName().equals("android")) {
                Settings.System.putLong(cr, S_EGG_UNLOCK_SETTING, locked ? 0 : System.currentTimeMillis());
            }
        } catch (Exception e) {
            Log.e(TAG, "Can't write settings", e);
        }
    }

    // --- INNER CLASS CLOCK ---
    public class SettableAnalogClock extends AnalogClock {
        private int mOverrideHour = -1;
        private int mOverrideMinute = -1;
        private boolean mOverride = false;

        public SettableAnalogClock(Context context) {
            super(context);
        }

        // Kita tidak bisa override now() di SDK standar, jadi kita buat method pembantu
        public Calendar getCustomNow() {
            Calendar realNow = Calendar.getInstance();
            if (mOverride) {
                if (mOverrideHour < 0) mOverrideHour = realNow.get(Calendar.HOUR_OF_DAY);
                realNow.set(Calendar.HOUR_OF_DAY, mOverrideHour);
                realNow.set(Calendar.MINUTE, mOverrideMinute);
                realNow.set(Calendar.SECOND, 0);
            }
            return realNow;
        }

        private double toPositiveDegrees(double rad) {
            return (Math.toDegrees(rad) + 360 - 90) % 360;
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mOverride = true;
                    // Lanjut ke ACTION_MOVE
                case MotionEvent.ACTION_MOVE:
                    float x = ev.getX();
                    float y = ev.getY();
                    float cx = getWidth() / 2f;
                    float cy = getHeight() / 2f;
                    
                    // Hitung sudut sentuhan
                    float angle = (float) toPositiveDegrees(Math.atan2(x - cx, y - cy));
                    
                    // Logika jam: memutar view agar jarum terlihat bergerak
                    // Karena kita tidak bisa gerakkan jarum internal, kita putar seluruh jamnya
                    setRotation(angle); 

                    int minutes = (int) (angle / 6); // 360 derajat / 60 menit = 6 derajat per menit
                    mOverrideMinute = minutes;
                    
                    // Efek getar saat melewati angka menit
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    return true;

                case MotionEvent.ACTION_UP:
                    // Cek apakah "jarum" berada di posisi jam 1 siang (sekitar sudut 30-40 derajat)
                    // Atau sederhananya, jika mOverrideMinute mendekati angka yang ditargetkan (13:00)
                    // Pada Easter Egg T, targetnya adalah jam 13:00 (1:00 PM)
                    float currentRot = getRotation() % 360;
                    if (currentRot < 0) currentRot += 360;

                    // Toleransi sudut untuk jam 1 (30 derajat)
                    if (currentRot >= 25 && currentRot <= 35) {
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        launchNextStage(false);
                    }
                    return true;
            }
            return super.onTouchEvent(ev);
        }
    }

    // --- BUBBLES DRAWABLE (Logika Emoji) ---
    // (Gunakan kode BubblesDrawable yang sebelumnya sudah kamu miliki di sini)
    // ...
    // (Pastikan method getWallpaperDominantColor menggunakan try-catch agar tidak crash)
}
