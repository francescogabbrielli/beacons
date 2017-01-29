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

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.BaseAdapter;
import android.widget.TextView;
import au.com.smarttrace.beacons.Device;
import au.com.smarttrace.beacons.DeviceEvent;
import au.com.smarttrace.beacons.DeviceListener;
import au.com.smarttrace.beacons.DeviceManager;

public class DeviceListAdapter extends BaseAdapter implements DeviceListener {
		
	private Context context;
		
	public DeviceListAdapter(Context context) {
		this.context = context;
		DeviceManager.getInstance().addDeviceListener(this);
	}
		
	@Override
	public synchronized int getCount() {
		return DeviceManager.getInstance().countDevices();
	}

	@Override
	public synchronized Object getItem(int position) {
		return DeviceManager.getInstance().getDevice(position);
	}

	@Override
	public synchronized long getItemId(int position) {
		return position;//list.get(position).getDevice().getUuids()[0].getUuid().getMostSignificantBits();
	}

	@Override
	public synchronized View getView(int position, View convertView, ViewGroup parent) {
		
		Device device = (Device) getItem(position);
		
		LayoutInflater inflater = (LayoutInflater) 
				context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.row_device, parent, false);//XXX: needed for multiple ViewStubs 
		
		TextView tv = (TextView) view.findViewById(R.id.device_title);
		tv.setText(device.toString());

		if (device.getBattery()>=0) {
			tv = (TextView) view.findViewById(R.id.device_battery);
			tv.setText(String.format("%2d%%", device.getBattery()));
		}
		
		try {
			ViewStub stub = (ViewStub) view.findViewById(R.id.device_row_content_stub);
			View content = view.findViewById(R.id.device_row_content);
			content = device.getRowContent(stub, content);
		} catch(Throwable t) {
			Log.e(device.toString(), "Error in displaying stub: "+t);
		}
		
		tv = (TextView) view.findViewById(R.id.device_timestamp);
		tv.setText(R.string.device_last_seen);
		tv.append(": ");
		tv.append(
			SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM).format(
				new Date(device.getLastSeen())));
		
		return view;
	}
	
	@Override
	public void onDeviceChange(DeviceEvent event) {
		notifyDataSetChanged();
	}

}
