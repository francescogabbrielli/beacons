package au.com.smarttrace.beacons.transponder.gps;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
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

public class LocationService extends Service implements
                                    GoogleApiClient.ConnectionCallbacks,
                                    GoogleApiClient.OnConnectionFailedListener,
                                    ResultCallback<LocationSettingsResult> {

    private final static int REQUEST_PERMISSION_LOCATION = 1;
    private final static int REQUEST_CHECK_SETTINGS = 2;

    private final static String TAG = LocationService.class.getSimpleName();

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
        DeviceManager.getInstance().addInternalDevice(getApplicationContext(), GPSDevice.class);
        if (!googleApiClient.isConnected() && !googleApiClient.isConnecting())
            googleApiClient.connect();
        return START_STICKY;
    }

    private GPSDevice getGPS() {
        try {
            return (GPSDevice) DeviceManager.getInstance().getDevice(GPSDevice.IDENTIFIER);
        } catch(NoSuchDeviceException e) {
            Log.e(TAG, "GPS not found?", e);
        }
        return null;
    }

    public class LocalBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    private LocalBinder binder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            LocalBroadcastManager.getInstance(getApplicationContext())
                    .sendBroadcast(new Intent(ACTION_REQUEST_PERMISSION));
            return;
        }

        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(
                googleApiClient);

        if (lastLocation != null)
            getGPS().setLocation(lastLocation);

        //create request
        request = new LocationRequest();
        request.setInterval(25000);
        request.setFastestInterval(5000);
        request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

    }

    @Override
    public void onConnectionSuspended(int cause) {
        switch(cause) {
            case GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST:
                //XXX: reconnect? or wait for network to activated?
                break;
            case GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED:
                googleApiClient.connect();
                break;
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.w(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult result) {
        final Status status = result.getStatus();
        final LocationSettingsStates states = result.getLocationSettingsStates();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                // All location settings are satisfied. The client can
                // initialize location requests here.
                requestLocation();
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

    public void startUpdates() {

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(request);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient,
                        builder.build());

        result.setResultCallback(this);

    }

    public void stopUpdates() {
        LocationServices.FusedLocationApi
                .removeLocationUpdates(googleApiClient, getGPS());
    }

    private void requestLocation() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            LocalBroadcastManager.getInstance(getApplicationContext())
                    .sendBroadcast(getIntent(ACTION_REQUEST_PERMISSION));

            return;

        }

        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, request, getGPS());

    }

    @Override
    public void onDestroy() {
        DeviceManager.getInstance().removeInternalDevice(GPSDevice.IDENTIFIER);
        googleApiClient.disconnect();
    }

}
