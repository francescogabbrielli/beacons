package au.com.smarttrace.beacons.gps;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
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
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import au.com.smarttrace.beacons.DeviceManager;
import au.com.smarttrace.beacons.NoSuchDeviceException;
import au.com.smarttrace.beacons.Utils;

public class LocationService extends Service implements
                                    GoogleApiClient.ConnectionCallbacks,
                                    GoogleApiClient.OnConnectionFailedListener,
                                    ResultCallback<LocationSettingsResult> {

    private final static String TAG = LocationService.class.getSimpleName();

    /** Broadcast action to request for permissions */
    public final static String ACTION_REQUEST_PERMISSION = "action_request_permission";

    /** Broadcast action to request for permissions */
    public final static String ACTION_REQUEST_RESOLUTION = "action_request_resolution";

    /** Notify when location updates can be performed */
    public final static String ACTION_LOCATION_STATUS_CHANGE = "action_location_status_changed";

    /** Start this service */
    public final static String ACTION_START_SERVICE = "action_start_service";

    /** Start location updates*/
    public final static String ACTION_START_UPDATES = "action_start_updates";

    /** Stop location updates*/
    public final static String ACTION_STOP_UPDATES = "action_stop_updates";

    public final static String KEY_LOCATION_STATUS = "key_location_status";

    public final static String KEY_PARAM = "key_param";

    /** GoogleAPI client */
    private GoogleApiClient googleApiClient;

    /** Current location request */
    private LocationRequest request;

    private boolean updating, autoStart;

    private LocalBinder binder;

    @Override
    public void onCreate() {
        // Create an instance of GoogleAPIClient.
        Log.i(TAG, "Creating Location Service");
        if (googleApiClient == null)
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        DeviceManager.getInstance().addInternalDevice(getApplicationContext(), GPSDevice.class);
        binder = new LocalBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Request: " + intent);
        if (intent==null || ACTION_START_SERVICE.equals(intent.getAction())) {
            Log.i(TAG, "request connect");
            connect();
        } else if (ACTION_START_UPDATES.equals(intent.getAction())) {
            Log.i(TAG, "request start updates");
            startUpdates();
        } else if (ACTION_STOP_UPDATES.equals(intent.getAction())) {
            Log.i(TAG, "request stop updates");
            stopUpdates();
        } else {
            Log.w(TAG, "unknown action: "+intent.getAction());
            //connect();???
        }
        return START_STICKY;
    }

    private void connect() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        Log.i(TAG, "PROVIDERS: " + lm.getAllProviders());
        Log.i(TAG, "- network = " + lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
        Log.i(TAG, "- gps = " + lm.isProviderEnabled(LocationManager.GPS_PROVIDER));
        Log.i(TAG, "- googleaApi = " + googleApiClient.isConnected());
        if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
            if (!googleApiClient.isConnected() && !googleApiClient.isConnecting())
                googleApiClient.connect();
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

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Binding: "+intent);
        connect();
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "Unbinding: "+intent);
        return super.onUnbind(intent);
    }

    @Override
    public synchronized void onConnected(@Nullable Bundle bundle) {

        Log.i(TAG, "CONNECTED!");

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

        sendChange(true);

        if(autoStart)
            startUpdates();

    }

    @Override
    public synchronized void onConnectionSuspended(int cause) {

        Log.i(TAG, "DISCONNECTED!");

        switch(cause) {
            case GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST:
                //XXX: reconnect? or wait for network to activated?
                break;
            case GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED:
                googleApiClient.connect();
                break;
        }

        request = null;
        sendChange(false);

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.w(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    private void sendChange(boolean status) {
        getSharedPreferences(Utils.PREFS, MODE_PRIVATE).edit()
                .putBoolean(Utils.PREF_KEY_LOCATION_SERVICE_ENABLED, status).commit();
        Intent intent = new Intent(ACTION_LOCATION_STATUS_CHANGE);
        intent.putExtra(KEY_LOCATION_STATUS, status);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void sendAction(String action, Parcelable... params) {
        Intent intent = new Intent(action);
        int n = 0;
        if (params.length>0)
            for (Parcelable param : params)
                intent.putExtra(KEY_PARAM+"_"+(++n), param);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    //TODO: remove?
    public synchronized boolean isConnected() {
        return request!=null;
    }

    private synchronized void startUpdates() {

        if (updating)
            return;

        if (request==null) {
            autoStart = true;
            connect();
            return;
        }

        autoStart = false;
        updating = true;

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(request);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient,
                        builder.build());

        result.setResultCallback(this);

    }

    private synchronized void stopUpdates() {

        updating = false;

        LocationServices.FusedLocationApi
                .removeLocationUpdates(googleApiClient, getGPS());

    }

    @Override
    public void onResult(@NonNull LocationSettingsResult result) {
        final Status status = result.getStatus();
        final LocationSettingsStates states = result.getLocationSettingsStates();
        Log.i(TAG, status.getStatusCode()
                +"="+status.getStatusMessage()
                +": "+states.isGpsPresent()+"/"+states.isGpsUsable());
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                requestLocation();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                sendAction(ACTION_REQUEST_RESOLUTION, status);
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Toast.makeText(this, "Please change your settings to enable location management in this app", Toast.LENGTH_LONG).show();
                stopSelf();
                break;
        }

    }

    private void requestLocation() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            sendAction(ACTION_REQUEST_PERMISSION);
            return;

        }

        Log.i(TAG, "request location updates");

        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, request, getGPS());

    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Destroying Location Service");
        request = null;
        sendChange(false);
        DeviceManager.getInstance().removeInternalDevice(GPSDevice.IDENTIFIER);
        googleApiClient.disconnect();
        super.onDestroy();
    }

}
