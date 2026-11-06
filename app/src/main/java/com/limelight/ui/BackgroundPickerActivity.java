package com.limelight.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import com.limelight.R;
import com.limelight.utils.UiHelper;

/**
 * Xbox-style "MoonlightX background" picker. Choosing an option (tap, or A on a controller)
 * saves it and returns to the previous screen, which then shows the new background.
 */
public class BackgroundPickerActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UiHelper.setLocale(this);
        setContentView(R.layout.activity_background_picker);
        UiHelper.notifyNewRootView(this);

        View defaultOption = findViewById(R.id.optionDefault);
        View grey = findViewById(R.id.optionGrey);

        defaultOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choose(XboxUi.BACKGROUND_DEFAULT);
            }
        });
        grey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choose(XboxUi.BACKGROUND_DARK_GREY);
            }
        });

        // Start with the current choice selected
        if (XboxUi.BACKGROUND_DARK_GREY.equals(XboxUi.getBackground(this))) {
            grey.requestFocus();
        }
        else {
            defaultOption.requestFocus();
        }
    }

    private void choose(String background) {
        XboxUi.setBackground(this, background);
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // "Select A to apply": treat the controller A button as a click on the focused option
        if (keyCode == KeyEvent.KEYCODE_BUTTON_A) {
            View focused = getCurrentFocus();
            if (focused != null && focused.isClickable()) {
                focused.performClick();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
