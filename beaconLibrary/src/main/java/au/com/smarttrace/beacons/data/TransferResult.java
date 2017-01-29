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

public class TransferResult<T extends Data> {
	
	public final static int RESULT_FULL			 	= 0;	
	public final static int RESULT_OK 				= 1;
	public final static int RESULT_NO_MORE_DATA 	= 2;
	public final static int RESULT_SERVER_ERROR 	= 3;
	public final static int RESULT_CLIENT_ERROR 	= 4;
	
	private int code;
	
	private String message;
	
	private T data;
	
	public TransferResult(int code) {
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}
	
	public String getMessage() {
		return message;
	}
	
	public TransferResult<T> setMessage(String message) {
		this.message = message;
		return this;
	}
	
	public T getData() {
		return data;
	}
	
	public TransferResult<T> setData(T data) {
		this.data = data;
		return this;
	}
	

}
