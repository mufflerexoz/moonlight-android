package com.limelight.ui;

import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;

/**
 * Gives grid tiles an Xbox dashboard style focus treatment.
 *
 * The platform list selector is disabled in our grid layouts, so this helper marks the
 * tile under the controller/remote selection as "activated". The tile layouts react to
 * that state with a white focus ring and a scale-up animation (see xbox_tile_state).
 * In touch mode nothing is activated, and tiles instead react to presses.
 */
public final class TileFocusHelper {
    private TileFocusHelper() {}

    public static void attach(final AbsListView listView) {
        listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refresh(listView);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                refresh(listView);
            }
        });

        listView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                refresh(listView);
            }
        });

        final ViewTreeObserver.OnGlobalLayoutListener layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Views are recycled while scrolling and on data changes, so re-sync after layout
                refresh(listView);
            }
        };
        final ViewTreeObserver.OnTouchModeChangeListener touchModeListener = new ViewTreeObserver.OnTouchModeChangeListener() {
            @Override
            public void onTouchModeChanged(boolean isInTouchMode) {
                refresh(listView);
            }
        };

        listView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                v.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
                v.getViewTreeObserver().addOnTouchModeChangeListener(touchModeListener);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                v.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
                v.getViewTreeObserver().removeOnTouchModeChangeListener(touchModeListener);
            }
        });

        if (listView.isAttachedToWindow()) {
            listView.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
            listView.getViewTreeObserver().addOnTouchModeChangeListener(touchModeListener);
        }
    }

    private static void refresh(AbsListView listView) {
        int selected = AdapterView.INVALID_POSITION;
        if (listView.hasFocus() && !listView.isInTouchMode()) {
            selected = listView.getSelectedItemPosition();
        }

        int first = listView.getFirstVisiblePosition();
        for (int i = 0; i < listView.getChildCount(); i++) {
            View child = listView.getChildAt(i);
            boolean activated = (first + i) == selected;
            if (child.isActivated() != activated) {
                child.setActivated(activated);
            }
        }
    }
}
