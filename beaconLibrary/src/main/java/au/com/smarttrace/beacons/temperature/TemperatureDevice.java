package au.com.smarttrace.beacons.temperature;

import android.bluetooth.BluetoothGatt;
import android.util.Log;

import au.com.smarttrace.beacons.Device;
import au.com.smarttrace.beacons.tracker.Tracking;

/**
 * Generic temperature device with internal polling implementation
 */
public abstract class TemperatureDevice extends Device {

    public static final String KEY_TEMPERATURE		= "temperature";
    public static final String KEY_HUMIDITY			= "humidity";


    /** Disable polling... */
    protected final static int STATE_NULL = -1;

    /** Initial state (idle) when the device is first discovered */
    protected final static int STATE_INIT = 0;

    /** Polling state (passive), when the device is updating at slow intervals ({@link #pollInterval})*/
    protected final static int STATE_POLLING = 1;

    /** Tracking state (temporary), to start trackingOn */
    protected final static int STATE_TRACKING_START = 2;

    /** Tracking state (active), when the device triggers notifications with updates */
    protected final static int STATE_TRACKING = 3;

    /** Final state */
    protected final static int STATE_FINISH = 4;

    protected float temperature;

    protected float humidity;

    /**
     * Internal status
     *
     * @see #STATE_INIT
     * @see #STATE_POLLING
     * @see #STATE_TRACKING
     */
    protected int deviceStatus;

    /** Interval defining when to poll (slow updates, keep-alive) */
    private long pollInterval = 10000l;

    @Override
    public synchronized void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        switch (newState) {
            case BluetoothGatt.STATE_DISCONNECTED:
                switch (deviceStatus) {
                    //re-connect immediately
                    case STATE_TRACKING_START:
                        deviceStatus = STATE_TRACKING;
                        runOnThread(new Runnable() {
                            @Override
                            public void run() {
                                connect();
                            }
                        });
                        break;
                    //fallback
                    case STATE_TRACKING:
                        deviceStatus = STATE_POLLING;
                    //re-connect after polling interval
                    case STATE_POLLING:
                        runOnThread(new Runnable() {
                            @Override
                            public void run() {
                                connect();
                            }
                        }, pollInterval);
                        break;
                }
                break;
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        if (status!=BluetoothGatt.GATT_SUCCESS)
            return;

        switch(deviceStatus) {
            case STATE_INIT:
            case STATE_POLLING:
                polling();
                break;
            case STATE_TRACKING_START:
                deviceStatus = STATE_TRACKING;
            case STATE_TRACKING:
                trackingOn();
                break;
            case STATE_FINISH:
                disconnect();
        }
    }

    @Override
    public void onScan() {
        super.onScan();
        switch (deviceStatus) {
            case STATE_INIT:
                deviceStatus = STATE_POLLING;
                runOnThread(new Runnable() {
                    @Override
                    public void run() {
                        connect();
                    }
                });
                break;
        }
    }

    @Override
    public void onScanStop() {
        super.onScanStop();
        deviceStatus = STATE_FINISH;
        disconnect();
    }

    @Override
    public synchronized void onTrackingStart(Tracking tracking) {

        super.onTrackingStart(tracking);

        // this can happen in between the polling phases:
        // 0. OnConnectionStateChange ON: asks for services
        // STATE_TRACKING_START -> STATE_TRACKING
        // STATE_TRACKING -> [enableNotification instead of reading]
        // 1. OnServicesDiscovered: [client asks for characteristic reading]
        // STATE_TRACKING_START -> STATE_TRACKING
        // STATE_TRACKING -> [do nothing]
        // 2. OnCharacteristicRead: [client asks for disconnection after reading]
        // STATE_TRACKING_START -> [connect immediately instead of scheduling], STATE_TRACKING
        // STATE_TRACKING -> STATE_POLLING
        // 3. OnConnectionStateChange OFF: [client schedule reconnection]
        // STATE_TRACKING_START -> [do nothing]
        // STATE_TRACKING -> [do nothing]

        if (deviceStatus!=STATE_NULL)
            deviceStatus = STATE_TRACKING_START;
    }

    @Override
    public synchronized void onTrackingStop() {
        if (deviceStatus==STATE_TRACKING) {
            trackingOff();
            deviceStatus = STATE_POLLING;
            runOnThread(new Runnable() {
                @Override
                public void run() {
                    disconnect();
                }
            });
        }
    }

    protected abstract void polling();

    protected abstract void trackingOn();

    protected abstract void trackingOff();

    protected void polled() {
        switch(deviceStatus) {
            case STATE_POLLING:
                Log.d(toString(), "disconnecting after reading...");
                runOnThread(new Runnable() {
                    public void run() {
                        disconnect();
                    }
                });
                break;
            case STATE_TRACKING_START:
                deviceStatus = STATE_TRACKING;
                break;
        }

    }

    protected void update(float temperature, float humidity, int battery, String message) {

        this.temperature = temperature;
        this.humidity = humidity;
        this.battery = battery;

        //add samples if recording
        addSample(KEY_TEMPERATURE, temperature);
        addSample(KEY_HUMIDITY, humidity);
        if (battery>=0)
            addSample(KEY_BATTERY, battery);

        fireUpdate(message);
    }

}
