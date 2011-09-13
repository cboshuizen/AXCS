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

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
//import android.os.PowerManager;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.util.TimerTask;


public class UserActivity extends Activity
{
	//Graphical Variables:
		TextView text1;
		EditText edit1;
		Button button1;
		Button button2;
		Button button3;
	
		
	//More Important Variables
	//The alarm manager is what we schedule our alarms with. We tell it to call a certain method every x number of minutes
	AlarmManager am;
	//a pending intent that wraps our command module service's "onStartCommand" method.
	PendingIntent p;
	//numRestarts is maintained using a database. The STK has built in functionality that you can find by googling "android device storage"
	private static int numRestarts = 0;
	//the prefix "bound" means that when we start a service (like the CMS), we can directly make references to that running service to make
	//method calls, set variables, etc. Note that you have to jump through some ugly hoops to actually bind a service
	private CommandModuleService boundCMS;
	
	//this ServiceConnection is only used once, when binding to the CMS. Note that for any service you want to bind to, you
	//must have a dedicated ServiceConnection that grabs a pointer to that service.
	private ServiceConnection cmsConn = new ServiceConnection()
	{//a reference to this serviceConnection is passed to the JVM when we bind the CMS. When the connection happens, this method is called
		public void onServiceConnected(ComponentName arg0, IBinder ibinder)
		{//All we do is grab the binder, grab it's service, and store that pointer locally to boundCMS.
			boundCMS = ((CommandModuleService.CMSBinder)ibinder).getService();
		}//After that line executes, boundCMS will really point at the service that is actually running
		public void onServiceDisconnected(ComponentName name){}
	};
	
    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState)
    {
    	//This activity is purely a front-end for users to observe the code
    	//The auto-starter doesn't even start this activity, it only starts the CMS.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        //this line will prevent unwanted orientation changes that can wreck havoc on the current activity
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        //GUI elements:
        text1 = (TextView)findViewById(R.id.text1);
        edit1 = (EditText)findViewById(R.id.edit1);
        button1 = (Button)findViewById(R.id.button1);
        button2 = (Button)findViewById(R.id.button2);
        button3 = (Button)findViewById(R.id.exit);
        button1.setOnClickListener(button1Listener);
        button2.setOnClickListener(button2Listener);
        button3.setOnClickListener(button3Listener);
        
        //Bind the CMS to this activity using the cmsConn variable
        this.bindService(new Intent(this, CommandModuleService.class), cmsConn, BIND_AUTO_CREATE);
        //this line will call "onCreate" and "onBind" in the CMS
        //Note that onCreate causes CMS to spawn a thread that checks the IOService's command queue
        //Note that it may also call onStartCommand (i forget if it does or doesn't)
        
        //If we want to keep track of the number of program restarts (which should be the same as the number of axcs restarts)
        //uncomment the following code:
        /*SharedPreferences settings = this.getPreferences(MODE_PRIVATE);
        numRestarts = settings.getInt("NUM_RESTARTS", 0);
        Log.e("NUM_RESTARTS:",numRestarts+"");
        numRestarts += 1;
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("NUM_RESTARTS", numRestarts);
        editor.commit();*/
        
        //this chunk of code schedules a repeating alarm that will call the CMS "onStartCommand" method
        Intent cmsIntent = new Intent(this, CommandModuleService.class);
        Context context = this.getApplicationContext();
        p = PendingIntent.getService(context, 0, cmsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 
        		SystemClock.elapsedRealtime() + 2000, 
        		Constants.CMS_ON_START_COMMAND_REPEAT_DELAY_MS, p);
        
        //wl.release();
    }
    public static int getNumRestarts()
    {//this is a public accessor method so other parts of code can know how many restarts there have been
    	return numRestarts;
    }
    private void button1Action()
    {
    	//If the button's action takes too long, you should use a timerTask as follows:
    	//LongActionList lal = new LongActionList(this);
    	//Timer t = new Timer();
    	//t.schedule(lal, 10);
    	//Note that if any GUI element holds the thread for more than 5 seconds, the activity will force close
    	//so all methods called in the main GUI thread should be very very quick
    	//hence using the timerTask, which operates in a different thread
    }
    public class LongActionList extends TimerTask
    {
    	UserActivity ua;
    	public LongActionList(UserActivity ua2)
    	{
    		ua = ua2;
    	}
		public void run()
		{
			//ua.boundCMS.processCommand("c start TPS");
			//IOService.pause(5000);//this line is fine in a timerTask but would be illegal in the UI thread
			//ua.boundCMS.processCommand("c start SPS");
		}
    }
    
    private void button2Action()
    {	 
    	
    }
    private void button3Action() 
    {
    	am.cancel(p);
    	IOService.closeSerial();
    	this.finish();
    	System.exit(0);
    }
    private OnClickListener button1Listener = new OnClickListener()
    {
        public void onClick(View v)
        {
        	button1Action();
        }
    };
    private OnClickListener button2Listener = new OnClickListener()
    {
        public void onClick(View v)
        {
       		button2Action();
        }
    };
    private OnClickListener button3Listener = new OnClickListener()
    {
        public void onClick(View v)
        {
       		button3Action();
        }
    };	
}