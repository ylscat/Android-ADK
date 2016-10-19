package com.fangstar.keystore.data;

import android.util.Log;

import com.fangstar.keystore.Utils;
import com.fangstar.keystore.adk.DataReceiver;

/**
 * Created at 2016/10/18.
 *
 * @author YinLanShan
 */
public class DataAssembler implements DataReceiver {
    private static final String TAG = "Assembler";
    private Response mCurrentResponse;
    private final AdkReceiver mReceiver;

    public DataAssembler(AdkReceiver receiver) {
        mReceiver = receiver;
    }

    @Override
    public void onDataReceive(byte[] buf, int length) {
        if (length > 0) {
            int index = 0;
            while (index < length) {
                Response response = mCurrentResponse;
                if (response == null) {
                    response = new Response();
                    mCurrentResponse = response;
                }

                int r = response.push(buf, index, length);
                if (r == 0) {
                    Log.e(TAG, "Resp Push Err");
                    int p = -1;
                    for (int i = 1; i < response.length; i++)
                        if (response.DATA[i] == '#') {
                            p = i;
                            int len = response.length - p;
                            System.arraycopy(response.DATA, p, response.DATA, 0, len);
                            response.length = len;
                            break;
                        }

                    if (p == -1) {
                        for (int i = 0; i < length; i++)
                            if (buf[i] == '#') {
                                p = i;
                                if (response.length > 0)
                                    mCurrentResponse = null;
                                index = p;
                            }
                    }

                    if (p == -1) {
                        if (response.length > 0)
                            mCurrentResponse = null;
                        return;
                    }
                } else {
                    index += r;
                    int check = response.check();
                    final Response resp = response;
                    if(check == 1) {
                        Utils.HANDLER.post(new Runnable() {
                            @Override
                            public void run() {
                               mReceiver.onResponse(resp);
                            }
                        });

                        mCurrentResponse = null;
                    }
                }
            }
        } else {
            final boolean en = length == 0;
            Utils.HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mReceiver.onAttachOrNot(en);
                }
            });
        }
    }

    public interface AdkReceiver {
        void onResponse(Response resp);
        void onAttachOrNot(boolean attached);
    }
}
