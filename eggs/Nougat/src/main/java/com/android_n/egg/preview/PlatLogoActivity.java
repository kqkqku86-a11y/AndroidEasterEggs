package com.android_n.egg.preview;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class PlatLogoActivity extends Activity {

    FrameLayout mLayout;
    int mTapCount;
    int mKeyCount;

    PathInterpolator mInterpolator = new PathInterpolator(0f, 0f, 0.5f, 1f);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLayout = new FrameLayout(this);
        setContentView(mLayout);
    }

    @Override
    public void onAttachedToWindow() {
        final DisplayMetrics dm = getResources().getDisplayMetrics();
        final float dp = dm.density;

        final int size = (int)
                (Math.min(Math.min(dm.widthPixels, dm.heightPixels), 600 * dp) - 100 * dp);

        final ImageView im = new ImageView(this);

        final int pad = (int) (40 * dp);
        im.setPadding(pad, pad, pad, pad);
        im.setTranslationZ(20);
        im.setScaleX(0.5f);
        im.setScaleY(0.5f);
        im.setAlpha(0f);

        Drawable logo = getDrawable(R.drawable.n_android_logo);

        im.setBackground(new RippleDrawable(
                ColorStateList.valueOf(0xFFFFFFFF),
                logo,
                null
        ));

        im.setClickable(true);

        im.setOnClickListener(v -> {
            mTapCount++;

            im.setOnLongClickListener(v1 -> {
                if (mTapCount < 5) return false;

                Log.d("PlatLogo", "Easter egg triggered!");

                try {
                    startActivity(new Intent(Intent.ACTION_MAIN));
                } catch (Exception e) {
                    Log.e("PlatLogo", "No activity", e);
                }

                return true;
            });
        });

        // Keyboard support
        im.setFocusable(true);
        im.requestFocus();

        im.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode != KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                mKeyCount++;
                if (mKeyCount > 2) {
                    if (mTapCount > 5) {
                        im.performLongClick();
                    } else {
                        im.performClick();
                    }
                }
                return true;
            }
            return false;
        });

        mLayout.addView(im, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));

        im.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setInterpolator(mInterpolator)
                .setDuration(500)
                .setStartDelay(800)
                .start();
    }
}                            
