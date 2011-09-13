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
 * The sensor data service makes internal and external sensor data available 
 * to other services in Axcs, typically a payload module, the command module 
 * service, or the health data service.
 */
package gov.nasa.arc.axcs;

import java.io.BufferedWriter;
import java.text.DecimalFormat;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class SensorDataService extends Service implements LocationListener
{
	//This class is a state machine that passively maintains all data from the sensors
	//For on board sensors, that includes being the registered listener for the gyro, compass, and accelerometers
	//For external sensors, it includes receiving commands from the I/O service and again, maintaining recent sensor updates
	//The RCS service will be actively pinging this service
	SensorManager sensorManager;
	LocationManager lm;
    
    //for field test - wrong place for this.
	static BufferedWriter Mag, Accel, Gps;
	
	float[] accelValues = new float[3];
	float[] gyroValues = new float[3];
	float[] compassValues = new float[3];
	double[] gpsValues = new double[4];
	
	float battVoltage, outsideTemp, insideTemp;
	
	protected void setbattVoltage(int bv){
		battVoltage = ((float)(bv * 5)) / 1023;
	}
	
	protected void setOutsideTemp(int t){
		outsideTemp = ((float )(t * 500)) / 1023; //in Kelvin
	}
	
	protected void setInsideTemp(int t){
		insideTemp = ((float )(t * 500)) / 1023; //in Kelvin
	}
	
	public float getBattVoltage(){
		return battVoltage;
	}
	
	public float getOutsideTemp(){
		return outsideTemp;
	}
	
	public float getInsideTemp(){
		return insideTemp;
	}
	
	public float[] getAccelValues()
	{
		return accelValues;	
	}
	
	public float[] getCompassValues()
	{
		return compassValues;	
	}
	
	public float[] getGyroValues()
	{
		return gyroValues;
	}
	
	public double[] getGpsValues()
	{
		return gpsValues;
	}
	
	public String accelString()
	{
	    DecimalFormat myFormatter = new DecimalFormat("###.####");

		String msg = myFormatter.format(accelValues[0]) + ";" + 
					 myFormatter.format(accelValues[1]) + ";" + 
					 myFormatter.format(accelValues[2]);
		return msg;
	}
	
	public String compassString()
	{
		DecimalFormat myFormatter = new DecimalFormat("###.####");
		
		String msg = myFormatter.format(compassValues[0]) + ";" + 
					 myFormatter.format(compassValues[1]) + ";" + 
					 myFormatter.format(compassValues[2]);
		return msg;
	}
	
	public String gpsString()
	{
		DecimalFormat myFormatter = new DecimalFormat("###.#######");
		
		String msg = myFormatter.format(gpsValues[0]) + ";" + 
					 myFormatter.format(gpsValues[1]) + ";" + 
					 myFormatter.format(gpsValues[2]) + ";" + 
					 myFormatter.format(gpsValues[3]);
		return msg;
	}
	
	private final IBinder mBinder = new sdsBinder();
	
	public class sdsBinder extends Binder
	{
		SensorDataService getService()
        {
            return SensorDataService.this;
        }
    };
	
	public void onCreate()
	{
		IOService.writeSerialDebug("SDS Created\n");
	}
	
	@Override
	//Will it matter if this is executed twice? Because CMS and IOService are both bound to the SDS.
	public IBinder onBind(Intent intent)
	{
		IOService.writeSerialDebug("SDS Bound\n");
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		return mBinder;
	}
	
	public void initializeAll()
	{
		initializeAccel();
        initializeCompass();
        //initializeGyros();
        initializeGps();
        //sensorDataOutput(); //remove after test
        IOService.writeSerialDebug("Initialized sensors\n");
	}
	
	public void initializeAccel()
    {
		accelValues[0] = 0;
		accelValues[1] = 0; 
		accelValues[2] = 0;
		
		try {
    	Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    	sensorManager.registerListener(accelListener, accel, SensorManager.SENSOR_DELAY_FASTEST);
		}
		catch (Exception e) {
			if (Constants.EPIC_LOGCATS) Log.e("SensorDataService","Couldn't init Accel service.");
			e.printStackTrace();
		}
    }
	
	private SensorEventListener accelListener = new SensorEventListener()
    {
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy){}

		@Override
		public void onSensorChanged(SensorEvent event)
		{
			accelValues[0] = event.values[0];
			accelValues[1] = event.values[1]; 
			accelValues[2] = event.values[2];
			//IOService.writeSerialDirect("Accel Update:\n"+accelValues[0]+"\n"+accelValues[1]+"\n"+accelValues[2]+"\n"+System.nanoTime()+"\n\n");
			IOService.writeSerialDebug(System.nanoTime()+"\n");
			
//			writeToFile(String.format("%d \t %f \t %f \t %f \r", 
//					System.currentTimeMillis(), event.values[0], event.values[1], event.values[2]), Accel);
			
			
		}
    };
    
	private void initializeCompass()
    {
		compassValues[0] = 0;
		compassValues[1] = 0;
		compassValues[2] = 0;
		
		try {
			Sensor magnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
			sensorManager.registerListener(magnetListener, magnet, SensorManager.SENSOR_DELAY_FASTEST);
		} 		
		catch (Exception e) {
			if (Constants.EPIC_LOGCATS) Log.e("SensorDataService","Couldn't init Compass service.");
			e.printStackTrace();
		}
    }
	
	private SensorEventListener magnetListener = new SensorEventListener()
    {
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy){}

		@Override
		public void onSensorChanged(SensorEvent event)
		{
			compassValues[0] = event.values[0];
			compassValues[1] = event.values[1];
			compassValues[2] = event.values[2];
			
//			writeToFile(String.format("%d \t %f \t %f \t %f \r", 
//					System.currentTimeMillis(), event.values[0], event.values[1], event.values[2]), Mag);
			
		}
    };
    
    private void initializeGps(){
    	
    	gpsValues[0] = 0;
    	gpsValues[1] = 0;
    	gpsValues[2] = 0;
    	gpsValues[3] = 0;
    	
    	try {
    		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    	}
		catch (Exception e) {
			if (Constants.EPIC_LOGCATS) Log.e("SensorDataService","Couldn't init Gps service.");
			e.printStackTrace();
		}
    }

	public void onLocationChanged(Location location) {
		if (location != null) {
//			Log.d("LOCATION CHANGED", location.getLatitude() + "");
//			Log.d("LOCATION CHANGED", location.getLongitude() + "");
//			Log.d("LOCATION CHANGED", location.getAltitude() + "");

			gpsValues[0] = location.getLatitude();
			gpsValues[1] = location.getLongitude();
			gpsValues[2] = location.getAltitude();
			gpsValues[3] = location.getSpeed();
			
//			writeToFile(String.format("%d \t %f \t %f \t %f \t %f \r", 
//					System.currentTimeMillis(), location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getSpeed()), Gps);

		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
}