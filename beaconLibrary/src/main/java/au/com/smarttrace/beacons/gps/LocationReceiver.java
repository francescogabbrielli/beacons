package au.com.smarttrace.beacons.gps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

public class LocationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if(LocationManager.PROVIDERS_CHANGED_ACTION.equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, LocationService.class);
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {

                serviceIntent.setAction(LocationService.ACTION_START_SERVICE);
                context.startService(serviceIntent);

            } else {

                context.stopService(serviceIntent);

            }
        }

    }
}
