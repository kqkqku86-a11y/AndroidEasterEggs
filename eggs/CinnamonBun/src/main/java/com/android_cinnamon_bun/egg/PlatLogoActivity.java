package com.android_cinnamon_bun.egg;

import android.animation.ObjectAnimator;
import android.animation.TimeAnimator;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.*;
import android.provider.Settings;
import android.util.*;
import android.view.*;
import android.widget.*;

import org.json.JSONObject;

import java.util.Random;

public class PlatLogoActivity extends Activity {

    private static final String TAG = "PlatLogoActivity";

    private ImageView mLogo;
    private Starfield mStarfield;
    private FrameLayout mLayout;
    private TimeAnimator mAnim;
    private ObjectAnimator mWarpAnim;
    private Random mRandom;
    private float mDp;
    private RumblePack mRumble;
    private boolean mAnimationsEnabled = true;

    private static final float MIN_WARP = 1f;
    private static final float MAX_WARP = 10f;
    private static final long LAUNCH_TIME = 5000;

    private final View.OnTouchListener mTouchListener = (v, event) -> {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startWarp();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                stopWarp();
                break;
        }
        return true;
    };

    private final Runnable mLaunchNextStage = () -> {
        stopWarp();
        launchNextStage(false);
    };

    private final TimeAnimator.TimeListener mTimeListener = (animation, totalTime, deltaTime) -> {
        mStarfield.update(deltaTime);
        final float warpFrac = (mStarfield.getWarp() - MIN_WARP) / (MAX_WARP - MIN_WARP);

        if (mAnimationsEnabled) {
            mLogo.setTranslationX(mRandom.nextFloat() * warpFrac * 5 * mDp);
            mLogo.setTranslationY(mRandom.nextFloat() * warpFrac * 5 * mDp);
        }

        if (warpFrac > 0f) {
            mRumble.rumble(warpFrac);
        }

        mLayout.postInvalidate();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setDecorFitsSystemWindows(false);
        getWindow().setNavigationBarColor(0);
        getWindow().setStatusBarColor(0);
        getWindow().getDecorView().getWindowInsetsController()
                .hide(WindowInsets.Type.systemBars());

        ActionBar ab = getActionBar();
        if (ab != null) ab.hide();

        mRumble = new RumblePack();
        mLayout = new FrameLayout(this);
        mRandom = new Random();
        mDp = getResources().getDisplayMetrics().density;

        mStarfield = new Starfield(mRandom, mDp * 2f);
        mLayout.setBackground(mStarfield);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int size = (int) (Math.min(dm.widthPixels, dm.heightPixels) * 0.75);

        FrameLayout.LayoutParams lp =
                new FrameLayout.LayoutParams(size, size, Gravity.CENTER);

        mLogo = new ImageView(this);
        mLogo.setImageResource(R.drawable.platlogo); // ✔ sudah diganti
        mLogo.setOnTouchListener(mTouchListener);

        mLayout.addView(mLogo, lp);
        setContentView(mLayout);
    }

    private void startWarp() {
        stopWarp();
        mWarpAnim = ObjectAnimator.ofFloat(mStarfield, "warp", MIN_WARP, MAX_WARP)
                .setDuration(LAUNCH_TIME);
        mWarpAnim.start();
        mLogo.postDelayed(mLaunchNextStage, LAUNCH_TIME);
    }

    private void stopWarp() {
        if (mWarpAnim != null) {
            mWarpAnim.cancel();
            mWarpAnim = null;
        }
        mStarfield.setWarp(1f);
    }

    private void startAnimating() {
        mAnim = new TimeAnimator();
        mAnim.setTimeListener(mTimeListener);
        mAnim.start();
    }

    private void stopAnimating() {
        if (mAnim != null) mAnim.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startAnimating();
    }

    @Override
    protected void onPause() {
        stopWarp();
        stopAnimating();
        super.onPause();
    }

    private void launchNextStage(boolean locked) {
        try {
            startActivity(new Intent(Intent.ACTION_MAIN)
                    .addCategory("com.android.internal.category.PLATLOGO"));
        } catch (Exception ignored) {}
    }

    // ======================
    // STARFIELD (INNER CLASS)
    // ======================
    private static class Starfield extends Drawable {

        private final Paint mPaint = new Paint();
        private float mWarp = 1f;
        private final Random mRng;

        Starfield(Random rng, float size) {
            mRng = rng;
            mPaint.setColor(Color.WHITE);
        }

        void update(long dt) {}

        void setWarp(float warp) {
            mWarp = warp;
        }

        float getWarp() {
            return mWarp;
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawColor(Color.BLACK);
        }

        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(ColorFilter cf) {}
        @Override public int getOpacity() { return PixelFormat.OPAQUE; }
    }

    // ======================
    // RUMBLE PACK (INNER CLASS)
    // ======================
    private class RumblePack implements Handler.Callback {

        private final VibratorManager mVibeMan;
        private final HandlerThread mThread;
        private final Handler mHandler;

        RumblePack() {
            mVibeMan = getSystemService(VibratorManager.class);
            mThread = new HandlerThread("vibe");
            mThread.start();
            mHandler = Handler.createAsync(mThread.getLooper(), this);
        }

        void rumble(float warpFrac) {
            Message msg = Message.obtain();
            msg.arg1 = (int)(warpFrac * 100);
            mHandler.sendMessage(msg);
        }

        @Override
        public boolean handleMessage(Message msg) {
            return false;
        }

        void destroy() {
            mThread.quit();
        }
    }
}