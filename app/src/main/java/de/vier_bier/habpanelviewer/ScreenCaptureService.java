package de.vier_bier.habpanelviewer;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * Foreground service that owns the MediaProjection and a ScreenCapturer
 * instance.
 */
public class ScreenCaptureService extends Service {
    private static final String TAG = "HPV-ScrCapService";
    public static final String ACTION_START = "de.vier_bier.habpanelviewer.action.START_CAPTURE";
    public static final String ACTION_STOP = "de.vier_bier.habpanelviewer.action.STOP_CAPTURE";
    public static final String EXTRA_RESULT_CODE = "de.vier_bier.habpanelviewer.extra.RESULT_CODE";
    public static final String EXTRA_RESULT_INTENT = "de.vier_bier.habpanelviewer.extra.RESULT_INTENT";

    private static final String CHANNEL_ID = "screen_capture_channel";
    private static final int NOTIF_ID = 1001;

    private final IBinder binder = new LocalBinder();

    private MediaProjection mProjection;
    private ScreenCapturer mCapturer;

    public class LocalBinder extends Binder {
        public ScreenCaptureService getService() {
            return ScreenCaptureService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_START.equals(action)) {
            int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED);
            Intent resultData = intent.getParcelableExtra(EXTRA_RESULT_INTENT);

            NotificationCompat.Builder nb = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.capturing_screen))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, nb.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            } else {
                startForeground(NOTIF_ID, nb.build());
            }

            if (resultData != null && mProjection == null) {
                try {
                    MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(
                            Context.MEDIA_PROJECTION_SERVICE);
                    if (projectionManager != null) {
                        mProjection = projectionManager.getMediaProjection(resultCode, resultData);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "getMediaProjection failed in service", e);
                }

                if (mProjection != null) {
                    try {
                        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                        Display display = (wm != null) ? wm.getDefaultDisplay() : null;
                        DisplayMetrics metrics = new DisplayMetrics();
                        Point size = new Point();
                        if (display != null) {
                            display.getMetrics(metrics);
                            display.getSize(size);
                        } else {
                            // Fallback
                            metrics = getResources().getDisplayMetrics();
                            size.x = metrics.widthPixels;
                            size.y = metrics.heightPixels;
                        }

                        mCapturer = new ScreenCapturer(mProjection, size.x, size.y, metrics.densityDpi);
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to create ScreenCapturer", e);
                    }
                } else {
                    Log.w(TAG, "MediaProjection is null in service");
                }
            }
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        if (mCapturer != null) {
            mCapturer.terminate();
            mCapturer = null;
        }
        if (mProjection != null) {
            mProjection.stop();
            mProjection = null;
        }
        stopForeground(true);
        super.onDestroy();
    }

    public ScreenCapturer getCapturer() {
        return mCapturer;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.capturing_screen),
                        NotificationManager.IMPORTANCE_LOW);
                nm.createNotificationChannel(channel);
            }
        }
    }
}