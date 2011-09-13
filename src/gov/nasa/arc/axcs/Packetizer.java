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

package gov.nasa.arc.axcs;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.util.Log;

public class Packetizer
{
	private static Queue<String> packetQueue = new ConcurrentLinkedQueue<String>();
	//perhaps look up PriorityQueue
	
	private static int counter = 0;
	
	public static void addToPacketQueue(String packet)
	{
		packetQueue.add(packet);
	}
	
	public static boolean sendToRadioAscii85(byte[] msg)
	{
		if (msg == null)
		{
			return false;
		}
		
		ByteArrayOutputStream boas = new ByteArrayOutputStream(1024);
        Ascii85OutputStream os = new Ascii85OutputStream(boas, 200, false);
       
        try {
        	os.write(msg);
        	os.close();
        	boas.close();
		} 
        catch (IOException e) {
			Log.e("Packetizer", "sendToRadioAscii85: Couldn't encode message!");
			e.printStackTrace();
		}
        	
        byte[] encodedBytes = boas.toByteArray();
        
        Log.e("sendToRadioAscii85", "encodedBytes length = " + encodedBytes.length);
        
        String sendMsg = new String(encodedBytes);
        
        if (Constants.EPIC_LOGCATS) Log.e("packetizer", "sendToRadioDirect encoded string: "+ sendMsg);
		IOService.writeSerialDirect(Constants.STENSAT_COMMAND_SEND + 
									Constants.IMAGE_PACKET_HEADFOOT + 
									sendMsg + Constants.IMAGE_PACKET_HEADFOOT + Constants.STENSAT_VALUE_END);
		if (Constants.EPIC_LOGCATS) Log.e("packetizer", "sendToRadioDirect() end: "+ counter);
		counter++;
		
		return true;
	}
	
	
	public static boolean sendToRadioDirect(byte[] msg, boolean encode)
	{
		if (msg == null)
		{
			return false;
		}
		
		if (Constants.EPIC_LOGCATS) Log.e("packetizer", "sendToRadioDirect() start: "+ counter);
			
		String sendMsg = msg.toString();
		if (encode) {
			sendMsg = Base64.encodeToString(msg, false);
		}
			
		if (Constants.EPIC_LOGCATS) Log.e("packetizer", "sendToRadioDirect encoded string: "+ sendMsg);
		IOService.writeSerialDirect(Constants.STENSAT_COMMAND_SEND + 
				Constants.IMAGE_PACKET_HEADFOOT + 
				sendMsg + Constants.IMAGE_PACKET_HEADFOOT + Constants.STENSAT_VALUE_END);
		if (Constants.EPIC_LOGCATS) Log.e("packetizer", "sendToRadioDirect() end: "+ counter);
		counter++;
		return true;
	}
	
	public static void stensatSetAll()
	{
		stensatSetCallsign();
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			if (Constants.EPIC_LOGCATS) Log.e("Packetizer:stensatSetAll", "Failed to set Call Sign");
			e1.printStackTrace();
		}
		
//		stensatSetVia();
//		stensatSetDestination();
		stensatSetBaud();
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			if (Constants.EPIC_LOGCATS) Log.e("Packetizer:stensatSetAll", "Failed to set Baud");
			e.printStackTrace();
		}

		stensatSetPower();
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			if (Constants.EPIC_LOGCATS) Log.e("Packetizer:stensatSetAll", "Failed to set Power");
			e.printStackTrace();
		}

	}
	
	public static void stensatSetPower()
	{
		IOService.writeSerialDirect(Constants.STENSAT_COMMAND_POWER_LEVEL
				+ Constants.STENSAT_VALUE_POWER_LEVEL
				+ Constants.STENSAT_VALUE_END);
	}
	
	public static void stensatSetCallsign()
	{
		IOService.writeSerialDirect(Constants.STENSAT_COMMAND_CALLSIGN
				+ Constants.STENSAT_VALUE_CALLSIGN
				+ Constants.STENSAT_VALUE_END);
	}
	
	public static void stensatSetVia()
	{ 
		IOService.writeSerialDirect(Constants.STENSAT_COMMAND_VIA
				+ Constants.STENSAT_VALUE_VIA
				+ Constants.STENSAT_VALUE_END);
	}
	
	public static void stensatSetDestination()
	{
		IOService.writeSerialDirect(Constants.STENSAT_COMMAND_DESTINATION
				+ Constants.STENSAT_VALUE_DESTINATION
				+ Constants.STENSAT_VALUE_END);
	}
	
	public static void stensatSetBaud()
	{
		IOService.writeSerialDirect(Constants.STENSAT_COMMAND_BAUD_RATE
				+ Constants.STENSAT_VALUE_BAUD_RATE
				+ Constants.STENSAT_VALUE_END);
	}
}