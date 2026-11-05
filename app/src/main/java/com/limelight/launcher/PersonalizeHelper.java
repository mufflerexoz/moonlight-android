package com.limelight.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;
import android.widget.Toast;

import com.limelight.R;
import com.limelight.wallpaper.XboxWallpaperService;

/**
 * "Personalize" menu: apply the Xbox live wallpaper and/or make Moonlight the home screen.
 */
public final class PersonalizeHelper {
    private PersonalizeHelper() {}

    public static void show(final Activity activity) {
        CharSequence[] items = {
                activity.getString(R.string.xbox_set_live_wallpaper),
                activity.getString(R.string.xbox_set_home_launcher),
        };

        new AlertDialog.Builder(activity)
                .setTitle(R.string.xbox_personalize)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            setLiveWallpaper(activity);
                        }
                        else {
                            chooseHomeLauncher(activity);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public static void setLiveWallpaper(Activity activity) {
        Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                new ComponentName(activity, XboxWallpaperService.class));
        if (tryStart(activity, intent)) {
            return;
        }

        // Some devices only support the generic live wallpaper picker
        if (!tryStart(activity, new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))) {
            Toast.makeText(activity, R.string.xbox_wallpaper_unsupported, Toast.LENGTH_LONG).show();
        }
    }

    public static void chooseHomeLauncher(Activity activity) {
        if (tryStart(activity, new Intent(Settings.ACTION_HOME_SETTINGS))) {
            return;
        }

        // Fallback: firing a HOME intent shows the "choose home app" dialog on most devices
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (!tryStart(activity, home)) {
            tryStart(activity, new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private static boolean tryStart(Activity activity, Intent intent) {
        try {
            activity.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }
}
