package com.fangstar.keystore;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.fangstar.keystore.adk.AccessoryManager;
import com.fangstar.keystore.data.DataAssembler;
import com.fangstar.keystore.data.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created at 2016/8/5.
 *
 * @author YinLanShan
 */
public class Main extends Activity implements
        View.OnClickListener,
        DataAssembler.AdkReceiver {
    private ImageView mQrCode;
    private View mShowButton, mQrControl;
    private TextView mCountDown;
    private TextView mDeviceState;

    private QrCodeController mController;
    private DataAssembler mAssembler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mShowButton = findViewById(R.id.show_qrcode);
        mShowButton.setOnClickListener(this);
        mQrControl = findViewById(R.id.qr_control);
        mQrCode = (ImageView) findViewById(R.id.qrcode);
        mCountDown = (TextView) findViewById(R.id.count_down);
        mDeviceState = (TextView) findViewById(R.id.device_state);
        findViewById(R.id.cancel).setOnClickListener(this);
        findViewById(R.id.settings).setOnClickListener(this);

        mController = new QrCodeController();

        onAttachOrNot(false);
        mAssembler = new DataAssembler(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AccessoryManager.registerDataReceiver(this, mAssembler);
    }

    @Override
    protected void onPause() {
        super.onPause();
        AccessoryManager.unregisterDataReceiver(mAssembler);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.show_qrcode:
                mController.show();
                break;
            case R.id.cancel:
                mController.hide();
                break;
            case R.id.settings:
                Intent intent = new Intent(this, Debug.class);
                startActivity(intent);
                break;
        }
    }

    @Override
    public void onResponse(Response resp) {

    }

    @Override
    public void onAttachOrNot(boolean attached) {
        if(attached) {
            mDeviceState.setText("设备已连接");
            mDeviceState.setTextColor(getResources().getColor(R.color.green));
        }
        else {
            mDeviceState.setText("设备未连接");
            mDeviceState.setTextColor(getResources().getColor(R.color.red));
        }
    }

    class QrCodeController {
        static final int COUNTDOWN = 60;
        int mRemain;
        Timer mTimer = new Timer(true);
        Runnable mUiTask = new Runnable() {
            @Override
            public void run() {
                mCountDown.setText(String.valueOf(--mRemain));
                if(mRemain == 0)
                    hide();
            }
        };
        TimerTask mTicker;

        public void show() {
            mQrCode.setImageDrawable(null);
            generateQrCode();
            mQrCode.setVisibility(View.VISIBLE);
            mShowButton.setVisibility(View.GONE);
            mQrControl.setVisibility(View.VISIBLE);
            mRemain = COUNTDOWN;
            mCountDown.setText(String.valueOf(mRemain));
            mTicker = new TimerTask() {
                @Override
                public void run() {
                    mCountDown.post(mUiTask);
                }
            };
            mTimer.scheduleAtFixedRate(mTicker, 100, 1000);
        }

        public void hide() {
            mQrCode.setVisibility(View.GONE);
            mShowButton.setVisibility(View.VISIBLE);
            mQrControl.setVisibility(View.INVISIBLE);
            mCountDown.setText(null);
            if(mTicker != null)
                mTicker.cancel();
            mTimer.purge();
        }

        private void generateQrCode() {
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
            String url = "http://www.fangstar.net/fserp/key/app?type=box&" +
                    "data=" + jsonCode;
            Utils.genQrCode(url, 600, new Callback<Bitmap>() {
                @Override
                public void onCallback(Bitmap data, Object tag) {
                    ImageView iv = (ImageView) findViewById(R.id.qrcode);
                    iv.setImageBitmap(data);
                }
            });
        }
    }
}
