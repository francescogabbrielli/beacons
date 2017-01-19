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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import au.com.smarttrace.beacons.tracker.Recording;
import au.com.smarttrace.beacons.tracker.Tracking;

/**
 * Manage scanned devices in an ordered list and notify listeners for changes
 * happening in any of these devices.
 *
 * @see DeviceListener
 * @see DeviceEvent
 */
public class DeviceManager {
	
	/** Log tag */
	private final static String TAG = DeviceManager.class.getSimpleName();
	
	private final static long DELAY_CHECK = 10000l;//10 sec -> TODO: configure
	private final static long TIME_LIMIT = 18 * DELAY_CHECK;//3 min -> TODO: configure
	
	/** The singleton instance */
	private static DeviceManager instance;
	
	/** The list of devices */
	private ArrayList<Device> list;
	
	/** A thread for the timer */
	private HandlerThread timerThread;
	
	/** Handler for the event firing on the separate event thread */
	private Handler timerHandler;
		
	/** The main handler (main thread) */
	private Handler mainHandler;

	/** Listeners for changes in the devices */
	private List<DeviceListener> listenersList;
	
	/** 
	 * Synchronise a task on this DeviceManager lock 
	 */
	private abstract class SynchronizedTask implements Runnable {
		
		/** The device involved in the task, if any */
		protected Device device;
		
		/**
		 * Default constructor
		 */
		public SynchronizedTask() {}
		
		/**
		 * Device construcor
		 * 
		 * @param d
		 * 			the device
		 */
		protected SynchronizedTask(Device d) {this.device = d;}
		
		@Override
		public void run() {
			synchronized (DeviceManager.this) {syncRun();}
		}
		
		/**
		 * Override to implement synchronised operations 
		 */
		protected abstract void syncRun();
	}

	/**
	 *  Check the devices at regular intervals (on the event thread!)
	 *  synchronising the main thread on the manager instance.
	 *  
	 *  XXX: For now just removes too long inactive devices
	 */
	private Runnable timedCheck = new SynchronizedTask() {
		@Override
		public void run() {
			super.run();
			timerHandler.postDelayed(timedCheck, DELAY_CHECK);
		}
		@Override
		protected void syncRun() {
			for (Device d : list) {
				if (d instanceof InternalDevice)
					continue;
				long diff = d.getElapsedTime();
				if (diff > TIME_LIMIT) {
					Log.i(TAG, "Device inactive for "+(diff/1000l)+"s (" + d.getTitle()+")");
					mainHandler.post(new Remover(d));
				}
			}
		}
	};

	/** 
	 * Remove a device synchronising the main thread on the manager instance 
	 */
	private class Remover extends SynchronizedTask {
		Remover(Device d) {super(d);}
		@Override
		public void syncRun() {
			int pos = Collections.binarySearch(list, device);
			if (pos>=0) {
				list.remove(pos);
				fireDeviceEvent(device, DeviceEvent.TYPE_DEVICE_REMOVED);
				Log.d(TAG, "Removed device (pos. "+pos+"): "+device.getTitle());
				device.onScanStop();
			}
		}
	}
	
	/** 
	 * Add a device synchronising the main thread on the manager instance.
	 * The device will only be updated if it is already present in the list 
	 */
	private class Adder extends SynchronizedTask {
		Adder(Device d) {super(d);}		
		@Override
		public void syncRun() {
			
			int pos = Collections.binarySearch(list, device);
			
			// update a device already present
			if (pos>=0) {
				
				Device old = list.get(pos);
				long elapsed = old.getElapsedTime()/1000l;
				old.setScanResult(device.getScanResult());
				fireDeviceEvent(device, DeviceEvent.TYPE_DEVICE_UPDATED);
				Log.d(TAG, "Updated device (pos. "+pos+") after "
						+ elapsed + "s: " 
						+ device.getTitle());
				
			// insert a new device 
			} else {
				
				try {
					// replace the fake device used just for sorting purposes 
					// with the actual device built by the "factory" 
					device = DeviceFactory.getInstance().newDevice(device);
					if(device.getClass()!=Device.class) {//TODO: configure in settings
						list.add(-pos - 1, device);
						fireDeviceEvent(device, DeviceEvent.TYPE_DEVICE_ADDED);
						Log.d(TAG, "Added device (pos. " + (-pos - 1) + "): " + device.getTitle());
					}

				} catch(NoSuchDeviceException e) {
					Log.e(TAG, "Unrecognised device?", e);
				}
			}

			device.onScan();

		}
	}
	
	/**
	 * Private singleton constructor
	 */
	private DeviceManager() {
		
		listenersList = new LinkedList<DeviceListener>();
		
		list = new ArrayList<Device>();
		timerThread = new HandlerThread("Device Timer Thread");
		timerThread.start();
		timerHandler = new Handler(timerThread.getLooper());
		mainHandler = new Handler(Looper.getMainLooper());

	}
	
	/**
	 * Get the singleton instance
	 * 
	 * @return
	 * 			the instance
	 */
	public static DeviceManager getInstance() {
		if (instance==null)
			instance = new DeviceManager();
		return instance; 
	}
	
	/**
	 * Remove a {@link DeviceListener}
	 * 
	 * @param listener
	 * 				the listener
	 */
	public synchronized void addDeviceListener(DeviceListener listener) {
		if (!listenersList.contains(listener))
			listenersList.add(listener);
	}
	
	/**
	 * Add a new {@link DeviceListener}
	 * 
	 * @param listener
	 * 				the listener
	 */
	public synchronized void removeDeviceListener(DeviceListener listener) {
		listenersList.remove(listener);
	}
	
	/**
	 * Fire an event to all interested listeners
	 * 
	 * @param source
	 * 				the device source of the event
	 * @param eventType
	 * 				the type of event
	 */
	void fireDeviceEvent(Device source, int eventType) {
		fireDeviceEvent(source, eventType, null);
	}

	/**
	 * Fire an event to all interested listeners
	 *
	 * @param source
	 * 				the device source of the event
	 * @param eventType
	 * 				the type of event
	 * @param data
	 * 				the data of the event
	 */
	void fireDeviceEvent(Device source, int eventType, Object data) {
		final DeviceEvent event = new DeviceEvent(source, eventType, data);
		mainHandler.post(new SynchronizedTask() {
			@Override
			protected void syncRun() {
				for (DeviceListener listener : listenersList)
					listener.onChange(event);
			}
		});
	}
	
	/**
	 * Called when bluetooth is turned on
	 * //TODO find a better solution
	 */
	public synchronized void onBluetoothOn() {
		timerHandler.post(timedCheck);
	}
	
	/**
	 * Called when bluetooth is turned off
	 * //TODO find a better solution
	 */
	public synchronized void onBluetoothOff() {
		timerHandler.removeCallbacks(timedCheck);
		for (ListIterator<Device> li = list.listIterator(); li.hasNext() ;) {
			li.next().onScanStop();
			li.remove();
		}
	}
	
	/**
	 * Add a new managed device, synchronising on the main thread
	 * 
	 * @param context
	 * 				the application context, needed for UI components
	 * @param result
	 * 				the device, as scanned
	 * 
	 * @see Device
	 */
	public void addDevice(Context context, ScanResult result) {

		if (result.getDevice().getType()!=BluetoothDevice.DEVICE_TYPE_LE)
			return;
		
		mainHandler.post(
			new Adder(
				new Device(context, result)));
	}

	public synchronized void addInternalDevice(Context context, Class<? extends InternalDevice> deviceClass) {
		try {
			InternalDevice device = deviceClass.newInstance();
			device.init(context, null);
			int pos = Collections.binarySearch(list, device);
			if (pos<0) {
				list.add(-pos - 1, device);
				fireDeviceEvent(device, DeviceEvent.TYPE_DEVICE_ADDED);
			}
		} catch(Exception e) {
			Log.e(TAG, "Cannot add device "+deviceClass.getSimpleName(), e);
		}
	}

	public synchronized void removeInternalDevice(String identifier) {
		InternalDevice temp = new InternalDevice(identifier);
		int pos = Collections.binarySearch(list, temp);
		if (pos>=0) {
			Device removed = list.remove(pos);
			fireDeviceEvent(removed, DeviceEvent.TYPE_DEVICE_REMOVED);
		}
	}
	
	/**
	 * Return the discovered devices
	 * 
	 * @return
	 * 			the number of devices 
	 */
	public synchronized int countDevices() {
		return list.size();
	}
	
	/**
	 * Get a device by position in the list  
	 * 
	 * @param position
	 * 				the position in the sorted list
	 * @return
	 * 			the {@link Device}
	 * @throws
	 * 			ArrayIndexOutOfBoundsException
	 */
	public synchronized Device getDevice(int position) {
		return list.get(position);
	}
	
	/**
	 * Get a device by its identifier (unique?)
	 * 
	 * @param identifier
	 * 				the identifier
	 * @return
	 * 			the {@link Device}
	 * @throws
	 * 			NoSuchDeviceException if not found
	 */
	public synchronized Device getDevice(String identifier) throws NoSuchDeviceException {
		for (Device device : list)
			if (device.getId().equals(identifier))
				return device;
		throw new NoSuchDeviceException("ID="+identifier);
	}

	private Recording recording;

	public boolean isRecording() {
		return recording!=null;
	}

	/**
	 * Start tracking all the devices listed
	 *
	 * TODO: configure user based and config based devices
	 */
	public synchronized void startTracking() {
		recording = new Recording();
		addDeviceListener(recording);
		for (Device d : list)
			recording.addDevice(d);
	}

	public synchronized void stopTracking() {
		if (recording!=null) {
			recording.stop();
			removeDeviceListener(recording);
			recording = null;
		}
	}

}
