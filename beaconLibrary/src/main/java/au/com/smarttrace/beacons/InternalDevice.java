package au.com.smarttrace.beacons;

import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;

/**
 * Fake device for internal use
 */
public class InternalDevice extends Device {

    protected String id;

    public InternalDevice() {
    }

    public InternalDevice(String identifier) {
        id = identifier;
    }

    @Override
    public synchronized void setScanResult(ScanResult deviceResult) {

    }

    @Override
    public synchronized void connect() {

    }

    @Override
    public synchronized void disconnect() {

    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getConnectionState() {
        return BluetoothProfile.STATE_CONNECTED;//fake
    }
}
