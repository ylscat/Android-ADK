package com.fangstar.keystore.adk;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

/**
 * Created at 2016/8/25.
 *
 * @author YinLanShan
 */
public class AccessoryManager extends BroadcastReceiver {
    private static final String TAG = "AccessoryManager";
    private static final String ACTION_USB_PERMISSION =
            "com.fangstar.keystore.USB_PERMISSION";

    private static Accessory sAccessory;
    private static DataReceiver sReceiver;
    private static AccessoryService mService;

    public static void registerDataReceiver(Context context, DataReceiver receiver) {
        if (receiver != null) {
            sReceiver = receiver;
        }
        if(sAccessory == null) {
            getAccessory(context);
        }
        else {
            mService.setDataReceiver(receiver);
            if(receiver != null)
                receiver.onDataReceive(null, 0);
        }
    }

    public static void unregisterDataReceiver(DataReceiver listener) {
        if (listener == sReceiver && listener != null) {
            sReceiver = null;
        }
    }

    private static void getAccessory(Context context) {
        UsbManager um = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        UsbAccessory[] accessories = um.getAccessoryList();
        if (accessories == null || accessories.length == 0) {
            Log.d(TAG, "No Accessory");
            return;
        }
        UsbAccessory accessory = accessories[0];

        if (um.hasPermission(accessory)) {
            ParcelFileDescriptor fd = um.openAccessory(accessory);
            setFileDescriptor(accessory, fd);
        } else {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, 0, new Intent(ACTION_USB_PERMISSION), 0);
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    context.unregisterReceiver(this);
                    UsbAccessory accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        UsbManager um = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                        ParcelFileDescriptor fd = um.openAccessory(accessory);
                        setFileDescriptor(accessory, fd);
                    } else {
                        Log.d(TAG, "Permission denied");
                    }
                }
            }, filter);
            new Exception("UM REQ PERM").printStackTrace();
            um.requestPermission(accessory, pendingIntent);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Toast.makeText(context, action, Toast.LENGTH_SHORT).show();
        if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
            if (sAccessory != null) {
                sAccessory.release();
                sAccessory = null;
                mService = null;
            }
            Log.d(TAG, "Accessory FD RELEASED!!!");
            if(sReceiver != null)
                sReceiver.onDataReceive(null, -1);
        }
        else {
            Log.d(TAG, action);
        }
    }

    private static void setFileDescriptor(UsbAccessory accessory, ParcelFileDescriptor fd) {
        if (fd == null) {
            Log.e(TAG, "Um return null fd");
        } else {
            Log.d(TAG, "Accessory FD GOT!!!");
            if (sAccessory != null && !sAccessory.accessory.equals(accessory)) {
                sAccessory.release();
            }
            sAccessory = new Accessory(accessory, fd);
            mService = new AccessoryService(sReceiver, sAccessory.inputStream);
            mService.start();
            if(sReceiver != null)
                sReceiver.onDataReceive(null, 0);
        }
    }

    public static boolean send(byte[] data, int length) {
        if(sAccessory == null)
            return false;
        try {
            sAccessory.outputStream.write(data, 0, length);
            return true;
        }
        catch (IOException e) {
            Log.e(TAG, "Output Err", e);
            return false;
        }
    }
}
