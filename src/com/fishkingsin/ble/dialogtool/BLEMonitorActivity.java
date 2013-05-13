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
import java.util.Set;
import java.util.UUID;

import com.samsung.android.sdk.bt.gatt.BluetoothGattCharacteristic;
import com.samsung.android.sdk.bt.gatt.BluetoothGattDescriptor;
import com.samsung.android.sdk.bt.gatt.BluetoothGattService;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.util.Log;

public class BLEMonitorActivity extends Activity {
	private static final int REQUEST_SELECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;
	private static final int STATE_READY = 10;
	public static final String TAG = "BLEMonitorActivity";
	private static final int BLE_PROFILE_CONNECTED = 20;
	private static final int BLE_PROFILE_DISCONNECTED = 21;
	private static final int STATE_OFF = 10;
	
	static UUID PE128_CHAR_STREAMING = UUID.fromString("00002a35-0000-1000-8000-00805f9b34fb");
	static final UUID PE128_CHAR_RCVD = UUID
			.fromString("c231ff08-8d74-4fa9-a7dd-13abdfe5cbff");
	private static final UUID PE128_CHAR_XFER = UUID
			.fromString("c231ff02-8d74-4fa9-a7dd-13abdfe5cbff");
//	
	private int mState = BLE_PROFILE_DISCONNECTED;
	RadioGroup mRg;

	private BLEService mService = null;
	private BluetoothDevice mDevice = null;
	private BluetoothAdapter mBtAdapter = null;
	private static int mAlertLevel = BLEService.HIGH_ALERT;
	ListView mServiceList, mCharList;
	ServiceAdapter mServiceAdapter;
	CharacteristicAdapter mCharacteristicAdapter;
	List<BluetoothGattService> serviceList;
	List<BluetoothGattCharacteristic> charList;
	UUID selectedServiceUUID, selectedCharacteristicUUID;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
//		Set<BluetoothDevice> pairedDevices = (Set<BluetoothDevice>) mBtAdapter.getBondedDevices();
//		// If there are paired devices
//		if (pairedDevices.size() > 0) {
//		    // Loop through paired devices
//		    for (BluetoothDevice device : pairedDevices) {
//		        // Filter LE Only devices
//		        
//		            // Add the name and address to an array adapter to show in a ListView
//		            Log.v(TAG,device.getName() + "\n" + device.getAddress());
//		        
//		    }
//		}
		if (mBtAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		init();
		serviceList = new ArrayList<BluetoothGattService>();
		mServiceAdapter = new ServiceAdapter(this, serviceList);
		mServiceList = ((ListView) findViewById(R.id.new_service));
		mServiceList.setAdapter(mServiceAdapter);
		mServiceList.setOnItemClickListener(mServiceClickListener);

		charList = new ArrayList<BluetoothGattCharacteristic>();

		mCharacteristicAdapter = new CharacteristicAdapter(this, charList);
		mCharList = ((ListView) findViewById(R.id.new_characteristic));
		mCharList.setAdapter(mCharacteristicAdapter);
		mCharList.setOnItemClickListener(mCharacteristicClickListener);

		((Button) findViewById(R.id.btn_select))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (!mBtAdapter.isEnabled()) {
							Log.i(TAG, "onClick - BT not enabled yet");
							Intent enableIntent = new Intent(
									BluetoothAdapter.ACTION_REQUEST_ENABLE);
							startActivityForResult(enableIntent,
									REQUEST_ENABLE_BT);
						} else {
							Intent newIntent = new Intent(
									BLEMonitorActivity.this,
									DeviceListActivity.class);
							startActivityForResult(newIntent,
									REQUEST_SELECT_DEVICE);
						}
					}
				});

		((Button) findViewById(R.id.btn_connect))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						mService.connect(mDevice, false);
					}
				});

		((Button) findViewById(R.id.btn_disconnect))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						mService.disconnect(mDevice);
					}
				});
		((Button) findViewById(R.id.btn_readinfo))
				.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						mService.read_serial_number(mDevice);
//						mService.readRssi(mDevice);
					}
				});

		// Set initial UI state
		setUiState();
	}

	private OnItemClickListener mServiceClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			BluetoothGattService service = serviceList.get(position);

			selectedServiceUUID = service.getUuid();
			charList.clear();

			List<BluetoothGattCharacteristic> c = service.getCharacteristics();
			if (c.size() == 0) {
				Log.i(TAG, "No Characteristic in Service !!");
				mCharacteristicAdapter.notifyDataSetChanged();
				return;
			}
			for (int j = 0; j < c.size(); j++) {
				UUID uuid = c.get(j).getUuid();

				charList.add(c.get(j));
				mCharacteristicAdapter.notifyDataSetChanged();
			}
			

			return;

		}
	};
	private OnItemClickListener mCharacteristicClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			selectedCharacteristicUUID = charList.get(position).getUuid();

			// BluetoothGattCharacteristic characteristic =
			// charList.get(position);
			// for(int i = 0; i < characteristic.getValue().length;i++)
			// {
			// Log.v(TAG,"value "+i+" "+characteristic.getValue()[i]);
			// }
			// c231ff08
			if (selectedCharacteristicUUID.equals(PE128_CHAR_XFER)) {
				Intent newIntent = new Intent(
						BLEMonitorActivity.this,
						CommandListActivity.class);
				startActivityForResult(newIntent,
						0);

			} else if (selectedCharacteristicUUID.equals(PE128_CHAR_RCVD)) {
				mService.enableWristBandNoti(mDevice,
						 selectedServiceUUID,selectedCharacteristicUUID);
			} 
			else if (selectedCharacteristicUUID.equals(PE128_CHAR_STREAMING)) {

				mService.enableWristBandNoti(mDevice,
						 selectedServiceUUID,selectedCharacteristicUUID);
				
			}
			else
			{
				mService.ReadWristBand(mDevice,
						 selectedServiceUUID,selectedCharacteristicUUID);
			}
			return;

		}
	};

	private ServiceConnection mServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className,
				IBinder rawBinder) {
			mService = ((BLEService.LocalBinder) rawBinder).getService();
			Log.d(TAG, "onServiceConnected mService= " + mService);
			mService.setActivityHandler(mHandler);
		}

		public void onServiceDisconnected(ComponentName classname) {
			mService.disconnect(mDevice);
			mService = null;
		}
	};

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			final Bundle data = msg.getData();
			switch (msg.what) {
			case BLEService.BLE_CONNECT_MSG:
				runOnUiThread(new Runnable() {
					public void run() {
						if (mDevice != null
								&& mDevice
										.getAddress()
										.equals(data
												.getString(BluetoothDevice.EXTRA_DEVICE))) {
							Log.d(TAG, "BLE_CONNECT_MSG");
							mState = BLE_PROFILE_CONNECTED;
							setUiState();
						}
					}
				});
				break;
			case BLEService.BLE_DISCONNECT_MSG:
				runOnUiThread(new Runnable() {
					public void run() {
						if (mDevice != null
								&& mDevice
										.getAddress()
										.equals(data
												.getString(BluetoothDevice.EXTRA_DEVICE))) {
							Log.d(TAG, "BLE_DISCONNECT_MSG");
							mState = BLE_PROFILE_DISCONNECTED;

							setUiState();
						}
					}
				});
				break;

			case BLEService.BLE_READY_MSG:
				Log.d(TAG, "BLE_READY_MSG");
				runOnUiThread(new Runnable() {
					public void run() {
						mState = STATE_READY;
						setUiState();
					}
				});
				break;
			case BLEService.BLE_CHARACTERISTIC_WROTE:
				mService.ReadWristBand(mDevice,
						 selectedServiceUUID,PE128_CHAR_RCVD);
				break;
			case BLEService.BLE_VALUE_MSG:
				Log.d(TAG, "BLE_VALUE_MSG");

				final byte[] value = data.getByteArray(BLEService.EXTRA_VALUE);
				String s;
				try {
					s = new String(value, "UTF-8");
					showMessage("To String Message : " + s);
				} catch (UnsupportedEncodingException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				String _msg = "";
				for (int i = 0; i < value.length; i++) {
					_msg += value[i] +" , ";
				}

				Log.v(TAG, _msg);
				runOnUiThread(new Runnable() {
					public void run() {
						try {

						} catch (Exception e) {
							Log.e(TAG, e.toString());
						}
					}
				});
				break;
			case BLEService.GATT_CHARACTERISTIC_RSSI_MSG:
				Log.d(TAG, "GATT_CHARACTERISTIC_RSSI_MSG");
				final BluetoothDevice RemoteRssidevice = data
						.getParcelable(BLEService.EXTRA_DEVICE);
				final int rssi = data.getInt(BLEService.EXTRA_RSSI);
				Log.d(TAG, "rssi value is" + rssi + " and remote device is"
						+ RemoteRssidevice);
				runOnUiThread(new Runnable() {
					public void run() {
						if (mDevice != null && RemoteRssidevice != null
								&& mDevice.equals(RemoteRssidevice)) {
							if (rssi != 0) {
								showMessage(String.valueOf(rssi));
								// mRemoteRssiVal.setText("Rssi Value =" +
								// String.valueOf(rssi));
							}
						}
					}
				});
				break;
			case BLEService.BLE_WRITE_REQUEST_MSG: {
				final byte[] value1 = data.getByteArray(BLEService.EXTRA_VALUE);

				String __msg = "";
				for (int i = 0; i < value1.length; i++) {
					__msg += "BLE_WRITE_REQUEST_MSG " + i + " : " + value1[i]
							+ "\n";
				}
				;
				Log.v(TAG, __msg);
				// final byte alertLevel = value1[0];
				// Log.d(TAG,
				// "PROXIMITY_ALERT_LEVEL_CHANGED_MSG value of alertlevel is " +
				// value1[0]);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// if (alertLevel != BLEService.NO_ALERT) {
						// try {
						// Uri notification =
						// RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
						// Ringtone r =
						// RingtoneManager.getRingtone(getApplicationContext(),
						// notification);
						// r.play();
						// } catch (Exception e) {
						// Log.e(TAG, e.toString());
						// }
						//
						// switch (alertLevel) {
						// case BLEService.LOW_ALERT:
						// showMessage(getString(R.string.low_alert));
						// break;
						// case BLEService.HIGH_ALERT:
						// showMessage(getString(R.string.high_alert));
						// break;
						// default:
						// Log.e(TAG,"wrong alert level");
						// break;
						// }
						// }
					}
				});
			}
				break;
			case BLEService.BLE_SERVICE_DISCOVER_MSG: {
				serviceList.clear();
				Bundle _data = msg.getData();
				final BluetoothDevice device = _data
						.getParcelable(BluetoothDevice.EXTRA_DEVICE);

				List<BluetoothGattService> services = mService.mBluetoothGatt
						.getServices(device);
				for (int i = 0; i < services.size(); i++) {
					UUID uuid = services.get(i).getUuid();
					// Log.v(TAG, "Services " + i + "UUID: " + uuid);
					serviceList.add(services.get(i));
					mServiceAdapter.notifyDataSetChanged();
				}
				

				
				
				mServiceList.setSelection(0);

			}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	};

	private final BroadcastReceiver proximityStatusChangeReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			final Intent mIntent = intent;
			if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
				BluetoothDevice device = mIntent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				Log.d(TAG, "BluetoothDevice.ACTION_BOND_STATE_CHANGED");
				if (device.equals(mDevice)) {
					runOnUiThread(new Runnable() {
						public void run() {
							setUiState();
						}
					});
				}
			}
			if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				final int state = mIntent.getIntExtra(
						BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				Log.d(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED" + "state is"
						+ state);
				runOnUiThread(new Runnable() {
					public void run() {
						if (state == STATE_OFF) {
							mState = BLE_PROFILE_DISCONNECTED;
							setUiStateForBTOff();
						}
					}
				});
			}
		}
	};

	private void init() {
		Intent bindIntent = new Intent(this, BLEService.class);
		startService(bindIntent);
		bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		this.registerReceiver(proximityStatusChangeReceiver, filter);
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, "onStart() mService= " + mService);
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy()");
		try {
			unregisterReceiver(proximityStatusChangeReceiver);
		} catch (Exception ignore) {
			Log.e(TAG, ignore.toString());
		}
		unbindService(mServiceConnection);
		stopService(new Intent(this, BLEService.class));
		super.onDestroy();
	}

	@Override
	protected void onStop() {
		Log.d(TAG, "onStop");
		super.onStop();
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.d(TAG, "onRestart");
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		if (!mBtAdapter.isEnabled()) {
			Log.i(TAG, "onResume - BT not enabled yet");
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}
		updateUi();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
//grab the result of the CommandListActivity and DeviceListActivity
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {

		case REQUEST_SELECT_DEVICE:
			if (resultCode == Activity.RESULT_OK && data != null) {
				String deviceAddress = data
						.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
				mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(
						deviceAddress);
				Log.d(TAG, "... onActivityResultdevice.address==" + mDevice
						+ "mserviceValue" + mService);
				setUiState();
				mService.connect(mDevice, false);
			}
			break;
			
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				Toast.makeText(this, "Bluetooth has turned on ",
						Toast.LENGTH_SHORT).show();

			} else {
				// User did not enable Bluetooth or an error occurred
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, "Problem in BT Turning ON ",
						Toast.LENGTH_SHORT).show();
				finish();
			}
			break;
		
		case 0:
			if (resultCode == Activity.RESULT_OK && data != null) {
				final byte[] d = data
						.getByteArrayExtra(CommandListActivity.COMMAND_LIST_RETURN);
				if (selectedServiceUUID != null
						&& selectedCharacteristicUUID != null) {
					mService.WriteWristBand(mDevice, selectedServiceUUID,
							selectedCharacteristicUUID, d);
//					List<BluetoothGattService > services = serviceList;
//					for (BluetoothGattService service : services)
//					{
//						List<BluetoothGattCharacteristic>  characteristics = service.getCharacteristics();
//						for (BluetoothGattCharacteristic characteristic : characteristics)
//						{
//							characteristic.setValue(d);
//							
//							characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE );
//							if(! mService.mBluetoothGatt.writeCharacteristic(characteristic))
//							{
//								Log.v(TAG,"Failed "+characteristic.getUuid());
//							}
//						}
//					}
				}
			}
			break;
			
		default:
			Log.e(TAG, "wrong request code");
			break;
		}
	}

	private void updateUi() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				setUiState();
			}
		});
	}

	private void setUiStateForBTOff() {
		Log.d(TAG, "... setUiStateForBTOff.mState" + mState);
		findViewById(R.id.btn_select).setEnabled(true);
		findViewById(R.id.btn_disconnect).setEnabled(false);
		findViewById(R.id.btn_connect).setEnabled(false);
		findViewById(R.id.btn_readinfo).setEnabled(false);

		if (mDevice != null
				&& mDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
			Log.i(TAG, "in no device zone");
			((TextView) findViewById(R.id.deviceName))
					.setText(R.string.no_device);
		}
	}

	private void setUiState() {
		findViewById(R.id.btn_select).setEnabled(
				mState == BLE_PROFILE_DISCONNECTED);
		Log.d(TAG, "... setUiState.mState" + mState);
		findViewById(R.id.btn_disconnect).setEnabled(
				mState == STATE_READY || mState == BLE_PROFILE_CONNECTED);
		findViewById(R.id.btn_readinfo).setEnabled(
				mState == STATE_READY || mState == BLE_PROFILE_CONNECTED);
		if (mDevice != null) {

			findViewById(R.id.btn_connect)
					.setEnabled(
							mState == BLE_PROFILE_DISCONNECTED
									&& mDevice.getBondState() == BluetoothDevice.BOND_BONDED);
		} else
			findViewById(R.id.btn_connect).setEnabled(false);

		if (mDevice != null
				&& mDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
			Log.i(TAG, "in no device zone");
			((TextView) findViewById(R.id.deviceName))
					.setText(R.string.no_device);
		}
		if (mDevice != null
				&& mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
			((TextView) findViewById(R.id.deviceName)).setText(mDevice
					.getName());
		}

		TextView status = ((TextView) findViewById(R.id.statusValue));

		switch (mState) {
		case BLE_PROFILE_CONNECTED:
			Log.i(TAG, "STATE_CONNECTED::device name" + mDevice.getName());
			status.setText(R.string.connected);
			((TextView) findViewById(R.id.deviceName)).setText(mDevice
					.getName());

			break;
		case BLE_PROFILE_DISCONNECTED:
			Log.i(TAG, "disconnected");
			status.setText(R.string.disconnected);
			serviceList.clear();
			mServiceAdapter.notifyDataSetChanged();
			charList.clear();
			mCharacteristicAdapter.notifyDataSetChanged();
			break;
		case STATE_READY:
			status.setText(R.string.ready);
			((TextView) findViewById(R.id.deviceName)).setText(mDevice
					.getName());
			break;

		default:
			Log.e(TAG, "wrong mState");
			break;
		}
	}

	private void showMessage(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onBackPressed() {
		if (mState == STATE_READY) {
			Intent startMain = new Intent(Intent.ACTION_MAIN);
			startMain.addCategory(Intent.CATEGORY_HOME);
			startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(startMain);
		} else {
			new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(R.string.popup_title)
					.setMessage(R.string.popup_message)
					.setPositiveButton(R.string.popup_yes,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									finish();
								}
							}).setNegativeButton(R.string.popup_no, null)
					.show();
		}
	}
//Data adaptor of Serivec list of GATT 
	class ServiceAdapter extends BaseAdapter {
		Context context;
		List<BluetoothGattService> services;
		LayoutInflater inflater;

		public ServiceAdapter(Context context,
				List<BluetoothGattService> services) {
			this.context = context;
			inflater = LayoutInflater.from(context);
			this.services = services;
		}

		@Override
		public int getCount() {
			return services.size();
		}

		@Override
		public Object getItem(int position) {
			return services.get(position);
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
				vg = (ViewGroup) inflater.inflate(R.layout.service_element,
						null);
			}

			BluetoothGattService service = services.get(position);

			final TextView tvname = ((TextView) vg
					.findViewById(R.id.service_name));

			tvname.setVisibility(View.VISIBLE);
			
			tvname.setText(service.getUuid().toString());

			return vg;
		}
	}
	//Data adaptor of Characteristic list of each Service
	class CharacteristicAdapter extends BaseAdapter {
		Context context;
		List<BluetoothGattCharacteristic> characteristices;
		LayoutInflater inflater;

		public CharacteristicAdapter(Context context,
				List<BluetoothGattCharacteristic> Characteristic) {
			this.context = context;
			inflater = LayoutInflater.from(context);
			this.characteristices = Characteristic;
		}

		@Override
		public int getCount() {
			return characteristices.size();
		}

		@Override
		public Object getItem(int position) {
			return characteristices.get(position);
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
				vg = (ViewGroup) inflater.inflate(R.layout.service_element,
						null);
			}

			BluetoothGattCharacteristic c = characteristices.get(position);

			final TextView tvname = ((TextView) vg
					.findViewById(R.id.service_name));

			tvname.setVisibility(View.VISIBLE);
			
			tvname.setText(c.getUuid().toString());

			return vg;
		}
	}

}
