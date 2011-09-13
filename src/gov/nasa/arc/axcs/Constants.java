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
 * Config parameters for all the subsystems.
 */

package gov.nasa.arc.axcs;

import gov.nasa.arc.axcs.ImageAnalyzer.optimizationScheme;
import android.os.Environment;

public class Constants  
{
	// unique ID for each Axcs
	public static char AXCS_ID = 'A';
	
	//if disabled, the bluetooth hardware will never be programmatically woken up. This saves on battery draw
	public static boolean BLUETOOTH_ENABLED = false;
	
	//if disabled, then IOService.printSerialDebug( will do nothing at all
	public static boolean SERIAL_DEBUG_ENABLED = false;
	
	//The delay between consecutive calls of the CMS's "onStartCommand"
	public static int CMS_ON_START_COMMAND_REPEAT_DELAY_MS = 5000;
	
	public static boolean START_ON_BOOT = true;
	
	public static boolean EPIC_LOGCATS = true; // are we logging everything to the loGcat?
											   // NO, of course not. Some over-the-top debugging was needed once,  
	                                           // and since it was amusing, we called the on/off flag "epic_LOLcats"
	                                           // it was a joke =)
											   // *** Every instance of logging preceded by *** 
	                                           // *** this if epic_LOLcats should be removed.  ***
	
	public static final boolean TEST_MODE = false;  // Used to switch from testing mode
	
	//TODO:
	//radio power and radio callsign, radio via, radio destination
	//serial port baud rate
	
	// !!! all command data MUST BE IN lower case, or it will be mistaken for a command !!!
	
//	public static String STENSAT_VALUE_CALLSIGN = "abcdef"; 		//Benjamin Howard;
	public static final String STENSAT_VALUE_CALLSIGN = "kj6kkz"; 		//Benjamin Howard;
	public static final String STENSAT_VALUE_VIA = "kj6krw";				//
	public static final String STENSAT_VALUE_DESTINATION = "kf6jbp";		//Eric
	public static String STENSAT_VALUE_POWER_LEVEL = "9c";			//must be stored in hex
	public static final String STENSAT_VALUE_BAUD_RATE = "1200";			//1200 AFSK, do not change
	public static String STENSAT_VALUE_END = "\r";//+ (char)13;		//be very careful when changing this. "\r" may also be valid but "\n" is not
		
	public static String IMAGE_PACKET_HEADFOOT = "" + (char) 255;
	//public static String IMAGE_PACKET_HEADFOOT = "(13)";
	
	public static final String STENSAT_COMMAND_CALLSIGN = "C";
	public static final String STENSAT_COMMAND_VIA = "V";
	public static final String STENSAT_COMMAND_DESTINATION = "D";
	public static final String STENSAT_COMMAND_POWER_LEVEL = "P";
	public static final String STENSAT_COMMAND_BAUD_RATE = "M";
	public static final String STENSAT_COMMAND_SEND = "S";
	
	public static int SERIAL_VALUE_BAUD_RATE = 38400;
	
	// Use ASCII85 otherwise use Base64
	public static final boolean USE_ASCII85 = true;
	
	//File Paths
	public static final String AXCS_STORAGE_DIR = Environment.getExternalStorageDirectory().toString() + "/Axcs";
	public static final String GOOD_PICS_DIRECTORY = AXCS_STORAGE_DIR + "/Good_Images";
	public static final String RAW_PICS_DIRECTORY = AXCS_STORAGE_DIR + "/Raw_Images";
	public static final String PACKET_PICS_DIRECTORY = AXCS_STORAGE_DIR + "/Packet_Images";
	public static final String USED_PICS_DIRECTORY = AXCS_STORAGE_DIR + "/Used_Images";
	public static final String INIT_LOG_FILE = AXCS_STORAGE_DIR + "/initLog.txt";
	public static final String PREFERENCES_FILE = "axcsPreferences";
	public static final String HEALTH_LOG_FILE = AXCS_STORAGE_DIR + "/HealthLog.txt";
	public static final String HEALTH_LOG_TMP_FILE = AXCS_STORAGE_DIR + "/HealthTmpLog.txt";
	
	
	//Serial Device Name -- (We could most likely have a script that chooses for us...)
	//public static String SERIAL_DEVICE = "/dev/s3c2410_serial2"; //Nexus S
	public static final String SERIAL_DEVICE = "/dev/ttyMSM0"; //Nexus 1
	
	public static final int healthPacketByteSize = 108;
	public static final int healthLogSize = 30;
	public static final String textMsg = "hello from the axcs";
	public static final String safeMsg = "SAFEMODE";
	
	//Phase definitions
	
	public static boolean phase1Active=true;
	public static final int phase1Limit=1;//1440;  // Phase one will last for 24 hrs
	
	public static boolean safeModeActive=true;
	public static final int safeModeLimit=40;
	public static final int safeModeIncrement = 2;
	public static final int safeModeSize = 10;
	public static final int safeModeMsgByteSize = 134;
	
	public static boolean phase2Active=true;
	public static final int phase2HealthInterval=9; //not a limit, just how often we send health instead of other stuff (PICS)	
	public static final int stensatReprogramInterval = 12;
	public static boolean stensatReprogram = true;
	public static final int numRawImages = 8;
	public static final int lengthOfLowResSend = 1440; // First two day of transmits will be low and medium res
	public static final float lowResWeight1 = (float) .05; //total weight of each resolution for the first interval
 	public static final float medResWeight1 = (float) .84; 
	public static final float highResWeight1 = (float) .1;
	public static final float superResWeight1 = (float) .01;
	public static final float lowResWeight2 = (float) .01; //total weight of each resolution after the first interval
	public static final float medResWeight2 = (float) .2; 
	public static final float highResWeight2 = (float) .79;
	public static final float superResWeight2 = (float) .005;
	public static final int mediumThreshold = 1;
	public static final int maxProcessTimes = 10;//5;
	public static final int packetThreshold = 50;
	public static final int maxImageTransmit = 7600;  // Will transmit current image packet for 7 days corrected for health packets
	public static final float probabilityReceived = (float) .05;
	public static final boolean saveProbAggregate = true; //Whether to save the probabilities and an aggregate image
	public static final optimizationScheme scheme = optimizationScheme.MOST_EDGES;
	
	// WebP image compression parameters
	public static final int quality = 50;
	public static final int targetSize = 145;     // Target image size in bytes
	public static final int maxImageBytes = 155;
	public static final int tileWidth = 20;
	public static final int tileHeight = 15;
	public static final int medWidth = 160;
	public static final int medHeight = 120;
	public static final int fullWidth = 320;
	public static final int fullHeight = 240;
	public static final int superWidth = 1200;
	public static final int superHeight = 900;
	public static final int medScale = medWidth/tileWidth;
	public static final int smallScale = fullWidth/tileWidth;
	public static final int startSmall = medWidth/tileWidth*medHeight/tileHeight;
	public static final int startSuper = startSmall+fullWidth/tileWidth*fullHeight/tileHeight;	
	
	//rm Good_Images/*.png Good_Images/SUCCESS Raw_Images/*.jpg Packet_Images/*.png Packet_Images/*.webp; ls Good_Images/ Raw_Images/ Packet_Images/
}
