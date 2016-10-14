package com.fangstar.keystore.adk;

/**
 * Created at 2016/8/25.
 *
 * @author YinLanShan
 */
public interface DataReceiver {
    void onDataReceive(byte[] buf, int length);
}
