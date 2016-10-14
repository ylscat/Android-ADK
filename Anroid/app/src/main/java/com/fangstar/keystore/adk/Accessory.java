package com.fangstar.keystore.adk;

import android.hardware.usb.UsbAccessory;
import android.os.ParcelFileDescriptor;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created at 2016/8/25.
 *
 * @author YinLanShan
 */
public class Accessory {
    public final UsbAccessory accessory;
    public final ParcelFileDescriptor fileDescriptor;
    public final FileInputStream inputStream;
    public final FileOutputStream outputStream;

    public Accessory(UsbAccessory accessory, ParcelFileDescriptor fd) {
        this.accessory = accessory;
        fileDescriptor = fd;
        inputStream = new FileInputStream(fd.getFileDescriptor());
        outputStream = new FileOutputStream(fd.getFileDescriptor());
    }

    public void release() {
        try {
            inputStream.close();
        } catch (IOException e) {
            //ignore
        }
        try {
            outputStream.close();
        } catch (IOException e) {
            //ignore
        }
        try {
            fileDescriptor.close();
        } catch (IOException e) {
            //ignore
        }
    }
}
