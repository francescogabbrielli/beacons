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
package au.com.smarttrace.beacons.transponder.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Activate transfers to the remote servers when the network connection
 * becomes available
 */
public class NetworkReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
	    ConnectivityManager conn = (ConnectivityManager)
	            context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo info = conn.getActiveNetworkInfo();
	    
	    Intent serviceIntent = new Intent(context, RemoteTransferService.class);

		if (info!=null)
			if (info.isConnected()) {
				context.startService(serviceIntent);
			} else {
				context.stopService(serviceIntent);
			}
		
	}

}
