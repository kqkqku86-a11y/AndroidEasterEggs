/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android_t.egg;

import static android.graphics.PixelFormat.TRANSLUCENT;

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
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;

public class PlatLogoActivity extends Activity {
    private static final String TAG = "PlatLogoActivity";
    private static final String S_EGG_UNLOCK_SETTING = "egg_mode_s";
    private SettableAnalogClock mClock;
    private ImageView mLogo;
    private BubblesDrawable mBg;

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setNavigationBarColor(0);
        getWindow().setStatusBarColor(0);
        final ActionBar ab = getActionBar();
        if (ab != null) ab.hide();
        final FrameLayout layout = new FrameLayout(this);
        mClock = new SettableAnalogClock(this);
        final DisplayMetrics dm = getResources().getDisplayMetrics();
        final float dp = dm.density;
        final int minSide = Math.min(dm.widthPixels, dm.heightPixels);
        final int widgetSize = (int) (minSide * 0.75);
        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(widgetSize, widgetSize);
        lp.gravity = Gravity.CENTER;
        layout.addView(mClock, lp);
        mLogo = new ImageView(this);
        mLogo.setVisibility(View.GONE);
        mLogo.setImageResource(R.drawable.t_android_logo);
        layout.addView(mLogo, lp);
        mBg = new BubblesDrawable(this);
        mBg.setLevel(0);
        mBg.avoid = widgetSize / 2;
        mBg.padding = 0.5f * dp;
        mBg.minR = 1 * dp;
        layout.setBackground(mBg);
        layout.setOnLongClickListener(mBg);
        setContentView(layout);
    }

    private boolean shouldWriteSettings() {
        return getPackageName().equals("android");
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
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setInterpolator(new OvershootInterpolator())
                .start();
        mLogo.postDelayed(() -> {
                    final ObjectAnimator anim = ObjectAnimator.ofInt(mBg, "level", 0, 10000);
                    anim.setInterpolator(new DecelerateInterpolator(1f));
                    anim.start();
                },
                500
        );
        final ContentResolver cr = getContentResolver();
        try {
            if (shouldWriteSettings()) {
                Log.v(TAG, "Saving egg unlock=" + locked);
                syncTouchPressure();
                Settings.System.putLong(cr,
                        S_EGG_UNLOCK_SETTING,
                        locked ? 0 : System.currentTimeMillis());
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Can't write settings", e);
        }
        try {
            startActivity(new Intent(Intent.ACTION_MAIN)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .addCategory("com.android.internal.category.PLATLOGO"));
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "No more eggs.");
        }
    }

    static final String TOUCH_STATS = "touch.stats";
    double mPressureMin = 0, mPressureMax = -1;

    private void measureTouchPressure(MotionEvent event) {
        final float pressure = event.getPressure();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (mPressureMax < 0) {
                    mPressureMin = mPressureMax = pressure;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (pressure < mPressureMin) mPressureMin = pressure;
                if (pressure > mPressureMax) mPressureMax = pressure;
                break;
        }
    }

    private void syncTouchPressure() {
        try {
            final String touchDataJson = Settings.System.getString(
                    getContentResolver(), TOUCH_STATS);
            final JSONObject touchData = new JSONObject(
                    touchDataJson != null ? touchDataJson : "{}");
            if (touchData.has("min")) {
                mPressureMin = Math.min(mPressureMin, touchData.getDouble("min"));
            }
            if (touchData.has("max")) {
                mPressureMax = Math.max(mPressureMax, touchData.getDouble("max"));
            }
            if (mPressureMax >= 0) {
                touchData.put("min", mPressureMin);
                touchData.put("max", mPressureMax);
                if (shouldWriteSettings()) {
                    Settings.System.putString(getContentResolver(), TOUCH_STATS,
                            touchData.toString());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Can't write touch settings", e);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        syncTouchPressure();
    }

    @Override
    public void onStop() {
        syncTouchPressure();
        super.onStop();
    }

    /**
     * Custom analog clock dengan tampilan scalloped (bergerigi) seperti UI asli Android T.
     * Jarum jam mengikuti waktu perangkat saat pertama dibuka.
     */
    public class SettableAnalogClock extends View {
        // Waktu perangkat sebagai default
        private int mOverrideHour;
        private int mOverrideMinute;

        private final Paint mPaintFace   = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mPaintHour   = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mPaintMinute = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mPaintCenter = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path  mScallopPath = new Path();

        public SettableAnalogClock(Context context) {
            super(context);

            // Inisialisasi jarum dari waktu perangkat sekarang
            java.util.Calendar now = java.util.Calendar.getInstance();
            mOverrideHour   = now.get(java.util.Calendar.HOUR_OF_DAY);
            mOverrideMinute = now.get(java.util.Calendar.MINUTE);

            // Warna abu-abu muda untuk face (seperti screenshot)
            mPaintFace.setColor(0xFFD8D8E0);
            mPaintFace.setStyle(Paint.Style.FILL);

            // Jarum menit: abu-abu keunguan
            mPaintMinute.setColor(0xFF8888AA);
            mPaintMinute.setStrokeCap(Paint.Cap.ROUND);
            mPaintMinute.setStyle(Paint.Style.STROKE);

            // Jarum jam: biru
            mPaintHour.setColor(0xFF2244CC);
            mPaintHour.setStrokeCap(Paint.Cap.ROUND);
            mPaintHour.setStyle(Paint.Style.STROKE);

            // Titik tengah
            mPaintCenter.setColor(0xFF2244CC);
            mPaintCenter.setStyle(Paint.Style.FILL);
        }

        /**
         * Membuat path scalloped (bergerigi keluar) — lingkaran dengan tonjolan setengah lingkaran
         * mengelilingi tepian, mirip tampilan jam Android T asli.
         */
        private void buildScallopPath(float cx, float cy, float radius) {
            mScallopPath.reset();
            int scallops = 16;         // jumlah gerigi
            float scallopR = radius * 0.08f; // radius tiap gerigi
            float innerR   = radius - scallopR;

            for (int i = 0; i < scallops; i++) {
                double angle = 2 * Math.PI * i / scallops - Math.PI / 2;
                float peakX = cx + (float) Math.cos(angle) * (innerR + scallopR);
                float peakY = cy + (float) Math.sin(angle) * (innerR + scallopR);
                RectF oval = new RectF(
                        peakX - scallopR, peakY - scallopR,
                        peakX + scallopR, peakY + scallopR
                );
                float startDeg = (float) Math.toDegrees(angle) + 90f;
                if (i == 0) {
                    float startX = cx + (float) Math.cos(angle - Math.PI / scallops) * innerR;
                    float startY = cy + (float) Math.sin(angle - Math.PI / scallops) * innerR;
                    mScallopPath.moveTo(startX, startY);
                }
                mScallopPath.arcTo(oval, startDeg + 180f, 180f);
            }
            mScallopPath.close();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            final float cx = getWidth() / 2f;
            final float cy = getHeight() / 2f;
            final float radius = Math.min(cx, cy) * 0.88f;
            final float strokeWidth = radius * 0.055f;

            // Gambar background scalloped
            buildScallopPath(cx, cy, radius);
            canvas.drawPath(mScallopPath, mPaintFace);

            // Jarum menit
            final float minAngle = (float) Math.toRadians(mOverrideMinute * 6 - 90);
            final float minLen   = radius * 0.62f;
            mPaintMinute.setStrokeWidth(strokeWidth);
            canvas.drawLine(
                    cx, cy,
                    cx + (float) Math.cos(minAngle) * minLen,
                    cy + (float) Math.sin(minAngle) * minLen,
                    mPaintMinute
            );

            // Jarum jam
            final float hourAngle = (float) Math.toRadians(
                    (mOverrideHour % 12) * 30f + mOverrideMinute * 0.5f - 90f);
            final float hourLen = radius * 0.42f;
            mPaintHour.setStrokeWidth(strokeWidth * 1.15f);
            canvas.drawLine(
                    cx, cy,
                    cx + (float) Math.cos(hourAngle) * hourLen,
                    cy + (float) Math.sin(hourAngle) * hourLen,
                    mPaintHour
            );

            // Titik tengah
            canvas.drawCircle(cx, cy, strokeWidth * 0.9f, mPaintCenter);
        }

        double toPositiveDegrees(double rad) {
            return (Math.toDegrees(rad) + 360 - 90) % 360;
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    return true;
                case MotionEvent.ACTION_MOVE:
                    measureTouchPressure(ev);
                    float x  = ev.getX();
                    float y  = ev.getY();
                    float cx = getWidth() / 2f;
                    float cy = getHeight() / 2f;
                    float angle = (float) toPositiveDegrees(Math.atan2(x - cx, y - cy));
                    int minutes = (75 - (int) (angle / 6)) % 60;
                    int minuteDelta = minutes - mOverrideMinute;
                    if (minuteDelta != 0) {
                        if (Math.abs(minuteDelta) > 45 && mOverrideHour >= 0) {
                            int hourDelta = (minuteDelta < 0) ? 1 : -1;
                            mOverrideHour = (mOverrideHour + 24 + hourDelta) % 24;
                        }
                        mOverrideMinute = minutes;
                        if (mOverrideMinute == 0) {
                            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                            if (getScaleX() == 1f) {
                                setScaleX(1.05f);
                                setScaleY(1.05f);
                                animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                            }
                        } else {
                            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                        }
                        postInvalidate();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (mOverrideMinute == 0 && (mOverrideHour % 12) == 1) {
                        Log.v(TAG, "13:00");
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        launchNextStage(false);
                    }
                    return true;
            }
            return false;
        }
    }

    private static final String[][] EMOJI_SETS = {
            {"🍇", "🍈", "🍉", "🍊", "🍋", "🍌", "🍍", "🥭", "🍎", "🍏", "🍐", "🍑",
                    "🍒", "🍓", "🫐", "🥝"},
            {"😺", "😸", "😹", "😻", "😼", "😽", "🙀", "😿", "😾"},
            {"😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "🙃", "🫠", "😉", "😊",
                    "😇", "🥰", "😍", "🤩", "😘", "😗", "☺️", "😚", "😙", "🥲", "😋", "😛", "😜",
                    "🤪", "😝", "🤑", "🤗", "🤭", "🫢", "🫣", "🤫", "🤔", "🫡", "🤐", "🤨", "😐",
                    "😑", "😶", "🫥", "😏", "😒", "🙄", "😬", "🤥", "😌", "😔", "😪", "🤤", "😴",
                    "😷"},
            {"🤩", "😍", "🥰", "😘", "🥳", "🥲", "🥹"},
            {"🫠"},
            {"💘", "💝", "💖", "💗", "💓", "💞", "💕", "❣", "💔", "❤", "🧡", "💛",
                    "💚", "💙", "💜", "🤎", "🖤", "🤍"},
            {"👽", "🛸", "✨", "🌟", "💫", "🚀", "🪐", "🌙", "⭐", "🌍"},
            {"🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘"},
            {"🐙", "🪸", "🦑", "🦀", "🦐", "🐡", "🦞", "🐠", "🐟", "🐳", "🐋", "🐬", "🫧", "🌊",
                    "🦈"},
            {"🙈", "🙉", "🙊", "🐵", "🐒"},
            {"♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓"},
            {"🕛", "🕧", "🕐", "🕜", "🕑", "🕝", "🕒", "🕞", "🕓", "🕟", "🕔", "🕠", "🕕", "🕡",
                    "🕖", "🕢", "🕗", "🕣", "🕘", "🕤", "🕙", "🕥", "🕚", "🕦"},
            {"🌺", "🌸", "💮", "🏵️", "🌼", "🌿"},
            {"🐢", "✨", "🌟", "👑"}
    };

    public static class Bubble {
        public float x, y, r;
        public int color;
        public CharSequence text = null;
        public Drawable drawable = null;
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

        BubblesDrawable(Context context) {
            int baseColor = getWallpaperDominantColor(context);

            mColors = new int[]{
                    darken(baseColor, 0.4f),
                    darken(baseColor, 0.65f),
                    baseColor,
                    baseColor,
                    lighten(baseColor, 0.5f),
                    lighten(baseColor, 0.75f),
            };

            for (int j = 0; j < mBubbs.length; j++) {
                mBubbs[j] = new Bubble();
            }
        }

        private int getWallpaperDominantColor(Context context) {
            try {
                WallpaperManager wm = WallpaperManager.getInstance(context);
                Drawable wallpaperDrawable = wm.getDrawable();
                Bitmap wallpaperBitmap = ((BitmapDrawable) wallpaperDrawable).getBitmap();

                long r = 0, g = 0, b = 0;
                int width = wallpaperBitmap.getWidth();
                int height = wallpaperBitmap.getHeight();
                int count = 0;

                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        int pixel = wallpaperBitmap.getPixel(x, y);
                        r += Color.red(pixel);
                        g += Color.green(pixel);
                        b += Color.blue(pixel);
                        count++;
                    }
                }

                return Color.rgb((int) (r / count), (int) (g / count), (int) (b / count));
            } catch (Exception e) {
                return ContextCompat.getColor(context, android.R.color.holo_blue_light);
            }
        }

        private int darken(int color, float factor) {
            return Color.rgb(
                    (int) (Color.red(color) * factor),
                    (int) (Color.green(color) * factor),
                    (int) (Color.blue(color) * factor)
            );
        }

        private int lighten(int color, float factor) {
            return Color.rgb(
                    (int) (Color.red(color) + (255 - Color.red(color)) * factor),
                    (int) (Color.green(color) + (255 - Color.green(color)) * factor),
                    (int) (Color.blue(color) + (255 - Color.blue(color)) * factor)
            );
        }

        @Override
        public void draw(Canvas canvas) {
            if (getLevel() == 0) return;
            final float f = getLevel() / 10000f;
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setTextAlign(Paint.Align.CENTER);
            int drawn = 0;
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
                drawn++;
            }
        }

        public void chooseEmojiSet() {
            mEmojiSet = (int) (Math.random() * EMOJI_SETS.length);
            final String[] emojiSet = EMOJI_SETS[mEmojiSet];
            for (int j = 0; j < mBubbs.length; j++) {
                mBubbs[j].text = emojiSet[(int) (Math.random() * emojiSet.length)];
            }
            invalidateSelf();
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
                int tries = 5;
                while (tries-- > 0) {
                    float x = (float) Math.random() * w;
                    float y = (float) Math.random() * h;
                    float r = Math.min(Math.min(x, w - x), Math.min(y, h - y));
                    for (int i = 0; i < mNumBubbs; i++) {
                        r = (float) Math.min(r,
                                Math.hypot(x - mBubbs[i].x, y - mBubbs[i].y) - mBubbs[i].r
                                        - padding);
                        if (r < minR) break;
                    }
                    if (r >= minR) {
                        r = Math.min(maxR, r);
                        mBubbs[mNumBubbs].x = x;
                        mBubbs[mNumBubbs].y = y;
                        mBubbs[mNumBubbs].r = r;
                        mBubbs[mNumBubbs].color = mColors[(int) (Math.random() * mColors.length)];
                        mNumBubbs++;
                        break;
                    }
                }
            }
            Log.v(TAG, String.format("successfully placed %d bubbles (%d%%)",
                    mNumBubbs, (int) (100f * mNumBubbs / MAX_BUBBS)));
        }

        @Override
        public void setAlpha(int alpha) { mPaint.setAlpha(alpha); }

        @Override
        public void setColorFilter(ColorFilter colorFilter) { mPaint.setColorFilter(colorFilter); }

        @Override
        public int getOpacity() { return PixelFormat.TRANSLUCENT; }

        @Override
        public boolean onLongClick(View v) {
            if (getLevel() == 0) return false;
            chooseEmojiSet();
            return true;
        }
    }
}
