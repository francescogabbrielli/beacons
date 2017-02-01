package au.com.smarttrace.beacons.gps;

import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationListener;

import au.com.smarttrace.beacons.Device;
import au.com.smarttrace.beacons.InternalDevice;
import au.com.smarttrace.beacons.R;

/**
 *
 */
public class GPSDevice extends InternalDevice implements LocationListener {

    public static final String IDENTIFIER = "01_internal_GPS";

    private int signal;
    private ResultReceiver receiver;
    private Location location;
    private String address;
    LocationCompacter compacter;

    public GPSDevice() {
        compacter = new LocationCompacter();
    }

    @Override
    public Device init(final Context context, ScanResult deviceResult) {
        super.init(context, deviceResult);
        name = context.getResources().getString(R.string.title_gps);
        signal = -100;
        receiver = new ResultReceiver(new Handler(context.getMainLooper())) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                String res = resultData.getString(AddressService.EXTRA_RESULT);
                if (resultCode==AddressService.RESULT_OK) {
                    address = res;
                    fireUpdate(res);
                } else {
                    Toast.makeText(context, res, Toast.LENGTH_SHORT);
                }
            }
        };
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

    public synchronized void setLocation(Location location) {
        if (location!=null) {
            if (this.location==null || !compacter.inThreshold(location, this.location)) {
                address = null;
                AddressService.start(context, location, receiver);
            }
            this.location = location;
            signal = - (int) (location.getAccuracy()/10);
            fireUpdate(location!=null ? location.toString(): "-");
            addSample(KEY_LOCATION, location);
        }
    }

    @Override
    public synchronized void connect() {
        //XXX: override bluetooth behaviour -> TODO: abstract Device hierarchy
    }

    @Override
    public synchronized void disconnect() {
        //XXX: override bluetooth behaviour -> TODO: abstract Device hierarchy
    }

    @Override
    public int getConnectionState() {
        return signal<-100 ? BluetoothProfile.STATE_DISCONNECTED : BluetoothProfile.STATE_CONNECTED;//XXX: fake -> TODO: abstract Device hierarchy
    }

    @Override
    protected int getRowContentLayout() {
        return R.layout.row_gps_content;
    }

    @Override
    protected synchronized void populateRowContent(View content) {
        TextView tv = (TextView) content.findViewById(R.id.gps_latitude);
        tv.setText(R.string.latitude);
        tv.append(": ");
        tv.append(location!=null ? String.format("%3.2f", location.getLatitude()) : " - ");
        tv = (TextView) content.findViewById(R.id.gps_longitude);
        tv.setText(R.string.longitude);
        tv.append(": ");
        tv.append(location!=null ? String.format("%3.2f", location.getLongitude()) : " - ");
        tv = (TextView) content.findViewById(R.id.gps_address);
        tv.setText(address!=null ? address : " - ");
    }

    @Override
    public void onLocationChanged(final Location location) {
        setLocation(location);
    }

}
