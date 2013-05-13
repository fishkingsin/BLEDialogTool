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

import java.io.UnsupportedEncodingException;
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
//import com.samsung.android.sdk.bt.gatt.BluetoothGattServer;
//import com.samsung.android.sdk.bt.gatt.BluetoothGattServerCallback;
import com.samsung.android.sdk.bt.gatt.BluetoothGattService;
import com.samsung.android.sdk.bt.gatt.MutableBluetoothGattCharacteristic;
import com.samsung.android.sdk.bt.gatt.MutableBluetoothGattService;

public class BLEService extends Service {
	//set true to print debug message;
	static final boolean bDebug = true;
	
	static final String TAG = "BLEService";
	
	public static final UUID DEVICE_INFORMATION = UUID
			.fromString("0000180A-0000-1000-8000-00805f9b34fb");
	public static final UUID SERIAL_NUMBER_STRING = UUID
			.fromString("00002A25-0000-1000-8000-00805f9b34fb");

	public static final UUID CCC = UUID
			.fromString("00002902-0000-1000-8000-00805f9b34fb");
	public static final UUID FIRMWARE_REVISON_UUID = UUID
			.fromString("00002a26-0000-1000-8000-00805f9b34fb");
	public static final UUID DIS_UUID = UUID
			.fromString("0000180a-0000-1000-8000-00805f9b34fb");
	public static final String SERIAL_STRING = "com.fishkingsin.ble.serialstring";

	public static final int BLE_CONNECT_MSG = 21;
	public static final int BLE_DISCONNECT_MSG = 22;
	public static final int BLE_READY_MSG = 23;
	public static final int BLE_VALUE_MSG = 24;
	
	// James Kong 20130506
	// ------------------------------------------------------
	public static final int BLE_SERVICE_DISCOVER_MSG = 25;
	// ------------------------------------------------------
	public static final int GATT_DEVICE_FOUND_MSG = 25;
	public static final int GATT_CHARACTERISTIC_RSSI_MSG = 26;
	public static final int BLE_WRITE_REQUEST_MSG = 27;
	public static final int BLE_CHARACTERISTIC_WROTE = 28;

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
		if(bDebug)Log.d(TAG, "onCreate()");
		if (mBtAdapter == null) {
			mBtAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBtAdapter == null)
				return;
		}
		BluetoothGattAdapter.getProfileProxy(this, mProfileServiceListener,
				BluetoothGattAdapter.GATT);

	}

	public void setActivityHandler(Handler mHandler) {
		if(bDebug)Log.d(TAG, "Activity Handler set");
		mActivityHandler = mHandler;
	}

	public void setDeviceListHandler(Handler mHandler) {
		if(bDebug)Log.d(TAG, "Device List Handler set");
		mDeviceListHandler = mHandler;
	}

	@Override
	public void onDestroy() {
		if(bDebug)Log.d(TAG, "onDestroy()");
		if (mBtAdapter != null && mBluetoothGatt != null) {
			BluetoothGattAdapter.closeProfileProxy(BluetoothGattAdapter.GATT,
					mBluetoothGatt);
		}
		super.onDestroy();
	}

	private BluetoothProfile.ServiceListener mProfileServiceListener = new BluetoothProfile.ServiceListener() {
		@SuppressLint("NewApi")
		@Override
		public void onServiceConnected(int profile, BluetoothProfile proxy) {
			if(bDebug)Log.d(TAG, "onServiceConnected() - client. profile is" + profile);

			if (profile == BluetoothGattAdapter.GATT) {
				if(bDebug)Log.d(TAG,
						" Inside GATT onServiceConnected() - client. profile is"
								+ profile);
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

	/**
	 * GATT client callbacks
	 */
	private BluetoothGattCallback mGattCallbacks = new BluetoothGattCallback() {
		public void onAppRegistered(int status)
		{
			Log.v(TAG,"onAppRegistered ()");
			checkGattStatus(status);
		}
		@Override
		public void onScanResult(BluetoothDevice device, int rssi,
				byte[] scanRecord) {
//			if(bDebug)Log.d(TAG, "onScanResult() - device=" + device + ", rssi=" + rssi);
			if (!checkIfBroadcastMode(scanRecord)) {
				Bundle mBundle = new Bundle();
				Message msg = Message.obtain(mDeviceListHandler,
						GATT_DEVICE_FOUND_MSG);
				mBundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, device);
				mBundle.putInt(EXTRA_RSSI, rssi);
				mBundle.putInt(EXTRA_SOURCE, DEVICE_SOURCE_SCAN);
				msg.setData(mBundle);
				msg.sendToTarget();
			} else
				Log.i(TAG, "device =" + device
						+ " is in Brodacast mode, hence not displaying");
		}

		@Override
		public void onConnectionStateChange(BluetoothDevice device, int status,
				int newState) {
			if(bDebug)Log.d(TAG,
					" Client onConnectionStateChange (" + device.getAddress()
							+ ")");
			// Device has been connected - start service discovery
			if (newState == BluetoothProfile.STATE_CONNECTED
					&& mBluetoothGatt != null) {
				Bundle mBundle = new Bundle();
				Message msg = Message.obtain(mActivityHandler, BLE_CONNECT_MSG);
				mBundle.putString(BluetoothDevice.EXTRA_DEVICE,
						device.getAddress());
				msg.setData(mBundle);
				msg.sendToTarget();
				ParcelUuid uuids[] = device.getUuids();

				mBluetoothGatt.discoverServices(device);

			}
			if (newState == BluetoothProfile.STATE_DISCONNECTED
					&& mBluetoothGatt != null) {
				Bundle mBundle = new Bundle();
				Message msg = Message.obtain(mActivityHandler,
						BLE_DISCONNECT_MSG);
				mBundle.putString(BluetoothDevice.EXTRA_DEVICE,
						device.getAddress());
				msg.setData(mBundle);
				msg.sendToTarget();
			}
		}

		@Override
		public void onCharacteristicChanged(
				BluetoothGattCharacteristic characteristic) {
			if(bDebug)Log.d(TAG, "onCharacteristicChanged()");
			
			String s = "";
			try {
				s = new String(characteristic.getValue(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.v(TAG, "characteristic.getValue() = " + s);
			Bundle mBundle = new Bundle();
			Message msg = Message.obtain(mActivityHandler, BLE_VALUE_MSG);
			mBundle.putByteArray(EXTRA_VALUE, characteristic.getValue());
			msg.setData(mBundle);
			msg.sendToTarget();
		}

		@Override
		public void onServicesDiscovered(BluetoothDevice device, int status) {
			List<BluetoothGattService > services = mBluetoothGatt.getServices(device);
			/*scan through the list to check the uuid if
			 *   match the notification uuid
			 */
			for (BluetoothGattService service : services)
			{
				if(bDebug)Log.v(TAG,"Services : "+service.getUuid() );
				List<BluetoothGattCharacteristic>  characteristics = service.getCharacteristics();
				for (BluetoothGattCharacteristic characteristic : characteristics)
				{
					if(bDebug)Log.v(TAG,"Characteristic : "+characteristic.getUuid() );
//					if(bDebug)Log.v(TAG,"Characteristic getProperties: "+characteristic.getProperties());
					checkPropertiesTyle(characteristic.getProperties());
					//if(characteristic.getUuid().equals(BLEMonitorActivity.PE128_CHAR_STREAMING) )
					{
						enableWristBandNoti(device,service.getUuid(),characteristic.getUuid());
//						if(!enableNotification(true,
//								characteristic))
//						{
//							Log.e(TAG,"enableNotification Failed !!!");
//						}
					}
//					else if(characteristic.getUuid().equals(BLEMonitorActivity.PE128_CHAR_RCVD) )
//					{
//						enableNotification(true,
//								characteristic);
//					}
				}
			}
			
			if(bDebug)Log.d(TAG, "onServicesDcovered()");
			Message msg = Message.obtain(mActivityHandler,
					BLE_SERVICE_DISCOVER_MSG);
			checkGattStatus(status);
			Bundle mBundle = new Bundle();

			mBundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, device);
			msg.setData(mBundle);
			msg.sendToTarget();
			DummyReadForSecLevelCheck(device);
		}

		@Override
		public void onCharacteristicWrite(
				BluetoothGattCharacteristic characteristic, int status) {
			if(bDebug)Log.d(TAG, "onCharacteristicWrite()");
			checkGattStatus(status);
		}

		@Override
		public void onCharacteristicRead(
				BluetoothGattCharacteristic characteristic, int status) {
			UUID charUuid = characteristic.getUuid();
			if(bDebug)Log.d(TAG, "onCharacteristicRead()");
			checkGattStatus(status);
			Bundle mBundle = new Bundle();
			Message msg = Message.obtain(mActivityHandler, BLE_VALUE_MSG);

			if (charUuid.equals(SERIAL_NUMBER_STRING)) {
				mBundle.putString(SERIAL_STRING,
						characteristic.getStringValue(0));
			}
			mBundle.putByteArray(characteristic.getUuid().toString(),
					characteristic.getValue());
			mBundle.putByteArray(EXTRA_VALUE, characteristic.getValue());
			String s = "";
			try {
				s = new String(characteristic.getValue(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.v(TAG, "characteristic.getValue() = "
					+ s);
			msg.setData(mBundle);
			msg.sendToTarget();

		}

		public void onDescriptorWrite(BluetoothGattDescriptor descriptor,
				int status) {
			Log.i(TAG, "onDescriptorWrite status: " + status);
			Bundle mBundle = new Bundle();
			Message msg = Message.obtain(mActivityHandler, BLE_VALUE_MSG);
			String s = "";
			s = String.format("%02X", descriptor.getValue()); 

			Log.v(TAG, "descriptor.getValue() = " + s);
			mBundle.putByteArray(EXTRA_VALUE, descriptor.getValue());
			
			msg.setData(mBundle);
			msg.sendToTarget();
			checkGattStatus(status);
		}
		

		public void onDescriptorRead(BluetoothGattDescriptor descriptor,
				int status) {
			Log.i(TAG, "onDescriptorRead : " + status);
			checkGattStatus(status);
			BluetoothGattCharacteristic mWristBandccc = descriptor
					.getCharacteristic();
			Log.i(TAG, "Registering for notification UUID : "+mWristBandccc.getUuid());

			String s = "";
			try {
				s = new String(descriptor.getValue(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.v(TAG, "descriptor.getValue() = " + s);
			
			boolean isenabled = enableNotification(true, mWristBandccc);
			Log.i(TAG, "Notification status =" + isenabled);
		}

		public void onReadRemoteRssi(BluetoothDevice device, int rssi,
				int status) {
			Log.i(TAG, "onRssiRead rssi value is " + rssi);
			Bundle mBundle = new Bundle();
			Message msg = Message.obtain(mActivityHandler,
					GATT_CHARACTERISTIC_RSSI_MSG);
			mBundle.putParcelable(EXTRA_DEVICE, device);
			mBundle.putInt(EXTRA_RSSI, rssi);
			mBundle.putInt(EXTRA_STATUS, status);
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

	//it is going to read serial 
	
	public void read_serial_number(BluetoothDevice device) {
		Log.i(TAG, "read 0x2A25 uuid charachteristic");
		BluetoothGattService mDI = mBluetoothGatt.getService(device,
				DEVICE_INFORMATION);
		if (mDI == null) {
			Log.e(TAG, "Device Information Service Not Found!!!");
			return;
		}
		BluetoothGattCharacteristic mSNS = mDI
				.getCharacteristic(SERIAL_NUMBER_STRING);
		if (mSNS == null) {
			Log.e(TAG, "Serial Number String Characteristic Not Found!!!");
			return;
		}
		mBluetoothGatt.readCharacteristic(mSNS);
	}

	public void ReadWristBand(BluetoothDevice iDevice, UUID serviceUUID,
			UUID charUUID) {
		Log.i(TAG, "ReadWristBand charachteristic");
		BluetoothGattService mDI = mBluetoothGatt.getService(iDevice,
				serviceUUID);
		if (mDI == null) {
			Log.e(TAG, "ReadWristBand Device Information Service Not Found!!!");
			return;
		}
		BluetoothGattCharacteristic mSNS = mDI.getCharacteristic(charUUID);
		if (mSNS == null) {
			Log.e(TAG, "ReadWristBand Characteristic Not Found!!!");
			return;
		}
		if(!mBluetoothGatt.readCharacteristic(mSNS))
		{
			Log.e(TAG, "ReadWristBand Characteristic Fail to Read!!! UUID: " +mSNS.getUuid());
			return;
		}
	}

	public boolean enableNotification(boolean enable,
			BluetoothGattCharacteristic characteristic) {

        if (mBluetoothGatt == null)
            return false;
        if (!mBluetoothGatt.setCharacteristicNotification(characteristic, enable))
            return false;

        BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(CCC);
        if (clientConfig == null)
            return false;

        if (enable) {
             Log.i(TAG,"enable notification");
            clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            Log.i(TAG,"disable notification");
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

	public BluetoothGattService getService(BluetoothDevice idevice,
			UUID ServiceUUID) {
		if (mBluetoothGatt != null) {
			if(bDebug)Log.d(TAG, "getService() - ServiceUUID=" + ServiceUUID);
			return mBluetoothGatt.getService(idevice, ServiceUUID);
		}
		return null;
	}

	public BluetoothGattCharacteristic getCharacteristic(
			BluetoothGattService iService, UUID CharUUID) {
		if (iService != null) {
			if(bDebug)Log.d(TAG, "getService() - CharUUID=" + CharUUID);
			return iService.getCharacteristic(CharUUID);
		}
		return null;
	}

	public boolean readCharacteristic(BluetoothGattCharacteristic Char) {
		boolean result = false;
		if (mBluetoothGatt != null) {
			result = mBluetoothGatt.readCharacteristic(Char);
			if(bDebug)Log.d(TAG, "readCharacteristic() - Char=" + Char);
			return result;
		}
		return false;
	}

	public void DummyReadForSecLevelCheck(BluetoothDevice device) {
		boolean result = false;
		if (mBluetoothGatt != null && device != null) {
			BluetoothGattService disService = mBluetoothGatt.getService(device,
					DIS_UUID);
			Log.v(TAG, "disService : " + disService.getUuid());

			if (disService == null) {
				showMessage("Dis service not found!");
				return;
			}
			
			BluetoothGattCharacteristic firmwareIdcharc = disService
					.getCharacteristic(FIRMWARE_REVISON_UUID);
			if (firmwareIdcharc == null) {
				showMessage("firmware revision charateristic not found!");
				return;
			}
			result = mBluetoothGatt.readCharacteristic(firmwareIdcharc);

			if (result == false) {
				showMessage("firmware revision reading is failed!");
			} else {
				Log.v(TAG, "firmwareIdcharc : " + firmwareIdcharc);
			}
		}

	}

	public void WriteWristBand(BluetoothDevice iDevice, UUID serviceUUID,
			UUID charUUID, byte[] data) {

		BluetoothGattService service = mBluetoothGatt.getService(iDevice,
				serviceUUID);
		if (service == null) {
			showMessage("WristBand service not )found!");
			return;
		}
		BluetoothGattCharacteristic characteristic = service
				.getCharacteristic(charUUID);
		if (characteristic == null) {
			showMessage("WristBand charateristic not found!");
			return;
		}
		boolean status = false;
		int storedLevel = characteristic.getWriteType();
		if(bDebug)Log.d(TAG, "WriteWristBand storedLevel=" + storedLevel);

		characteristic.setValue(data);
		
		characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
		status = mBluetoothGatt.writeCharacteristic(characteristic);
		if(bDebug)Log.d(TAG, "WriteWristBand - status = " + status);
	}

	public void enableWristBandNoti(BluetoothDevice iDevice, UUID serviceUUID,
			UUID charUUID) {
		boolean result = false;
		if(bDebug)Log.d(TAG, "enableWristBandNoti=" + isNoti);

		BluetoothGattService service = mBluetoothGatt.getService(iDevice,
				serviceUUID);
		if (service == null) {
			showMessage("Tx power service not found!");
			return;
		}

		BluetoothGattCharacteristic characteristic = service
				.getCharacteristic(charUUID);
		if (characteristic == null) {
			showMessage("charateristic not found!");
			return;
		}
		BluetoothGattDescriptor descriptor = (BluetoothGattDescriptor) characteristic.getDescriptor(CCC);
		if (descriptor == null) {
			Log.e(TAG, "CCC for charateristic not found!");
			return;
		}

		result = mBluetoothGatt.readDescriptor(descriptor);
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
		if(bDebug)Log.d(TAG, "isBLEDevice after" + result);
		return result;
	}

	void checkPropertiesTyle(int properties) {
		switch (properties) {
//		case BluetoothGattCharacteristic.FORMAT_FLOAT:
//			Log.v(TAG, "BluetoothGattCharacteristic.FORMAT_FLOAT");
//			break;
//		case BluetoothGattCharacteristic.FORMAT_SFLOAT:
//			Log.v(TAG, "BluetoothGattCharacteristic.FORMAT_SFLOAT");
//			break;
//		case BluetoothGattCharacteristic.FORMAT_SINT16:
//			Log.v(TAG, "BluetoothGattCharacteristic.FORMAT_SINT16");
//			break;
//		case BluetoothGattCharacteristic.FORMAT_SINT32:
//			Log.v(TAG, "BluetoothGattCharacteristic.FORMAT_SINT32");
//			break;
//		case BluetoothGattCharacteristic.FORMAT_SINT8:
//			Log.v(TAG, "BluetoothGattCharacteristic.FORMAT_SINT8");
//			break;
//		case BluetoothGattCharacteristic.FORMAT_UINT16:
//			Log.v(TAG, "BluetoothGattCharacteristic.FORMAT_UINT16");
//			break;
//		case BluetoothGattCharacteristic.FORMAT_UINT32:
//			Log.v(TAG, "BluetoothGattCharacteristic.FORMAT_UINT32");
//			break;
//		case BluetoothGattCharacteristic.FORMAT_UINT8:
//			Log.v(TAG, "BluetoothGattCharacteristic.FORMAT_UINT8");
//			break;
//		case BluetoothGattCharacteristic.PERMISSION_NOT_DETERMINED:
//			Log.v(TAG, "BluetoothGattCharacteristic.PERMISSION_NOT_DETERMINED");
//			break;
//		case BluetoothGattCharacteristic.PERMISSION_READ:
//			Log.v(TAG, "BluetoothGattCharacteristic.PERMISSION_READ");
//			break;
//		case BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED:
//			Log.v(TAG, "BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED");
//			break;
//		case BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM:
//			Log.v(TAG,
//					"BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM");
//			break;
//		case BluetoothGattCharacteristic.PERMISSION_WRITE:
//			Log.v(TAG, "BluetoothGattCharacteristic.PERMISSION_WRITE");
//			break;
//		case BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED:
//			Log.v(TAG, "BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED");
//			break;
//		case BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM:
//			Log.v(TAG,
//					"BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM");
//			break;
//		case BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED:
//			Log.v(TAG, "BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED");
//			break;
//		case BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM:
//			Log.v(TAG,
//					"BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM");
//			break;
		case BluetoothGattCharacteristic.PROPERTY_BROADCAST:
			Log.v(TAG, "BluetoothGattCharacteristic.PROPERTY_BROADCAST");
			break;
		case BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS:
			Log.v(TAG, "BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS");
			break;
		case BluetoothGattCharacteristic.PROPERTY_INDICATE:
			Log.v(TAG, "BluetoothGattCharacteristic.PROPERTY_INDICATE");
			break;
		case BluetoothGattCharacteristic.PROPERTY_NOTIFY:
			Log.v(TAG, "BluetoothGattCharacteristic.PROPERTY_NOTIFY");
			break;
		case BluetoothGattCharacteristic.PROPERTY_READ:
			Log.v(TAG, "BluetoothGattCharacteristic.PROPERTY_NOTIFY");
			break;
		case BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE:
			Log.v(TAG, "BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE");
			break;
		case BluetoothGattCharacteristic.PROPERTY_WRITE:
			Log.v(TAG, "BluetoothGattCharacteristic.PROPERTY_WRITE");
			break;
		case BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE:
			Log.v(TAG, "BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE");
			break;
//		case BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT:
//			Log.v(TAG, "BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT");
//			break;
//		case BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE:
//			Log.v(TAG, "BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE");
//			break;
//		case BluetoothGattCharacteristic.WRITE_TYPE_SIGNED:
//			Log.v(TAG, "BluetoothGattCharacteristic.WRITE_TYPE_SIGNED");
//			break;
		}
	}
	void checkGattStatus(int status) {
		switch (status) {
		case BluetoothGatt.GATT_ALREADY_OPEN:
			Log.v(TAG, "BluetoothGatt.GATT_ALREADY_OPEN");
			break;
		case BluetoothGatt.GATT_ERROR:
			Log.v(TAG, "BluetoothGatt.GATT_ERROR");
			break;
		case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
			Log.v(TAG, "BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION");
			break;
		case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
			Log.v(TAG, "BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION");
			break;
		case BluetoothGatt.GATT_INTERNAL_ERROR:
			Log.v(TAG, "BluetoothGatt.GATT_INTERNAL_ERROR");
			break;
		case BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH:
			Log.v(TAG, "BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH");
			break;
		case BluetoothGatt.GATT_INVALID_OFFSET:
			Log.v(TAG, "BluetoothGatt.GATT_INVALID_OFFSET");
			break;
		case BluetoothGatt.GATT_NO_RESOURCES:
			Log.v(TAG, "BluetoothGatt.GATT_NO_RESOURCES");
			break;
		case BluetoothGatt.GATT_READ_NOT_PERMITTED:
			Log.v(TAG, "BluetoothGatt.GATT_READ_NOT_PERMITTED");
			break;
		case BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED:
			Log.v(TAG, "BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED");
			break;
		case BluetoothGatt.GATT_SUCCESS:
			Log.v(TAG, "BluetoothGatt.GATT_SUCCESS");
			break;
		case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
			Log.v(TAG, "BluetoothGatt.GATT_WRITE_NOT_PERMITTED");
			break;
		default:
			Log.v(TAG, "BluetoothGatt status unkonwn :"+status);
			break;

		}
	}

}
