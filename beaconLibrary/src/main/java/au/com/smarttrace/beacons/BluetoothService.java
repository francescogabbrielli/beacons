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

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * <p>
 * Bluetooth devices discovery service
 * 
 * <p>
 * 
 * 
 * @author Francesco Gabbrielli <francescogabbrielli at gmail>
 */
public class BluetoothService extends Service {
	
	private static final String TAG = BluetoothService.class.getCanonicalName();
	
	private BluetoothManager btManager;
	private BluetoothAdapter btAdapter;
	private BluetoothLeScanner btScanner;
	private ScanSettings btSettings;
	private List<ScanFilter> btFilters;

	/**
	 * Bluetooth callback to receive the scan results
	 */
	private ScanCallback scanCallback = new ScanCallback() {
		
		@Override
		public void onScanResult(int callbackType, ScanResult result) {
			Log.v(TAG, "ScanResult (" + callbackType+"): "+result.toString());
			DeviceManager.getInstance().addDevice(getApplicationContext(), result);
		}
		
	    @Override
	    public void onBatchScanResults(List<ScanResult> results) {
	        for (ScanResult sr : results) {
	        	Log.v("ScanResult - Results", sr.toString());
	        	DeviceManager.getInstance().addDevice(getApplicationContext(), sr);
	        }
	    }
	    
		@Override
		public void onScanFailed(int errorCode) {
			Log.w(TAG, "Error " + errorCode);
		}
		
	};
	
	public class LocalBinder extends Binder {
		public BluetoothService getService() {
			return BluetoothService.this;
		}
	}
	
	private LocalBinder binder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	/** Init bluetooth adapter */
	private void initBluetooth() {
        if (btManager == null)
			btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		btAdapter = btManager.getAdapter();
		ScanSettings.Builder builder = new ScanSettings.Builder()
				.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
			builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
		btSettings = builder.build();
		btFilters = new ArrayList<>();
	}

	@Override
	public void onCreate() {
	    super.onCreate();
		initBluetooth();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
//		if (intent!=null)
//			Toast.makeText(getApplicationContext(), R.string.bluetooth_service_start, Toast.LENGTH_LONG).show();
//		else
//			Toast.makeText(getApplicationContext(), "RESTARTING BLUETOOTH", Toast.LENGTH_SHORT).show();
		Log.i(TAG, "BLE START SCANNING... " + intent);
		btScanner = btAdapter.getBluetoothLeScanner();
		btScanner.startScan(btFilters, btSettings, scanCallback);
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		if (btScanner!=null) {
			if (btAdapter.isEnabled())
				btScanner.stopScan(scanCallback);
//			Toast.makeText(getApplicationContext(), R.string.bluetooth_service_stop, Toast.LENGTH_LONG).show();
			Log.i(TAG, "...BLE STOP SCANNING");
		}
		super.onDestroy();
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
