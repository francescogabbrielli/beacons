package au.com.smarttrace.beacons.transponder.gps;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import au.com.smarttrace.beacons.DeviceManager;
import au.com.smarttrace.beacons.NoSuchDeviceException;
import au.com.smarttrace.beacons.transponder.LocationActivity;

public class LocationService extends Service implements
                                    GoogleApiClient.ConnectionCallbacks,
                                    GoogleApiClient.OnConnectionFailedListener,
                                    ResultCallback<LocationSettingsResult>,
                                    LocationListener {

    private final static int REQUEST_PERMISSION_LOCATION = 1;
    private final static int REQUEST_CHECK_SETTINGS = 2;

    /** Broadcast action to request for permissions */
    private final static String ACTION_REQUEST_PERMISSION = "action_request_permission";

    /** Broadcast action to request for permissions */
    private final static String ACTION_REQUEST_RESOLUTION = "action_request_resolution";

    /** GoogleAPI client */
    private GoogleApiClient googleApiClient;

    /** Current location request */
    private LocationRequest request;

    public LocationService() {
    }

    private Intent getIntent(String action) {
        return new Intent(getPackageName() + "." + action);
    }

    @Override
    public void onCreate() {
        // Create an instance of GoogleAPIClient.
        if (googleApiClient == null)
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        googleApiClient.connect();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

//        if (ActivityCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
//                && ActivityCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//
//            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(requestPermissionIntent);
//            return;
//        }
//
//        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(
//                googleApiClient);
//
//        if (lastLocation != null)
//            updateUI(lastLocation);

        requestLocation();

    }

    private void requestLocation() {

        request = new LocationRequest();
        request.setInterval(20000);
        request.setFastestInterval(5000);
        request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(request);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient,
                        builder.build());

        result.setResultCallback(this);

    }

    private void requestLocationImpl() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            LocalBroadcastManager.getInstance(getApplicationContext())
                    .sendBroadcast(getIntent(ACTION_REQUEST_PERMISSION));

            return;

        }

        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, request, this);

    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onResult(@NonNull LocationSettingsResult result) {
        final Status status = result.getStatus();
        final LocationSettingsStates states = result.getLocationSettingsStates();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                // All location settings are satisfied. The client can
                // initialize location requests here.
                requestLocationImpl();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                LocalBroadcastManager.getInstance(getApplicationContext())
                                .sendBroadcast(getIntent(ACTION_REQUEST_RESOLUTION));
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Toast.makeText(this, "Please change your settings to enable location management in this app", Toast.LENGTH_LONG).show();
                stopSelf();
                break;
        }

    }

    @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(this, "NEW LOCATION=" + location, Toast.LENGTH_LONG).show();
        try {
            GPSDevice gps = (GPSDevice) DeviceManager.getInstance().getDevice(GPSDevice.IDENTIFIER);
            gps.setLocation(location);
        }catch(NoSuchDeviceException e) {
            Log.e("LocationService", "GPS not found?", e);
        }
    }

    @Override
    public void onDestroy() {
        googleApiClient.disconnect();
    }

}
