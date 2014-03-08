package com.metodica.wemoplugin;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.metodica.nodeplugin.INodePluginV1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.util.ArrayList;

public class WeMoPlugin extends Service {
	// PLUGIN VARIABLES
	static final String LOG_TAG = "WeMoPlugin";
	static final String PLUGIN_NAME = "WEMOPLUGIN";
	static final String ACTION = "com.metodica.wemoplugin";
	static final String CATEGORY = "com.metodica.nodeplugin.WEMO_PLUGIN";

    // WeMo Plugin stuff
    SocketAddress mSSDPMulticastGroup;
    MulticastSocket mSSDPSocket;
    InetAddress broadcastAddress;
    ArrayList<WeMoDevice> deviceList = new ArrayList<WeMoDevice>();

    static final String NEWLINE = "\r\n";
    private final INodePluginV1.Stub addBinder = new INodePluginV1.Stub() {
    	    	
    	// Function Name: 	run
    	// Description: 	Uses the COMMAND and DATAs configured in "getXMLDefaults" to 
    	//					launch real actions.
    	// Parameters:
    	//					String command:
    	//					String data1:
    	//					String data2:
    	// 					String data3
    	//					String actionID:
    	//
    	// Return:			XML String
    	
		public boolean runPlugin(String command, String data1, String data2, String data3, String actionID, int flagsInUse) throws RemoteException {
			// 	flagsInUse EXPLANATION:
			//	00000001	PLUGIN UP (NOT USED HERE)
			//	00000010	PLUGIN WORKING (NOT USED HERE)
			//	00000100	USING AN ACTIVITY
			//	00001000	USING CAMERA HARDWARE
			//	00RR0000	RESERVED FLAGS
			//	XX000000	CUSTOM FLAGS FOR YOUR PLUGINS
			
			// FIRST set WORKING flag ON
			setFlag(FLAG_WORKING);
			
			if (command.equalsIgnoreCase("DEVICEON")) {
				try {
                    int device = Integer.parseInt(data1);
                    Log.d(PLUGIN_NAME, "List size: " + deviceList.size() + ", device ID: " + device);
                    deviceList.get(device).sendCommandON();
			        
				} catch (Throwable e) {
					e.printStackTrace();
					return false;
				}
			} else if (command.equalsIgnoreCase("DEVICEOFF")) {
                try {
                    int device = Integer.parseInt(data1);
                    Log.d(PLUGIN_NAME, "List size: " + deviceList.size() + ", device ID: " + device);
                    deviceList.get(device).sendCommandOFF();

                } catch (Throwable e) {
                    e.printStackTrace();
                    return false;
                }

            } else if (command.equalsIgnoreCase("RESCAN")) {
                try {
                    deviceList.clear();
                    SSDPDiscoverTest();

                } catch (Throwable e) {
                    e.printStackTrace();
                    return false;
                }
            } else {

            }
			
			// FREE the plugin if it is not WORKING IN ACTIVITY		
			// Unset Work Flag only if you don't launch any activity.. Otherwise do it in the activity 
			unsetFlag(FLAG_WORKING);
			return true;
		}
		
		
		
		// Function Name: 	getXMLDefaults
		// Description: 	Each <DEFAULT> will be an action you can use from your eKameno Client 
		//					and it needs a <COMMAND> (Which should be the ID of the action to do)
		//					and three <DATA>s which are variables to launch this COMMAND.
		// Parameters:		None
		// Return:			XML String (Take care to close everything you open)

		@Override
		public String getXMLDefaults() throws RemoteException {
            String defaults = "";

            if (deviceList.size() == 0) Log.d(LOG_TAG, "DEVICE LIST EMPTY");
            for (int i=0; i<deviceList.size(); i++) {
                Log.d(LOG_TAG, "DEVICE ADDED CORRECTLY: " +
                        deviceList.get(i).getName() +
                        " AND IS " +
                        (deviceList.get(i).getStatus() == 0 ? "OFF" : "ON"));

                defaults += "<DEFAULT>" +
                                "<NAME>" + deviceList.get(i).getName() + " ON</NAME>" +
                                "<COMMAND>DEVICEON</COMMAND>" +
                                "<DATA1>" + i + "</DATA1>" +
                                "<DATA2></DATA2>" +
                                "<DATA3></DATA3>" +
                            "</DEFAULT>" +
                            "<DEFAULT>" +
                                "<NAME>" + deviceList.get(i).getName() + " OFF</NAME>" +
                                "<COMMAND>DEVICEOFF</COMMAND>" +
                                "<DATA1>" + i + "</DATA1>" +
                                "<DATA2></DATA2>" +
                                "<DATA3></DATA3>" +
                            "</DEFAULT>";
            }

            defaults += "<DEFAULT>" +
                            "<NAME>Re-scan</NAME>" +
                            "<COMMAND>RESCAN</COMMAND>" +
                            "<DATA1></DATA1>" +
                            "<DATA2></DATA2>" +
                            "<DATA3></DATA3>" +
                        "</DEFAULT>";

            return defaults;
		}

		
		
		// Function Name: 	getXMLCustomOptions
		// Description: 	Not working in this version yet.
		// Parameters:		None
		// Return:			XML String
		
		@Override
		public String getXMLCustomOptions() throws RemoteException {
			return "";
		}
		
		
		
		// Function Name: 	initiate
		// Description: 	Some plugins need to initiate something before executing, this function
		//					executes when the plugin becomes active one only time (if it not crash).
		// Parameters:		None
		// Return:			None

		@Override
		public void initiate() throws RemoteException {
            try {
                SSDPDiscoverTest();
            } catch (IOException e) {
                e.printStackTrace();
            }
		}

		
		
		// Function Name: 	is_compatible
		// Description: 	Return True if the plugin can be executed in this platform and
		//					False if it is not.
		// Parameters:		None
		// Return:			Boolean
		
		@Override
		public boolean is_compatible() throws RemoteException {
			// The example works in every platform cause it do nothing
			return true;
		}
		
		
		// IF YOUR ACTION SHOULD BE MANAGED INTO AN ACTIVITY USE THIS FUNCION
//		private void sendActionToActivity(String command, String data1, String data2, String data3, String actionID) {
//			Intent dialogIntent = new Intent(WeMoPlugin.this, TemplateActivity.class);
//			dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//	    	
//			Log.d(LOG_TAG, actionID);
//	    	dialogIntent.putExtra("ACTIONID", actionID);
//	    	dialogIntent.putExtra("COMMAND", command);
//	    	dialogIntent.putExtra("DATA1", data1);
//	    	dialogIntent.putExtra("DATA2", data2);
//	    	dialogIntent.putExtra("DATA3", data3);
//	        	
//	    	setFlag(FLAG_ACTIVITY);
//	    	getApplication().startActivity(dialogIntent);
//		}

        private void SSDPDiscoverTest() throws IOException {
            mSSDPSocket = new MulticastSocket();
            broadcastAddress  = InetAddress.getByName("239.255.255.250");
            mSSDPSocket.joinGroup(broadcastAddress);

            StringBuilder content = new StringBuilder();
            content.append("M-SEARCH * HTTP/1.1").append(NEWLINE);
            content.append("Man:\"ssdp:discover\"").append(NEWLINE);
            content.append("MX:3").append(NEWLINE);
            content.append("Host:239.255.255.250:1900").append(NEWLINE);
            content.append("ST:upnp:rootdevice").append(NEWLINE);


            content.append(NEWLINE);

            final String data = content.toString();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(LOG_TAG, "TRY TO SEND SSDP: " + NEWLINE + data);
                        DatagramPacket dp = new DatagramPacket(data.getBytes(), data.length(),
                                broadcastAddress, 1900);

                        Log.d(LOG_TAG, "SEND UDP PACKET");
                        mSSDPSocket.send(dp);

                        Log.d(LOG_TAG, "UDP PACKET SENT LET'S TRY TO RECEIVE SOMETHING");
                        while (true) {
                            DatagramPacket temp = receive();
                            String c = new String(temp.getData()).toLowerCase();
                            Log.d(LOG_TAG, "RECEIVED(" + temp.getSocketAddress().toString() + "): " + NEWLINE + c);

                            if (c.contains("x-user-agent: redsonic") && c.contains("usn: uuid:socket")) {
                                Log.d(LOG_TAG, "FOUND!!!");
                                addWeMo(c);
                                break;
                            }
                        }

                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        Log.d(LOG_TAG, "ERROR: " + e.getMessage());
                    }
                }
            }).start();
        }

        private DatagramPacket receive() throws IOException {
            byte[] buf = new byte[1024];
            DatagramPacket dp = new DatagramPacket(buf, buf.length);

            mSSDPSocket.receive(dp);

            return dp;
        }

        private void addWeMo(String UDPacket) {
            String locationTemp0 = "location: ";
            String locationTemp1 = "/setup.xml";
            String setupURL = UDPacket.substring(
                    UDPacket.indexOf(locationTemp0) + locationTemp0.length(),
                    UDPacket.indexOf(locationTemp1) + locationTemp1.length()
            );

            Log.d(LOG_TAG, "WeMo URL: " + setupURL);
            WeMoDevice dev = new WeMoDevice(setupURL);
            if (!dev.isError()) deviceList.add(dev);
        }


		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
	    //////////////////\\\\\\\\\\\\\\\\\\\
	    /////							\\\\\
	    /////    - DO NOT TOUCH PART -	\\\\\
	    /////	   SYSTEM FUNCTIONS		\\\\\
	    /////							\\\\\
	    //////////////////\\\\\\\\\\\\\\\\\\\
		
		// DO NOT CROSS THIS LINE!!															\\
		// --------------------------------------------------------------------------------	\\
		
		@Override
		public String getPluginShowName() throws RemoteException {
			return getString(R.string.PluginShowName);
		}
		
		@Override
		public String getPluginName() throws RemoteException {
			return PLUGIN_NAME;
		}
		
		@Override
		public int getStatusFlag() throws RemoteException {
			return statusFlag;
		}
		
		@Override
		public boolean run(String command, String data1, String data2, String data3, String actionID, int flagsInUse) throws RemoteException {
			sendReturn(200, "VOID", "MEDIUM", command + " received", actionID);
			return runPlugin(command, data1, data2, data3, actionID, flagsInUse);
		}

		@Override
		public String getResource() throws RemoteException {
			// TODO Auto-generated method stub
			return null;
		}
	};
	
	
	static final String PLUGINRESPONSE = "com.metodica.ekamenoserver.PLUGINRESPONSE";
	
	// 	FLAGS EXPLANATION:
	//	00000001	PLUGIN UP
	//	00000010	PLUGIN WORKING
	//	00000100	USING AN ACTIVITY
	//	00001000	USING CAMERA HARDWARE
	//	00RR0000	RESERVED FLAGS
	//	XX000000	CUSTOM FLAGS FOR YOUR PLUGINS
	public static final int FLAG_LINKED = 1 << 0;
	public static final int FLAG_WORKING = 1 << 1;
	public static final int FLAG_ACTIVITY = 1 << 2;
	public static final int FLAG_CAMERA = 1 << 3;
	public static final int FLAG_RESERVED1 = 1 << 4;
	public static final int FLAG_RESERVED2 = 1 << 5;
	public static final int FLAG_CUSTOM1 = 1 << 6;
	public static final int FLAG_CUSTOM2 = 1 << 7;
	
	private static int statusFlag;

	public void onStart(Intent intent, int startId) {
		Log.d(LOG_TAG, "onStart()");
		statusFlag = FLAG_LINKED;
		super.onStart( intent, startId );
	}

	public void onDestroy() {
		Log.d(LOG_TAG, "onDestroy()");
		super.onDestroy();
	}

	public IBinder onBind(Intent intent) {
		Log.d(LOG_TAG, "onBind()");
      	return addBinder;
	}
	
	
    //////////////////\\\\\\\\\\\\\\\\\\\
    /////		FLAGS WORKZONE		\\\\\
    //////////////////\\\\\\\\\\\\\\\\\\\
	
	public static synchronized void setFlag(int newFlag) {
		statusFlag |= newFlag;
	}
	
	public static synchronized void unsetFlag(int newFlag) {
		statusFlag &= ~newFlag;
	}
	
	public static synchronized boolean isFlag(int status, int newFlag) {
		return ((status & newFlag) == newFlag);
	}
	
    //////////////////\\\\\\\\\\\\\\\\\\\
    /////		SEND RETURNS		\\\\\
    //////////////////\\\\\\\\\\\\\\\\\\\
	
    private void sendReturn(int errorCode, String type, String criticity, String data, String _actionID) {
		// SEND IMAGE AS RETURN AND CLOSE
		Intent i = new Intent(WeMoPlugin.PLUGINRESPONSE);
		Bundle extras = new Bundle();
		
		if (_actionID != null) extras.putString("ACTIONID", _actionID);
		else extras.putString("ACTIONID", "");

		extras.putInt("ERRORCODE", errorCode);
		extras.putString("TYPE", type);
		extras.putString("DATA", data);
		extras.putString("CRITICITY", criticity);
		
		i.putExtras(extras);
		sendBroadcast(i);
	}
}
