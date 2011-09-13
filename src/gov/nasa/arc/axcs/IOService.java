/*
 * Copyright (c) 2011 United States Government as represented by
 * the Administrator of the National Aeronautics and Space Administration.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * The I/O service provides a two-way serial interface for external devices to talk to the Axcs.
 * It handles both incoming serial data and external serial data.
 */

package gov.nasa.arc.axcs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class IOService extends Service
{
	
	//This class manages all I/O operations from the Axcs hardware to the serial port
	//It is responsible for receiving commands and forwarding them to the command module
	//It will also receive sensor updates from the Arduino and forward them to the RCS module (version 2 only)
	static OutputStreamWriter out;
	static FileReader inStream;
	static char[] inBuffer = new char[32];
	static int size;
	
	public static int packetsSent = 0;
	public static boolean DEBUG = false;
	private final IBinder mBinder = new IOServiceBinder();
	
	//Debugging Stuff - START
	
	public static String ourStr;
	
	public static void setStrg(String str){
		ourStr = str;
	}
	
	public String getStrg(){
		return ourStr;
	}
	
	//Debugging Stuff - END
	
	public static SensorDataService ioBoundSDS;

	private ServiceConnection ioSDSConn = new ServiceConnection()
	{
		public void onServiceConnected(ComponentName arg0, IBinder ibinder)
		{
			ioBoundSDS = ((SensorDataService.sdsBinder)ibinder).getService();
		}
		public void onServiceDisconnected(ComponentName name){}
	};
	
	static int counter = 0;
	
	public class IOServiceBinder extends Binder
	{
        IOService getService()
        {
            return IOService.this;
        }
    };
    
	public IBinder onBind(Intent intent)
	{
		//Toast.makeText(this, "Bound IO to Cmd", Toast.LENGTH_LONG).show();
		
		ReadSerialThread rst = new ReadSerialThread(this);
		rst.start();
		
		return mBinder;
	}
	
	public void onCreate()
	{
		//Create boundSDService
		
		//TODO: Somewhere we should check if the SDS has already been created (Perhaps make SDS a singleton).
		//Of course, then having multiple threads accessing the methods in SDS is dangerous.
		//We don't want multiple SDS's running accidentally.
		Intent i1 = new Intent(this, SensorDataService.class);
		this.bindService(i1, ioSDSConn, BIND_AUTO_CREATE);
		
		Toast.makeText(this, "Bound IO to SDS", Toast.LENGTH_LONG).show();
	}
	
    public static void writeSerialDirect(String msg)
    {
    	
    	if (out == null)
		{
			try
			{
				
				FileOutputStream fstream = new FileOutputStream(Constants.SERIAL_DEVICE);
				//Writer 
				out = new OutputStreamWriter(fstream,"ISO8859_1");
				out.write(msg);
		        out.flush();	
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
    	else
    	{
    		try
        	{
    			
    	        out.write(msg);
    	        out.flush();

    	        packetsSent++;
        	}
        	catch(Exception e)
        	{
        		e.printStackTrace();
        	}
    	}

    }
    
	public static void writeSerialDebug(String msg)
	{
		if (Constants.SERIAL_DEBUG_ENABLED)
		{
			writeSerialDirect(msg);
		}
	}
	
	public static String readSerialWait()
	{
		while(true)
		{
			pause(10);
			String s = readSerialDirect();
			if (s != null)
			{
				return s;
			}
		}
	}
	
	public static String readSerialDirect()
	{
		if (inStream == null)
		{
			try
			{
				inStream = new FileReader(Constants.SERIAL_DEVICE);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		
		try
    	{
			//Right now inBuffer is a 32 char array. Need to Adjust based on Input, or decide on a default max length.
    		size = inStream.read(inBuffer);
    		if (Constants.EPIC_LOGCATS) Log.e("IOSERVICE", "Size = "+ size);
    		if(size > 0){
				return new String(inBuffer);
    		}
    		else
    		{
    			return null;
    		}
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    		return null;
    	}
	}
	
	public static void writeRadio(String msg)
	{
		//writeSerialDirect(radioCommandPower + radioPowerLevel + radioCommandEnd);
		//writeSerialDirect(radioCommandCallSign + radioCallsign + radioCommandEnd);
		//writeSerialDirect(radioCommandBaudrate + radioBaudrate + radioCommandEnd);
		
		if (Constants.EPIC_LOGCATS) Log.e("IOSERVICE", "writeRadio() Start");
		DecimalFormat threePlaces = new DecimalFormat("0.000");
		double timeNow = System.currentTimeMillis() / 1000.0;
		threePlaces.format(timeNow);
		
		writeSerialDirect(Constants.STENSAT_COMMAND_SEND + "(" + counter + ")" + msg + Constants.STENSAT_VALUE_END);
		counter++;
		if (Constants.EPIC_LOGCATS) Log.e("IOSERVICE", "writeRadio() done");
	}
	
	public static void closeSerial()
	{
		try
		{
			out.close();
			inStream.close();
		}
		catch(Exception e)
    	{
    		e.printStackTrace();
    	}
	} 
	
	public static void pause(long ms)
	{
		try
		{
			Thread.sleep(ms);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public class ReadSerialThread extends Thread
	{
		IOService owner;
		public ReadSerialThread(IOService ios)
		{
			owner = ios;
		}
		public void run()
		{
			while(true)
			{
				IOService.pause(5);
				String msg = IOService.readSerialDirect();
				
				
				if (msg==null || msg.equals(""))
				{//damn, nothing at all.
					continue;
				}
				
				if(msg != null && msg.charAt(0) == 's'){//Sensor packet "s(battVoltage,temp)"
					
					if (Constants.EPIC_LOGCATS) Log.e("****** IOService","raw IO message: " + msg + "******");
					
					if(ioBoundSDS != null){
					
					ioBoundSDS.setbattVoltage(Integer.valueOf(msg.substring(msg.indexOf("(") + 1 , msg.indexOf(","))));
					ioBoundSDS.setOutsideTemp(Integer.valueOf(msg.substring(msg.indexOf(",") + 1 , msg.lastIndexOf(","))));
					ioBoundSDS.setInsideTemp(Integer.valueOf(msg.substring(msg.lastIndexOf(",") + 1 , msg.indexOf(")"))));
					
					}else{
						//Fix connection between IOService and SensorDataService.
					}
				}
			}
		}
	}
	
}