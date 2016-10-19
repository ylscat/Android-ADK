package com.fangstar.keystore;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.fangstar.keystore.adk.AccessoryManager;
import com.fangstar.keystore.data.DataAssembler;
import com.fangstar.keystore.data.Response;

import java.util.HashSet;

/**
 * Created at 2016/8/12.
 *
 * @author YinLanShan
 */
public class Debug extends Activity implements
        TextView.OnEditorActionListener,
        View.OnClickListener,
        DataAssembler.AdkReceiver {
    private static final String TAG = "DEBUG";

    private ArrayAdapter<String> mAdapter;
    private ListView mListView;
    private TextView mBtnQuery, mBtnOpen, mBtnScan, mResult, mState;

    private DataAssembler mAssembler;

    private HashSet<String> mKeys = new HashSet<>(240);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug);

        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        ListView lv = (ListView) findViewById(R.id.list);
        lv.setAdapter(mAdapter);
        mListView = lv;

        mBtnQuery = (TextView) findViewById(R.id.query);
        mBtnOpen = (TextView) findViewById(R.id.open);
        mBtnScan = (TextView) findViewById(R.id.scan);
        mResult = (TextView) findViewById(R.id.scan_result);
        mState = (TextView) findViewById(R.id.device_state);

        mBtnQuery.setOnClickListener(this);
        mBtnOpen.setOnClickListener(this);
        mBtnScan.setOnClickListener(this);

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
    public void onResponse(Response resp) {
        if(mAdapter.getCount() == 20)
            mAdapter.remove(mAdapter.getItem(0));
        mAdapter.add(resp.toString());
        mAdapter.notifyDataSetChanged();
        mListView.smoothScrollToPosition(mAdapter.getCount() - 1);
        if(resp.getCmd() == 0x30) {
            int len = resp.getCmd() - 1;
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < len; i++) {
                sb.append(String.format("%02X", resp.DATA[3 + i]));
            }
            if(mKeys.add(sb.toString())) {
                mResult.setText(String.valueOf(mKeys.size()));
            }
        }
    }

    @Override
    public void onAttachOrNot(boolean attached) {
        if(attached) {
            mState.setText("已连接");
            mState.setTextColor(getResources().getColor(R.color.green));
            setEnable(true, null);
        }
        else {
            mState.setText("未连接");
            mState.setTextColor(getResources().getColor(R.color.red));
            setEnable(false, null);
        }
    }

    private void setEnable(boolean en, View button) {
        mBtnQuery.setEnabled(en);
        mBtnQuery.setSelected(false);
        mBtnOpen.setEnabled(en);
        mBtnOpen.setSelected(false);
        mBtnScan.setEnabled(en);
        mBtnScan.setSelected(false);

        if(!en && button != null)
            button.setSelected(true);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if(event.getAction() != KeyEvent.ACTION_UP)
            return true;
        byte[] data = v.getText().toString().getBytes();
        AccessoryManager.send(data, data.length);
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.query:
                byte[] data = new byte[]{0x70};
                AccessoryManager.send(data, data.length);
                break;
            case R.id.open:
                data = new byte[]{0x20};
                AccessoryManager.send(data, data.length);
                break;
            case R.id.scan:
                if(mBtnScan.isSelected()) {
                    data = new byte[]{0x40};
                    AccessoryManager.send(data, data.length);
                    mBtnScan.setSelected(false);
                }
                else {
                    data = new byte[]{0x30};
                    AccessoryManager.send(data, data.length);
                    mBtnScan.setSelected(true);
                    mKeys.clear();
                    mResult.setText("0");
                }
                break;
        }
    }
}
