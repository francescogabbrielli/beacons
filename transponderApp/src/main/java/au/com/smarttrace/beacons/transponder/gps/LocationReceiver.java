package au.com.smarttrace.beacons.transponder.gps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

import au.com.smarttrace.beacons.DeviceManager;
import au.com.smarttrace.beacons.transponder.BeaconTransponder;

public class LocationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if(LocationManager.PROVIDERS_CHANGED_ACTION.equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, LocationService.class);
            BeaconTransponder app = (BeaconTransponder) context.getApplicationContext();
            if (app.isLocationEnabled()) {
                context.startService(serviceIntent);
            } else {
                context.stopService(serviceIntent);
            }
        }

    }
}
