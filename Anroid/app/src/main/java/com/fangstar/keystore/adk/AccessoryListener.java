package com.fangstar.keystore.adk;

import android.os.ParcelFileDescriptor;

public interface AccessoryListener {
    void onAccessoryAttached(Accessory accessory);
    void onAccessoryDetached(Accessory accessory);
}