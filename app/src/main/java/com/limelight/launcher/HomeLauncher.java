package com.limelight.launcher;

import android.content.Intent;

import com.limelight.PcView;

/**
 * The Moonlight dashboard registered as an Android home screen (launcher).
 *
 * It is a separate activity from {@link PcView} so that Moonlight's normal launcher icon keeps
 * its usual behavior, while this one runs in its own singleTask "home" task, shows the system
 * wallpaper (e.g. the Xbox live wallpaper) behind the tiles, and never exits on Back.
 */
public class HomeLauncher extends PcView {
    @Override
    protected boolean isLauncherMode() {
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Pressing Home while already on the dashboard: just stay here
    }

    @Override
    public void onBackPressed() {
        // A home screen must not close when Back is pressed
    }
}
