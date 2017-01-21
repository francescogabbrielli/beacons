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
import au.com.smarttrace.beacons.transponder.gps.LocationService;

public class MainActivity extends AppCompatActivity implements
        ServiceConnection, View.OnClickListener, DeviceListener {

    private final static int REQUEST_PERMISSION_LOCATION = 1;
    private final static int REQUEST_CHECK_SETTINGS = 2;

    private BluetoothService bleService;

    private LocationService locService;

    private BroadcastReceiver statusReceiver;

    private BroadcastReceiver permissionReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
        initReceivers();
    }

    private void initUI() {
        ToggleButton b = (ToggleButton) findViewById(R.id.recording_button);
        b.setOnClickListener(this);
    }

    private void initReceivers() {
        statusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent.hasExtra(BluetoothService.KEY_BLUETOOTH_STATUS))
                    Toast.makeText(context,
                                "BT: "+intent.getBooleanExtra(BluetoothService.KEY_BLUETOOTH_STATUS, false),
                                Toast.LENGTH_LONG).show();
                else if (intent.hasExtra(LocationService.KEY_LOCATION_STATUS))
                    Toast.makeText(context,
                            "LOC: "+intent.getBooleanExtra(LocationService.KEY_LOCATION_STATUS, false),
                            Toast.LENGTH_LONG).show();

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

                    // anyway, this should never happen too...
                    Log.i(BeaconTransponder.TAG, "Resolution for location requested?");
                    Status s = intent.getParcelableExtra(LocationService.KEY_PARAM+"_1");
                    try {
                        s.startResolutionForResult(
                                MainActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException e) {

                    }

                }

            }
        };
    }

    @Override
    protected void onStart() {

        super.onStart();

        Intent bService = new Intent(this, BluetoothService.class);
        bindService(bService, this, Context.BIND_ABOVE_CLIENT);
        Intent lService = new Intent(this, LocationService.class);
        bindService(lService, this, Context.BIND_ABOVE_CLIENT);

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
    protected void onResume() {
        super.onResume();
        updateUIStatus();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(this);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(permissionReceiver);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

        if (service instanceof BluetoothService.LocalBinder) {
            bleService = ((BluetoothService.LocalBinder) service).getService();
        } else if (service instanceof LocationService.LocalBinder) {
            locService = ((LocationService.LocalBinder) service).getService();
        }

        Log.i(BeaconTransponder.TAG, "Service bound: "+name.getClassName());

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

        if(BluetoothService.class.getSimpleName().equals(name.getClassName())) {
            bleService = null;
        } else if(LocationService.class.getSimpleName().equals(name.getClassName())) {
            locService = null;
        }

        Log.i(BeaconTransponder.TAG, "Service unbound: "+name.getClassName());
    }

    private void updateUIStatus() {
        Toast.makeText(this, "Updating L/B="
                + (locService!=null && locService.isConnected()) + "/"
                + (bleService!=null && bleService.isConnected()),
                Toast.LENGTH_LONG).show();
        ToggleButton b = (ToggleButton) findViewById(R.id.recording_button);
        b.setEnabled(locService!=null && locService.isConnected()
                && bleService!=null && bleService.isConnected());
    }

    @Override
    public void onClick(View v) {

        ToggleButton b = (ToggleButton) v;

        if (locService == null) {
            Toast.makeText(this, "Unexpected location error", Toast.LENGTH_SHORT).show();
            return;
        }

        if (b.isChecked()) {

            locService.startUpdates();
            DeviceManager.getInstance().startTracking();
            DeviceManager.getInstance().addDeviceListener(this);

        } else {

            locService.stopUpdates();
            DeviceManager.getInstance().stopTracking();
            DeviceManager.getInstance().removeDeviceListener(this);

        }

    }

    @Override
    public void onChange(DeviceEvent event) {
        Log.i(BeaconTransponder.TAG, String.valueOf(event.getData()));
        TextView tv = (TextView) findViewById(R.id.recording_log);
        tv.append(String.valueOf(event.getDevice()));
        tv.append("> ");
        tv.append(String.valueOf(event.getData()));
        tv.append("\n");
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
                    finish();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_LOCATION:
                break;
        }
    }

}
