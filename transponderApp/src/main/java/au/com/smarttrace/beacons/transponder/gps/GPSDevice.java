package au.com.smarttrace.beacons.transponder.gps;

import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.location.Location;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewStub;

import au.com.smarttrace.beacons.Device;
import au.com.smarttrace.beacons.DeviceEvent;
import au.com.smarttrace.beacons.InternalDevice;
import au.com.smarttrace.beacons.tracker.Tracking;
import au.com.smarttrace.beacons.transponder.R;

/**
 *
 */
public class GPSDevice extends InternalDevice {

    public static final String IDENTIFIER = "_internal_GPS";

    private int signal;

    private Location location;

    public GPSDevice() {
    }

    @Override
    public Device init(Context context, ScanResult deviceResult) {
        super.init(context, deviceResult);
        name = "GPS";
        signal = -100;
        return this;
    }

    @Override
    public int getTitle() {
        return R.string.title_gps;
    }

    @Override
    public int getSignal() {
        return signal;
    }

    @Override
    public String getId() {
        return IDENTIFIER;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        if (location!=null) {
            this.location = location;
            this.lastTime = SystemClock.elapsedRealtime();
            signal = - 100 + (int) (100/Math.min(location.getAccuracy(), 1d));
            fireEvent(DeviceEvent.TYPE_DEVICE_UPDATED);
        }
    }

    @Override
    public synchronized void connect() {

    }

    @Override
    public synchronized void disconnect() {

    }

    @Override
    protected int getRowContentLayout() {
        return R.layout.row_gps_content;
    }

    @Override
    public View getRowContent(ViewStub stub, View content) {
        return super.getRowContent(stub, content);
    }

    @Override
    public void onScan() {
        super.onScan();
    }

    @Override
    public void onScanStop() {
        super.onScanStop();
    }

    @Override
    public void onTrackingStart(Tracking tracking) {
        super.onTrackingStart(tracking);
    }

    @Override
    public void onTrackingStop() {
        super.onTrackingStop();
    }
}
