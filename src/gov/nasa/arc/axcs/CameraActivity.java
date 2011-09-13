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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;

public class CameraActivity extends Activity implements SurfaceHolder.Callback, OnClickListener
{
	static final int FOTO_MODE = 0;
	private static final String TAG = "CameraTest";
	Camera mCamera;
	boolean mPreviewRunning = false;
	//private Context mContext = this;
	public static int picsTaken = 0;
	public static byte[] data;
	public static final String CUSTOM_ACTION = "AXCS_PICTURE_TAKEN";
	
	public void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);

		Log.e(TAG, "onCreate");
		
		//Bundle extras = getIntent().getExtras();
		
		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.camera_surface);
		mSurfaceView = (SurfaceView) findViewById(R.id.surface_camera);
		mSurfaceView.setOnClickListener(this);
		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		TakePictureTask tpt = new TakePictureTask(this);
		Timer t = new Timer();
		t.schedule(tpt, 2500);
	}

	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
	}

	Camera.PictureCallback mPictureCallback = new Camera.PictureCallback()
	{
		public void onPictureTaken(byte[] imageData, Camera c)
		{
			if (imageData != null)
			{
				
				Intent mIntent = new Intent();
				//FileUtilities.StoreByteImage(mContext, imageData, 50, "ImageName");
				mIntent.setAction(CameraActivity.CUSTOM_ACTION);
				
				Log.e(TAG, "nonNull imageData!");
				try
				{
					int time = (int)(System.currentTimeMillis()/1000L);
					FileOutputStream fos = new FileOutputStream(new File(Constants.RAW_PICS_DIRECTORY + "/" + time + ".jpg"));
					fos.write(imageData);
					fos.close();
					Log.e("SUCCESS","WROTE TO FILE");
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				
				mCamera.startPreview();
				//setResult(FOTO_MODE,mIntent);
				finish();
			}
		}
	};

	protected void onResume()
	{
		Log.e(TAG, "onResume");
		super.onResume();
	}

	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
	}

	protected void onStop()
	{
		Log.e(TAG, "onStop");
		super.onStop();
	}

	public void surfaceCreated(SurfaceHolder holder)
	{
		Log.e(TAG, "surfaceCreated");
		mCamera = Camera.open();
		mCamera.setDisplayOrientation(90);
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h)
	{
		Log.e(TAG, "surfaceChanged");

		// XXX stopPreview() will crash if preview is not running
		if (mPreviewRunning)
		{
			mCamera.stopPreview();
		}

		Camera.Parameters p = mCamera.getParameters();
		p.setColorEffect(Parameters.EFFECT_NONE);
		p.setSceneMode(Parameters.SCENE_MODE_ACTION);
		p.setAntibanding(Parameters.ANTIBANDING_50HZ);
		p.setExposureCompensation(0);
		
		p.setFocusMode(Parameters.FOCUS_MODE_INFINITY);
		mCamera.setParameters(p);
		try
		{
			mCamera.setPreviewDisplay(holder);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		mCamera.startPreview();
		mPreviewRunning = true;
	}

	public void surfaceDestroyed(SurfaceHolder holder)
	{
		Log.e(TAG, "surfaceDestroyed");
		mCamera.stopPreview();
		mPreviewRunning = false;
		mCamera.release();
	}

	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	public void takePicture()
	{
		mCamera.takePicture(null, null, mPictureCallback);
	}
	public void onClick(View arg0)
	{
		mCamera.takePicture(null, null, mPictureCallback);
	}
	
	public class TakePictureTask extends TimerTask
	{
		CameraActivity cv;
		public TakePictureTask(CameraActivity cv2)
		{
			cv = cv2;
		}
		public void run()
		{
			cv.takePicture();
		}	
	}
}