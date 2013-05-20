package com.fishkingsin.ble.dialogtool;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

public class CommandListActivity extends Activity {
    class CommandData {
        byte[] data;
        String name;

        CommandData(String name, byte[] d) {
            this.name = name;
            data = d;
        }
    }

    final static String TAG = "CommandListActivity";
    final static String COMMAND_LIST_RETURN = "COMMAND_LIST_RETURN";

    final byte[] TEST_MODE_ON = {(byte) 0xAA, (byte) 0x50, (byte) 0xF0, (byte) 0x00, (byte) 0x03, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x55};
    final byte[] TEST_MODE_OFF = {(byte) 0xAA, (byte) 0x50, (byte) 0xF0, (byte) 0x00, (byte) 0x03, (byte) 0x01, (byte) 0xFF, (byte) 0xFF, (byte) 0x55};

    final byte[] TEST_LED1 = {(byte) 0xAA, (byte) 0x50, (byte) 0xF0, (byte) 0x00, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x55};
    final byte[] TEST_LED2 = {(byte) 0xAA, (byte) 0x50, (byte) 0xF0, (byte) 0x00, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x55};
    final byte[] TEST_LED3 = {(byte) 0xAA, (byte) 0x50, (byte) 0xF0, (byte) 0x00, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x55};
    final byte[] TEST_LED4 = {(byte) 0xAA, (byte) 0x50, (byte) 0xF0, (byte) 0x00, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x04, (byte) 0x55};
    final byte[] TEST_LED5 = {(byte) 0xAA, (byte) 0x50, (byte) 0xF0, (byte) 0x00, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x05, (byte) 0x55};
    final byte[] TEST_LED6 = {(byte) 0xAA, (byte) 0x50, (byte) 0xF0, (byte) 0x00, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x06, (byte) 0x55};

    //	AA-50-F1-00-03-01-02-01-55
    final byte[] ACC = {(byte) 0xAA, (byte) 0x50, (byte) 0xF1, (byte) 0x00, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x55};
    final byte[] SERIAL = {(byte) 0xAA, (byte) 0x50, (byte) 0xF1, (byte) 0x00, (byte) 0x03, (byte) 0x01, (byte) 0x05, (byte) 0x01, (byte) 0x55};
    final byte[] CURRENT = {(byte) 0xAA, (byte) 0x50, (byte) 0xF0, (byte) 0x00, (byte) 0x03, (byte) 0x01, (byte) 0x06, (byte) 0x05, (byte) 0x55};
    final byte[] STREAM_MODE = {(byte) 0xAA, (byte) 0x50, (byte) 0xF1, (byte) 0x00, (byte) 0x04, (byte) 0x02, (byte) 0x01, (byte) 0x05, (byte) 0x01, (byte) 0x55};
    final byte[] VERSION = {(byte) 0xAA, (byte) 0x50, (byte) 0xF1, (byte) 0x00, (byte) 0x03, (byte) 0x01, (byte) 0x07, (byte) 0x01, (byte) 0x55};

    final byte[] SSMART_WRATCH_01 = {(byte) 0x01};
    final byte[] SSMART_WRATCH_02 = {(byte) 0x02};
    final byte[] SSMART_WRATCH_03 = {(byte) 0x03};
    final byte[] SSMART_WRATCH_04 = {(byte) 0x04};
    final byte[] SSMART_WRATCH_05 = {(byte) 0x05};
    final byte[] SSMART_WRATCH_06 = {(byte) 0x06};
    final byte[] SSMART_WRATCH_07 = {(byte) 0x07};

    final byte[] SSMART_WRATCH_00 = {(byte) 0x0};
    final byte[] SSMART_WRATCH_12_ON = {12, 0, 20, 2, 1};
    final byte[] SSMART_WRATCH_12_OFF = {12, 0, 0, 0, 0};
    final byte[] SSMART_WRATCH_11 = {11};


    List<CommandData> commandList;
    CommandAdapter commandAdapter = null;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Log.d(TAG, "onCreate");
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar);
        setContentView(R.layout.commmand_list);
        Button cancelButton = (Button) findViewById(R.id.btn_cancel);
        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
//        setContentView(R.layout.commmand_list);
        commandList = new ArrayList<CommandData>();
        commandAdapter = new CommandAdapter(this, commandList);


        ListView newDevicesListView = (ListView) findViewById(R.id.new_command);
        newDevicesListView.setAdapter(commandAdapter);
        newDevicesListView.setOnItemClickListener(mClickListener);
        commandList.add(new CommandData("TEST_MODE_ON", TEST_MODE_ON));
        commandList.add(new CommandData("TEST_MODE_OFF", TEST_MODE_OFF));
        commandList.add(new CommandData("TEST_LED1", TEST_LED1));
        commandList.add(new CommandData("TEST_LED2", TEST_LED2));
        commandList.add(new CommandData("TEST_LED3", TEST_LED3));
        commandList.add(new CommandData("TEST_LED4", TEST_LED4));
        commandList.add(new CommandData("TEST_LED5", TEST_LED5));
        commandList.add(new CommandData("TEST_LED6", TEST_LED6));
        commandList.add(new CommandData("ACC", ACC));
        commandList.add(new CommandData("SERIAL", SERIAL));
        commandList.add(new CommandData("CURRENT", CURRENT));
        commandList.add(new CommandData("STREAM_MODE", STREAM_MODE));
        commandList.add(new CommandData("VERSION", VERSION));
        commandList.add(new CommandData("SSMART_WRATCH_01", SSMART_WRATCH_01));
        commandList.add(new CommandData("SSMART_WRATCH_02", SSMART_WRATCH_02));
        commandList.add(new CommandData("SSMART_WRATCH_03", SSMART_WRATCH_03));
        commandList.add(new CommandData("SSMART_WRATCH_04", SSMART_WRATCH_04));
        commandList.add(new CommandData("SSMART_WRATCH_05", SSMART_WRATCH_05));
        commandList.add(new CommandData("SSMART_WRATCH_06", SSMART_WRATCH_06));
        commandList.add(new CommandData("SSMART_WRATCH_07", SSMART_WRATCH_07));
        commandList.add(new CommandData("SSMART_WRATCH_00", SSMART_WRATCH_00));
        commandList.add(new CommandData("SSMART_WRATCH_11", SSMART_WRATCH_11));
        commandList.add(new CommandData("SSMART_WRATCH_12_ON", SSMART_WRATCH_12_ON));
        commandList.add(new CommandData("SSMART_WRATCH_12_OFF", SSMART_WRATCH_12_OFF));


        commandAdapter.notifyDataSetChanged();
    }

    private OnItemClickListener mClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Bundle b = new Bundle();
            byte[] d = commandList.get(position).data;
            String s = "";
            for (int i = 0; i < d.length; i++) {
                s += "," + (int) d[i];

            }
            Log.v(TAG, s);
            b.putByteArray(COMMAND_LIST_RETURN, d);

            Intent result = new Intent();
            result.putExtras(b);

            setResult(Activity.RESULT_OK, result);
            finish();


        }
    };

    class CommandAdapter extends BaseAdapter {
        Context context;
        List<CommandData> btyes;
        LayoutInflater inflater;

        public CommandAdapter(Context context, List<CommandData> btyes) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            this.btyes = btyes;
        }

        @Override
        public int getCount() {
            return btyes.size();
        }

        @Override
        public Object getItem(int position) {
            return btyes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewGroup vg;

            if (convertView != null) {
                vg = (ViewGroup) convertView;
            } else {
                vg = (ViewGroup) inflater.inflate(R.layout.command_element, null);
            }
            final TextView tvadd = ((TextView) vg.findViewById(R.id.name));

            tvadd.setText(commandList.get(position).name);


            return vg;
        }
    }
}
