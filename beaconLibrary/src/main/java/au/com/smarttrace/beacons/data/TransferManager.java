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

import java.util.LinkedList;
import java.util.List;

import android.util.Log;

/**
 * Manage the data transfer between a client and a server whenever
 * possible
 * 
 * @param <T> The data type being transferred
 * 
 * @author Francesco Gabbrielli <francescogabbrielli at gmail>
 */
public class TransferManager<T extends Data> {
	
	/** Log TAG */
	private final static String TAG = TransferManager.class.getSimpleName();
		
	/** The client */
	private TransferClient<T> client;
	
	/** The server */
	private TransferServer<T> server;
	
	private boolean transferring;
	
	private List<TransferListener<T>> listenersList;
	
	/**
	 * Create a new transfer manager between a client and a server
	 * 
	 * @param client
	 * 			the {@link TransferClient} client
	 * @param server
	 * 			the {@link TransferServer} server
	 */
	public TransferManager(TransferClient<T> client, TransferServer<T> server) {
		this.client = client;
		this.server = server;
		listenersList = new LinkedList<TransferListener<T>>();
	}
	
	/**
	 * <p>
	 * Start transferring data. Data is removed from the client as soon
	 * each transfer is confirmed by the server.
	 * 
	 * <p>
	 * This procedure halts on these conditions:
	 * <ul>
	 * <li>No more data to read on the client 
	 * <li>An server error occurs
	 * <li>An I/O or unexpected error occurs
	 * </ul>
	 */
	public synchronized void transfer() {
		
		if (client.isReachable() && server.isReachable()) {
			
			TransferResult<T> result = null;
			
			try {
				
				fireBegin();				
				transferring = true;
				
				// transfer cycle
				while (transferring && (result=transferData()) != null)
					if(result.getCode() == TransferResult.RESULT_OK) {
						client.remove(result.getData());
						fireProgress(result);
					} else
						break;
					
			} catch(Exception e) {

				Log.e(TAG, "Data transfer client "+client+" error", e);
				result = new TransferResult<T>(TransferResult.RESULT_CLIENT_ERROR)
					.setMessage(e.getMessage());
				
			} finally {
				
				transferring = false;
				fireEnd(result);
				
			}
		}
		
	}
			
	/**
	 * Actual implementation of a single data transfer
	 * 
	 * @return
	 * 			the result of the transfer
	 */
	protected TransferResult<T> transferData() {
		TransferResult<T> result = null;
		T data = null;
		try {
			data = client.read();
			result = data!=null 
					? server.write(data)
					: new TransferResult<T>(TransferResult.RESULT_NO_MORE_DATA);
		} catch(Exception e) {
			Log.e(TAG, "Data transfer error from "+client+" to " +server, e);
			//throw new TransferException("Data transfer error from "+client+" to " +server, e);
			result = new TransferResult<T>(TransferResult.RESULT_SERVER_ERROR)
					.setMessage("Data transfer error from "+client+" to " +server);
		}
		return result.setData(data);
	}
	
	/**
	 * Check for ongoing data transfer
	 * 
	 * @return
	 * 			true if a transfer is ongoing
	 */
	public boolean isTransferring() {
		return transferring;
	}

	/**
	 * Stop an eventually active transfer
	 */
	public void stop() {
		transferring = false;
	}
	
	public synchronized void addTransferListener(TransferListener<T> listener) {
		listenersList.add(listener);
	}
	
	public synchronized void removeTransferListener(TransferListener<T> listener) {
		listenersList.remove(listener);
	}
	
	protected synchronized void fireBegin() {
		for (TransferListener<T> listener : listenersList)
			listener.onBegin();
	}
	
	protected synchronized void fireProgress(TransferResult<T> result) {
		for (TransferListener<T> listener : listenersList)
			listener.onProgress(result);
	}

	protected synchronized void fireEnd(TransferResult<T> lastResult) {
		for (TransferListener<T> listener : listenersList)
			listener.onEnd(lastResult);
	}
	
}
