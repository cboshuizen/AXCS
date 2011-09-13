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
 * The CMS is the heart of the AVCS software, providing the command set that is executed, 
 * including the decision and logic tree for autonomous operation. Sequenced activation of 
 * external processes (such as starting and stopping an external device, or commencing a 
 * particular side-task) is specified in the command module.
 */

package gov.nasa.arc.axcs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

public class CommandModuleService extends Service
{
	//This class serves as the primary executive for all software in Axcs
	//It issues commands to all other modules.
	public static IOService boundIOService;
	public static SensorDataService boundSDS;
	public static boolean BOOT_ON_STARTUP = true;
	private final IBinder mBinder = new CMSBinder();

	public static String logcatFileName;
	public ImageAnalyzer mImageAnalyzer = new ImageAnalyzer();
	private boolean probabilitiesUpdated = false;
	
	private ServiceConnection ioConn = new ServiceConnection()
	{
		public void onServiceConnected(ComponentName arg0, IBinder ibinder)
		{
			boundIOService = ((IOService.IOServiceBinder)ibinder).getService();
		}
		public void onServiceDisconnected(ComponentName name){}
	};
	
	private ServiceConnection sdsConn = new ServiceConnection()
	{
		public void onServiceConnected(ComponentName arg0, IBinder ibinder)
		{
			boundSDS = ((SensorDataService.sdsBinder)ibinder).getService();
		}
		public void onServiceDisconnected(ComponentName name){}
	};
	
	public class CMSBinder extends Binder
	{
		CommandModuleService getService()
        {
            return CommandModuleService.this;
        }
    };
	
    public ArrayList<String> allStrings = new ArrayList<String>();
    int whichString = 0;
    
	public IBinder onBind(Intent intent){return mBinder;}
	public Context mContext;
	public static boolean alive = true;
	public static boolean startedSDS = false;
	public static boolean safeModeComplete = false;
	public static boolean restartIncremented = false;
	public static boolean phase1Complete = false;
	public static boolean phase2Complete = false;
	public static boolean firstBoot = false; // if we cannot determine if we finished commissioning (phase1) skip straight to Ops/safemode. However, we will do our best to determine the correct value.
	public static int restarts = 1; //same for restarts. Might actually be zero, but assume  
	public static int safeModeCounter = 0, phase1Counter = 0, phase2Counter = 0, elapsedTime = 0;
	ArrayList<float[]> dataLog = new ArrayList<float[]>(10); //Global Variable BAD!
	public static int packetIndex = 0;
	public static int numProcessTime = 0;
	public static boolean imagePacketizeSuccess = false;
	private static String imageName;
	//private static ArrayList<byte[]>imageArrayList = null;
	private static File packetListFiles[] = new File[10];
	private static ArrayList<String> logHistory;
	private static BufferedWriter logWriter;
	//private static StringBuilder logHistory;
	private static String logHistoryMsg = "";
	private byte statusArray[] = new byte[Constants.healthPacketByteSize];
	private int byteCounter = 0;
	private byte safeModeMsg[];// = new byte[100];
	private Vector<Packet> packetVector = null;
	
	int counter = 0;
	
	private void parseHealthLog(float[] insideTemperatureLog, float[] outsideTemperatureLog, float[] voltageLog)
	{
		ArrayList<String> healthLog = new ArrayList<String>();
		
		BufferedReader bufferedReader;
		try {
			bufferedReader = new BufferedReader(new FileReader(Constants.HEALTH_LOG_FILE));
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if(line.contains("hello from")) {
					healthLog.add(line);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			Log.e("CMS", "Couldn't open Health log file");
			return;
		}

		Log.e("safemode", "here2 healthLog.size = " + healthLog.size());
		
		// parse out the voltage and temperature from the health message
		int j = 0;
		int i = healthLog.size() - 1;
		
		while (i > 0 && j < Constants.safeModeSize)
		{
			String[] result = healthLog.get(i).split(";");
			if (result.length > 10) {
				//for (int x=0; x < 10; x++)
					//Log.e("safemode", "i = " + i + " x = " + x + " === " + result[x]);
				//Log.e("safemode", "*****************");
				insideTemperatureLog[j] = Float.parseFloat(result[9]);
				outsideTemperatureLog[j] = Float.parseFloat(result[8]);
				voltageLog[j] = Float.parseFloat(result[7]);
				++j;
			}
			i -= Constants.safeModeIncrement;
		}
		
		for (int x = 0; x < voltageLog.length; ++x)
		{
			logHistoryMsg += Float.toString(voltageLog[x]);
			logHistoryMsg += ";";
			logHistoryMsg += Float.toString(outsideTemperatureLog[x]);
			logHistoryMsg += ";";
			logHistoryMsg += Float.toString(insideTemperatureLog[x]);
		}
	}
	
	private byte[] generateSafeModeBytes(float[] insideTemperatureLog, float[] outsideTemperatureLog, float[] voltageLog)
	{
		byte msg[] = new byte[Constants.safeModeMsgByteSize];
		byte tempByteArray[] = new byte[4];
		int i = 0;
		
		msg[i++] = (byte)Constants.AXCS_ID;

		for (int x = 0; x < voltageLog.length; ++x)
		{
			tempByteArray = floatToByteArray(voltageLog[x]);
			msg[i++] = tempByteArray[0];
			msg[i++] = tempByteArray[1];
			msg[i++] = tempByteArray[2];
			msg[i++] = tempByteArray[3];
		}
		for (int x = 0; x < outsideTemperatureLog.length; ++x)
		{
			tempByteArray = floatToByteArray(outsideTemperatureLog[x]);
			msg[i++] = tempByteArray[0];
			msg[i++] = tempByteArray[1];
			msg[i++] = tempByteArray[2];
			msg[i++] = tempByteArray[3];
		}
		for (int x = 0; x < insideTemperatureLog.length; ++x)
		{
			tempByteArray = floatToByteArray(insideTemperatureLog[x]);
			msg[i++] = tempByteArray[0];
			msg[i++] = tempByteArray[1];
			msg[i++] = tempByteArray[2];
			msg[i++] = tempByteArray[3];
		}
		for (int x = 0; x < Constants.safeMsg.length(); ++x)
		{
			msg[i++] = (byte)Constants.safeMsg.charAt(x);
		}
		
		return msg;
	}
	
	public void safemode(){
		safeModeCounter++;
		String msg = "";

		if(safeModeCounter >= Constants.safeModeLimit){					
			safeModeComplete = true;
		}
		
		if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "safemode() counter: "+ safeModeCounter);

		if (safeModeCounter % 2 == 0) {
			msg="SAFEMODE:"+getHealthData();
			Log.e("CommandModuleService", msg);
			
			if (Constants.USE_ASCII85)
				Packetizer.sendToRadioAscii85(statusArray);
			else
				Packetizer.sendToRadioDirect(statusArray, true);
			
		}
		else {
			Log.e("safemode", "Sending previous health data");
			// generate safemode message based on previous health data
			if (logHistoryMsg == "") {
				
				float insideTemperatureLog[] = new float[Constants.safeModeSize];
				float outsideTemperatureLog[] = new float[Constants.safeModeSize];
				float voltageLog[] = new float[Constants.safeModeSize];
				
				parseHealthLog(insideTemperatureLog, outsideTemperatureLog, voltageLog);
				
				for (int i = 0; i < 10; ++i)
				{
					Log.e("safemode", "insideTemp: " + insideTemperatureLog[i] + "outsideTemp: " + outsideTemperatureLog[i] + " voltage: " + voltageLog[i]);
				}
				safeModeMsg = generateSafeModeBytes(insideTemperatureLog, outsideTemperatureLog, voltageLog);
			}
			
			if (Constants.USE_ASCII85)
				Packetizer.sendToRadioAscii85(safeModeMsg);
			else
				Packetizer.sendToRadioDirect(safeModeMsg, true);
		}
		
		if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "safemode() safemode end ");
	}

	public void phase1()
	{
		phase1Counter++;
		
		if (phase1Counter >= Constants.phase1Limit) { // represents 10 packets have passed
			phase1Complete = true;
		}			
				
		if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "phase1() counter: "+ phase1Counter);
			
		sendHealthData();
	}

	public String sendHealthData()
	{
		String msg=getHealthData();
		Log.e("CommandModuleService;", "HealthData;"+msg);
		
		if (Constants.USE_ASCII85)
			Packetizer.sendToRadioAscii85(statusArray);
		else
			Packetizer.sendToRadioDirect(statusArray, true);
		
		if (logHistory.size() > Constants.healthLogSize) {
			Log.e("CMS", "logHistory size = " + logHistory.size());
			//logHistory.subList(0, logHistory.size() - Constants.healthLogSize).clear();
			logHistory.remove(0);
			Log.e("CMS", "after logHistory size = " + logHistory.size());
		}
		
		logHistory.add(msg+"\n");
		
		return msg;
	}
	
	public static final byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
	}
	
	public static byte[] floatToByteArray(float f) 
	{
		int i = Float.floatToRawIntBits(f);
		return intToByteArray(i);
	}
	
	public static final byte[] longToByteArray(long value) {
        return new byte[] {
        		(byte)(value >>> 56),
        		(byte)(value >>> 48),
        		(byte)(value >>> 40),
        		(byte)(value >>> 32),
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
	}
	
	public static byte[] doubleToByteArray(double d) 
	{
		long i = Double.doubleToRawLongBits(d);
		return longToByteArray(i);
	}
	
	public static void printByteArray(ArrayList<byte[]> arrary)
	{
		for (int i = 0; i < arrary.size(); ++i)
		{
			Log.e("printByteArray", "array[ " + i + " ] = " + arrary.get(i));
		}
	}
	
	public static void printByteArray(byte[] array)
	{
		for (int i = 0; i < array.length; ++i)
		{
			Log.e("printByteArray", "array[ " + i + " ] = " + array[i]);
		}
	}
	
	private void addByteData(byte[] data)
	{
		for (int i = 0; i < data.length; ++i) {
			statusArray[byteCounter++] = data[i];
		}
	}
	
	public String getHealthData () {
		final StringBuilder msgBuilder = new StringBuilder();
		float ourOutsideTemp = CommandModuleService.boundSDS.getOutsideTemp();
		float ourInsideTemp = CommandModuleService.boundSDS.getInsideTemp();
		float ourVoltage = CommandModuleService.boundSDS.getBattVoltage();
		int timeNow = (int) (System.currentTimeMillis() / 1000.0);
		
		float accel[] = CommandModuleService.boundSDS.getAccelValues();
		float compass[] = CommandModuleService.boundSDS.getCompassValues();
		double gps[] = CommandModuleService.boundSDS.getGpsValues();
		
		msgBuilder.append(restarts);
		msgBuilder.append(";");
		msgBuilder.append(counter);
		msgBuilder.append(";");
		msgBuilder.append(phase1Counter);
		msgBuilder.append(";");
		msgBuilder.append(phase2Counter);
		msgBuilder.append(";");
		msgBuilder.append(timeNow);
		msgBuilder.append(";");
		msgBuilder.append(ourVoltage);
		msgBuilder.append(";");
		msgBuilder.append(ourOutsideTemp);
		msgBuilder.append(";");
		msgBuilder.append(ourInsideTemp);
		msgBuilder.append(";");
		msgBuilder.append(CommandModuleService.boundSDS.accelString());
		msgBuilder.append(";");
		msgBuilder.append(CommandModuleService.boundSDS.compassString());
		msgBuilder.append(";");
		msgBuilder.append(CommandModuleService.boundSDS.gpsString());
		msgBuilder.append(";");
		msgBuilder.append("hello from the avcs");
		
		byteCounter = 0;
		
		statusArray[byteCounter++] = (byte)Constants.AXCS_ID;

		addByteData(intToByteArray(restarts));
		
		addByteData(intToByteArray(counter));

		addByteData(intToByteArray(phase1Counter));
		
		addByteData(intToByteArray(phase2Counter));
		
		addByteData(intToByteArray(timeNow));
		
		addByteData(floatToByteArray(ourVoltage));
		
		addByteData(floatToByteArray(ourOutsideTemp));
		
		addByteData(floatToByteArray(ourInsideTemp));
		
		for (int x = 0; x < accel.length; ++x)
		{
			addByteData(floatToByteArray(accel[x]));
		}
		
		for (int x = 0; x < compass.length; ++x)
		{
			addByteData(floatToByteArray(compass[x]));
		}
		
		for (int x = 0; x < gps.length; ++x)
		{
			addByteData(doubleToByteArray(gps[x]));
		}
		
		for (int x = 0; x < Constants.textMsg.length(); ++x)
		{
			statusArray[byteCounter++] = (byte)Constants.textMsg.charAt(x);
		}
		
		//Log.e("CommnadModuleService", "***** encoded byte: " + Base64.encodeToString(statusArray, false));

		return msgBuilder.toString();
	}
	
	public void phase2(){
		phase2Counter++;
		
		if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "Start phase 2: "+phase2Counter);

		if(phase2Counter % Constants.phase2HealthInterval == 0){
			sendHealthData(); 
		}
		else if (!Constants.TEST_MODE) {
			File Raw_Images = new File(Constants.RAW_PICS_DIRECTORY);
			File[] allRaw_Images = Raw_Images.listFiles();
			int numRaw_Images = allRaw_Images.length;
			
			File good_Images = new File(Constants.GOOD_PICS_DIRECTORY);
			File[] allGood_Images = good_Images.listFiles();
			int numGood_Images = allGood_Images.length;
			
			File packet_Images = new File(Constants.PACKET_PICS_DIRECTORY);
			File[] allPacket_Images = packet_Images.listFiles();
			int numPacket_Images = allPacket_Images.length;
			
			Log.e("CommandModuleService", "Num of Raw Files: "+numRaw_Images);
			Log.e("CommandModuleService", "Num of Good Files: "+numGood_Images);
			Log.e("CommandModuleService", "Num of Packet Files: "+numPacket_Images);
			
			if (numGood_Images == 1)
			{
				// send out health packet when we are packetizing image
				sendHealthData();
				
				// packetize image
				if (Constants.EPIC_LOGCATS) Log.e("CMS", "Packetize image...");
				packetizeImageService(allGood_Images[0].toString());
			}
			else if ((numPacket_Images > Constants.packetThreshold) && imagePacketizeSuccess)
			{
				if (packetVector == null) {
					ImagePacketizer ip = new ImagePacketizer();
					packetVector = ip.resetPacketVector();
				}
				// send image packets
				if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "Send image packet to radio...");
				
				FilenameFilter lowResFilter = new FilenameFilter() {
					public boolean accept(File dir, String name) {
						if (name.contains("_lowRes.png.webp")) {
							return true;
						} else {
							return false;
						}
					}
				};
				
				FilenameFilter medResFilter = new FilenameFilter() {
					public boolean accept(File dir, String name) {
						if (name.contains("_mediumRes.png.webp")) {
							return true;
						} else {
							return false;
						}
					}
				};
				
				FilenameFilter highResFilter = new FilenameFilter() {
					public boolean accept(File dir, String name) {
						if (name.contains("_highRes.png.webp")) {
							return true;
						} else {
							return false;
						}
					}
				};
				
				File[] lowResPacket = packet_Images.listFiles(lowResFilter);
				File[] medResPacket = packet_Images.listFiles(medResFilter);
				File[] highResPacket = packet_Images.listFiles(highResFilter);
				
				if (packetIndex > Constants.lengthOfLowResSend && !probabilitiesUpdated) {
					Iterator packetIterator = packetVector.iterator();
					while(packetIterator.hasNext()) {
						Packet curPack = (Packet) packetIterator.next();
						curPack.updateProbability();
					}
					probabilitiesUpdated = true;
				}
				Random rand = new Random();
				Double packetPicked = rand.nextDouble();
				Double counter = 0.0;
				int indexSelected = -1;
				String packetSelected = null;
				Iterator packetIterator = packetVector.iterator();
				while(packetIterator.hasNext()) {
					Packet curPack = (Packet) packetIterator.next();
					counter += curPack.probabilitySent;
					if (counter >= packetPicked) 
					{
						indexSelected = curPack.index;
						packetSelected = curPack.filename + ".webp";
						break;
					}
				}
				Log.e("CommandModuleService", "Sending file: " + packetSelected + 
						" packetIndex = " + packetIndex);
			
				if (Constants.USE_ASCII85)
					Packetizer.sendToRadioAscii85(openImageFile(packetSelected));
				else
					Packetizer.sendToRadioDirect(openImageFile(packetSelected), true);
				
				packetIndex++;
				// that was enough image packets sent, nuke the packets files
				if (packetIndex > Constants.maxImageTransmit)
				{
					Log.e("CMS", "Deleting packet files...XXXXXXXXXXX");
					for (int x = 0; x < allPacket_Images.length; ++x)
					{
						allPacket_Images[x].delete();
					}
					
					for (int x = 0; x < allGood_Images.length; ++x)
					{
						Log.e("CMS", "######### deleting all good images");
						allGood_Images[x].delete();
					}
					packetIndex = 0;
					imagePacketizeSuccess = false;
				}
			}
			else if (numGood_Images > 1) 
			{
				boolean done = false;
				
				// send out health packet when we are packetizing image
				sendHealthData();
				
				for (int x = 0; x < allGood_Images.length; ++x)
				{
					if (allGood_Images[x].toString().contains("SUCCESS")) {
						imagePacketizeSuccess = true;
						done = true;
						Log.e("CMS", "Image processing SUCCESS");
						break;
					}
					else if (allGood_Images[x].toString().contains("FAILED")) {
						Log.e("CMS", "Image processing FAILED");
						done = true;
						break;
					}
				}
				
				if (done)
				{	
					//clean up directory once packets are created
					for (int x = 0; x < allGood_Images.length; ++x)
					{
						// move image to another directory to keep it around for debugging.
						if (allGood_Images[x].toString().contains("jpg") && imagePacketizeSuccess)
						{
							String token[] = allGood_Images[x].toString().split("/");
							File renamed = new File(Constants.USED_PICS_DIRECTORY+ "/" + token[token.length - 1]);
							Log.e("CMS", "******* image done moving to: " + renamed.toString());
							allGood_Images[x].renameTo(renamed);
						}
						//else
						//{
							//Log.e("CMS", "========== deleting all good images");
							//allGood_Images[x].delete();
						//}
					}
					
					// delete all the packets that where generated if it failed.
					if (imagePacketizeSuccess == false) 
					{
						for (int x = 0; x < allPacket_Images.length; ++x)
						{
							Log.e("CMS", "******* deleting all good images");
							allPacket_Images[x].delete();
						}
					}
				}
				else 
				{
					Log.e("CommandModuleService", "In process of Packetize image...");
					++numProcessTime;
					
					// Processing in a bad loop, kill the images and start over.
					if (numProcessTime > Constants.maxProcessTimes)
					{
						Log.e("CommandModuleService", "Processing in a bad loop...");
						for (int x = 0; x < allPacket_Images.length; ++x)
						{
							allPacket_Images[x].delete();
						}
						
						for (int x = 0; x < allGood_Images.length; ++x)
						{
							Log.e("CMS", "######### deleting all good images");
							allGood_Images[x].delete();
						}
						numProcessTime = 0;
					}
				}
			}
			else if (numRaw_Images < Constants.numRawImages)
			{
				// start camera activity to acquire image
				if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "Taking picture...");
					imageService();
				
				// send out health packet when we take pictures
				sendHealthData();
				
				//sleep for a few seconds to allow the picture to take
				/*try
				{
					Thread.sleep(7000);
				}
				catch (Exception e){}
				*/
				
				//make a list of all the raw images
				File Raw_Images_New = new File(Constants.RAW_PICS_DIRECTORY);
				
				//score any which have not yet been scored.
				mImageAnalyzer.scoreAllImages(Raw_Images_New.list());
				
			}
			else
			{
				// send out health packet when we are packetizing image
				sendHealthData();
				
				//Todo: run image analysis, for now, just copy image from raw to good directory
				//for (int i = 0; i < numRaw_Images; ++i) {
					//if (allRaw_Images[i].toString().contains("earth5.jpg")) {
					
						//make a list of all the raw images
						File Raw_Images_New = new File(Constants.RAW_PICS_DIRECTORY);
						 
						//score any which have not yet been scored.
						mImageAnalyzer.scoreAllImages(Raw_Images_New.list());
						String bestImageName = mImageAnalyzer.getBestImage();
						File bestImageFile = new File(bestImageName);
						
						Log.e("best image: ", "The best image was: "+bestImageName);
						
						Log.e("CommandModuleService", "Renaming image " + bestImageName+ " to " + 
								Constants.GOOD_PICS_DIRECTORY + "/image" +counter+ ".jpg");
						File renamer = new File(Constants.GOOD_PICS_DIRECTORY + "/image" + counter + ".jpg");
						imageName = renamer.toString();
						bestImageFile.renameTo(renamer);
						//}
					//else {
						//Log.e("******", "Wrong file name = " + allRaw_Images[i]);
					//}
				//}
			}
		}
		else{
			if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "phase2Counter NOT 10: "+phase2Counter);
			FileInputStream f_in;
			try {
				if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "phase2 try begin "+phase2Counter);

				f_in = new FileInputStream(Constants.GOOD_PICS_DIRECTORY + "/" + "packets160x120.ArrayList_data");
				if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "new fileinput stream "+phase2Counter);

				ObjectInputStream obj_in = new ObjectInputStream (f_in);
				if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "new object input stream "+phase2Counter);

				ArrayList<byte[]>arrayList_fetched = 
					new ArrayList<byte[]>((ArrayList<byte[]>)obj_in.readObject());
					if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "new array list "+phase2Counter);
					if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "packetIndex: "+packetIndex);
					if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "arrayList_fetched.size(): "+arrayList_fetched.size());
					
					if(packetIndex < arrayList_fetched.size()){
						if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "packetIndex < arrayList_fetched.size "+phase2Counter);
						if (Constants.EPIC_LOGCATS)  Log.e("CommandModuleService", "sendToRadioDirect() start "+phase2Counter);
						if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService","MSG LENGTH: "+new String(arrayList_fetched.get(packetIndex)).length());
						//Packetizer.sendToRadioDirect(new String(arrayList_fetched.get(packetIndex)), false);
						if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "sendToRadioDirect() end "+phase2Counter);
						packetIndex++;		
					} else
						packetIndex=0;
				
					if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "phase2 try end "+phase2Counter);
	
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (StreamCorruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
	}
	
	public int onStartCommand (Intent intent, int flags, int startId)
	{
		if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "onStartCommand Started "+System.currentTimeMillis());
		
		// Acquire the wake lock so that we don't go to sleep while we go through the command phase
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AxcsV1.0");
        wl.acquire();
        
		if (counter==0) {
			//setBaudRate();
		}
		//It may take a little while for the binding to complete.
		if (boundSDS != null) { //If this isn't here, starting on boot crashes.
			if (!startedSDS) {
				sensorDataService();
				startedSDS = true;
			}

			if(counter % Constants.stensatReprogramInterval == 0 && Constants.stensatReprogram){
				// Sets the callsign, baud, and power for the radio
				Packetizer.stensatSetAll();
				
				saveLogHistory();
			} 
			else {
				//Actual Start of ConOps
				if(!firstBoot && Constants.safeModeActive && !safeModeComplete){ // Something wrong, entering safemode!
					if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "SAFE MODE!");
					
					safemode();
				} else if (Constants.phase1Active && !phase1Complete) {
					if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "Phase 1 start: "+ counter);
					phase1();
				} else if(Constants.phase2Active){ //
					if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "We are in phase 2 "+ counter);
					//phase2 = true;
					phase2();
				} else {
					Log.e("CommandModuleService", " **** We are not in any phase **** ");
				}
		
			}

			saveSystemState();
				
			if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "onStartCommand Finished "+ counter);

			counter++;
		}
		
		wl.release();
		//pm.goToSleep(15);
		return 0;
	}
	
	public void onCreate()
	{	
		Log.e("CMS", "%%%%%%% onCreate()");
		//this is where the code really "Wakes up"
		//Write over serial using static methods. This should work regardless of anything.					
		IOService.writeSerialDebug("CMS service started");
		
		//Create boundIOService and boundSDService and boundCameraService
		Intent i = new Intent(this, IOService.class); 
		this.bindService(i, ioConn, BIND_AUTO_CREATE);
		IOService.writeSerialDebug("\nbound io to CMS");
		
		Intent i2 = new Intent(this, SensorDataService.class);
		this.bindService(i2, sdsConn, BIND_AUTO_CREATE);
		IOService.writeSerialDebug("\nbound sds to CMS\n");
		
		Toast.makeText(this, "Started CMS, bound IO and SDS", Toast.LENGTH_LONG).show();
		//It will start looping through its command thread, executing pre-existing commands
		//from a pre-existing text file, and simultaneously waiting for any word from the ground
	    
		readSystemState();
		
		logHistoryMsg = "";
		
		//elapsedTime = phase2Counter;
		if (restarts ==0) 
		{
			firstBoot=true;
			safeModeComplete=true; //make sure we don't go into safemode this time around. 
			// we should also log the system time in the logfile so we know then the mission began. Add a MissionStartTime line.
		} else 
			firstBoot=false;

		if (!restartIncremented) 
		{	restarts++;
		restartIncremented=true;
		}
		
		// create a writer for us to save the health packet logs
		logHistory = new ArrayList<String>();
		
		saveSystemState();
		
		if (Constants.EPIC_LOGCATS) Log.e("CommandModuleServicepublic","Created CMS "+ counter);
	}
	
	private void saveLogHistory()
	{
		if (logHistory.size() == 0)
		{
			Log.e("CMS", "Log history is zero not saving to file");
			return;
		}
		try {
			logWriter = new BufferedWriter(new FileWriter(Constants.HEALTH_LOG_TMP_FILE));
			for(int i = 0; i < logHistory.size(); ++i) {
				logWriter.write(logHistory.get(i));
			}
			logWriter.flush();
			logWriter.close();
			
			Runtime.getRuntime().exec("mv " + Constants.HEALTH_LOG_TMP_FILE + " " + Constants.HEALTH_LOG_FILE + "\n");
		} catch (IOException e) {
			e.printStackTrace();
			Log.e("CMS", "Failed to create log writer");
		}
	}
	
	private void saveSystemState()
	{
		SharedPreferences settings = getSharedPreferences(Constants.PREFERENCES_FILE, 0);
	    SharedPreferences.Editor editor = settings.edit();
	    editor.putInt("ElapseMissionTime", elapsedTime);
	    editor.putBoolean("First Boot", firstBoot);
	    editor.putInt("Restarts", restarts);
	    editor.putInt("packetIndex", packetIndex);
	    editor.putInt("Phase1Counter", phase1Counter);
	    editor.putInt("Phase2Counter", phase2Counter);

	    // Commit the edits!
	    editor.commit();
	}
	
	private void readSystemState()
	{
	    SharedPreferences settings = getSharedPreferences(Constants.PREFERENCES_FILE, 0);
	    restarts=settings.getInt("Restarts", 0);
	    phase1Counter=settings.getInt("Phase1Counter", 0);
		phase2Counter=settings.getInt("Phase2Counter", 0);
		packetIndex=settings.getInt("packetIndex", 0);
		
	}
	
	private void setBaudRate()
	{
		Process p;
		try
		{					
			//Nexus S Specific		
			if(Constants.SERIAL_DEVICE == "/dev/s3c2410_serial2")
			{
			
				//Wont need to do this once we get a better kernel.
				p = Runtime.getRuntime().exec("su"); //get root
				
				DataOutputStream os = new DataOutputStream(p.getOutputStream());
				
				os.writeBytes("chmod 777 /dev/s3c2410_serial2\n"); //Change permissions
			
				os.writeBytes("stty -F " + Constants.SERIAL_DEVICE + " " + Constants.SERIAL_VALUE_BAUD_RATE + "\n"); //change baud rate
			}
			
			//Old Nexus 1 Stuff
			else {
				p = Runtime.getRuntime().exec("stty -F " + Constants.SERIAL_DEVICE + " " + Constants.SERIAL_VALUE_BAUD_RATE + "\n");
				try {
					p.waitFor();
					if (p.exitValue() != 255) {
						if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "CMS set serial port success");
					} else {
						if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "CMS set serial port fail");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				// Send empty \r to the serial port so we don't lose it.
				//IOService.writeSerialDirect(Constants.STENSAT_VALUE_END);
			}
			if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService", "CMS set baud");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void sensorDataService()
	{
		//start the sensor polling
		IOService.writeSerialDebug("starting SDS");
		
		if(boundSDS == null){
			if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService","boundSDS "+ counter);
		}
		else {
			CommandModuleService.boundSDS.initializeAll();
		}
	}
	
	private void imageService() 
	{
		//take picture
		if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService","start TPS");
		Intent i = new Intent(this, CameraActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		this.startActivity(i);
	}
	
	private void packetizeImageService(String imageFile)
	{
		//send picture service
		if (Constants.EPIC_LOGCATS) Log.e("CommandModuleService","start SPS "+ counter);
		
		ImagePacketizer ip = new ImagePacketizer();
		ip.packetizeBestPhoto(imageFile);
		packetVector = ip.getPacketVector();
	}
	
	private byte[] openImageFile(String fileName)
	{
		byte imageBytes[] = new byte[200];
		byte returnBytes[] = null;
		try {
			FileInputStream fileStream = new FileInputStream(fileName);
			int numBytes = fileStream.read(imageBytes);

			if (numBytes > Constants.maxImageBytes) {
				Log.e("CommandModuleService","ERROR: Image bytes size : " + numBytes + fileName);
				return returnBytes;
			}
			
			// Add 4 bytes for the tile index
			returnBytes = new byte[numBytes+4];
			
			int slashIndex = fileName.lastIndexOf("/");
			
			returnBytes[0] = (byte)Constants.AXCS_ID;
			
			returnBytes[1] = (byte)fileName.charAt(slashIndex+1);
			returnBytes[2] = (byte)fileName.charAt(slashIndex+2);
			returnBytes[3] = (byte)fileName.charAt(slashIndex+3);
			returnBytes[4] = (byte)fileName.charAt(slashIndex+4);
			
			for (int i = 0; i < numBytes; ++i)
			{
				returnBytes[i+5] = imageBytes[i];
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return returnBytes;
	}

}