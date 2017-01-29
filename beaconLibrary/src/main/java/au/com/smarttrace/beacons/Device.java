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

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.TextView;
import android.widget.Toast;

import au.com.smarttrace.beacons.tracker.Tracking;

/**
 * <p>
 * The base class for a device managed by the application 
 * 
 * <p>
 * Implementations would need to override the GATT server callback methods to
 * implement the communication
 * 
 * <p>
 * GATT callbacks are multicasted from this device by adding new 
 * {@link BluetoothGattCallback}s  
 */
public class Device extends BluetoothGattCallback implements Comparable<Device> {

	public static final String KEY_LOCATION			= "location";
	public static final String KEY_TEMPERATURE		= "temperature";
	public static final String KEY_HUMIDITY			= "humidity";
	public static final String KEY_LIGHT 			= "light";

	public final static int PROGRESS_NONE = -1;
	public final static int PROGRESS_COMPLETE = 100;
	public final static int PROGRESS_INDEFINITE = Integer.MAX_VALUE;
	
	/** Application context */
	protected Context context;
	
	/** The bluetooth device as scanned by the adapter */
	protected ScanResult deviceResult;
	
	/** Connection state */
	private int connectionState;
	
	/** Connection error detected */
	private boolean connectionError;
	
	/** Name to identify string resources in the configuration */
	protected String name;
	
	/** Raw scan data bytes */ 
	protected byte[] bytes; 
	
	/** Raw scan data in hex format */
	protected String scanData; 
	
	/** Last time device interacted */ 
	protected long lastTime;
	
	/** Battery level XXX:implementation specific? */
	protected int battery;
	
	/** Serial number XXX:implementation specific? */
	protected String serialNumber;
	
	/** Gatt server connection */
	protected BluetoothGatt gatt;
	
	/** Additional GATT callbacks */
	private List<BluetoothGattCallback> callbacks;
	
	/** 
	 * The current GATT operation progress:
	 * <ul>
	 * <li><b>0-100</b> = current operation progress;
	 * <li><b>INFINITY</b> = indefinite progress;
	 * <li><b>-1</b> = no current operation;
	 * </ul>
	 */
	private int gattProgress = PROGRESS_NONE;
	
	
	/** Standard empty constructor (does nothing) */
	public Device() {
		callbacks = new LinkedList<BluetoothGattCallback>();
	}
	
	/**
	 * Generic device constructor
	 * 
	 * @param context
	 * 				the application context
	 * @param deviceResult
	 * 				the bluetooth device, as scanned by the adapter
	 */
	public Device(Context context, ScanResult deviceResult) {
		init(context, deviceResult);
	}	
	
	/**
	 * Initialise a "fake" device
	 * 
	 * @param context
	 * 				the application context
	 * @param deviceResult
	 * 				the bluetooth device, as scanned by the adapter
	 * @return
	 * 			this
	 */
	public Device init(Context context, ScanResult deviceResult) {
		return init(context, deviceResult, null);
	}
	
	/**
	 * Initialise an actual device
	 * 
	 * @param context
	 * 				the application context
	 * @param deviceResult
	 * 				the bluetooth device, as scanned by the adapter
	 * @param name
	 * 				the name that identifies string resources in the configuration
	 * @return
	 * 			this
	 */
	public Device init(Context context, ScanResult deviceResult, String name) {
		this.context = context;
		this.name = name;
		battery = -1;
		serialNumber = "";
		setScanResult(deviceResult);
		return this;
	}
	
	public BluetoothGatt getGatt() {
		return gatt;
	}
	
	public int getGattProgress() {
		return gattProgress;
	}
	
	/**
	 * Get last scan result
	 * 
	 * @return
	 * 			the scan result
	 */
	public ScanResult getScanResult() {
		return deviceResult;
	}
	
	/**
	 * Update the device from a new scan result
	 * 
	 * @param deviceResult
	 * 				the new scan result
	 */
	public synchronized void setScanResult(ScanResult deviceResult) {
		if (deviceResult!=null) {
			this.deviceResult = deviceResult;
			bytes = deviceResult.getScanRecord().getBytes();
			scanData = Utils.bytesToHex(bytes);
			lastTime = deviceResult.getTimestampNanos();
		}
	}

    public Context getContext() {
        return context;
    }

    /**
	 * Unique identifier for a device (XXX: experimental implementation)
	 * 
	 * @return
	 * 			a string representation of the identifier
	 */
	public String getId() {
		return deviceResult.getDevice().getAddress();
	}
	
	public String getSerialNumber() {
		return serialNumber;
	}
	
	public int getBattery() {
		return battery;
	}
	
	public int getConnectionState() {
		return connectionState;//XXX: local copy of connection state
	}
	
	public boolean isAutoConnect() {
		return false;
	}
	
	/**
	 * Title of the device displayed on the main device tab
	 * 
	 * @return
	 * 			the title string resource
	 */
	public int getTitle() {
		return 0;
	}

	/**
	 * TODO: calculate TX - RX
	 * 
	 * @return
	 * 			signal strength
	 */
	public int getSignal() {
		return deviceResult.getRssi();
	}
	
	/**
	 * Return the time that has elapsed since the time the device was last seen
	 * 
	 * @return
	 * 			elapsed time in millis
	 */
	public long getElapsedTime() {
		return (SystemClock.elapsedRealtimeNanos() - lastTime) / 1000000l;
	}

	public long getLastSeen() {
//		long bootTime = System.currentTimeMillis()-SystemClock.elapsedRealtime();
//		return lastTime/1000000l - bootTime;TODO: time independent of System
		return System.currentTimeMillis() - getElapsedTime();
	}

	/**
	 * Utility method to build UUID from a resource
	 * 
	 * @param fromResource
	 * 				string resource id
	 * @return
	 * 			the UUID
	 */
	protected UUID getUUID(int fromResource) {
		return UUID.fromString(context.getResources().getString(fromResource));
	}
	
	/**
	 * Utility method to build UUID from a resource
	 * 
	 * @param fromString
	 * 				the resource string 
	 * @return
	 * 			the UUID
	 */
	protected UUID getUUID(String fromString) {
		int id = context.getResources()
				.getIdentifier(name + "_" + fromString, "string", context.getPackageName());
		if (id==0)
			Log.e(toString(), "Resource error "+name+"_"+fromString);
		return getUUID(id);
	}
	
	
//_____________________________________________________________________________
//
// o <editor-fold desc="-> UI CUSTOMISATION">
// |
// V
	
	/**
	 * 
	 * @return
	 */
	public Class<? extends Activity> getMainActivity() {
		return DeviceActivity.class;
	}
	
	/**
	 * Expand the content from the stub 
	 * 
	 * @param stub
	 * 				the stub
	 * @return 
	 * 			the content
	 */
	//XXX: reconsider all this implementation
	public View getRowContent(ViewStub stub, View content) {
		if (stub!=null && content==null) {
			Log.v(toString(), "Inflating stub...");
			stub.setLayoutResource(getRowContentLayout());
			stub.setInflatedId(R.id.device_row_content);
			content = stub.inflate();
			Log.v(toString(), "Inflated stub ->" + content);
		} else
			Log.v(toString(), "Stub Content ->" + content);
		populateRowContent(content);
		return content;
	}
	
	/**
	 * <p>
	 * Get the content layout for this device in the device list
	 * <p>
	 * Implementations will override this method
	 */
	protected int getRowContentLayout() {
		return R.layout.row_device_content;
	}
	
	/**
	 * <p>
	 * populate the content view in the device list
	 * <p>
	 * Implementations will override this method
	 * 
	 * @param content
	 * 				the content view to populate
	 */
	protected void populateRowContent(View content) {
		TextView tv = (TextView) content.findViewById(R.id.device_id);
		tv.setText(getId());
		tv = (TextView) content.findViewById(R.id.device_scan);
		tv.setText(scanData);
	}
	
// |
// V
// o END UI CUSTOMISATION</editor-fold>
//_____________________________________________________________________________

	
	/**
	 * Connect to this device
	 */
	public synchronized void connect() {
		if (gatt==null) {
			connectionState = BluetoothProfile.STATE_CONNECTING;
			gatt = deviceResult.getDevice().connectGatt(context, false, this);
		} else
			Log.w(toString(), "Try to reconnect?");
	}
	
	/**
	 * Disconnect from this device
	 */
	public synchronized void disconnect() {
		if (gatt!=null) {
			connectionState = BluetoothProfile.STATE_DISCONNECTING;
			//gatt.disconnect();
			gatt.close();
			gatt = null;
		}
	}
	
	@Override
	public int compareTo(Device other) {
		return getId().compareTo(other.getId());
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof Device && ((Device) obj).getId().equals(getId());
	}
	
	@Override
	public int hashCode() {
		return getId().hashCode();
	}

	@Override
	public String toString() {
		int id = getTitle();
		if (id!=0)
			return context.getResources().getString(id);
		String name = String.valueOf(deviceResult.getDevice().getName());
		if ("null".equalsIgnoreCase(name) || "".equals(name))
			name = "<"+deviceResult.getDevice().getAddress()+">";
		return name;
	}
	
	/**
	 * Notify interested listeners in change to the device originated
	 * in the device (readings)
	 * 
	 * TODO: implements internal listeners? 
	 */
	protected void fireUpdate() {
		lastTime = SystemClock.elapsedRealtimeNanos();
		DeviceManager.getInstance()
				.fireDeviceEvent(this, DeviceEvent.TYPE_DEVICE_UPDATED);		
	}

	protected void fireUpdate(String message) {
		lastTime = SystemClock.elapsedRealtimeNanos();
		DeviceManager.getInstance()
				.fireDeviceEvent(this, DeviceEvent.TYPE_DEVICE_UPDATED, message);
	}

	protected void fireError(int message) {
		fireError(context.getResources().getString(message));
	}
	
	protected void fireError(String message) {
		fireEvent(DeviceEvent.TYPE_DEVICE_ERROR, message);
		Log.w(toString(), message);
	}
	
	protected void fireEvent(int type) {
		DeviceManager.getInstance()
			.fireDeviceEvent(this, type, null);
	}
	
	protected void fireEvent(int type, Object data) {
		DeviceManager.getInstance()
			.fireDeviceEvent(this, type, data);
	}
		
	public synchronized void addGattCallback(BluetoothGattCallback callback) {
		callbacks.add(callback);
	}
	
	public synchronized void removeGattCallback(BluetoothGattCallback callback) {
		callbacks.remove(callback);
	}
	

	class ReadLock {UUID uuid;}
	private final static long TIMEOUT_READ_ALL = 10000;
	private final ReadLock readLock = new ReadLock();
	private AsyncTask<Void, Integer, Exception> activeTask;
	public AsyncTask<Void, Integer, Exception> getActiveTask() {
		return activeTask;
	}
	
	/**
	 * Start reading all characteristics of a service
	 * 
	 * @param service
	 * 					the service to read from
	 */
	protected void readAllCharacteristics(final BluetoothGattService service) {
		
		if (activeTask!=null)
			return;
		
		activeTask = new AsyncTask<Void, Integer, Exception>() {
			@Override
			protected Exception doInBackground(Void... params) {
				
				synchronized (readLock) {
					
					try {
						
						long t = System.currentTimeMillis();
						List<BluetoothGattCharacteristic> list = 
								service.getCharacteristics();
						int tot = list.size();
						int count = 0;
						for (BluetoothGattCharacteristic characteristic : list) { 
							if (isCancelled())
								break;
							if (gatt.readCharacteristic(characteristic)) {
								readLock.uuid = characteristic.getUuid();
								publishProgress((count * 100) / tot);
								while(readLock.uuid!=null)
									readLock.wait(TIMEOUT_READ_ALL);
								if(System.currentTimeMillis()-t > TIMEOUT_READ_ALL)
									return new TimeoutException();
							}
							count++;
						}
						publishProgress(PROGRESS_COMPLETE);
						
					} catch(Exception e) {
						
						return e;
						
					}
				}
				
				return null;
				
			}
			
			@Override
			protected void onProgressUpdate(Integer... values) {
				gattProgress = values[0];
				fireEvent(DeviceEvent.TYPE_DEVICE_PROGRESS);
			}
			
			@Override
			protected void onCancelled() {
				gattProgress = PROGRESS_NONE;
				fireEvent(DeviceEvent.TYPE_DEVICE_PROGRESS);
			}
			
			@Override
			protected void onPostExecute(Exception result) {
				if (result!=null) {
					Toast.makeText(context, result.getMessage(), Toast.LENGTH_SHORT).show();
					Log.e(toString(), "Error while reading characteristics for service "+service.getUuid(), result);
				}
				gattProgress = PROGRESS_NONE;
				activeTask = null;
				fireEvent(DeviceEvent.TYPE_DEVICE_PROGRESS);
			}
			
		}.execute();
				
	}

	protected void runOnUIThread(Runnable r) {
		new Handler(context.getMainLooper()).post(r);
	}

	protected void runOnUIThread(Runnable r, long delay) {
		new Handler(context.getMainLooper()).postDelayed(r, delay);
	}

//____________________________________________________________________________
//
// o <editor-fold desc="-> APPLICATION CALLBACKS IMPLEMENTATION" defaultstate="collapsed">
// |
// V

	/**
	 * Called when a new scan of the device is performed by the bluetooth adapter
	 */
	public void onScan() {

	}

	/**
	 * Called when the device is removed from the scanned list
	 *
	 * @see DeviceManager
	 */
	public void onScanStop() {

	}

    private Tracking tracking;

    public boolean isTracking() {
        return tracking!=null;
    }

	/**
	 * Add a sample to the current tracking (if the device is being tracked)
	 * at the current time (now)
	 *
	 * @param sampleKey
	 * 				the data key
	 * @param sampleValue
	 * 				the data
	 * @param <T>
	 *     		type of data (unused at this leve)
     */
	protected synchronized <T> void addSample(String sampleKey, T sampleValue) {
		if (tracking!=null)
			tracking.addSample(sampleKey, sampleValue);
	}

	/**
	 * Add a sample to the current tracking (if the device is being tracked)
	 * at the specified time
	 *
	 * @param time
	 * 				the time
	 * @param sampleKey
	 * 				the data key
	 * @param sampleValue
	 * 				the data
	 * @param <T>
	 *     		type of data (unused at this leve)
	 */
	protected synchronized <T> void addSample(long time, String sampleKey, T sampleValue) {
		if (tracking!=null)
			tracking.addSample(time, sampleKey, sampleValue);
	}

	/**
	 * Called when the tracking is started
	 */
	public synchronized void onTrackingStart(Tracking tracking) {
        this.tracking = tracking;
	}

	/**
	 * Called when the tracking is stopped
	 */
	public synchronized void onTrackingStop() {
        tracking = null;
	}

// |
// V
// o END APPLICATION CALLBACKS</editor-fold>
//_____________________________________________________________________________

//_____________________________________________________________________________
//
// o <editor-fold desc="-> GATT CALLBACK IMPLEMENTATION" defaultstate="collapsed">
// |
// V

	@Override
    public synchronized void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {

		super.onConnectionStateChange(gatt, status, newState);
		
		Log.i(toString(), "Connection state change " + connectionState + " -> " + newState
				+ ": " + Utils.getStatusMessage(status));
		
		// disconnect in case of wrong connection change 
		if (status!=BluetoothGatt.GATT_SUCCESS) {
			// avoid loops!
			if (!connectionError) {
				disconnect();
				DeviceManager.getInstance().fireDeviceEvent(this,
						DeviceEvent.TYPE_DEVICE_DISCONNECTED, 
						Utils.getStatusMessage(status));
			}
			connectionError = true;
			return;
		}

		connectionError = false;
    	connectionState = newState;
        switch (newState) {
            case BluetoothProfile.STATE_CONNECTED:
				runOnUIThread(new Runnable() {
					@Override
					public void run() {
						gatt.discoverServices();
					}
				});
                Log.i(toString(), "Connected");
                DeviceManager.getInstance().fireDeviceEvent(this, DeviceEvent.TYPE_DEVICE_CONNECTED);
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                Log.i(toString(), "Disconnected");
                DeviceManager.getInstance().fireDeviceEvent(this, DeviceEvent.TYPE_DEVICE_DISCONNECTED);
                Device.this.gatt = null;
                break;
        }
		
		for (BluetoothGattCallback callback : callbacks)
			callback.onConnectionStateChange(gatt, status, newState);
        
    }
	
	@Override
	public synchronized void onServicesDiscovered(BluetoothGatt gatt, int status) {
		
		super.onServicesDiscovered(gatt, status);
		
		Log.i(toString(), String.format(
				"Services discovered: %s", Utils.getStatusMessage(status)));
		
		for (BluetoothGattCallback callback : callbacks)
			callback.onServicesDiscovered(gatt, status);
	}
	
	@Override
	public synchronized void onCharacteristicChanged(BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic) {
		
		super.onCharacteristicChanged(gatt, characteristic);
		
		Log.i(toString(), String.format(
				"Characteristic changed %s=%s",
					characteristic.getUuid(),
					Utils.bytesToHex(characteristic.getValue())));
		
		for (BluetoothGattCallback callback : callbacks)
			callback.onCharacteristicChanged(gatt, characteristic);
	}
	
	@Override
	public synchronized void onCharacteristicRead(BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic, int status) {
		
		super.onCharacteristicRead(gatt, characteristic, status);
		
		Log.i(toString(), "Characteristic read "
				+ characteristic.getUuid()
				+ "=" + Utils.bytesToHex(characteristic.getValue())
				+ ": " + Utils.getStatusMessage(status));
		
		synchronized (readLock) {
			if (characteristic.getUuid().equals(readLock.uuid)) {
				readLock.uuid = null;
				readLock.notify();
			}
		}
		
		for (BluetoothGattCallback callback : callbacks)
			callback.onCharacteristicRead(gatt, characteristic, status);
	}
	
	@Override
	public synchronized void onCharacteristicWrite(BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic, int status) {
		
		super.onCharacteristicWrite(gatt, characteristic, status);
		
		Log.i(toString(), "Characteristic write "
				+ characteristic.getUuid()
				+ "=" + Utils.bytesToHex(characteristic.getValue())
				+ ": " + Utils.getStatusMessage(status));
		
		for (BluetoothGattCallback callback : callbacks)
			callback.onCharacteristicWrite(gatt, characteristic, status);
	}
	
	@Override
	public synchronized void onDescriptorRead(BluetoothGatt gatt,
			BluetoothGattDescriptor descriptor, int status) {
		
		super.onDescriptorRead(gatt, descriptor, status);
		
		Log.i(toString(), "Descriptor read "
				+ descriptor.getUuid()
				+ "=" + Utils.bytesToHex(descriptor.getValue())
				+ ": " + Utils.getStatusMessage(status));

		for (BluetoothGattCallback callback : callbacks)
			callback.onDescriptorRead(gatt, descriptor, status);
		
	}
		
	@Override
	public synchronized void onDescriptorWrite(BluetoothGatt gatt,
			BluetoothGattDescriptor descriptor, int status) {
		
		super.onDescriptorWrite(gatt, descriptor, status);
		
		Log.i(toString(), "Descriptor write "
				+ descriptor.getUuid()
				+ "=" + Utils.bytesToHex(descriptor.getValue())
				+ ": " + Utils.getStatusMessage(status));
		
		for (BluetoothGattCallback callback : callbacks)
			callback.onDescriptorWrite(gatt, descriptor, status);
		
	}
	
// |
// V
// o END GATT CALLBACK</editor-fold>
//_____________________________________________________________________________

}
