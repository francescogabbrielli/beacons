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

import java.io.IOException;

import android.bluetooth.BluetoothDevice;

public class RemoteServer implements TransferServer<RemoteData> {
	
	private static RemoteServer instance;
	
	private RemoteServer() {
		
	}
	
	public static RemoteServer getInstance() {
		if (instance==null)
			return new RemoteServer();
		return instance;
	}

	@Override
	public TransferResult<RemoteData> write(RemoteData data) throws IOException {
		return new TransferResult<RemoteData>(TransferResult.RESULT_SERVER_ERROR)
				.setMessage("?");
	}

	@Override
	public boolean isReachable() {
		return true;
	}
	
	public boolean isManaged(BluetoothDevice device) {
		return true;
	}
	
}
