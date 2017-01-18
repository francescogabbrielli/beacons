package au.com.smarttrace.beacons.transponder.gps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

import au.com.smarttrace.beacons.DeviceManager;

public class LocationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if(LocationManager.PROVIDERS_CHANGED_ACTION.equals(intent.getAction())) {

            Intent serviceIntent = new Intent(context, LocationService.class);

            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {

                context.startService(serviceIntent);
                DeviceManager.getInstance().addInternalDevice(context.getApplicationContext(), GPSDevice.class);

            } else {

                DeviceManager.getInstance().removeInternalDevice(GPSDevice.IDENTIFIER);
                context.stopService(serviceIntent);

            }

        }

    }
}
