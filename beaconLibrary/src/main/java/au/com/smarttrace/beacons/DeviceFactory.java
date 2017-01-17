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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.util.Log;

/**
 * Simple factory pattern for plugging in different manufacturers devices.
 * 
 * @author Francesco Gabbrielli 
 */
public class DeviceFactory {
	
	private static final String TAG = DeviceFactory.class.getCanonicalName();
	
	/**
	 * The static singleton instance
	 */
	private static DeviceFactory instance;
	
	/**
	 * Private singleton constructor
	 */
	private DeviceFactory() {
		
	}
	
	/**
	 * Get the {@link DeviceFactory} instance
	 * 
	 * @return
	 * 			the singleton instance
	 */
	public synchronized static DeviceFactory getInstance() {
		if (instance==null)
			instance = new DeviceFactory();
		return instance;
	}
	
	/**
	 * <p>
	 * Get the device management classes for a particular bluetooth device.
	 * 
	 * <p>
	 * Check the configuration in the devices library - included in the string 
	 * resources - to map the exact {@link Device} management class for each 
	 * {@link BluetoothDevice} device.
	 * 
	 * 
	 * @param context
	 * 				the current application context
	 * @param deviceResult
	 * 				the bluetooth device
	 * @return
	 * 				the manufacturer's specific {@link Device} instance. A generic device 
	 * 				instance will be returned in case the device is not managed yet.
	 *    
	 * @throws NoSuchDeviceException
	 * 				if the device is misconfigured
	 */
	public Device newDevice(Context context, ScanResult deviceResult) throws NoSuchDeviceException {
		
		//XXX: Temporarily is using only the device name to identify a device configuration 
		String name = String.valueOf(deviceResult.getDevice().getName()).replaceAll("\\.", "_");
		Device ret = null;
		
		try {
			
			int cn = context.getResources().getIdentifier(
					name.toLowerCase(), "string", context.getPackageName());
			
			ret = cn==0 ? new Device() : (Device) 
					Class.forName(context.getResources().getString(cn))
					.newInstance();
			
		} catch(NoClassDefFoundError e) {
			
			Log.e(TAG, "Class not found for "+name, e);
			throw new NoSuchDeviceException("Device not configured properly: '"+name+"'", e);
			
		} catch(Exception e) {
			
			Log.e(TAG, "Unexpected error for device '"+name+"'", e);
			throw new NoSuchDeviceException("Unexpected error for device '"+name+"'", e);
			
		}
		
		return ret.init(context, deviceResult, name);
		
	}
	
	/**
	 * Make an actual new device from a temporary one
	 * 
	 * @see DeviceFactory#newDevice(Context, ScanResult)
	 */
	public Device newDevice(Device temp) throws NoSuchDeviceException {
		return newDevice(temp.context, temp.deviceResult);
	}
	
}
