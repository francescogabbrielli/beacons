/*******************************************************************************
 * 
 *      This file is part of Beacon Transponder.
 *  
 *      Beacon Transponder is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *  
 *      Beacon Transponder is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *  
 *      You should have received a copy of the GNU General Public License
 *      along with Beacon Transponder.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *      Francesco Gabbrielli 2017
 * 
 ******************************************************************************/
package au.com.smarttrace.beacons;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.util.Log;
import android.widget.ExpandableListView;
import android.widget.Toast;

public class DeviceActivity extends Activity implements DeviceListener {
	
	private final static String TAG = "BLE Devices";
		
	private Device device;
	
	private ServiceListAdapter listAdapter;
	
	private int previousState;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device);
		setTitle(R.string.title_activity_device);
		initActivity();
		//initTabs();
	}
	
	private void initActivity() {
		try {
			device = DeviceManager.getInstance().getDevice(getIntent().getStringExtra("id"));
			setTitle(device.toString());
		} catch(NoSuchDeviceException e) {
			Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
			Log.e(TAG, "Device not found: "+getIntent().getStringExtra("id"), e);
		}
	}
	
	@Override
	protected void onResume() {
		
		super.onResume();
		
		DeviceManager.getInstance().addDeviceListener(this);
		
		synchronized (device) {
			previousState = device.getConnectionState();
			device.addGattCallback(callback);
			switch(previousState) {
			case BluetoothProfile.STATE_CONNECTED:
				initListAdapter(device.getGatt());
				break;
			default:
				device.connect();
				break;
			}
		}
		
	}
	
	@Override
	protected void onPause() {
		
		super.onPause();
		
		DeviceManager.getInstance().removeDeviceListener(this);
			
		synchronized (device) {
			device.removeGattCallback(callback);
			if (previousState==BluetoothProfile.STATE_DISCONNECTED)
				device.disconnect();
			if (listAdapter!=null)
				listAdapter.dispose();
		}
		
	}
	
	@Override
	public void onDeviceChange(DeviceEvent event) {
		//TODO? or remove?
	}
	
	private BluetoothGattCallback callback = new BluetoothGattCallback() {
			    
	    @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            runOnUiThread(new Runnable() {
				@Override
				public void run() {
					initListAdapter(gatt);
				}
			});
        }

        @Override
        public void onCharacteristicRead(
        		BluetoothGatt gatt,
        		final BluetoothGattCharacteristic characteristic, 
        		int status) {
        	
            runOnUiThread(new Runnable() {
				@Override
				public void run() {
					synchronized (DeviceActivity.this) {
						listAdapter.setCharacteristic(characteristic);
					}
				}
			});
            
        }
        
        @Override
        public void onCharacteristicChanged(
        		BluetoothGatt gatt, 
        		final BluetoothGattCharacteristic characteristic) {

            runOnUiThread(new Runnable() {
				@Override
				public void run() {
					synchronized (DeviceActivity.this) {
						listAdapter.setCharacteristic(characteristic);
					}
				}
			});
        	
        }
	
	};
	
//	private void initTabs() {
//		// Get the ViewPager and set it's PagerAdapter so that it can display items
//        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
//        viewPager.setAdapter(new DeviceFragmentAdapter(getSupportFragmentManager(), this, deviceResult));
//
//        // Give the TabLayout the ViewPager
//        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
//        tabLayout.setupWithViewPager(viewPager);
//	}	
	
    private synchronized void initListAdapter(BluetoothGatt gatt) {
        ExpandableListView list = (ExpandableListView) findViewById(R.id.list_services);
        listAdapter = new ServiceListAdapter(getApplicationContext(), gatt);
        list.setAdapter(listAdapter);
        list.setOnGroupExpandListener(listAdapter);
        list.setOnGroupCollapseListener(listAdapter);
        list.setOnChildClickListener(listAdapter);
	}

}
