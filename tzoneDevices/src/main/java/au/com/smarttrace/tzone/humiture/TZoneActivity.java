package au.com.smarttrace.tzone.humiture;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import au.com.smarttrace.beacons.BluetoothService;
import au.com.smarttrace.beacons.Device;
import au.com.smarttrace.beacons.DeviceEvent;
import au.com.smarttrace.beacons.DeviceListener;
import au.com.smarttrace.beacons.DeviceManager;
import au.com.smarttrace.beacons.NoSuchDeviceException;

public class TZoneActivity extends Activity 
					implements OnCheckedChangeListener, DeviceListener, ServiceConnection {
	
	/** The Bluetooth Low Energy app service */
	private BluetoothService bleService;

	/** The TZONE device */
	private TZoneHumitureDevice device;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tzone);
		initUI();
	}

	/**
	 * Init the activity UI
	 */
	private void initUI() {
		try {
			device = (TZoneHumitureDevice) 
					DeviceManager.getInstance().getDevice(
							getIntent().getStringExtra("id"));
			setTitle(device.toString());
		} catch(NoSuchDeviceException e) {
			Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
			Log.e("", "Device not found: "+getIntent().getStringExtra("id"), e);
			finish();
		}
		ToggleButton tb = (ToggleButton) findViewById(R.id.tzone_sync_button);
		tb.setOnCheckedChangeListener(this);
	}
		
	/**
	 * Refresh the data in the activity UI
	 */
	private void refreshUI() {
		
		int progress = device.getGattProgress();
		ProgressBar bar = (ProgressBar) findViewById(R.id.tzone_humiture_all_progress);
		if (progress != Device.PROGRESS_NONE) {
			bar.setIndeterminate(progress==Device.PROGRESS_INDEFINITE);
			if (!bar.isIndeterminate())
				bar.setProgress(progress);
			bar.setVisibility(View.VISIBLE);
		} else
			bar.setVisibility(View.GONE);

		((TextView) findViewById(R.id.tzone_humiture_all_serial))
						.setText(device.getSerialNumber());
		((TextView) findViewById(R.id.tzone_humiture_all_temperature))
						.setText(String.format("%2.2f°C", device.temperature));
		((TextView) findViewById(R.id.tzone_humiture_all_humidity))
						.setText(String.format("%2.2f%%", device.humidity));
		((TextView) findViewById(R.id.tzone_humiture_all_records))
						.setText(String.format("%d", device.records));
		((TextView) findViewById(R.id.tzone_humiture_all_lastseen))
						.setText(SimpleDateFormat
							.getTimeInstance(SimpleDateFormat.MEDIUM)
								.format(
									new Date(System.currentTimeMillis()-device.getElapsedTime())));
	
		findViewById(R.id.tzone_sync_button)
				.setEnabled(device.getConnectionState()==BluetoothProfile.STATE_CONNECTED
						&& progress==Device.PROGRESS_NONE);
	}

	@Override
	protected void onStart() {
		super.onStart();
		//bind the BT service
		Intent service = new Intent(this, BluetoothService.class);
		bindService(service, this, Context.BIND_ABOVE_CLIENT);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		DeviceManager.getInstance().addDeviceListener(this);
		refreshUI();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		DeviceManager.getInstance().removeDeviceListener(this);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.tzone, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		synchronized (this) {
			if (disconnect())
				unbindService(this);
		}
	}
	
	/**
	 * Disconnect the device if it's idle
	 * 
	 * @return
	 * 			true if it's actually disconnecting
	 */
	private boolean disconnect() {
		if (bleService!=null) {
			bleService.disconnect(device);
			bleService = null;
			return true;
		}
		return false;
	}
	
	@Override
	public synchronized void onServiceConnected(ComponentName name, IBinder service) {
		BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
		bleService = binder.getService();
		bleService.connect(device);
	}
	
	@Override
	public synchronized void onServiceDisconnected(ComponentName name) {
		disconnect();
	}
	
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		device.sync(isChecked);
	}
	
	@Override
	public void onChange(DeviceEvent event) {
		
		if (event.getDevice()==device) {
			
			switch (event.getType()) {
			case DeviceEvent.TYPE_DEVICE_UPDATED:
			case DeviceEvent.TYPE_DEVICE_PROGRESS:
				refreshUI();
				break;
			case DeviceEvent.TYPE_DEVICE_ERROR:
				Toast.makeText(getApplicationContext(), 
						String.valueOf(event.getData()), Toast.LENGTH_LONG).show();
			}
						
		}
		
	}
	
}
