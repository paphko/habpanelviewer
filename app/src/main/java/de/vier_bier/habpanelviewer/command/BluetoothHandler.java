package de.vier_bier.habpanelviewer.command;

import android.Manifest;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;

/**
 * Handles BLUETOOTH_ON and BLUETOOTH_OFF commands.
 */
public class BluetoothHandler implements ICommandHandler {
    private final Context mContext;
    private final BluetoothManager mManager;

    public BluetoothHandler(Context ctx, BluetoothManager manager) {
        mContext = ctx;
        mManager = manager;
    }

    private boolean hasBluetoothPermission() {
        // Check BLUETOOTH_ADMIN permission (required for all versions)
        if (ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        // Check new permissions for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(mContext,
                    Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            if (ContextCompat.checkSelfPermission(mContext,
                    Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean handleCommand(Command cmd) {
        final String cmdStr = cmd.getCommand();

        if ("BLUETOOTH_ON".equals(cmdStr)) {
            if (!hasBluetoothPermission()) {
                cmd.failed("bluetooth permission missing");
            }

            cmd.start();
            mManager.getAdapter().enable();
        } else if ("BLUETOOTH_OFF".equals(cmdStr)) {
            if (!hasBluetoothPermission()) {
                cmd.failed("bluetooth permission missing");
            }

            cmd.start();
            mManager.getAdapter().disable();
        } else {
            return false;
        }

        cmd.finished();
        return true;
    }
}
