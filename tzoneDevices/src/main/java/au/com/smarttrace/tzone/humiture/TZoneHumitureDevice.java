package au.com.smarttrace.tzone.humiture;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import au.com.smarttrace.beacons.Utils;
import au.com.smarttrace.beacons.temperature.TemperatureDevice;

import com.TZONE.Bluetooth.Temperature.Model.CharacteristicHandle;

/**
 * TZone Digital Humiture Recorder
 */
public class TZoneHumitureDevice extends TemperatureDevice implements Runnable{

	/** Number of data logger records */
	int records;
	
	/** Use TZONE library */
	private com.TZONE.Bluetooth.Temperature.Model.Device tzoneDevice;
	
	public TZoneHumitureDevice() {
		tzoneDevice = new com.TZONE.Bluetooth.Temperature.Model.Device();
        deviceStatus = STATE_NULL;
	}
	
	@Override
	public void setScanResult(ScanResult result) {
		
		super.setScanResult(result);
		
		tzoneDevice.fromScanData(
				deviceResult.getDevice().getName(),
				deviceResult.getDevice().getAddress(), 
				deviceResult.getRssi(), 
				bytes);
		
		copyFromTzone();
		
	}
	
	private void copyFromTzone() {
		serialNumber= tzoneDevice.SN;
		battery		= tzoneDevice.Battery;
		temperature = (float) tzoneDevice.Temperature;
		humidity 	= (float) tzoneDevice.Humidity;
		records		= tzoneDevice.SavaCount;
		lastTime	= SystemClock.elapsedRealtimeNanos();
	}
	
	@Override
	public int getTitle() {
		return R.string.rt_t_device_name;
	}
	
	public Class<? extends Activity> getMainActivity() {
		return TZoneActivity.class;
	}
	
	protected int getRowContentLayout() {
		return R.layout.tzone_humiture_row_device_content;
	}
		
	@Override
	public void populateRowContent(View content) {
		TextView tv = (TextView) content.findViewById(R.id.tzone_humiture_temperature);
		tv.setText(String.format("%2.2f°C", temperature));
		tv = (TextView) content.findViewById(R.id.tzone_humiture_humidity);
		tv.setText(String.format("%2.2f%%", humidity));
	}
	
	/**
	 * Get a characteristic by id (from the main service)
	 * 
	 * @param stringUUID
	 * 				UUID of the characteristic 
	 * @return
	 * 			the characteristic
	 */
	private BluetoothGattCharacteristic getCharacteristic(int stringUUID) {
		if (gatt!=null && getConnectionState()==BluetoothProfile.STATE_CONNECTED) {
	        BluetoothGattService s = gatt.getService(
	        				getUUID(R.string.tzone_humiture_SERVICE_MAIN));
	        if (s!=null)
	        	return s.getCharacteristic(getUUID(stringUUID));
		}
		fireError(R.string.rt_t_error_service_not_found);
		return null;
	}
	
	private void readAll(String... characteristicUUIDs) {
		if (gatt!=null && getConnectionState()==BluetoothProfile.STATE_CONNECTED) {
			BluetoothGattService s = gatt.getService(
					getUUID(R.string.tzone_humiture_SERVICE_MAIN));
			
			if (s==null)
				return;
		
			readAllCharacteristics(s, characteristicUUIDs);
		}
	}
	
    /** 
     * Start/stop reading the data logger
     * 	
     * @param enable
     * 				enable/disable the sync process
     */
	public void sync(boolean enable) {
		
		BluetoothGattCharacteristic c = 
				getCharacteristic(R.string.tzone_humiture_CHARACTERISTIC_SYSN);
		
		if (c==null)
			return;
			
		Log.i(toString(), "Enable notifications");
		gatt.setCharacteristicNotification(c, enable);
		
		// guess descriptor...
        if (c.getDescriptors().isEmpty())
        	return;
        
        String descriptorUuid = c.getDescriptors().get(0).getUuid().toString();
        if (descriptorUuid == null || descriptorUuid == "")
        	return;
        
        BluetoothGattDescriptor descriptor = c.getDescriptor(UUID.fromString(descriptorUuid));
        if (descriptor != null) {
        	Log.i(toString(), "Enable notification descriptor");
            descriptor.setValue(enable
            		? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            		: BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
        }
        
        gatt.readCharacteristic(
        		getCharacteristic(R.string.tzone_humiture_CHARACTERISTIC_LOG_RECORDS));

	}

	/**
	 * Check password token against the device
	 * 
	 * @param pw
	 * 				the password
	 */
	public void checkToken(char[] pw) {
		
		BluetoothGattCharacteristic c = 
				getCharacteristic(R.string.tzone_humiture_CHARACTERISTIC_TOKEN);
		
		Log.i(toString(), "Check password");
		byte[] bytes = new byte[pw.length];
		for (int i=0;i<bytes.length;i++)
			bytes[i] = (byte) (0xFF & Character.getNumericValue(pw[i]));
		
		c.setValue(bytes);
		
		gatt.writeCharacteristic(c);
	}
	
	private void addLoggerEntry(byte[] bytes) {
		com.TZONE.Bluetooth.Temperature.Model.Device d = new com.TZONE.Bluetooth.Temperature.Model.Device();
        d.HardwareModel = tzoneDevice.HardwareModel;
        d.Firmware = tzoneDevice.Firmware;
		d.fromNotificationData(bytes);
		Log.i(toString(), 
				String.format("[%s %d%%]: %2.2f°C %2.2f%%",
						d.UTCTime,
						d.Battery,
						d.Temperature,
						d.Humidity));
	}


    private boolean authenticated;

    @Override
    protected void polling() {
//        if (!authenticated) {
//            checkToken("000000".toCharArray());
//        } else {
//            readAll(R.string.tzone_humiture_CHARACTERISTIC_TEMPERATURE,
//                    R.string.tzone_humiture_CHARACTERISTIC_HUMIDITY,
//                    R.string.tzone_humiture_CHARACTERISTIC_BATTERY);
//        }
        polled();
    }

    @Override
    public void onReadAll() {
        super.onReadAll();
    }

    private ScheduledExecutorService trackingExec;
    private ScheduledFuture trackingFuture;

    @Override
    protected void trackingOn() {
//        if (trackingExec==null)
//            trackingExec = Executors.newSingleThreadScheduledExecutor();
//        trackingFuture = trackingExec.scheduleAtFixedRate(this, 0l, 5l, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
//        readAll(R.string.tzone_humiture_CHARACTERISTIC_TEMPERATURE,
//                R.string.tzone_humiture_CHARACTERISTIC_HUMIDITY,
//                R.string.tzone_humiture_CHARACTERISTIC_BATTERY);
    }

    @Override
    protected void trackingOff() {
//        if (trackingFuture!=null) {
//            trackingFuture.cancel(true);
//            trackingFuture = null;
//        }
    }

    @Override
    public void onScan() {
        super.onScan();
        update(temperature, humidity, battery,
                String.format("%2.2f\u00b0C", temperature) + "; "
                        + String.format("%2.2f%%", humidity));
    }

    //_____________________________________________________________________________
//
// o GATT CALLBACK IMPLEMENTATION
// | ----------------------------
// V
		
	@Override
	public synchronized void onConnectionStateChange(BluetoothGatt gatt,
			int status, int newState) {
		
		super.onConnectionStateChange(gatt, status, newState);
		
		// attempt reconnection 1s after an unexpected connection error
		if (newState!=BluetoothGatt.STATE_CONNECTED && status!=BluetoothGatt.GATT_SUCCESS)

			runOnThread(
			//Executors.newSingleThreadScheduledExecutor().schedule(
				new Runnable() {
					@Override public void run() {connect();}
				}, 
			//	1l, TimeUnit.SECONDS);
			1000l);

        // reset connection variables
        if (newState==BluetoothGatt.STATE_DISCONNECTED) {
            authenticated = false;
            trackingOff();
        }

	}

    @Override
	public synchronized void onCharacteristicRead(BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic, int status) {

		super.onCharacteristicRead(gatt, characteristic, status);
		
		if (status==BluetoothGatt.GATT_SUCCESS) {
			//TODO: update a single value
			CharacteristicHandle ch = new CharacteristicHandle();
			ch.SetItemValue(
					tzoneDevice, 
					ch.GetCharacteristicType(characteristic.getUuid()),
					characteristic.getValue());
			copyFromTzone();
		}
		
	}
	
	@Override
	public synchronized void onCharacteristicChanged(BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic) {
		
		super.onCharacteristicChanged(gatt, characteristic);
		
		//TODO: CRC
		byte[] bytes = characteristic.getValue();
        if (bytes.length >= 9) {
        	byte[] sub = Arrays.copyOfRange(bytes, 0, 6);
            addLoggerEntry(sub);
        }
        if (bytes.length >= 15) {
        	byte[] sub = Arrays.copyOfRange(bytes, 6, 12);
            addLoggerEntry(sub);
        }
		
	}

	@Override
	public synchronized void onCharacteristicWrite(BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic, int status) {
		
		super.onCharacteristicWrite(gatt, characteristic, status);
		
		// password check
		if (characteristic.getUuid().equals(getUUID(R.string.tzone_humiture_CHARACTERISTIC_TOKEN))) {
			if (status==BluetoothGatt.GATT_SUCCESS) {
                authenticated = true;
                polling();
            } else
				fireError(Utils.getStatusMessage(status));
		}
		
	}
	
// |
// V
// o END GATT CALLBACK
//_____________________________________________________________________________
	
}
