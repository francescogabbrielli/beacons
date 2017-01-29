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
package au.com.smarttrace.beacons.data;

import android.app.IntentService;
import android.content.Intent;

public class RemoteTransferService extends IntentService {
	
	public RemoteTransferService() {
		super("Remote TransferService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
		DataManager
			.getInstance(getApplicationContext())
				.getTransferManager()
					.transfer();
		
	}
	
	@Override
	public void onDestroy() {		
		super.onDestroy();
	}

}
