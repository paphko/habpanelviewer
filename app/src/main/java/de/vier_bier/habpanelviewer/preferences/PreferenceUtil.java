package de.vier_bier.habpanelviewer.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.obsez.android.lib.filechooser.ChooserDialog;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import de.vier_bier.habpanelviewer.Constants;
import de.vier_bier.habpanelviewer.R;
import de.vier_bier.habpanelviewer.UiUtil;

class PreferenceUtil {
    private static final String TAG = "HPV-PreferenceUtil";

    /**
     * Returns true if the preferences directory requires READ/WRITE external
     * storage
     * permission (i.e., it's the public external documents directory). Otherwise
     * returns false and no runtime permission is necessary.
     */
    public static boolean needsExternalStoragePermission(Context ctx) {
        try {
            File prefDir = getPreferenceDirectory(ctx);
            File publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            if (prefDir == null || publicDir == null)
                return false;
            String prefPath = prefDir.getCanonicalPath();
            String publicPath = publicDir.getCanonicalPath();
            return prefPath.equals(publicPath);
        } catch (IOException e) {
            return false;
        }
    }

    static void saveSharedPreferencesToFile(Context ctx, View v) {
        ChooserDialog d = new ChooserDialog(ctx)
                .withFilter(true, false)
                .withStartFile(getPreferenceDirectory(ctx).getPath())
                .withResources(R.string.chooseTargetDirectory, R.string.okay, R.string.cancel)
                .withChosenListener(
                        (path, pathFile) -> saveSharedPreferencesToFile(ctx, v, new File(path, "HPV.prefs")));

        d.build().show();
    }

    static void loadSharedPreferencesFromFile(Activity ctx, View v) {
        new ChooserDialog(ctx)
                .withFilter(file -> "HPV.prefs".equals(file.getName()) || file.isDirectory())
                .withStartFile(getPreferenceDirectory(ctx).getPath())
                .withResources(R.string.choose_file, R.string.okay, R.string.cancel)
                .withChosenListener((path, pathFile) -> {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
                    try {
                        loadSharedPreferencesFromFile(ctx, pathFile);

                        if (UiUtil.themeChanged(prefs, ctx)) {
                            UiUtil.showSnackBar(v, R.string.themeChangedRestartRequired, R.string.action_restart,
                                    view -> EventBus.getDefault().post(new Constants.Restart()));
                        } else {
                            UiUtil.showSnackBar(v, R.string.prefsImported);
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                        UiUtil.showSnackBar(v, R.string.prefsImportFailed);
                    }
                })
                .build()
                .show();
    }

    private static File getPreferenceDirectory(Context ctx) {
        // Use app-specific external files directory for scoped storage compatibility
        // (API 30+)
        File externalFilesDir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (externalFilesDir != null && (externalFilesDir.exists() || externalFilesDir.mkdirs())) {
            return externalFilesDir;
        }
        // Fallback to public documents directory for older APIs
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
    }

    private static void saveSharedPreferencesToFile(Context ctx, View v, File dst) {
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(dst))) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
            output.writeObject(pref.getAll());
            UiUtil.showSnackBar(v, R.string.prefsExported);
        } catch (IOException e) {
            UiUtil.showSnackBar(v, R.string.prefsExportFailed);
        }
    }

    // Stream-based helpers for Storage Access Framework (SAF)
    public static void saveSharedPreferencesToStream(Context ctx, java.io.OutputStream os) throws IOException {
        try (ObjectOutputStream output = new ObjectOutputStream(os)) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
            output.writeObject(pref.getAll());
        }
    }

    @SuppressWarnings({ "unchecked" })
    public static void loadSharedPreferencesFromStream(Context ctx, java.io.InputStream is)
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream input = new ObjectInputStream(is)) {
            SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
            prefEdit.clear();
            Map<String, ?> entries = (Map<String, ?>) input.readObject();
            for (Map.Entry<String, ?> entry : entries.entrySet()) {
                Object v = entry.getValue();
                String key = entry.getKey();

                if (v instanceof Boolean)
                    prefEdit.putBoolean(key, (Boolean) v);
                else if (v instanceof Float)
                    prefEdit.putFloat(key, (Float) v);
                else if (v instanceof Integer)
                    prefEdit.putInt(key, (Integer) v);
                else if (v instanceof Long)
                    prefEdit.putLong(key, (Long) v);
                else if (v instanceof String)
                    prefEdit.putString(key, ((String) v));
                else {
                    Log.d(TAG, "could not restore preference of class " + v.getClass());
                }
            }
            prefEdit.apply();
        }
    }

    @SuppressWarnings({ "unchecked" })
    private static void loadSharedPreferencesFromFile(Context ctx, File src)
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(src))) {
            SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
            prefEdit.clear();
            Map<String, ?> entries = (Map<String, ?>) input.readObject();
            for (Map.Entry<String, ?> entry : entries.entrySet()) {
                Object v = entry.getValue();
                String key = entry.getKey();

                if (v instanceof Boolean)
                    prefEdit.putBoolean(key, (Boolean) v);
                else if (v instanceof Float)
                    prefEdit.putFloat(key, (Float) v);
                else if (v instanceof Integer)
                    prefEdit.putInt(key, (Integer) v);
                else if (v instanceof Long)
                    prefEdit.putLong(key, (Long) v);
                else if (v instanceof String)
                    prefEdit.putString(key, ((String) v));
                else {
                    Log.d(TAG, "could not restore preference of class " + v.getClass());
                }
            }
            prefEdit.apply();
        }
    }
}
