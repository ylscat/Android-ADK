package com.fangstar.keystore;

/**
 * Created at 2016/10/17.
 *
 * @author YinLanShan
 */
public interface Callback<T> {
    void onCallback(T data, Object tag);
}
