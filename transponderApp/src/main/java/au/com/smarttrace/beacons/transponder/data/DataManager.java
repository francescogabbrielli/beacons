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
import java.util.LinkedList;
import java.util.Queue;

import android.content.Context;
import android.content.Intent;
import android.location.Location;

/**
 * <p>
 * Data transfer manager
 * 
 * <p>
 * Synchronises data from the smartphone to the remote server using a queue 
 * to store incoming data from connected devices 
 * 
 * @author Francesco Gabbrielli <francescogabbrielli at gmail>
 */
public class DataManager implements TransferClient<RemoteData>, TransferServer<BTData> {
	
	/** Data queue to be uploaded */
	private Queue<RemoteData> queue;
	
	/** Singleton instance */
	private static DataManager instance;
	
	/** Transfer manager to the remote server */
	private TransferManager<RemoteData> transferManager;
	
	/** Android context */
	private Context context;
	
	/** Intent for starting the background service implementing the transfer */
	private Intent serviceIntent; 

	/** Private singleton constructor */
	private DataManager(Context context) {
		queue = new LinkedList<RemoteData>();
		this.context = context.getApplicationContext();
		serviceIntent = new Intent(context, RemoteTransferService.class); 
		transferManager = new TransferManager<RemoteData>(this, RemoteServer.getInstance());
		transferManager.addTransferListener(new TransferListener<RemoteData>() {

			@Override
			public void onBegin() {
				
			}

			@Override
			public void onProgress(TransferResult<RemoteData> result) {
				
			}

			@Override
			public void onEnd(TransferResult<RemoteData> lastResult) {
				
			}
			
			
		});
	}
	
	/**
	 * Get the singleton instance
	 * 
	 * @param context
	 * 			only effective on the first call
	 * @return
	 * 			the data manager
	 */
	public static DataManager getInstance(Context context) {
		if (instance==null)
			instance = new DataManager(context);
		return instance;
	}
			
	/**
	 * Add a new real-time reading from the device (client) 
	 * 
	 * @param rawData
	 * 			the data 
	 * @param location
	 * 			the current location in the smartphone
	 * @return
	 * 			true is the operation is successful
	 */
	public synchronized boolean addData(BTData rawData, Location location) {
		if (queue.offer(new RemoteData(rawData, location))) {
			context.startService(serviceIntent);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean isReachable() {
		return true;
	}
	
	public TransferManager<RemoteData> getTransferManager() {
		return transferManager;
	}
	
	/**
	 * Read the last data from the actual data queue
	 */
	@Override
	public synchronized RemoteData read() throws IOException {
		return queue.peek();
	}
	
	/**
	 * Remove the last data from the actual data queue (if it still matches...)
	 */
	@Override
	public synchronized void remove(RemoteData data) {
		if (queue.peek()==data)
			queue.poll();
	}

	/**
	 * Add incoming data from a device data logger (not real time)
	 */
	@Override
	public synchronized TransferResult<BTData> write(BTData rawData) throws IOException {
		return addData(rawData, null) 
				? new TransferResult<BTData>(TransferResult.RESULT_OK) 
				: new TransferResult<BTData>(TransferResult.RESULT_FULL);
	}
	
}
