/*******************************************************************************
 * 
 *      This file is part of Beacon Transponder.
 *  
 *      Beacon Transponder is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *  
 *      Beacon Transponder is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *  
 *      You should have received a copy of the GNU General Public License
 *      along with Beacon Transponder.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *      Francesco Gabbrielli 2017
 *      
 ******************************************************************************/
package au.com.smarttrace.beacons;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

/**
 * <p>
 * Bluetooth LE devices discovery service
 * 
 * <p>
 * 
 * @author Francesco Gabbrielli <francescogabbrielli at gmail>
 */
public class BluetoothService extends Service {

	private static final String TAG = BluetoothService.class.getSimpleName();

	private BluetoothManager btManager;
	private BluetoothAdapter btAdapter;
	private BluetoothLeScanner btScanner;
	private ScanSettings btSettings;
	private List<ScanFilter> btFilters;

	public final static String ACTION_BLUETOOTH_STATUS_CHANGE = "action_bluetooth_status_change";
	public final static String KEY_BLUETOOTH_STATUS = "key_bluetooth_status";

	private LocalBinder binder;

	private boolean scanning;

	private ScheduledExecutorService executor;
	ScheduledFuture future;

	/**
	 * Bluetooth callback to onReceive the scan results
	 */
	private ScanCallback scanCallback = new ScanCallback() {

		@Override
		public void onScanResult(int callbackType, ScanResult result) {
			Log.d(TAG, "ScanResult (" + callbackType+"): "+result.toString());
			DeviceManager.getInstance().addDevice(getApplicationContext(), result);
		}

	    @Override
	    public void onBatchScanResults(List<ScanResult> results) {
//	        for (ScanResult sr : results) {
//	        	Log.d("ScanResult - Results", sr.toString());
//	        	DeviceManager.getInstance().addDevice(getApplicationContext(), sr);
//	        }
	    }

		@Override
		public void onScanFailed(int errorCode) {
			Log.w(TAG, "Scan error " + errorCode);
		}

	};

	@Override
	public void onCreate() {
		super.onCreate();
		initBluetooth();
		binder = new LocalBinder();
		scanning = false;
		executor = Executors.newSingleThreadScheduledExecutor();
	}

	@Override
	public synchronized int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "Starting: " + intent);
		connect();
		DeviceManager.getInstance().onBluetoothOn();
		return START_STICKY;
	}

	private void connect()  {
		btScanner = btAdapter.getBluetoothLeScanner();
		if (btScanner!=null && btAdapter.isEnabled()) {
			Log.d(TAG, "START SCANNING!");
			btScanner.startScan(btFilters, btSettings, scanCallback);
			scanning = true;
			sendChange(true);
//			future = executor.scheduleAtFixedRate(this, 0l, 2l, TimeUnit.SECONDS);
		}
	}

	private void disconnect() {
		if (btScanner != null && btAdapter.isEnabled()) {
			scanning = false;
			Log.d(TAG, "STOP SCANNING!");
			btScanner.stopScan(scanCallback);
		}
		if (future!=null && !future.isDone())
			future.cancel(true);
		btScanner = null;
		sendChange(false);
	}

	public class LocalBinder extends Binder {
		public BluetoothService getService() {
			return BluetoothService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "Binding: "+intent);
		connect();
		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.i(TAG, "Unbinding: "+intent);
		return super.onUnbind(intent);
	}

	/** Init bluetooth adapter */
	private void initBluetooth() {
        if (btManager == null)
			btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		btAdapter = btManager.getAdapter();
		ScanSettings.Builder builder = new ScanSettings.Builder()
				.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
			builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
		btSettings = builder.build();
		btFilters = new ArrayList<>();
	}

	@Override
	public synchronized void onDestroy() {
		Log.i(TAG, "Destroying");
		disconnect();
		DeviceManager.getInstance().onBluetoothOff();
		scanning = false;
		executor.shutdownNow();
		super.onDestroy();
	}

	private void sendChange(final boolean status) {
		SharedPreferences prefs = getSharedPreferences(Utils.PREFS, MODE_PRIVATE);
		prefs.edit().putBoolean(Utils.PREF_KEY_BLUETOOTH_SERVICE_ENABLED, status).apply();
		Intent intent = new Intent(ACTION_BLUETOOTH_STATUS_CHANGE);
		intent.putExtra(KEY_BLUETOOTH_STATUS, status);
		LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
	}

	public synchronized boolean isConnected() {
		return btScanner!=null;
	}

	public void connect(Device device) {
		device.connect();
	}

	public void disconnect(Device device) {
		device.disconnect();
	}

//	public BluetoothAdapter getAdapter() {
//		return btAdapter;
//	}

}
