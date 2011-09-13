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
 * The packetizer service accepts data from any other service or device and formats
 * it correctly for safe transmission over the I/O service.
 */

package gov.nasa.arc.axcs;

import gov.nasa.arc.axcs.Packet.packetSize;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Vector;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.util.Log;


public class ImagePacketizer {

	public ImagePacketizer() 
	{
	}
	
	static {
		System.loadLibrary("png");
		System.loadLibrary("webp");
	}

	public native int encodeWrapper(String jpegTileName, String encodeTileName, int quality, int targetSize);
	public native int encoder(int width, int height, int rgb[], int output[]);
	
	private Vector<Packet> packetVector = new Vector<Packet>();
	private int bestVarianceIndex = 0;
	public Vector<Packet> getPacketVector(){
		return packetVector;
	}
	
	public void packetizeBestPhoto(String imageFile) 
	{
		Log.e("ImagePacketizer","packetizeBestPhoto");
		//the intent here is that this method only needs to be called once.
		//It will automatically packetize the data however it wants to, then
		//send it over IOService
		//the exact scheduling (involving sleeping) has not yet been worked out entirely
		ImagePacketThread ipt = new ImagePacketThread(imageFile);
		ipt.start();
	}
	
	public Vector<Packet> resetPacketVector()
	{
		File packet_Images = new File(Constants.PACKET_PICS_DIRECTORY);
		File[] allPacket_Images = packet_Images.listFiles();
		Bitmap imageTile = null;
		for (int x = 0; x < allPacket_Images.length; ++x){
			String currentPacket = allPacket_Images[x].getName();
			if (currentPacket.contains(".png")){
				int index = Integer.parseInt(currentPacket.substring(0,4));
				if (currentPacket.contains("lowRes")){
					packetVector.add(new Packet(currentPacket, packetSize.LOW_RES, 0, index, 0, 0));
				}
				else if (currentPacket.contains("mediumRes")){
					imageTile = BitmapFactory.decodeFile(allPacket_Images[x].getName());
					double packetVariance = runVariance(imageTile);
					if (packetVariance > Constants.mediumThreshold) packetVariance = 1;
					else packetVariance = 0;
					int startx = ((index-1)/Constants.medScale)*(Constants.superWidth/Constants.medScale);
					int starty = ((index-1)%Constants.medScale)*(Constants.superHeight/Constants.medScale);
					packetVector.add(new Packet(currentPacket, packetSize.MEDIUM_RES, packetVariance, index, startx, starty));
				}
				else if (currentPacket.contains("highRes")){
					imageTile = BitmapFactory.decodeFile(allPacket_Images[x].getName());
					double packetVariance = runVariance(imageTile);
					int startx = ((index-1-Constants.startSmall)/Constants.smallScale)*(Constants.superWidth/Constants.smallScale);
					int starty = ((index-1-Constants.startSmall)%Constants.smallScale)*(Constants.superHeight/Constants.smallScale);
					packetVector.add(new Packet(currentPacket, packetSize.HIGH_RES, packetVariance, index, startx, starty));
				}
				else if (currentPacket.contains("superRes")){
					int startx = (bestVarianceIndex/Constants.smallScale)*(Constants.superWidth/Constants.smallScale);
					int starty = (bestVarianceIndex%Constants.smallScale)*(Constants.superHeight/Constants.smallScale);
					packetVector.add(new Packet(currentPacket, packetSize.SUPER_RES, 0, index, startx, starty));
				}
			}
		}
		imageTile.recycle();
		return packetVector;
	}
	
	public static Bitmap scaleBitmap(Bitmap orig, int w, int h)
	{
		
		return Bitmap.createScaledBitmap(orig, w, h, true);
	}
	
	class ImagePacketThread extends Thread
    {
		private String imageFile;
		
		public ImagePacketThread(String imageFile_)
		{
			imageFile = imageFile_;
		}
		
		public Bitmap loadImage()
		{
	    	BitmapFactory.Options options = new BitmapFactory.Options(); 
	    	options.inPreferredConfig = Bitmap.Config.ARGB_8888; 
	    	options.inSampleSize = 1;
	    	return BitmapFactory.decodeFile(imageFile, options); 
	
		}
		
		public int createSuperTile(Bitmap fullImage, int tileStart){
			int x = (bestVarianceIndex/Constants.smallScale)*(Constants.superWidth/Constants.smallScale);
			int y = (bestVarianceIndex%Constants.smallScale)*(Constants.superHeight/Constants.smallScale);
			Bitmap imageTile = Bitmap.createBitmap(fullImage, x, y, Constants.tileWidth, Constants.tileHeight);
			String fullImageName= Constants.PACKET_PICS_DIRECTORY + "/" + String.format("%04d", bestVarianceIndex) +
								  "_superRes.png";
			packetVector.add(new Packet(fullImageName, packetSize.SUPER_RES, 0, tileStart, x, y));
			Log.e("createImageTiles", "Saving image: " + fullImageName + x + y);
			saveImage(imageTile, fullImageName);
			String encodedFileName = fullImageName + ".webp";
			encodeWrapper(fullImageName, encodedFileName, Constants.quality, Constants.targetSize);
			packetVector.lastElement().chooseProbability(0);
			return 1;
		}
		
		public int createImageTiles(Bitmap fullImage, String baseName, int tileWidth, int tileHeight, int tileStart)
		{
			int width = fullImage.getWidth();
			int height = fullImage.getHeight();
			
			int numWidth = width/tileWidth;
			int numHeight = height/tileHeight;
			
			int tileNum = tileStart;
			int tileCount = 0;
			
			
			double totalLevelVariance = 0;
			packetSize curPackSize = packetSize.LOW_RES;
			if (baseName.equals("mediumRes")) curPackSize = packetSize.MEDIUM_RES;
			else if (baseName.equals("highRes")) curPackSize = packetSize.HIGH_RES;
			Bitmap imageTile;
			double maxVariance = 0;
			for (int i = 0; i < numWidth; ++i)
			{
				for (int j = 0; j < numHeight; ++j)
				{
					imageTile = Bitmap.createBitmap(fullImage, i*tileWidth, j*tileHeight, tileWidth, tileHeight);
					String fullImageName= Constants.PACKET_PICS_DIRECTORY + "/" + String.format("%04d", tileNum++) +
										  "_" + baseName + ".png";
					double packetVariance = runVariance(imageTile);
					int startx = 0;
					int starty = 0;
					if (curPackSize == packetSize.MEDIUM_RES) {
						Log.e("MediumVariance", "" + packetVariance);
						if (packetVariance > Constants.mediumThreshold) packetVariance = 1;
						else packetVariance = 0;
						startx = (tileCount/Constants.medScale)*(Constants.superWidth/Constants.medScale);
						starty = (tileCount%Constants.medScale)*(Constants.superHeight/Constants.medScale);
					} else if (curPackSize == packetSize.HIGH_RES){
						if (packetVariance > maxVariance) {
							maxVariance = packetVariance;
							bestVarianceIndex = tileCount;
						}
						startx = (tileCount/Constants.smallScale)*(Constants.superWidth/Constants.smallScale);
						starty = (tileCount%Constants.smallScale)*(Constants.superHeight/Constants.smallScale);
					}
					packetVector.add(new Packet(fullImageName, curPackSize, packetVariance, tileNum, startx, starty));
					totalLevelVariance += packetVariance;
					Log.e("createImageTiles", "Saving image: " + fullImageName);
					saveImage(imageTile, fullImageName);
					String encodedFileName = fullImageName + ".webp";
					encodeWrapper(fullImageName, encodedFileName, Constants.quality, Constants.targetSize);
					
					boolean finished = false; 
				 	int targetSize = Constants.targetSize; 
				 	
				 	// Hack to make sure the low res image tile is not too big 
				 	// since the webp target size can sometimes go over. 
				 	do { 
				 		encodeWrapper(fullImageName, encodedFileName, Constants.quality, targetSize); 
				 	
				 		//check the file size 
				 		File file = new File(encodedFileName); 
				 		long numBytes = file.length(); 
				 	
				 		if (numBytes > Constants.maxImageBytes) { 
				 			Log.e("ImagePacketizer","ERROR: Image bytes size : " + numBytes); 
				 			targetSize -= 10;
				 		} 
				 		else { 
				 			finished = true; 
				 			Log.e("ImagePacketizer","Image bytes size : " + numBytes); 
				 		} 
				 		
				 	} while(!finished); 
					
					++tileCount;
				}
			}
			Iterator<Packet> packetIterator = packetVector.iterator();
			while(packetIterator.hasNext()) {
				Packet curPack = (Packet) packetIterator.next();
				if (curPack.pSize == curPackSize) curPack.chooseProbability(totalLevelVariance);
			}
			return tileCount;
		}
				
		public Bitmap scaleImage(Bitmap bitmapOrg, int newWidth, int newHeight)
		{
			int width = bitmapOrg.getWidth();
	        int height = bitmapOrg.getHeight();
	        
	        float scaleWidth = ((float) newWidth) / width;
	        float scaleHeight = ((float) newHeight) / height;
	        
	        Matrix matrix = new Matrix();
	        matrix.postScale(scaleWidth, scaleHeight);
	
	        Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0, width, height, matrix, true);
	        return resizedBitmap;
		}
		
		public void saveImage(Bitmap bitmap, String filename)
		{
			try
	        {
				OutputStream fOut = null;
				
				File file = new File(filename);
				fOut = new FileOutputStream(file);
				bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
				
				fOut.flush();
				fOut.close();
							
			}
	        catch (Exception e)
	        {
				e.printStackTrace();
	        }
		}
		
		
		public void run()
		{
			Bitmap bitmapOrg = loadImage();
	    	System.out.println("finished loading: " + imageFile);
	    		    	
	    	
	    	Bitmap resizedBitmapSmall = ImagePacketizer.scaleBitmap(bitmapOrg, Constants.tileWidth, Constants.tileHeight);//scaleImage(bitmapOrg, 32, 24);
	    	String filename0 = Constants.GOOD_PICS_DIRECTORY+"/"+"imageResizeSmall.png";
	        saveImage(resizedBitmapSmall, filename0);
	    	Bitmap resizedBitmapMed = ImagePacketizer.scaleBitmap(bitmapOrg, Constants.medWidth, Constants.medHeight);//scaleImage(bitmapOrg, 160, 120);
	    	String filename1 = Constants.GOOD_PICS_DIRECTORY+"/"+"imageResizeMed.png";
	        saveImage(resizedBitmapMed, filename1);
	    	Bitmap resizedBitmapLarge = ImagePacketizer.scaleBitmap(bitmapOrg, Constants.fullWidth, Constants.fullHeight);//scaleImage(bitmapOrg, 640, 480);
	    	String filename2 = Constants.GOOD_PICS_DIRECTORY+"/"+"imageResizeLarge.png";
	        saveImage(resizedBitmapLarge, filename2);
	        Bitmap resizedBitmapSuper = ImagePacketizer.scaleBitmap(bitmapOrg, Constants.superWidth, Constants.superHeight);//scaleImage(bitmapOrg, 640, 480);
	    	String filename3 = Constants.GOOD_PICS_DIRECTORY+"/"+"imageResizeSuper.png";
	        saveImage(resizedBitmapSuper, filename3);
	        bitmapOrg.recycle();
	    	System.out.println("finished resizing");
	    	
	    	int numPackets = 0;
	    	// creating image tiles from full image
	    	numPackets =  createImageTiles(resizedBitmapSmall, "lowRes", Constants.tileWidth, Constants.tileHeight, 0);
	    	numPackets += createImageTiles(resizedBitmapMed, "mediumRes", Constants.tileWidth, Constants.tileHeight, 1);
	    	numPackets += createImageTiles(resizedBitmapLarge, "highRes", Constants.tileWidth, Constants.tileHeight, Constants.startSmall+1);
	    	numPackets += createSuperTile(resizedBitmapSuper, Constants.startSuper+1);
	    	
	        File status = null;
	        Log.e("ImagePacketizer", "!!!!!!!!!!!numPackets = " + numPackets);
	        if(Constants.saveProbAggregate) runProbAggregate(resizedBitmapSmall);
	        if(numPackets >= Constants.packetThreshold) 
	        {
	        	status = new File(Constants.GOOD_PICS_DIRECTORY + "/SUCCESS");
	        }
	        else
	        {
	        	status = new File(Constants.GOOD_PICS_DIRECTORY + "/FAILED"); 
	        }
	
	        try
	        {
	        	status.createNewFile();
	        }
	        catch(IOException e)
	        {
	        	Log.e("ImageProcessor","Couldn't create done file");
	        }
	        
	        Log.e("************", "DONE IMAGE PROCESS!!!!!!!!!!!!");
		}
    }
	
	public void runProbAggregate(Bitmap resizedBitmapSmall){
		Bitmap fullLowRes = scaleBitmap(resizedBitmapSmall, Constants.superWidth, Constants.superHeight);
        Bitmap curBitmap = null;
        Canvas c = new Canvas();
        c.setBitmap(fullLowRes);
        try{
        	  // Create file 
        	  FileWriter filestream = new FileWriter(Constants.USED_PICS_DIRECTORY + "/probabilities.csv");
        	  BufferedWriter probWriter = new BufferedWriter(filestream);
        	  Iterator<Packet> packetIterator = packetVector.iterator();
  			  while(packetIterator.hasNext()) {
  					Packet curPack = (Packet) packetIterator.next();
  					int numTransmitted = 0;
  					boolean transmitted = false;
  					switch (curPack.pSize){
  					case LOW_RES: 
  						numTransmitted = (int)(Constants.lengthOfLowResSend*curPack.probabilitySent+(Constants.maxImageTransmit-Constants.lengthOfLowResSend)*curPack.probabilitySent*Constants.lowResWeight2/Constants.lowResWeight1);
	  					transmitted = Math.pow((1-Constants.probabilityReceived), numTransmitted) < .5;
  						break;
  					case MEDIUM_RES:
  						numTransmitted = (int)(Constants.lengthOfLowResSend*curPack.probabilitySent+(Constants.maxImageTransmit-Constants.lengthOfLowResSend)*curPack.probabilitySent*Constants.medResWeight2/Constants.medResWeight1);
  						transmitted = Math.pow((1-Constants.probabilityReceived), numTransmitted) < .5;
  						break;
  					case HIGH_RES:
  						numTransmitted = (int)(Constants.lengthOfLowResSend*curPack.probabilitySent+(Constants.maxImageTransmit-Constants.lengthOfLowResSend)*curPack.probabilitySent*Constants.highResWeight2/Constants.highResWeight1);
  						transmitted = Math.pow((1-Constants.probabilityReceived), numTransmitted) < .5;
  						break;
  					case SUPER_RES:
  						numTransmitted = (int)(Constants.lengthOfLowResSend*curPack.probabilitySent+(Constants.maxImageTransmit-Constants.lengthOfLowResSend)*curPack.probabilitySent*Constants.superResWeight2/Constants.superResWeight1);
  						transmitted = Math.pow((1-Constants.probabilityReceived), numTransmitted) < .5;
  						break;
  					}
  					probWriter.write("\"" + curPack.index + "\", \"" + numTransmitted + "\", \"" + transmitted + "\"");
  					probWriter.newLine();
  					if(transmitted & curPack.pSize != packetSize.LOW_RES) {
  						switch(curPack.pSize){
	  						case MEDIUM_RES: 
	  							curBitmap = scaleBitmap(BitmapFactory.decodeFile(curPack.filename), Constants.superWidth/Constants.medScale, Constants.superHeight/Constants.medScale);
	  							break;
	  						case HIGH_RES:
	  							curBitmap = scaleBitmap(BitmapFactory.decodeFile(curPack.filename), Constants.superWidth/Constants.smallScale, Constants.superHeight/Constants.smallScale);
	  							break;
	  						case SUPER_RES:
	  							curBitmap = BitmapFactory.decodeFile(curPack.filename);
	  							//Paint p = new Paint ();
	  							//c.drawLine(curPack.startx-Constants.tileWidth, curPack.starty-Constants.tileHeight, curPack.startx, curPack.starty, p);
	  							break;
	  						default:
	  							break;
  						}
  						c.drawBitmap(curBitmap, curPack.startx, curPack.starty, null);
  					}
					//Log.e("aggregate", numTransmitted+ " " + curPack.index);
  			  }
  			  c.save();
  			  String aggregate = Constants.USED_PICS_DIRECTORY+"/"+"aggregate.png";
  			  OutputStream fOut = null;
			  File file = new File(aggregate);
			  fOut = new FileOutputStream(file);
			  fullLowRes.compress(Bitmap.CompressFormat.PNG, 100, fOut);
			  fOut.flush();
			  fOut.close();
			  probWriter.close();
        	  Log.e("Status", "Finished saving aggregate");
        }catch (Exception e){//Catch exception if any
        	  System.err.println("Error: " + e.getMessage());
        }
	}

	public double runVariance(Bitmap imageTile){
		int index = 0;
		double redArray[] = new double[Constants.tileWidth*Constants.tileHeight];
		double greenArray[] = new double[Constants.tileWidth*Constants.tileHeight];
		double blueArray[] = new double[Constants.tileWidth*Constants.tileHeight];
		for(int x = 0; x < Constants.tileWidth; ++x) {
			for(int y = 0; y < Constants.tileHeight; ++y) {
				int color = imageTile.getPixel(x, y);
				redArray[index] = Color.red(color);
				greenArray[index] = Color.green(color);
				blueArray[index] = Color.blue(color);
			}
		}
		return variance(redArray)+variance(greenArray)+variance(blueArray);
	}
	    
    //http://warrenseen.com/blog/2006/03/13/how-to-calculate-standard-deviation/   
    /**
    * @param population an array, the population
    * @return the variance
    */ public double variance(double[] population) {
        long n = 0;
        double mean = 0;
        double sum =0;
        
        for (double x : population) {
            n++;
            sum =sum+ x;
        }
        mean=sum/n;
        n=0;
        double s = 0.0;
        double delta=0.0;
        for (double x : population) {
                n++;
                delta = x-mean;
                mean += delta/n;
                s += delta * (x-mean);
        }
        // if you want to calculate std deviation
        // of a sample change this to (s/(n-1))
        return (s/n);
    }
    
    //http://warrenseen.com/blog/2006/03/13/how-to-calculate-standard-deviation/
	/**
	* @param population an array, the population
	* @return the standard deviation
	*/
	public double standard_deviation(double[] population) {
	        return Math.sqrt(variance(population));
	}
}


