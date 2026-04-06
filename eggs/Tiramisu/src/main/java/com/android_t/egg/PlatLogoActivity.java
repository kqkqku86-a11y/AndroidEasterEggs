/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

/**
 * PINDAHKAN KELAS BUBBLE KE LUAR AGAR KOTLIN BISA MEMBACANYA
 */
class Bubble {
    public float x, y, r;
    public int color;
    public CharSequence text = null;
    public Drawable drawable = null;
}

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
        mClock.setClickable(true);

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

        try {
            if (getPackageName().equals("android")) {
                Settings.System.putLong(getContentResolver(), S_EGG_UNLOCK_SETTING, locked ? 0 : System.currentTimeMillis());
            }
        } catch (Exception e) {
            Log.e(TAG, "Can't write settings", e);
        }
    }

    public class SettableAnalogClock extends AnalogClock {
        private int mOverrideHour = -1;
        private int mOverrideMinute = -1;
        private boolean mOverride = false;

        public SettableAnalogClock(Context context) {
            super(context);
        }

        public Calendar now() {
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
                case MotionEvent.ACTION_MOVE:
                    float x = ev.getX();
                    float y = ev.getY();
                    float cx = getWidth() / 2f;
                    float cy = getHeight() / 2f;
                    float angle = (float) toPositiveDegrees(Math.atan2(x - cx, y - cy));
                    
                    // Putar jam secara visual
                    setRotation(angle);

                    int minutes = (int) (angle / 6);
                    mOverrideMinute = minutes;
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    return true;

                case MotionEvent.ACTION_UP:
                    float currentRot = getRotation() % 360;
                    if (currentRot < 0) currentRot += 360;

                    // Cek jika jam diarahkan ke jam 1 (target Easter Egg T)
                    if (currentRot >= 25 && currentRot <= 35) {
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        launchNextStage(false);
                    }
                    return true;
            }
            return super.onTouchEvent(ev);
        }
    }

    class BubblesDrawable extends Drawable implements View.OnLongClickListener {
        private static final int MAX_BUBBS = 2000;
        private int[] mColors;
        private int mEmojiSet = -1;
        private final Bubble[] mBubbs = new Bubble[MAX_BUBBS];
        private int mNumBubbs;
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public float avoid = 0f;
        public float padding = 0f;
        public float minR = 0f;

        BubblesDrawable() {
            int baseColor = 0xFF1A6ECC; // Default blue
            mColors = new int[]{
                    Color.BLUE, Color.CYAN, baseColor, Color.LTGRAY
            };
            for (int j = 0; j < mBubbs.length; j++) {
                mBubbs[j] = new Bubble();
            }
        }

        @Override
        public void draw(Canvas canvas) {
            if (getLevel() == 0) return;
            final float f = getLevel() / 10000f;
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setTextAlign(Paint.Align.CENTER);
            for (int j = 0; j < mNumBubbs; j++) {
                if (mBubbs[j].color == 0 || mBubbs[j].r == 0) continue;
                if (mBubbs[j].text != null) {
                    mPaint.setTextSize(mBubbs[j].r * 1.75f);
                    canvas.drawText(mBubbs[j].text.toString(), mBubbs[j].x,
                            mBubbs[j].y + mBubbs[j].r * f * 0.6f, mPaint);
                } else {
                    mPaint.setColor(mBubbs[j].color);
                    canvas.drawCircle(mBubbs[j].x, mBubbs[j].y, mBubbs[j].r * f, mPaint);
                }
            }
        }

        @Override
        protected boolean onLevelChange(int level) {
            invalidateSelf();
            return true;
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
            randomize();
        }

        private void randomize() {
            final float w = getBounds().width();
            final float h = getBounds().height();
            if (w <= 0 || h <= 0) return;
            final float maxR = Math.min(w, h) / 3f;
            mNumBubbs = 0;
            if (avoid > 0f) {
                mBubbs[mNumBubbs].x = w / 2f;
                mBubbs[mNumBubbs].y = h / 2f;
                mBubbs[mNumBubbs].r = avoid;
                mBubbs[mNumBubbs].color = 0;
                mNumBubbs++;
            }
            for (int j = 0; j < MAX_BUBBS; j++) {
                float x = (float) Math.random() * w;
                float y = (float) Math.random() * h;
                float r = Math.min(Math.min(x, w - x), Math.min(y, h - y));
                for (int i = 0; i < mNumBubbs; i++) {
                    r = (float) Math.min(r, Math.hypot(x - mBubbs[i].x, y - mBubbs[i].y) - mBubbs[i].r - padding);
                }
                if (r >= minR) {
                    r = Math.min(maxR, r);
                    mBubbs[mNumBubbs].x = x;
                    mBubbs[mNumBubbs].y = y;
                    mBubbs[mNumBubbs].r = r;
                    mBubbs[mNumBubbs].color = mColors[(int) (Math.random() * mColors.length)];
                    mNumBubbs++;
                }
            }
        }

        @Override public void setAlpha(int alpha) { mPaint.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter colorFilter) { mPaint.setColorFilter(colorFilter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        @Override public boolean onLongClick(View v) { return false; }
    }
}
