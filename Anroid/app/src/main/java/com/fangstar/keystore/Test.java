package com.fangstar.keystore;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.fangstar.keystore.adk.AccessoryManager;
import com.fangstar.keystore.adk.DataReceiver;

/**
 * Created at 2016/8/12.
 *
 * @author YinLanShan
 */
public class Test extends Activity implements
        TextView.OnEditorActionListener,
        View.OnClickListener, DataReceiver {
    private static final String TAG = "ADK";

    private ArrayAdapter<String> mAdapter;
    private EditText mInput;
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test);

        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        ListView lv = (ListView) findViewById(R.id.list);
        lv.setAdapter(mAdapter);
        mListView = lv;
        mInput = (EditText) findViewById(R.id.input);
        mInput.setOnEditorActionListener(this);
        findViewById(R.id.send).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AccessoryManager.registerDataReceiver(this, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AccessoryManager.unregisterDataReceiver(this);
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
            case R.id.send:
                byte[] data = mInput.getText().toString().getBytes();
                AccessoryManager.send(data, data.length);
                break;
        }
    }

    @Override
    public void onDataReceive(byte[] buf, int length) {
        if(length > 0){
            final String str = new String(buf, 0, length);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.add(str);
                    mAdapter.notifyDataSetChanged();
                    mListView.setSelection(mAdapter.getCount() - 1);
                }
            });
        }
        else {
            final boolean en = length == 0;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mInput.setEnabled(en);
                }
            });
        }
    }
}
