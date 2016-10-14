package com.fangstar.keystore.adk;

import android.os.Process;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created at 2016/8/25.
 *
 * @author YinLanShan
 */
public class AccessoryService extends Thread {
    private static final String TAG = "AccessoryService";

    private DataReceiver mReceiver;
    private FileInputStream mInputStream;

    public AccessoryService(DataReceiver mReceiver, FileInputStream mInputStream) {
        this.mReceiver = mReceiver;
        this.mInputStream = mInputStream;
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        byte[] buf = new byte[64];

        try {
            while (true) {
                int len = mInputStream.read(buf);
                if(mReceiver != null)
                    mReceiver.onDataReceive(buf, len);
            }
        }
        catch (IOException e) {
            Log.e(TAG, "Read Err", e);
        }

        Log.i(TAG, "Stopped");
    }

    public void setDataReceiver(DataReceiver receiver) {
        mReceiver = receiver;
    }
}
