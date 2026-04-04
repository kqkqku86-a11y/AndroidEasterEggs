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
}                            overlay.setBounds(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
                            im.getOverlay().clear();
                            im.getOverlay().add(overlay);
                            overlay.setAlpha(0);
                            ObjectAnimator.ofInt(overlay, "alpha", 0, 255)
                                .setDuration(500)
                                .start();
                        }
                        final ContentResolver cr = getContentResolver();
                        if (Settings.System.getLong(cr, Settings.System.EGG_MODE, 0)
                                == 0) {
                            // For posterity: the moment this user unlocked the easter egg
                            try {
                                Settings.System.putLong(cr,
                                        Settings.System.EGG_MODE,
                                        System.currentTimeMillis());
                            } catch (RuntimeException e) {
                                Log.e("PlatLogoActivity", "Can't write settings", e);
                            }
                        }
                        im.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    startActivity(new Intent(Intent.ACTION_MAIN)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                                            .addCategory("com.android.internal.category.PLATLOGO"));
                                } catch (ActivityNotFoundException ex) {
                                    Log.e("PlatLogoActivity", "No more eggs.");
                                }
                                if (FINISH) finish();
                            }
                        });
                        return true;
                    }
                });
                mTapCount++;
            }
        });
        // Enable hardware keyboard input for TV compatibility.
        im.setFocusable(true);
        im.requestFocus();
        im.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode != KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                    ++mKeyCount;
                    if (mKeyCount > 2) {
                        if (mTapCount > 5) {
                            im.performLongClick();
                        } else {
                            im.performClick();
                        }
                    }
                    return true;
                } else {
                    return false;
                }
            }
        });
        mLayout.addView(im, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
        im.animate().scaleX(1f).scaleY(1f).alpha(1f)
                .setInterpolator(mInterpolator)
                .setDuration(500)
                .setStartDelay(800)
                .start();
    }
}
