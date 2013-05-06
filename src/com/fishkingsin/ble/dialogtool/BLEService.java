/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fishkingsin.ble.dialogtool;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import com.samsung.android.sdk.bt.gatt.BluetoothGatt;
import com.samsung.android.sdk.bt.gatt.BluetoothGattAdapter;
import com.samsung.android.sdk.bt.gatt.BluetoothGattCallback;
import com.samsung.android.sdk.bt.gatt.BluetoothGattCharacteristic;
import com.samsung.android.sdk.bt.gatt.BluetoothGattDescriptor;
import com.samsung.android.sdk.bt.gatt.BluetoothGattServer;
import com.samsung.android.sdk.bt.gatt.BluetoothGattServerCallback;
import com.samsung.android.sdk.bt.gatt.BluetoothGattService;
import com.samsung.android.sdk.bt.gatt.MutableBluetoothGattCharacteristic;
import com.samsung.android.sdk.bt.gatt.MutableBluetoothGattService;

public class BLEService extends Service {
    static final String TAG = "BLEService";

    public static final UUID IMMEDIATE_ALERT_UUID = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    public static final UUID LINK_LOSS_UUID = UUID.fromString("00001803-0000-1000-8000-00805f9b34fb");
    public static final UUID TX_POWER_UUID = UUID.fromString("00001804-0000-1000-8000-00805f9b34fb");
    public static final UUID ALERT_LEVEL_UUID = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
    public static final UUID TX_POWER_LEVEL_UUID = UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");
    public static final UUID CCC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID FIRMWARE_REVISON_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static final UUID DIS_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");

    public static final int BLE_CONNECT_MSG = 21;
    public static final int BLE_DISCONNECT_MSG = 22;
    public static final int BLE_READY_MSG = 23;
    public static final int BLE_VALUE_MSG = 24;
    //James Kong 20130506
    //------------------------------------------------------
    public static final int BLE_SERVICE_DISCOVER_MSG = 25;
    //------------------------------------------------------
    public static final int GATT_DEVICE_FOUND_MSG = 25;
    public static final int GATT_CHARACTERISTIC_RSSI_MSG = 26;
    public static final int PROXIMITY_ALERT_LEVEL_CHANGED_MSG = 27;

    /** Source of device entries in the device list */
    public static final int DEVICE_SOURCE_SCAN = 10;
    public static final int DEVICE_SOURCE_BONDED = 11;
    public static final int DEVICE_SOURCE_CONNECTED = 12;

    /** Intent extras */
    public static final String EXTRA_DEVICE = "DEVICE";
    public static final String EXTRA_RSSI = "RSSI";
    public static final String EXTRA_SOURCE = "SOURCE";
    public static final String EXTRA_ADDR = "ADDRESS";
    public static final String EXTRA_CONNECTED = "CONNECTED";
    public static final String EXTRA_STATUS = "STATUS";
    public static final String EXTRA_UUID = "UUID";
    public static final String EXTRA_VALUE = "VALUE";

    public static final byte NO_ALERT = 0;
    public static final byte LOW_ALERT = 1;
    public static final byte HIGH_ALERT = 2;

    private BluetoothAdapter mBtAdapter = null;
    public BluetoothGatt mBluetoothGatt = null;
    public BluetoothGattServer mBluetoothGattServer = null;
    private Handler mActivityHandler = null;
    private Handler mDeviceListHandler = null;
    public boolean isNoti = false;

    /**
     * Profile service connection listener
     */
    public class LocalBinder extends Binder {
        BLEService getService() {
            return BLEService.this;
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return binder;
    }

    private final IBinder binder = new LocalBinder();

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        if (mBtAdapter == null) {
            mBtAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBtAdapter == null)
                return;
        }
        BluetoothGattAdapter.getProfileProxy(this, mProfileServiceListener, BluetoothGattAdapter.GATT);
        BluetoothGattAdapter.getProfileProxy(this, mProfileServiceListener1, BluetoothGattAdapter.GATT_SERVER);

    }

    public void setActivityHandler(Handler mHandler) {
        Log.d(TAG, "Activity Handler set");
        mActivityHandler = mHandler;
    }

    public void setDeviceListHandler(Handler mHandler) {
        Log.d(TAG, "Device List Handler set");
        mDeviceListHandler = mHandler;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        if (mBtAdapter != null && mBluetoothGatt != null) {
            BluetoothGattAdapter.closeProfileProxy(BluetoothGattAdapter.GATT, mBluetoothGatt);
        }
        if (mBtAdapter != null && mBluetoothGattServer != null) {
            BluetoothGattAdapter.closeProfileProxy(BluetoothGattAdapter.GATT_SERVER, mBluetoothGattServer);
        }
        super.onDestroy();
    }

    private BluetoothProfile.ServiceListener mProfileServiceListener = new BluetoothProfile.ServiceListener() {
        @SuppressLint("NewApi")
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.d(TAG, "onServiceConnected() - client. profile is" + profile);
            
            if (profile == BluetoothGattAdapter.GATT) {
                Log.d(TAG, " Inside GATT onServiceConnected() - client. profile is" + profile);
                mBluetoothGatt = (BluetoothGatt) proxy;
                mBluetoothGatt.registerApp(mGattCallbacks);
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothGattAdapter.GATT) {

                if (mBluetoothGatt != null) {
                    mBluetoothGatt.unregisterApp();
                    mBluetoothGatt = null;
                }
            }

        }
    };

    private BluetoothProfile.ServiceListener mProfileServiceListener1 = new BluetoothProfile.ServiceListener() {
        @SuppressLint("NewApi")
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.d(TAG, "onServiceConnected() - server. profile is" + profile);
            if (profile == BluetoothGattAdapter.GATT_SERVER) {
                mBluetoothGattServer = (BluetoothGattServer) proxy;
                mBluetoothGattServer.registerApp(mGattServerCallbacks);
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothGattAdapter.GATT_SERVER) {
                if (mBluetoothGattServer != null) {
                    mBluetoothGattServer.unregisterApp();
                }

                mBluetoothGattServer = null;
            }
        }
    };

    /**
     * GATT client callbacks
     */
    private BluetoothGattCallback mGattCallbacks = new BluetoothGattCallback() {
        @Override
        public void onScanResult(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.d(TAG, "onScanResult() - device=" + device + ", rssi=" + rssi);
            if (!checkIfBroadcastMode(scanRecord)) {
                Bundle mBundle = new Bundle();
                Message msg = Message.obtain(mDeviceListHandler, GATT_DEVICE_FOUND_MSG);
                mBundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, device);
                mBundle.putInt(EXTRA_RSSI, rssi);
                mBundle.putInt(EXTRA_SOURCE, DEVICE_SOURCE_SCAN);
                msg.setData(mBundle);
                msg.sendToTarget();
            } else
                Log.i(TAG, "device =" + device + " is in Brodacast mode, hence not displaying");
        }

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            Log.d(TAG, " Client onConnectionStateChange (" + device.getAddress() + ")");
            // Device has been connected - start service discovery
            if (newState == BluetoothProfile.STATE_CONNECTED && mBluetoothGatt != null) {
                Bundle mBundle = new Bundle();
                Message msg = Message.obtain(mActivityHandler, BLE_CONNECT_MSG);
                mBundle.putString(BluetoothDevice.EXTRA_DEVICE, device.getAddress());
                msg.setData(mBundle);
                msg.sendToTarget();
                ParcelUuid uuids[] = device.getUuids();
//                for(int i = 0 ;i < uuids.length ; i++)
//                {
//                	Log.v(TAG,"UUID "+i+" :"+uuids[i]);
//                }
                 mBluetoothGatt.discoverServices(device);
                
                
                

            }
            if (newState == BluetoothProfile.STATE_DISCONNECTED && mBluetoothGatt != null) {
                Bundle mBundle = new Bundle();
                Message msg = Message.obtain(mActivityHandler, BLE_DISCONNECT_MSG);
                mBundle.putString(BluetoothDevice.EXTRA_DEVICE, device.getAddress());
                msg.setData(mBundle);
                msg.sendToTarget();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged()");
            if (TX_POWER_LEVEL_UUID.equals(characteristic.getUuid())) {
                Bundle mBundle = new Bundle();
                Message msg = Message.obtain(mActivityHandler, BLE_VALUE_MSG);
                mBundle.putByteArray(EXTRA_VALUE, characteristic.getValue());
                msg.setData(mBundle);
                msg.sendToTarget();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothDevice device, int status) {
//        	List<BluetoothGattService> services = mBluetoothGatt.getServices(device);
//			for( int i = 0 ; i <services.size() ;i++)
//			{
//				UUID uuid = services.get(i).getUuid();
//				Log.v(TAG,"Services "+ i +"UUID: "+uuid);
//				BluetoothGattService disService = mBluetoothGatt.getService(device, uuid);
//
//	           
//	            if (disService == null) {
//	                showMessage("Dis service not found!");
//	                return;
//	            }
//	            List<BluetoothGattCharacteristic> c = disService.getCharacteristics();
//	            for(int j = 0 ; j < c.size() ; j++)
//	            {
//	            	Log.v(TAG,"getCharacteristic "+j+" "+c.get(j).getUuid());
//	            }
//			}
			
            Log.d(TAG, "onServicesDcovered()");
            Message msg = Message.obtain(mActivityHandler, BLE_SERVICE_DISCOVER_MSG);
            
            Bundle mBundle = new Bundle();
            
            mBundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, device);
            msg.setData(mBundle);
            msg.sendToTarget();
            DummyReadForSecLevelCheck(device);
        }

        @Override
        public void onCharacteristicRead(BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicRead()");
            Log.d(TAG, "characteristic : "+characteristic.getUuid());
            
//            if (TX_POWER_LEVEL_UUID.equals(characteristic.getUuid())) {
                Bundle mBundle = new Bundle();
                Message msg = Message.obtain(mActivityHandler, BLE_VALUE_MSG);
                mBundle.putByteArray(EXTRA_VALUE, characteristic.getValue());
                msg.setData(mBundle);
                msg.sendToTarget();
//            }
        }
        public void onDescriptorRead(BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, "onDescriptorRead");
            BluetoothGattCharacteristic mTxPowerccc = descriptor.getCharacteristic();
            Log.i(TAG, "Registering for notification");

            boolean isenabled = enableNotification(true, mTxPowerccc);
            Log.i(TAG, "Notification status =" + isenabled);
        }

        public void onReadRemoteRssi(BluetoothDevice device, int rssi, int status) {
            Log.i(TAG, "onRssiRead rssi value is " + rssi);
            Bundle mBundle = new Bundle();
            Message msg = Message.obtain(mActivityHandler, GATT_CHARACTERISTIC_RSSI_MSG);
            mBundle.putParcelable(EXTRA_DEVICE, device);
            mBundle.putInt(EXTRA_RSSI, rssi);
            mBundle.putInt(EXTRA_STATUS, status);
            msg.setData(mBundle);
            msg.sendToTarget();
        }

    };

    /**
     * GATT sever callbacks
     */
    private BluetoothGattServerCallback mGattServerCallbacks = new BluetoothGattServerCallback() {
        @Override
        public void onAppRegistered(int status) {
            Log.d(TAG, "onAppRegistered() - status=" + status);

            if (status == 0) {
                byte[] value = { LOW_ALERT };
                MutableBluetoothGattCharacteristic alertLevel = new MutableBluetoothGattCharacteristic(
                        ALERT_LEVEL_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                        BluetoothGattCharacteristic.PERMISSION_WRITE);
                alertLevel.setValue(value);
                MutableBluetoothGattService immediateAlert = new MutableBluetoothGattService(IMMEDIATE_ALERT_UUID,
                        BluetoothGattService.SERVICE_TYPE_PRIMARY);
                immediateAlert.addCharacteristic(alertLevel);
                MutableBluetoothGattCharacteristic linkalertLevel = new MutableBluetoothGattCharacteristic(
                        ALERT_LEVEL_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE
                                | BluetoothGattCharacteristic.PROPERTY_READ,
                        BluetoothGattCharacteristic.PERMISSION_WRITE);

                linkalertLevel.setValue(value);
                MutableBluetoothGattService linkloss = new MutableBluetoothGattService(LINK_LOSS_UUID,
                        BluetoothGattService.SERVICE_TYPE_PRIMARY);
                immediateAlert.addCharacteristic(linkalertLevel);
                MutableBluetoothGattCharacteristic txpowerlevel = new MutableBluetoothGattCharacteristic(
                        TX_POWER_LEVEL_UUID, BluetoothGattCharacteristic.PROPERTY_NOTIFY
                                | BluetoothGattCharacteristic.PROPERTY_READ,
                        BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

                MutableBluetoothGattService txpower = new MutableBluetoothGattService(TX_POWER_UUID,
                        BluetoothGattService.SERVICE_TYPE_PRIMARY);
                immediateAlert.addCharacteristic(txpowerlevel);

                mBluetoothGattServer.addService(linkloss);
                mBluetoothGattServer.addService(immediateAlert);
                mBluetoothGattServer.addService(txpower);
            }
        }

        @Override
        public void onScanResult(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.d(TAG, "onScanResult() - device=" + device + ", rssi=" + rssi);
            if (!checkIfBroadcastMode(scanRecord)) {
                Bundle mBundle = new Bundle();
                Message msg = Message.obtain(mDeviceListHandler, GATT_DEVICE_FOUND_MSG);
                mBundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, device);
                mBundle.putInt(EXTRA_RSSI, rssi);
                mBundle.putInt(EXTRA_SOURCE, DEVICE_SOURCE_SCAN);
                msg.setData(mBundle);
                msg.sendToTarget();
            } else
                Log.i(TAG, "device =" + device + " is in Brodacast mode, hence not displaying");
        }
        
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            Log.d(TAG, "Server onConnectionStateChange (" + device.getAddress() + ")");
            // Device has been connected - start service discovery
            if (newState == BluetoothProfile.STATE_CONNECTED && mBluetoothGattServer != null) {
                Bundle mBundle = new Bundle();
                Message msg = Message.obtain(mActivityHandler, BLE_CONNECT_MSG);
                mBundle.putString(BluetoothDevice.EXTRA_DEVICE, device.getAddress());
                msg.setData(mBundle);
                msg.sendToTarget();
            }

            if (newState == BluetoothProfile.STATE_DISCONNECTED && mBluetoothGattServer != null) {
                Bundle mBundle = new Bundle();
                Message msg = Message.obtain(mActivityHandler, BLE_DISCONNECT_MSG);
                mBundle.putString(BluetoothDevice.EXTRA_DEVICE, device.getAddress());
                msg.setData(mBundle);
                msg.sendToTarget();
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset,
                byte[] value) {
            Log.d(TAG, "onCharacteristicWriteRequest() - responseNeeded=" + responseNeeded);
            Bundle mBundle = new Bundle();
            Message msg = Message.obtain(mActivityHandler, PROXIMITY_ALERT_LEVEL_CHANGED_MSG);
            mBundle.putByteArray(EXTRA_VALUE, value);
            msg.setData(mBundle);
            msg.sendToTarget();

        }
    };

    /*
     * Broadcast mode checker API
     */
    public boolean checkIfBroadcastMode(byte[] scanRecord) {
        int offset = 0;
        while (offset < (scanRecord.length - 2)) {
            int len = scanRecord[offset++];
            if (len == 0)
                break; // Length == 0 , we ignore rest of the packet
            // TODO: Check the rest of the packet if get len = 0

            int type = scanRecord[offset++];
            switch (type) {
            case 0x01:

                if (len >= 2) {
                    // The usual scenario(2) and More that 2 octets scenario.
                    // Since this data will be in Little endian format, we
                    // are interested in first 2 bits of first byte
                    byte flag = scanRecord[offset++];
                    /*
                     * 00000011(0x03) - LE Limited Discoverable Mode and LE
                     * General Discoverable Mode
                     */
                    if ((flag & 0x03) > 0)
                        return false;
                    else
                        return true;
                } else if (len == 1) {
                    continue;// ignore that packet and continue with the rest
                }
            default:
                offset += (len - 1);
                break;
            }
        }
        return false;
    }

//    public void writeIasAlertLevel(BluetoothDevice iDevice, int iAlertLevel) {
//
//        BluetoothGattService alertService = mBluetoothGatt.getService(iDevice, IMMEDIATE_ALERT_UUID);
//        if (alertService == null) {
//            showMessage("Immediate Alert service not found!");
//            return;
//        }
//        BluetoothGattCharacteristic alertLevel = alertService.getCharacteristic(ALERT_LEVEL_UUID);
//        if (alertLevel == null) {
//            showMessage("Immediate Alert Level charateristic not found!");
//            return;
//        }
//        boolean status = false;
//        int storedLevel = alertLevel.getWriteType();
//        Log.d(TAG, "storedLevel() - storedLevel=" + storedLevel);
//        alertLevel.setValue(iAlertLevel, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//        alertLevel.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
//        status = mBluetoothGatt.writeCharacteristic(alertLevel);
//        Log.d(TAG, "writeIasAlertLevel() - status=" + status);
//    }

//    public void writeLlsAlertLevel(BluetoothDevice iDevice, int iAlertLevel) {
//        BluetoothGattService linkLossService = mBluetoothGatt.getService(iDevice, LINK_LOSS_UUID);
//        if (linkLossService == null) {
//            showMessage("link loss Alert service not found!");
//            return;
//        }
//
//        BluetoothGattCharacteristic alertLevel = linkLossService.getCharacteristic(ALERT_LEVEL_UUID);
//        if (alertLevel == null) {
//            showMessage("link loss Alert Level charateristic not found!");
//            return;
//        }
//
//        alertLevel.setValue(iAlertLevel, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//        boolean status = false;
//        status = mBluetoothGatt.writeCharacteristic(alertLevel);
//        Log.d(TAG, "writeLlsAlertLevel() - status=" + status);
//    }

//    public void ReadTxPower(BluetoothDevice iDevice) {
//        byte Txpowervalue[];
//        BluetoothGattService TxPowerService = mBluetoothGatt.getService(iDevice, TX_POWER_UUID);
//        if (TxPowerService == null) {
//            showMessage("Tx power service not found!");
//            return;
//        }
//
//        BluetoothGattCharacteristic TxPowerLevel = TxPowerService.getCharacteristic(TX_POWER_LEVEL_UUID);
//        if (TxPowerLevel == null) {
//            showMessage("Tx power Level charateristic not found!");
//            return;
//        }
//
//        boolean result = mBluetoothGatt.readCharacteristic(TxPowerLevel);
//        if (result == false) {
//            showMessage("Tx power reading is failed!");
//        }
//        Txpowervalue = TxPowerLevel.getValue();
//        if (Txpowervalue != null)
//            Log.d(TAG, "level = " + Txpowervalue[0]);
//    }
    

    public boolean enableNotification(boolean enable, BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, "enableNotification status=" + characteristic);

        if (mBluetoothGatt == null)
            return false;
        if (!mBluetoothGatt.setCharacteristicNotification(characteristic, enable))
            return false;

        BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(CCC);
        if (clientConfig == null)
            return false;

        if (enable) {
            clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            clientConfig.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        return mBluetoothGatt.writeDescriptor(clientConfig);
    }

    private void showMessage(String msg) {
        Log.e(TAG, msg);
    }

    public void connect(BluetoothDevice device, boolean autoconnect) {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.connect(device, autoconnect);
        }
    }

    public void disconnect(BluetoothDevice device) {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.cancelConnection(device);
        }
    }

    public void removeBond(BluetoothDevice device) {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.removeBond(device);
        }
    }

    public void scan(boolean start) {
        if (mBluetoothGatt == null)
            return;

        if (start) {
            mBluetoothGatt.startScan();
        } else {
            mBluetoothGatt.stopScan();
        }
    }

    public BluetoothGattService getService(BluetoothDevice idevice, UUID ServiceUUID) {
        if (mBluetoothGatt != null) {
            Log.d(TAG, "getService() - ServiceUUID=" + ServiceUUID);
            return mBluetoothGatt.getService(idevice, ServiceUUID);
        }
        return null;
    }

    public BluetoothGattCharacteristic getCharacteristic(BluetoothGattService iService, UUID CharUUID) {
        if (iService != null) {
            Log.d(TAG, "getService() - CharUUID=" + CharUUID);
            return iService.getCharacteristic(CharUUID);
        }
        return null;
    }

    public boolean readCharacteristic(BluetoothGattCharacteristic Char) {
        boolean result = false;
        if (mBluetoothGatt != null) {
            result = mBluetoothGatt.readCharacteristic(Char);
            Log.d(TAG, "readCharacteristic() - Char=" + Char);
            return result;
        }
        return false;
    }

    public void DummyReadForSecLevelCheck(BluetoothDevice device) {
        boolean result = false;
        if (mBluetoothGatt != null && device != null) {
            BluetoothGattService disService = mBluetoothGatt.getService(device, DIS_UUID);
            Log.v(TAG,"disService : "+ disService.getUuid());
           
            if (disService == null) {
                showMessage("Dis service not found!");
                return;
            }
//            List<BluetoothGattCharacteristic> c = disService.getCharacteristics();
//            for(int i = 0 ; i < c.size() ; i++)
//            {
//            	Log.v(TAG,"getCharacteristic "+i+" "+c.get(i).getUuid());
//            }
            BluetoothGattCharacteristic firmwareIdcharc = disService.getCharacteristic(FIRMWARE_REVISON_UUID);
            if (firmwareIdcharc == null) {
                showMessage("firmware revision charateristic not found!");
                return;
            }
            result = mBluetoothGatt.readCharacteristic(firmwareIdcharc);
            
            if (result == false) {
                showMessage("firmware revision reading is failed!");
            }
            else
            {
            	Log.v(TAG,"firmwareIdcharc : "+firmwareIdcharc);
            }
        }

    }
    public void enableWristBandNoti(BluetoothDevice iDevice , UUID serviceUUID, UUID charUUID) {
        boolean result = false;
        Log.d(TAG, "enableTxPowerNoti=" + isNoti);

        BluetoothGattService service = mBluetoothGatt.getService(iDevice, UUID.fromString("c231ff01-8d74-4fa9-a7dd-13abdfe5cbff"));
        if (service == null) {
            showMessage("Tx power service not found!");
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString("c231ff03-8d74-4fa9-a7dd-13abdfe5cbff"));
        if (characteristic == null) {
            showMessage("charateristic not found!");
            return;
        }
        //public static final UUID CCC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        if (descriptor == null) {
            Log.e(TAG, "CCC for TX power level charateristic not found!");
            return;
        }

        result = mBluetoothGatt.readDescriptor(descriptor);
        if (result == false) {
            Log.e(TAG, "readDescriptor() is failed");
            return;
        }
    }

    public void enableTxPowerNoti(BluetoothDevice iDevice) {
        boolean result = false;
        Log.d(TAG, "enableTxPowerNoti=" + isNoti);

        BluetoothGattService TxPowerService = mBluetoothGatt.getService(iDevice, TX_POWER_UUID);
        if (TxPowerService == null) {
            showMessage("Tx power service not found!");
            return;
        }

        BluetoothGattCharacteristic TxPowerLevel = TxPowerService.getCharacteristic(TX_POWER_LEVEL_UUID);
        if (TxPowerLevel == null) {
            showMessage("Tx power Level charateristic not found!");
            return;
        }
        BluetoothGattDescriptor mTxPowerccc = TxPowerLevel.getDescriptor(CCC);
        if (mTxPowerccc == null) {
            Log.e(TAG, "CCC for TX power level charateristic not found!");
            return;
        }

        result = mBluetoothGatt.readDescriptor(mTxPowerccc);
        if (result == false) {
            Log.e(TAG, "readDescriptor() is failed");
            return;
        }
    }

    public boolean readRssi(BluetoothDevice device) {
        mBluetoothGatt.readRemoteRssi(device);
        return true;
    }

    public boolean isBLEDevice(BluetoothDevice device) {
        boolean result = false;
        result = mBluetoothGatt.isBLEDevice(device);
        Log.d(TAG, "isBLEDevice after" + result);
        return result;
    }

}
