package com.fangstar.keystore;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created at 2016/8/5.
 *
 * @author YinLanShan
 */
public class Main extends Activity {
    private ThreadPoolExecutor mExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ImageView iv = (ImageView) findViewById(R.id.qrcode);
        mExecutor = new ThreadPoolExecutor(
                4, 4, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10));
        iv.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, final int oldRight, int oldBottom) {
                if(right > left && bottom > top) {
                    v.removeOnLayoutChangeListener(this);
                    final int SIZE = Math.min(right - left, bottom - top);
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            long time = System.currentTimeMillis();
                            JSONObject json = new JSONObject();
                            try {
                                json.put("device", Build.SERIAL);
                                JSONArray array = new JSONArray();
                                for(int i = 0; i < 3; i++)
                                    array.put(String.format("E00123456%d", i));
                                json.put("in", array);
                                array = new JSONArray();
                                for(int i = 3; i < 4; i++)
                                    array.put(String.format("E00123456%d", i));
                                json.put("out", array);
                            } catch (JSONException e) {
                                //ignore
                            }
                            String jsonCode = Base64.encodeToString(json.toString().getBytes(),
                                    Base64.DEFAULT);
                            long t = System.currentTimeMillis();
                            Log.d("Prof", "JSON: " + (t - time));
                            time = t;
                            try {
                                BitMatrix matrix = new MultiFormatWriter().encode(
                                        "http://www.fangstar.net/fserp/key/app?type=box&" +
                                                "data=" + jsonCode, BarcodeFormat.QR_CODE, SIZE, SIZE);
                                int w = matrix.getWidth();
                                int h = matrix.getHeight();
                                int[] pixels = new int[w*h];

                                t = System.currentTimeMillis();
                                Log.d("Prof", "Bitmap: " + (t - time));
                                time = t;
                                for(int x = 0; x < w; x++) {
                                    for(int y = 0; y < h; y++) {
//                                        if(matrix.get(x, y))
//                                            bitmap.setPixel(x, y, Color.BLACK);
//                                        else
//                                            bitmap.setPixel(x, y, Color.WHITE);
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
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ImageView iv = (ImageView) findViewById(R.id.qrcode);
                                        iv.setImageBitmap(bitmap);
                                    }
                                });
                            }
                            catch (WriterException e) {
                                //ignore
                            }
                        }
                    });
                }
            }
        });

    }
}
