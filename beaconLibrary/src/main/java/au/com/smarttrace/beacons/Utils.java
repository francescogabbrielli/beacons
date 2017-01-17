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

import android.bluetooth.BluetoothGatt;

public class Utils {
	
	public final static int GATT_ERROR = 0x85;
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		if (bytes==null)
			return "null";
		if (bytes.length==0)
			return "";
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	public static String getStatusMessage(int status) {
		switch(status) {
		case BluetoothGatt.GATT_SUCCESS:
			return "Operation successful";
		case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
			return "Insufficient authentication";
		case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
			return "Insufficient encryption";
		case BluetoothGatt.GATT_CONNECTION_CONGESTED:
			return "Device congested";
		case BluetoothGatt.GATT_INVALID_OFFSET:
			return "Invalid offset";
		case BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH:
			return "Wrong attribute length"; 
		case BluetoothGatt.GATT_READ_NOT_PERMITTED:
			return "Read not permitted";
		case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
			return "Write not permitted";
		case BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED:
			return "Not supported";
		case GATT_ERROR:
			return "GATT error!";
		case BluetoothGatt.GATT_FAILURE:
			return "Operation failed";
		default:
			return "Unknown error code: "+status; 
		}
	}	

}
