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
package au.com.smarttrace.beacons.transponder;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;
import au.com.smarttrace.beacons.Device;

public class DeviceListActivity extends Activity implements OnItemClickListener {
	
	private DeviceListAdapter listAdapter;
	
	private void initBluetooth() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported", Toast.LENGTH_SHORT).show();
            finish();
        }
	}
	
	private void initList() {
		listAdapter = new DeviceListAdapter(getBaseContext());
		ListView list = (ListView) findViewById(R.id.list_devices);
		list.setAdapter(listAdapter);
		list.setOnItemClickListener(this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initList();
		initBluetooth();
	}
    
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    	Device device = (Device) listAdapter.getItem(position);
    	Intent intent = new Intent(getApplicationContext(), device.getMainActivity());
    	intent.putExtra("id", device.getId());
    	startActivity(intent);
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			startActivity(new Intent(getApplicationContext(), LocationActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
