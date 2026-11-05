package com.limelight.launcher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.limelight.R;
import com.limelight.ui.TileFocusHelper;
import com.limelight.utils.UiHelper;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Xbox-style "My apps" drawer listing the device's installed Android apps, so the Moonlight
 * dashboard can be used as a full Android home screen.
 */
public class AndroidAppsView extends Activity {
    public static final String EXTRA_LAUNCHER_MODE = "LauncherMode";

    private static final int MENU_APP_INFO = 1;

    private final ArrayList<AppEntry> apps = new ArrayList<>();
    private AppsAdapter adapter;
    private ProgressBar loading;
    private volatile boolean destroyed;

    private static final class AppEntry {
        final ComponentName component;
        final String label;
        Drawable icon;

        AppEntry(ComponentName component, String label, Drawable icon) {
            this.component = component;
            this.label = label;
            this.icon = icon;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UiHelper.setLocale(this);
        setContentView(R.layout.activity_android_apps);
        UiHelper.notifyNewRootView(this);

        if (getIntent().getBooleanExtra(EXTRA_LAUNCHER_MODE, false)) {
            // Show the home screen wallpaper (e.g. the Xbox live wallpaper) behind the drawer
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            findViewById(R.id.androidAppsRoot).setBackgroundColor(
                    getResources().getColor(R.color.xbox_wallpaper_scrim));
        }

        findViewById(R.id.backButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.personalizeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PersonalizeHelper.show(AndroidAppsView.this);
            }
        });

        loading = findViewById(R.id.appsLoading);

        GridView grid = findViewById(R.id.fragmentView);
        adapter = new AppsAdapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                launch(apps.get(position));
            }
        });
        UiHelper.applyStatusBarPadding(grid);
        TileFocusHelper.attach(grid);
        registerForContextMenu(grid);
        grid.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-query every time so installs/uninstalls show up
        loadApps();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyed = true;
    }

    private void loadApps() {
        if (apps.isEmpty()) {
            loading.setVisibility(View.VISIBLE);
        }

        new Thread() {
            @Override
            public void run() {
                PackageManager pm = getPackageManager();
                Intent query = new Intent(Intent.ACTION_MAIN);
                query.addCategory(Intent.CATEGORY_LAUNCHER);

                List<ResolveInfo> infos = pm.queryIntentActivities(query, 0);
                final ArrayList<AppEntry> result = new ArrayList<>();
                for (ResolveInfo info : infos) {
                    if (info.activityInfo == null) {
                        continue;
                    }
                    ComponentName cn = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
                    CharSequence label = info.loadLabel(pm);
                    result.add(new AppEntry(cn, label != null ? label.toString() : cn.getPackageName(),
                            info.loadIcon(pm)));
                }

                final Collator collator = Collator.getInstance();
                Collections.sort(result, new Comparator<AppEntry>() {
                    @Override
                    public int compare(AppEntry a, AppEntry b) {
                        return collator.compare(a.label, b.label);
                    }
                });

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (destroyed) {
                            return;
                        }
                        apps.clear();
                        apps.addAll(result);
                        adapter.notifyDataSetChanged();
                        loading.setVisibility(View.GONE);
                    }
                });
            }
        }.start();
    }

    private void launch(AppEntry app) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(app.component);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.xbox_app_launch_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        menu.setHeaderTitle(apps.get(info.position).label);
        menu.add(0, MENU_APP_INFO, 0, R.string.xbox_app_info);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (item.getItemId() == MENU_APP_INFO && info != null && info.position < apps.size()) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", apps.get(info.position).component.getPackageName(), null));
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException ignored) {
            }
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private class AppsAdapter extends BaseAdapter {
        private final LayoutInflater inflater = LayoutInflater.from(AndroidAppsView.this);

        @Override
        public int getCount() {
            return apps.size();
        }

        @Override
        public Object getItem(int position) {
            return apps.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.android_app_grid_item, parent, false);
                convertView.findViewById(R.id.grid_card).setClipToOutline(true);
            }
            AppEntry app = apps.get(position);
            ((ImageView) convertView.findViewById(R.id.grid_image)).setImageDrawable(app.icon);
            ((TextView) convertView.findViewById(R.id.grid_text)).setText(app.label);
            return convertView;
        }
    }
}
