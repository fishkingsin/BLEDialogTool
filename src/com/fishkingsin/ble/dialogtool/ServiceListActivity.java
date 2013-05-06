package com.fishkingsin.ble.dialogtool;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

public class ServiceListActivity extends Activity {
	private BLEService mService = null;
	private ServiceConnection onService = null;
	private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            // Gatt device found message.
            case BLEService.GATT_DEVICE_FOUND_MSG:
                
                break;
            default:
                super.handleMessage(msg);
            }
        }
    };
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_list);
        onService = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder rawBinder) {
                mService = ((BLEService.LocalBinder) rawBinder).getService();
                if (mService != null) {
                    mService.setDeviceListHandler(mHandler);
                }

            }

            public void onServiceDisconnected(ComponentName classname) {
                mService = null;
            }
        };
	}

}
