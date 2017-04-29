package au.com.smarttrace.beacons.transponder;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.api.Status;

import au.com.smarttrace.beacons.BluetoothService;
import au.com.smarttrace.beacons.DeviceEvent;
import au.com.smarttrace.beacons.DeviceListener;
import au.com.smarttrace.beacons.DeviceManager;
import au.com.smarttrace.beacons.Utils;
import au.com.smarttrace.beacons.gps.LocationService;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener, DeviceListener {

    private final static int REQUEST_PERMISSION_LOCATION = 1;
    private final static int REQUEST_CHECK_SETTINGS = 2;

    /**
     * Detects status change in the location and bluetooth adapter services:
     * <ul>
     *     <li>if the service is active and fully connected, bind the service</li>
     *     <li>if the service looses its connectivity, do nothing; the service
     *          will be unbound because it will be destroyed</li>
     *     <li>if the service is not active anymore, the same as above</li>
     * </ul>
     *
     * NB: The first time the services are bound onStart()
     */
    private BroadcastReceiver statusReceiver;

    /**
     * Detects a request for permissions in a service
     */
    private BroadcastReceiver permissionReceiver;

    private final static String KEY_PREF_LOG ="log";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("MAIN", "OnCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI(savedInstanceState);
        initReceivers();
//        initConnections();
//        log = savedInstanceState!=null ? savedInstanceState.getString(KEY_PREF_LOG) : "";
//        TextView tv = (TextView) findViewById(R.id.recording_log);
//        tv.setText(log);
    }

    private void initUI(Bundle savedInstanceState) {
        ToggleButton b = (ToggleButton) findViewById(R.id.recording_button);
        b.setOnClickListener(this);
        TextView tv = (TextView) findViewById(R.id.recording_log);
        tv.setMovementMethod(new ScrollingMovementMethod());
        if (savedInstanceState!=null)
            tv.setText(savedInstanceState.getCharSequence(KEY_PREF_LOG));
    }


    private void initReceivers() {

        statusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra(BluetoothService.KEY_BLUETOOTH_STATUS)) {
                    boolean active = intent.getBooleanExtra(BluetoothService.KEY_BLUETOOTH_STATUS, false);
                    Log.i(BeaconTransponder.TAG, "Bluetooth change: "+active);
                } else if (intent.hasExtra(LocationService.KEY_LOCATION_STATUS)) {
                    boolean active = intent.getBooleanExtra(LocationService.KEY_LOCATION_STATUS, false);
                    Log.i(BeaconTransponder.TAG, "Location change: "+active);
                }
                updateUIStatus();
            }
        };

        permissionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (LocationService.ACTION_REQUEST_PERMISSION.equals(intent.getAction())) {
                    // anyway, this should never happen...
                    Log.i(BeaconTransponder.TAG, "Permission for location requested?");
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                            }, REQUEST_PERMISSION_LOCATION);

                } else if (LocationService.ACTION_REQUEST_RESOLUTION.equals(intent.getAction())) {
                    Log.i(BeaconTransponder.TAG, "Resolution for location requested?");
                    Status s = intent.getParcelableExtra(LocationService.KEY_PARAM+"_1");
//                    DeviceManager.getInstance().cancelTracking();
//                    ((ToggleButton) findViewById(R.id.recording_button)).setChecked(false);
                    try {
                        s.startResolutionForResult(
                                MainActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException e) {
                        Toast.makeText(MainActivity.this, "Internal error: "+e, Toast.LENGTH_SHORT);
                        Log.e("MAIN", "Internal error: "+e, e);
                    }
                }
            }
        };
    }

    private void registerReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(LocationService.ACTION_LOCATION_STATUS_CHANGE);
        filter.addAction(BluetoothService.ACTION_BLUETOOTH_STATUS_CHANGE);
        LocalBroadcastManager.getInstance(this).registerReceiver(statusReceiver, filter);
        filter = new IntentFilter();
        filter.addAction(LocationService.ACTION_REQUEST_RESOLUTION);
        filter.addAction(LocationService.ACTION_REQUEST_PERMISSION);
        LocalBroadcastManager.getInstance(this).registerReceiver(permissionReceiver, filter);
    }

    @Override
    protected void onStart() {
        Log.d("MAIN", "OnStart");
//        bind();
        registerReceivers();
        DeviceManager.getInstance().addDeviceListener(this);
        super.onStart();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(KEY_PREF_LOG,
                ((TextView) findViewById(R.id.recording_log)).getText());
    }

    @Override
    protected void onResume() {
        updateUIStatus();
        if (DeviceManager.getInstance().isRecording()) {
            ToggleButton tb = (ToggleButton) findViewById(R.id.recording_button);
            tb.setChecked(true);
            tb.setEnabled(true);
            tb.callOnClick();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d("MAIN", "OnStop");
        DeviceManager.getInstance().removeDeviceListener(this);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(permissionReceiver);
//        unbind();
        super.onStop()  ;
    }

    private void updateUIStatus() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean bt = Utils.getBooleanPref(getApplicationContext(), Utils.PREF_KEY_BLUETOOTH_SERVICE_ENABLED);
                boolean gps = Utils.getBooleanPref(getApplicationContext(), Utils.PREF_KEY_LOCATION_SERVICE_ENABLED);

                TextView tv = (TextView) findViewById(R.id.recording_ic_bt);
                tv.setEnabled(bt);
                if (bt)
                    tv.setText(String.format("(%d)", DeviceManager.getInstance().countDevices()-(gps?1:0)));
                findViewById(R.id.recording_ic_gps).setEnabled(gps);

                if (!DeviceManager.getInstance().isRecording())
                    findViewById(R.id.recording_button).setEnabled(bt&&gps);
            }
        });

    }

    @Override
    public void onClick(View v) {

        ToggleButton b = (ToggleButton) v;

        if (b.isChecked()) {
            startService(new Intent(LocationService.ACTION_START_UPDATES, null,
                    getApplicationContext(), LocationService.class));
            if (DeviceManager.getInstance().startTracking())
                ((TextView) findViewById(R.id.recording_log)).setText("");
        } else {
            startService(new Intent(LocationService.ACTION_STOP_UPDATES, null,
                    getApplicationContext(), LocationService.class));
            DeviceManager.getInstance().stopTracking();
        }
    }

    @Override
    public void onDeviceChange(DeviceEvent event) {

        switch (event.getType()) {
            case DeviceEvent.TYPE_DEVICE_ADDED:
            case DeviceEvent.TYPE_DEVICE_REMOVED:
                updateUIStatus();
                break;
            case DeviceEvent.TYPE_DEVICE_ERROR:
            case DeviceEvent.TYPE_DEVICE_UPDATED:
                if (DeviceManager.getInstance().isRecording()) {
                    Log.i(event.getDevice().toString()+"["+event.getType()+"]", String.valueOf(event.getData()));
                    if ("scan".equalsIgnoreCase(String.valueOf(event.getData())))
                        return;
                    TextView tv = (TextView) findViewById(R.id.recording_log);
                    tv.append(Html.fromHtml("<b>" + event.getDevice() + "[" + event.getType() + "]</b>"));
                    tv.append(" ");
                    tv.append(String.valueOf(event.getData()));
                    tv.append("\n");
                }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_devices:
                startActivity(new Intent(this, DeviceListActivity.class));
                return true;
            case R.id.menu_recordings:
                startActivity(new Intent(this, RecordingListActivity.class));
                return true;
            case R.id.menu_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                if (resultCode==RESULT_OK) {
                    Toast.makeText(this, "You can start recording now!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Resolution not finalised: "+ resultCode, Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_LOCATION:
                //TODO
                break;
        }
    }

    @Override
    protected void onDestroy() {
        Log.d("MAIN", "OnDestroy");
        super.onDestroy();
    }

    @Override
    public boolean equals(Object obj) {
        return getClass().equals(obj.getClass());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
