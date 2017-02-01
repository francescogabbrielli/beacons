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
package au.com.smarttrace.axaet.sensebio;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.view.View;
import android.widget.TextView;

import au.com.smarttrace.beacons.temperature.TemperatureDevice;

/**
 * <p>
 * SenseBio Device from AXAET. Measures temperature and humidity.
 *
 * <p>
 * When <i>created</i>, the app (client) will connect to the device and read the characteristic for
 * temperature/humidity and then disconnect. This same operation is scheduled at regular
 * configurable intervals, until the device is destroyed.
 * <br/>
 * [scheduler implemented by reconnecting after disconnection when in {@link#STATUS_POLLING}]
 * <br/>
 * When <i>tracked</i>, the previous scheduling will halt, then the app will connect to the device
 * and subscribe to receive temperature/humidity notifications. On end of trackingOn the app will
 * turn off notifications and resume the previous scheduling.
 * If disconnected while trackingOn the appropriate behaviour will be resumed on re-scan and in a
 * similar way if an internal disconnection occurs, the previous scheduling will resume and thus
 * the appropriate behaviour.
 *
 * <p>
 * Services included for changing password and basic configuration. (TODO)
 *
 * @see #STATE_INIT
 * @see #STATE_POLLING
 * @see #STATE_TRACKING
 *
 * @author Francesco Gabbrielli
 */
public class SenseBioDevice extends TemperatureDevice {


	@Override
	public int getTitle() {
		return R.string.sense_bio_device_name;
	}
	
	protected int getRowContentLayout() {
		return R.layout.sense_bio_row_device_content;
	}

	@Override
	public void populateRowContent(View content) {
		TextView tv = (TextView) content.findViewById(R.id.sense_bio_temperature);
		tv.setText(String.format("%2.2f\u00b0C", temperature));
		tv = (TextView) content.findViewById(R.id.sense_bio_humidity);
		tv.setText(String.format("%2.2f%%", humidity));
	}

    @Override
    protected void polling() {
        if (gatt!=null)
            runOnThread(new Runnable() {
                @Override
                public void run() {
                    readTH(gatt);
                }
            });
    }

    private void readTH(BluetoothGatt gatt) {
        BluetoothGattService bsb =
                gatt.getService(
                        getUUID(R.string.sense_bio_SERVICE_TH));

        // read temperature/humidity
        if (bsb!=null) {
            BluetoothGattCharacteristic th =
                    bsb.getCharacteristic(
                            getUUID(R.string.sense_bio_CHARACTERISTIC_TH));
            gatt.readCharacteristic(th);
        }
    }

    @Override
    protected void trackingOn() {
        if (gatt!=null)
            runOnThread(new Runnable() {
                @Override
                public void run() {
                    enableNotifications(gatt, true);
                }
            });
    }

    @Override
    protected void trackingOff() {
        if (gatt!=null)
            runOnThread(new Runnable() {
                @Override
                public void run() {
                    enableNotifications(gatt, false);
                }
            });
    }

    private void enableNotifications(BluetoothGatt gatt, boolean enabled) {

        BluetoothGattService
                bsb =
                gatt.getService(
                        getUUID(R.string.sense_bio_SERVICE_TH));

        if (bsb!=null) {
            BluetoothGattCharacteristic en =
                    bsb.getCharacteristic(
                            getUUID(R.string.sense_bio_CHARACTERISTIC_ENABLE_TH));
            gatt.setCharacteristicNotification(en, enabled);
            BluetoothGattDescriptor d =
                    en.getDescriptor(
                            getUUID(R.string.sense_bio_DESCRIPTOR_ENABLER));
            d.setValue(enabled
                    ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
    }

	/**
	 * Single read from the device
	 */
	@Override
	public synchronized void onCharacteristicRead(BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic, int status) {

		super.onCharacteristicRead(gatt, characteristic, status);

		if (status==BluetoothGatt.GATT_SUCCESS)

            // temperature read from polling
			if (getUUID(R.string.sense_bio_CHARACTERISTIC_TH).equals(characteristic.getUuid())) {
                setTH(characteristic.getValue());
                polled();
            // temperature read by enabling notifications (one off)
			} else if (getUUID(R.string.sense_bio_CHARACTERISTIC_ENABLE_TH).equals(characteristic.getUuid())) {
                setTH(characteristic.getValue());
            }

	}

    /**
	 * Temperature/humidity read from device notifications
	 */
    @Override
    public void onCharacteristicChanged(
    		BluetoothGatt gatt, 
    		final BluetoothGattCharacteristic characteristic) {
    	
    	super.onCharacteristicChanged(gatt, characteristic);
		setTH(characteristic.getValue());
    }
	
    /**
     * Set temperature/humidity (bytes 0-1 and 2-3)
     * 
     * @param values
     * 				the bytes containing the values
     */
    private void setTH(byte[] values) {
        float temp = ((int) values[0]) + ((int) values[1]) * 0.01f;
        float humi = ((int) values[2]) + ((int) values[3]) * 0.01f;
        update(temp, humi, -1,
                String.format("%2.2f\u00b0C", temp) + "; "
                        + String.format("%2.2f%%", humi));
	}
	
}
