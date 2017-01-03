# beacons
   			
      Beacon                          Smartphone                             Server
=====================================================================================================
        | Advertise                       |Install the                          |
		| realtime                        |app                                  |
		| data                            |                                     |
        |-------------------------------->|                                     |
        |                                 |Check if the beacon                  |
		|								  |is managed                           |
        |                                 |------------------------------------>|
		|								  |                                     |YES
		|								  |<------------------------------------|
		|								  |Establish                            |
		|								  |GATT connection                      |NO
		|<--------------------------------|                                     |
		|								  |<------------------------------------|
										  |No action
										  |
										  |
					
	0. Definitions:
	===============
	
		- Device  			a bluetooth low energy (BLE) device with sensors and data logger
		
		- App  				the app running on a smartphone
		
		- DataManager		a component of the app that will record the data for each device
							and take care of transmitting them to the central repository (server)
							when the network is available;
							is also responsible for activating alerts and send real time notifications
							to the user
		
		- Server			database and backend engine
		
		- TransferService	is a component running in a thread both in the DataManager and the GATT 
							connection managing client/server data interchange when a connection is
							established:
							
								CLIENT                                  SERVER
								---*---------------------------------------*--
								   |                                       |
								   |[Send last data]>>-------------------->|
								   |                                       |
								   |                                       | 1. Data received (trigger actions)
								   |                                       | 2. Store
								   |                                       |
								   |<----------------------<<[Send confirm]|
								   |                                       |
								   | 1. Remove Data*                       |
								   | 2. Update pointer to last data        |
								   |                                       |
								   
								* the data removed is not necessarily the last one, to allow a recent data 
								  buffer on the client
							  
							If a connection is not available, data is stored in the client buffer queue
							(or whatever acts as one).
							
							In case of multiple data available to send, grouping and compression techniques
							might be used.
							  
							In this proejct there are two transfer services that work in a chain, linked through 
							the smartphone: the first client is the device and the server is the smartphone 
							app (GATT connection background server); the second client is the smartphone app
							(DataManager) and the server is the remote repository
							   
										  
	1. When the app is installed a background service will start whenever the bluetooth is turned on
	================================================================================================
	
		The purpose of the service is to discover bluetooth devices and, if they are managed by the current
		user/shipment, to connect to them establishing bidirectional GATT interaction
		
			Device                        Smartphone                         Server
			---*-------------------------------*--------------------------------*--
			   |<----------<<[Discover devices]|                                |
			   |                               |                                |
			   |[Discovered]>>---------------->|                                |
			   |                               | Updated info?                  |
			   |                               | |_NO_>[Query managed]>>------->|
			   |                               |                                | Query DB
			   |                               |<---------------<<[Managed info]|
			   |                               | Is Managed?                    |
			   |<-------------------<<[Connect]|<|_YES_                         |
			   |                               | |_NO__>[no action]             |
			   |[Connected]>>----------------->|                                |
			   |                               | SEE 2.                         |
			   |[Connected]>>----------------->|                                |
			   |                               | SEE 3.                         |
		
	2. When a device connection is established:
	===========================================
	
		1. a new GATT callback is created 
	
			The GATT callback will register for notifications and manage interaction displaying the updated 
			data/reading from the device sensors. The data is sent to the DataManager with extra information
			gathered from the smartphone (like position from GPS, time, etc...)
	
		2. a background service is started to extract data from the data logger
		
			It will begin from the pointer to the last data extracted and send it to the DataManager 
			(without additioanl info) and update the pointer to last data. It will also attempt to remove
			extracted data from the device. This service is very similar to the service present in the DataManager
			(see definitions)
			

	3.  When the device	disconnects:
	================================
	
		- Both the DataManager and the GATT data logger service are stopped istantaneously
		- Connection/Disconnection status is treated like other data and sent to the server
