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
package au.com.smarttrace.beacons.transponder;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

import au.com.smarttrace.beacons.BluetoothService;
import au.com.smarttrace.beacons.DeviceManager;
import au.com.smarttrace.beacons.transponder.gps.GPSDevice;
import au.com.smarttrace.beacons.transponder.gps.LocationService;

public class BeaconTransponder extends Application {
	
	public final static String TAG = BeaconTransponder.class.getSimpleName();
	
	public BeaconTransponder() {
		
	}

	public boolean isLocationEnabled() {
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
				|| locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
	}

	public boolean isBluetoothEnabled() {
		BluetoothManager bm = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
		BluetoothAdapter ba = bm.getAdapter();
		return ba!=null && ba.isEnabled();
	}

	
	@Override
	public void onCreate() {
		
		super.onCreate();
		
//		stopService(new Intent(this, BluetoothService.class));
		
		// check if bluetooth is on and start the bluetooth service
		if (isBluetoothEnabled()) {
			DeviceManager.getInstance().onBluetoothOn();
			startService(new Intent(this, BluetoothService.class));
		}

		// check if GPS is on and start the location service
		if (isLocationEnabled()) {
			startService(new Intent(this, LocationService.class));
			DeviceManager.getInstance().addInternalDevice(this, GPSDevice.class);
		}
		
	}
	
}
