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

import java.util.EventObject;

/**
 * A Device change event to use with a {@link DeviceListener}
 */
public class DeviceEvent extends EventObject {

	private static final long serialVersionUID = 7091772815689090943L;
	
	/** Added new device */
	public static final int TYPE_DEVICE_ADDED = 1;
	/** Updated a device */
	public static final int TYPE_DEVICE_UPDATED = 2;
	/** Removed a device */
	public static final int TYPE_DEVICE_REMOVED = 3;
	/** Connected to a device */
	public static final int TYPE_DEVICE_CONNECTED = 4;
	/** Disconnected from a device */
	public static final int TYPE_DEVICE_DISCONNECTED = 5;
	
	public static final int TYPE_DEVICE_PROGRESS = 7;	
	/** Device error */
	public static final int TYPE_DEVICE_ERROR = 9;
	
	/** Event type */
	protected int type;
	
	/** Event data */
	protected Object data;

	/**
	 * A device event
	 * 
	 * @param source 
	 *			The device scan result
	 * @param type
	 * 			the type of event
	 * 
	 * @see #TYPE_DEVICE_ADDED
	 * @see #TYPE_DEVICE_UPDATED
	 * @see #TYPE_DEVICE_REMOVED
	 * @see #TYPE_DEVICE_CONNECTED
	 * @see #TYPE_DEVICE_DISCONNECTED
	 */
	public DeviceEvent(Device source, int type) {
		this(source, type, null);
	}
	
	/**
	 * A device event
	 * 
	 * @param source 
	 *				The device scan result
	 * @param type
	 * 				The type of event
	 * @param data
	 * 				The data associated with the event
	 * 
	 * @see #TYPE_DEVICE_ADDED
	 * @see #TYPE_DEVICE_UPDATED
	 * @see #TYPE_DEVICE_REMOVED
	 * @see #TYPE_DEVICE_CONNECTED
	 * @see #TYPE_DEVICE_DISCONNECTED
	 */
	public DeviceEvent(Device source, int type, Object data) {
		super(source);
		this.type = type;
		this.data = data;
	}
	
	/**
	 * The device that originated the event
	 * 
	 * @return
	 * 			the device
	 */
	public Device getDevice() {
		return (Device) getSource();
	}
	
	/**
	 * An event type
	 * 
	 * @return
	 * 			the type
	 * 
	 * @see #TYPE_DEVICE_ADDED
	 * @see #TYPE_DEVICE_UPDATED
	 * @see #TYPE_DEVICE_REMOVED
	 * @see #TYPE_DEVICE_CONNECTED
	 * @see #TYPE_DEVICE_DISCONNECTED
	 */
	public int getType() {
		return type;
	}
	
	/**
	 * Get the message associated to the event, if any
	 * 
	 * @return
	 * 			the event message
	 */
	public Object getData() {
		return data;
	}
	

}
