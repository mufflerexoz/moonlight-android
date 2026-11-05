package com.limelight.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;

import com.limelight.R;

/**
 * Helpers for the Xbox-style dashboard look.
 */
public final class XboxUi {
    private XboxUi() {}

    public static final String BACKGROUND_BLUR_GLASS = "blur_glass";
    public static final String BACKGROUND_DARK_GREY = "dark_grey";

    private static final String PREFS_NAME = "xbox_ui";
    private static final String PREF_BACKGROUND = "background";

    public static String getBackground(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(PREF_BACKGROUND, BACKGROUND_BLUR_GLASS);
    }

    public static void setBackground(Context context, String background) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_BACKGROUND, background)
                .apply();
    }

    /**
     * Applies the chosen app background (Blur Glass by default, or Dark Grey) to this
     * activity's window. Safe to call again in onResume() to pick up a new choice.
     */
    public static void applyBackground(Activity activity) {
        if (BACKGROUND_DARK_GREY.equals(getBackground(activity))) {
            activity.getWindow().setBackgroundDrawableResource(R.color.xbox_dark_grey);
        }
        else {
            activity.getWindow().setBackgroundDrawableResource(R.drawable.xbox_background);
        }
    }

    /**
     * Makes the MoonlightX logo in the header open the background picker.
     */
    public static void attachLogoClick(final Activity activity) {
        View logo = activity.findViewById(R.id.xboxLogo);
        if (logo != null) {
            logo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.startActivity(new Intent(activity, BackgroundPickerActivity.class));
                }
            });
        }
    }

    /**
     * Hides the system status bar for a clean dashboard look and trims the
     * header on narrow screens. Call after setContentView().
     */
    public static void applyChrome(Activity activity) {
        applyBackground(activity);
        attachLogoClick(activity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = activity.getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
        else {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        // Narrow (portrait phone) screens: keep the logo and buttons, drop the home title text
        if (activity.getResources().getConfiguration().screenWidthDp < 480) {
            View title = activity.findViewById(R.id.xboxTitleBlock);
            if (title != null) {
                title.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Keeps exactly one tile "highlighted" (activated), like the Xbox home. The highlighted
     * tile grows slightly and shows its title in a semi-transparent black band. The highlight
     * follows controller/remote selection and the last tile touched.
     */
    @SuppressLint("ClickableViewAccessibility")
    public static void attachTileHighlight(final AbsListView listView) {
        final int[] highlighted = {0};

        listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                highlighted[0] = position;
                refresh(listView, highlighted[0]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Keep the last highlight
            }
        });

        listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    int position = listView.pointToPosition((int) event.getX(), (int) event.getY());
                    if (position != AdapterView.INVALID_POSITION) {
                        highlighted[0] = position;
                        refresh(listView, position);
                    }
                }
                // Never consume: clicks and long-press menus keep working
                return false;
            }
        });

        // Children are recycled on scroll and data changes, so re-apply after every layout
        listView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (listView.getCount() > 0 && highlighted[0] >= listView.getCount()) {
                    highlighted[0] = listView.getCount() - 1;
                }
                refresh(listView, highlighted[0]);
            }
        });
    }

    private static void refresh(AbsListView listView, int highlighted) {
        int first = listView.getFirstVisiblePosition();
        for (int i = 0; i < listView.getChildCount(); i++) {
            View child = listView.getChildAt(i);
            boolean activated = (first + i) == highlighted;
            if (child.isActivated() != activated) {
                child.setActivated(activated);
            }
        }
    }
}
