package com.fangstar.keystore;

import android.util.Log;

import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created at 2016/8/24.
 *
 * @author YinLanShan
 */
public class ReadTest {
    private static ExecutorService executor = Executors.newFixedThreadPool(2);
    private static class ReadTask implements Callable<Integer> {
        private InputStream mIs;
        private byte[] mBuf = new byte[64];

        public ReadTask(InputStream is) {
            this.mIs = is;
        }

        @Override
        public Integer call() throws Exception {
            int n = mIs.read(mBuf);
            Log.d("Read", "Read " + n);
            return n;
        }

    }

    public static int read(InputStream is) {
        Future<Integer> future = executor.submit(new ReadTask(is));
        try {
            int r = future.get(2000, TimeUnit.MILLISECONDS);
            Log.d("Read", "Future " + r);
            return r;
        } catch (InterruptedException e) {
            return -3;
        } catch (ExecutionException e) {
            return -2;
        } catch (TimeoutException e) {
            return -1;
        }

    }
}
