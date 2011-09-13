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
 * Receiver to start Axcs software on boot up.  Can be disabled by setting START_ON_BOOT to false
 */

package gov.nasa.arc.axcs;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
//import android.os.PowerManager;

import android.os.SystemClock;
import android.util.Log;

public class AutoStartReceiver extends BroadcastReceiver
{
	public void onReceive(Context context, Intent intent)
	{ 	
		//This is just to prove through the USB Debugger (check logcat) that the autostarter is properly receiving boot broadcasts
		if (Constants.EPIC_LOGCATS) Log.e("AutoStartReceiver","BOOTED");
		if (Constants.EPIC_LOGCATS) Log.e("AutoStartReceiver","BOOTED");
		if (Constants.EPIC_LOGCATS) Log.e("AutoStartReceiver","BOOTED");
		if (Constants.EPIC_LOGCATS) Log.e("AutoStartReceiver","BOOTED");
        //It shows up red in logcat and is easy to spot
        
        if (!Constants.START_ON_BOOT)
        {//If we aren't supposed to start the CMS on bootup, abort
        	if (Constants.EPIC_LOGCATS) Log.e("AutoStartReceiver", "Not starting program");
        	return;
        }
        
        //create a new intent that points toward the CommandModuleService, lending this broadcastReceiver's context
		Intent cmsIntent = new Intent(context, CommandModuleService.class);
		//resolve the intent to a service, telling that service to "update" if it is already running, not restart
		PendingIntent p = PendingIntent.getService(context, 0, cmsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		//grab the system's alarm manager
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        //tell the alarm manager to schedule calling that method every so often, with an initial delay
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 1000, Constants.CMS_ON_START_COMMAND_REPEAT_DELAY_MS, p);   

	}
}