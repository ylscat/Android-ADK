package com.fangstar.keystore;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created at 2016/10/17.
 *
 * @author YinLanShan
 */
public class Utils {
    public static final ArrayBlockingQueue<Runnable> QUEUE =
            new ArrayBlockingQueue<>(10);
    public static final ThreadPoolExecutor EXECUTOR =
            new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS, QUEUE);
    public static final Handler HANDLER = new Handler();

    public static void genQrCode(final String content,
                                 final int size,
                                 final Callback<Bitmap> callback) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                long time = System.currentTimeMillis();

                try {
                    BitMatrix matrix = new MultiFormatWriter().encode(content,
                            BarcodeFormat.QR_CODE, size, size);
                    int w = matrix.getWidth();
                    int h = matrix.getHeight();
                    int[] pixels = new int[w*h];

                    long t = System.currentTimeMillis();
                    Log.d("Prof", "Bitmap: " + (t - time));
                    time = t;
                    for(int x = 0; x < w; x++) {
                        for(int y = 0; y < h; y++) {
                            if(matrix.get(x, y))
                                pixels[y*w + x] = Color.BLACK;
                            else
                                pixels[y*w + x] = Color.WHITE;
                        }
                    }
                    t = System.currentTimeMillis();
                    Log.d("Prof", "Bm set: " + (t - time));
                    final Bitmap bitmap = Bitmap.createBitmap(pixels,
                            w, h, Bitmap.Config.RGB_565);
                    if(callback != null) {
                        HANDLER.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onCallback(bitmap, content);
                            }
                        });
                    }
                }
                catch (WriterException e) {
                    if(callback != null) {
                        HANDLER.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onCallback(null, content);
                            }
                        });
                    }
                }


            }
        };
        EXECUTOR.execute(task);
    }
}
