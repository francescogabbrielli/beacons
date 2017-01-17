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

import java.util.ArrayList;
import java.util.List;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.TextView;
import au.com.smarttrace.beacons.Utils;

public class ServiceListAdapter extends BaseExpandableListAdapter 
	implements OnGroupCollapseListener, OnGroupExpandListener, OnChildClickListener {
	
	private final static String TAG = ServiceListAdapter.class.getSimpleName();
	
	private final static long DELAY_TIME = 3000l;
	
	private Context context;
	
	private BluetoothGatt btGatt;
	
	class ServiceElement implements Runnable {
		
		BluetoothGattService service;
		Handler handler;
		List<BluetoothGattCharacteristic> characteristics;
		List<byte[]> values;
		boolean stop;
		
		ServiceElement(BluetoothGattService s) {
			service = s;
			characteristics = s.getCharacteristics();
			values = new ArrayList<byte[]>();
			for (BluetoothGattCharacteristic c:characteristics)
				values.add(c.getValue());
			handler = new Handler();
		}
		public void startRead() {
			stop = false;
			run();
		}
		public void stopRead() {
			stop = true;
			handler.removeCallbacks(this);
		}
		@Override
		public void run() {
			for (BluetoothGattCharacteristic c : characteristics)
				btGatt.readCharacteristic(c);
			if (!stop)
				handler.postDelayed(this, DELAY_TIME);
		}
	}
	
	private List<ServiceElement> list;

	public ServiceListAdapter(Context context, BluetoothGatt btGatt) {
		this.context = context;
		this.btGatt = btGatt;
		this.list = new ArrayList<ServiceElement>();
		for (BluetoothGattService service : btGatt.getServices())
			list.add(new ServiceElement(service));
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return list.get(groupPosition).characteristics.get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return groupPosition*1000 + childPosition;//XXX
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view==null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.characteristic, parent, false);
		}
		BluetoothGattCharacteristic characteristic = list.get(groupPosition).characteristics.get(childPosition);
		byte[] val = list.get(groupPosition).values.get(childPosition);
		TextView tv = (TextView) view.findViewById(R.id.characteristic_text);
		//List<BluetoothGattDescriptor> d = characteristic.getDescriptors();
		tv.setText(String.valueOf(characteristic.getUuid()));
		tv = (TextView) view.findViewById(R.id.characteristic_value);
		tv.setText(Utils.bytesToHex(val));
		tv = (TextView) view.findViewById(R.id.characteristic_string);
		tv.setText(val==null ? "" : new String(val));
		return view;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return list.get(groupPosition).characteristics.size();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return list.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return list.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition * 1000;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view==null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.service, parent, false);
		}
		BluetoothGattService service = list.get(groupPosition).service;
		TextView tv = (TextView) view.findViewById(R.id.service_text);
		tv.setText(String.valueOf(service.getUuid()));
		return view;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}
	
	@Override
	public void onGroupCollapse(int groupPosition) {
		//list.get(groupPosition).stopRead();
	}
	
	@Override
	public void onGroupExpand(int groupPosition) {
		//list.get(groupPosition).startRead();
	}
	
	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		btGatt.readCharacteristic(list.get(groupPosition).characteristics.get(childPosition));
		return false;
	}
	
	public void dispose() {
		for (ServiceElement e : list)
			e.stopRead();
	}
	
	public void setCharacteristic(BluetoothGattCharacteristic c) {
		byte[] val = c.getValue();
        Log.d(TAG, "Characteristic read: " + val);
        out:
		for(ServiceElement se : list)
			for(int i=0;i<se.characteristics.size();i++)
				if (se.characteristics.get(i).getUuid().equals(c.getUuid())) {
					Log.i(TAG, "Value updated: " + val);
					se.values.set(i, val);
					notifyDataSetChanged();
					break out;
				}
	}

}
