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
 * The processor service accepts data from any other service or device and formats
 * it correctly for safe transmission over the I/O service.
 */

package gov.nasa.arc.axcs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.util.Log;

public class ImageProcessor
{
	
	public ImageProcessor()
	{
	
	}
	
	public void packetizeBestPhoto(String imageFile) 
	{
		//the intent here is that this method only needs to be called once.
		//It will automatically packetize the data however it wants to, then
		//send it over IOService
		//the exact scheduling (involving sleeping) has not yet been worked out entirely
		ImageProcessingThread ipt = new ImageProcessingThread(imageFile);
		ipt.start();
	}
	
	class ImageProcessingThread extends Thread
    {
		private String imageFile;
		
		public ImageProcessingThread(String imageFile_)
		{
			imageFile = imageFile_;
		}
		
    	public RGBTriple[] createPalette()
    	{
    		RGBTriple[] palette =
            {		
    			// color.gif color palette
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Space -- allowed
   				new RGBTriple(97, 73, 81),
    			new RGBTriple(85, 74, 116),
    			new RGBTriple(118, 72, 67),
    			new RGBTriple(69, 84, 101),
    			new RGBTriple(118, 74, 83),
  				new RGBTriple(134, 73, 59),
   				new RGBTriple(50, 88, 160),
   				new RGBTriple(69, 87, 119),
    			new RGBTriple(103, 83, 72),
    			new RGBTriple(86, 87, 100),
    			new RGBTriple(68, 89, 136),
    			new RGBTriple(58, 95, 114),
    			new RGBTriple(103, 86, 86),
    			new RGBTriple(55, 94, 149),
    			new RGBTriple(85, 89, 118),
    			new RGBTriple(119, 84, 72),
    			new RGBTriple(68, 91, 162),
    			new RGBTriple(105, 89, 104),
    			new RGBTriple(87, 96, 91),
    			new RGBTriple(119, 87, 86),
    			new RGBTriple(97, 90, 129),
    			new RGBTriple(72, 100, 133),
    			new RGBTriple(135, 87, 77),
    			new RGBTriple(61, 101, 170),
    			new RGBTriple(71, 101, 153),
    			new RGBTriple(86, 101, 117),
    			new RGBTriple(131, 90, 96),
    			new RGBTriple(103, 101, 104),
    			new RGBTriple(86, 103, 135),
    			new RGBTriple(118, 100, 88),
    			new RGBTriple(103, 103, 119),
    			new RGBTriple(86, 105, 153),
    			new RGBTriple(119, 102, 103),
    			new RGBTriple(102, 105, 135),
    			new RGBTriple(61, 112, 193),
    			new RGBTriple(73, 115, 134),
    			new RGBTriple(161, 95, 78),
    			new RGBTriple(118, 104, 118),
    			new RGBTriple(72, 115, 154),
    			new RGBTriple(79, 111, 175),
    			new RGBTriple(144, 101, 86),
    			new RGBTriple(102, 107, 153),
    			new RGBTriple(135, 103, 100),
    			new RGBTriple(118, 106, 134),
    			new RGBTriple(88, 116, 132),
    			new RGBTriple(104, 115, 106),
    			new RGBTriple(117, 107, 150),
    			new RGBTriple(135, 106, 118),
    			new RGBTriple(89, 116, 158),
    			new RGBTriple(104, 116, 121),
    			new RGBTriple(104, 117, 136),
    			new RGBTriple(153, 106, 101),
    			new RGBTriple(120, 116, 105),
    			new RGBTriple(152, 107, 117),
    			new RGBTriple(104, 118, 152),
    			new RGBTriple(119, 118, 121),
    			new RGBTriple(86, 121, 193),
    			new RGBTriple(137, 116, 104),
    			new RGBTriple(167, 109, 90),
    			new RGBTriple(119, 119, 136),
    			new RGBTriple(106, 120, 170),
    			new RGBTriple(135, 118, 121),
    			new RGBTriple(89, 126, 181),
    			new RGBTriple(119, 121, 153),
    			new RGBTriple(153, 117, 103),
    			new RGBTriple(97, 132, 149),
    			new RGBTriple(152, 119, 120),
    			new RGBTriple(138, 122, 140),
    			new RGBTriple(89, 133, 202),
    			new RGBTriple(106, 132, 167),
    			new RGBTriple(103, 132, 187),
    			new RGBTriple(170, 120, 110),
    			new RGBTriple(121, 133, 128),
    			new RGBTriple(150, 129, 95),
    			new RGBTriple(120, 133, 148),
    			new RGBTriple(121, 134, 168),
    			new RGBTriple(136, 134, 135),
    			new RGBTriple(104, 137, 201),
    			new RGBTriple(79, 141, 236),
    			new RGBTriple(100, 142, 166),
    			new RGBTriple(148, 133, 120),
    			new RGBTriple(120, 136, 185),
    			new RGBTriple(136, 135, 152),
    			new RGBTriple(102, 139, 215),
    			new RGBTriple(179, 130, 95),
    			new RGBTriple(136, 136, 168),
    			new RGBTriple(118, 139, 201),
    			new RGBTriple(152, 136, 136),
    			new RGBTriple(194, 126, 112),
    			new RGBTriple(116, 140, 215),
    			new RGBTriple(135, 139, 185),
    			new RGBTriple(167, 135, 133),
    			new RGBTriple(151, 138, 152),
    			new RGBTriple(106, 148, 188),
    			new RGBTriple(0, 0, 0),	// Disallowed (delete char)
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
    			new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(0, 0, 0),	// Disallowed
   				new RGBTriple(138, 166, 202),
   				new RGBTriple(203, 154, 138),
   				new RGBTriple(162, 165, 154),
   				new RGBTriple(137, 167, 218),
   				new RGBTriple(122, 170, 233),
   				new RGBTriple(200, 155, 162),
   				new RGBTriple(153, 167, 186),
   				new RGBTriple(190, 164, 127),
   				new RGBTriple(170, 166, 170),
   				new RGBTriple(152, 169, 203),
   				new RGBTriple(194, 168, 94),
   				new RGBTriple(183, 166, 152),
   				new RGBTriple(151, 170, 217),
   				new RGBTriple(169, 168, 185),
   				new RGBTriple(185, 168, 169),
   				new RGBTriple(168, 170, 201),
   				new RGBTriple(227, 162, 131),
   				new RGBTriple(139, 181, 204),
   				new RGBTriple(184, 170, 184),
   				new RGBTriple(167, 172, 217),
   				new RGBTriple(136, 182, 223),
   				new RGBTriple(200, 170, 165),
   				new RGBTriple(150, 178, 233),
   				new RGBTriple(207, 170, 147),
   				new RGBTriple(184, 172, 200),
   				new RGBTriple(155, 181, 203),
   				new RGBTriple(230, 163, 161),
   				new RGBTriple(171, 181, 171),
   				new RGBTriple(183, 173, 216),
   				new RGBTriple(243, 170, 73),
   				new RGBTriple(154, 182, 218),
   				new RGBTriple(200, 172, 185),
   				new RGBTriple(217, 170, 163),
   				new RGBTriple(170, 182, 188),
   				new RGBTriple(187, 181, 169),
   				new RGBTriple(170, 183, 203),
   				new RGBTriple(216, 173, 184),
   				new RGBTriple(186, 183, 186),
   				new RGBTriple(169, 185, 218),
   				new RGBTriple(202, 183, 168),
   				new RGBTriple(233, 177, 147),
   				new RGBTriple(184, 186, 203),
   				new RGBTriple(172, 187, 234),
   				new RGBTriple(155, 192, 237),
   				new RGBTriple(184, 187, 218),
   				new RGBTriple(202, 185, 185),
   				new RGBTriple(217, 185, 165),
   				new RGBTriple(202, 187, 204),
   				new RGBTriple(165, 198, 224),
   				new RGBTriple(216, 187, 188),
   				new RGBTriple(236, 183, 169),
   				new RGBTriple(188, 198, 194),
   				new RGBTriple(184, 199, 217),
   				new RGBTriple(218, 196, 159),
   				new RGBTriple(183, 201, 236),
   				new RGBTriple(203, 200, 202),
   				new RGBTriple(201, 201, 219),
   				new RGBTriple(217, 200, 184),
   				new RGBTriple(235, 198, 163),
   				new RGBTriple(218, 202, 202),
   				new RGBTriple(217, 203, 218),
   				new RGBTriple(232, 204, 201),
   				new RGBTriple(203, 209, 237),
   				new RGBTriple(204, 214, 204),
   				new RGBTriple(232, 205, 219),
   				new RGBTriple(248, 203, 194),
   				new RGBTriple(200, 215, 224),
   				new RGBTriple(235, 211, 183),
   				new RGBTriple(220, 215, 202),
   				new RGBTriple(247, 206, 220),
   				new RGBTriple(219, 217, 220),
   				new RGBTriple(193, 223, 247),
   				new RGBTriple(235, 218, 202),
   				new RGBTriple(235, 219, 217),
   				new RGBTriple(225, 226, 188),
   				new RGBTriple(235, 219, 236),
   				new RGBTriple(245, 221, 195),
   				new RGBTriple(217, 225, 243),
   				new RGBTriple(247, 221, 219),
   				new RGBTriple(220, 231, 217),
   				new RGBTriple(238, 232, 204),
   				new RGBTriple(238, 232, 219),
   				new RGBTriple(249, 233, 186),
   				new RGBTriple(237, 233, 234),
   				new RGBTriple(251, 234, 203),
   				new RGBTriple(236, 235, 250),
   				new RGBTriple(250, 236, 218),
   				new RGBTriple(250, 237, 234),
   				new RGBTriple(250, 236, 251),
   				new RGBTriple(238, 247, 219),
   				new RGBTriple(246, 247, 235),
   				new RGBTriple(246, 247, 251),
   				new RGBTriple(255, 250, 203),
   				new RGBTriple(255, 251, 219),
   				new RGBTriple(255, 253, 235),
   				new RGBTriple(0, 0, 0),	// Disallowed
    				
            };
    		return palette;
    	}
    	
    	//http://stackoverflow.com/questions/477572/android-strange-out-of-memory-issue
    	//decodes image and scales it to reduce memory consumption
    	private Bitmap decodeFile(String fileName){
    	    try {
    	        //Decode image size
    	        BitmapFactory.Options o = new BitmapFactory.Options();
    	        o.inJustDecodeBounds = true;
    	        BitmapFactory.decodeStream(new FileInputStream(fileName),null,o);

    	        //The new size we want to scale to
    	        final int REQUIRED_SIZE=70;

    	        //Find the correct scale value. It should be the power of 2.
    	        int width_tmp=o.outWidth, height_tmp=o.outHeight;
    	        int scale=1;
    	        while(true){
    	            if(width_tmp/2<REQUIRED_SIZE || height_tmp/2<REQUIRED_SIZE)
    	                break;
    	            width_tmp/=2;
    	            height_tmp/=2;
    	            scale*=2;
    	        }

    	        //Decode with inSampleSize
    	        BitmapFactory.Options o2 = new BitmapFactory.Options();
    	        o2.inSampleSize=scale;
    	        return BitmapFactory.decodeStream(new FileInputStream(fileName), null, o2);
    	    } catch (FileNotFoundException e) {}
    	    return null;
    	}

    	
    	public Bitmap loadImage()
    	{
    	   	//BitmapFactory.Options options = new BitmapFactory.Options();
        	//options.inPreferredConfig = Bitmap.Config.RGB_565;
        	//options.inSampleSize = 8;
        	
        	BitmapFactory.Options options = new BitmapFactory.Options(); 
        	options.inPreferredConfig = Bitmap.Config.ARGB_8888; 
        	options.inSampleSize = 1;
        	Bitmap bitmapOrg = BitmapFactory.decodeFile(imageFile, options); 
        	//Bitmap bitmapOrg = BitmapFactory.decodeResource(getResources(), R.drawable.test, options); 
        	return bitmapOrg;
        	//return decodeFile(imageFile);
    	}
    	
    	public Bitmap scaleImage(Bitmap bitmapOrg, int nuWidth, int nuHeight)
    	{
    		int width = bitmapOrg.getWidth();
            int height = bitmapOrg.getHeight();
            int newWidth = nuWidth;
            int newHeight = nuHeight; 
            
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
    			//String outputName = Constants.GOOD_PICS_DIRECTORY, "/"+filename;
    			
    			
    			File file = new File(Constants.GOOD_PICS_DIRECTORY, "/"+filename+".png");
    			fOut = new FileOutputStream(file);
    			bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
    			
    			fOut.flush();
    			fOut.close();
    			
    			//MediaStore.Images.Media.insertImage(getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());
    		}
            catch (Exception e)
            {
    			e.printStackTrace();
            }
		}
		
		public RGBTriple[][] buildImage(Bitmap bitmap, int width, int height)
		{
			RGBTriple[][] image = new RGBTriple[width][height];

            for(int a = 0;a < bitmap.getWidth(); a++)
            {  	
            	for (int b = 0;b < bitmap.getHeight(); b++)
            	{
            		int pixel = bitmap.getPixel(a, b);
                    int redish = Color.red(pixel);
                    int greenish = Color.green(pixel);
                    int blueish = Color.blue(pixel);
                    image[a][b] = new RGBTriple(redish, greenish, blueish);
            	}
            }
            return image;
		}
		
		public ArrayList<byte[]> buildPacketArrayChris(byte[][] eightBitImage, int width, int height, int tileNumber)
		{
            int widthIterations = width/16;
            int heightIterations = height/12;
            int allPacketsSize = (width * height) / (16 * 12);
            if (Constants.EPIC_LOGCATS) Log.e("ImageProcessor","all packets pixel size:"  +allPacketsSize);
			ArrayList<byte[]> allPackets = new ArrayList<byte[]>(allPacketsSize);
            byte[] ImagePacket;

            for(int a=0;a<widthIterations;a++)
            {
            	for (int b = 0;b<heightIterations;b++)
            	{
            		//this is for each 16*12 tile
            		//scans from upper left to lower right
            		int startX = a*16;
            		int startY = b*12;
            		ImagePacket = new byte[196]; //192 for pixels, 4 bytes for metadata
            		
            		//insert a padded 4-digit number as human readable data in the start of the message. Wastes two bytes, but good enough for now. Can handle up to 9999 packets.
            		String tileNumberAsText=String.format("%04d", tileNumber);
            		//if (Constants.EPIC_LOGCATS) Log.e("ImageProcessor",tileNumberAsText);
               		ImagePacket[0]=(byte) (tileNumberAsText.charAt(0));             	            		
             		ImagePacket[1]=(byte) (tileNumberAsText.charAt(1));
            		ImagePacket[2]=(byte) (tileNumberAsText.charAt(2));
            		ImagePacket[3]=(byte) (tileNumberAsText.charAt(3));
               	            		
            		int index = 4; // start at 4 since we just used 4 bytes for the index
            		for(int x = 0;x<16;x++)
            		{
            			for(int y = 0;y<12;y++)
            			{
            				ImagePacket[index] = eightBitImage[startX+x][startY+y];
            				index++;
            				//this will populate every byte in the array from 0-192, leaving us 6 bytes left
            				//198 for message b/c opening (S) and closing (\r) bytes can't be used (needs verifying...)
            			}
            		}
            		if (highPassCheck(ImagePacket)) // check that the image packet actually contains anything useful. 
            			allPackets.add(ImagePacket);
            		tileNumber++;
            	}
            }
            
            return allPackets;
		}
		
		  
	    public boolean highPassCheck(byte[] packet) {
	    	double threshold=0.5;
	    	double[] sample=new double[192];
	    	int itemCnt=0;
	    	for (int i = 4; i < packet.length; i++) { //skip the first 4 characters since already we put the packet index in there. Really this should have been called before the packet was assembled.
	   	    	sample[itemCnt]=packet[i];
	    		itemCnt++;
	    	}
	    	double sampleVariance=variance(sample);
	    	//Log.e("ImageProcessor", "### Variance: "+ sampleVariance);
	    	if (sampleVariance > threshold)
	    		return true;
	    	else
	    		return false;
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
		
		public void savePacketsToFile(ArrayList<byte[]> allPackets, String filename)
		{
            File file = new File(Constants.PACKET_PICS_DIRECTORY, "/" + filename + ".ArrayList_data");
            try
            {
            	FileOutputStream fOut = new FileOutputStream(file);
            	
            	
            	//New  -- Ballmer Peak!
            	ObjectOutputStream obj_out = new ObjectOutputStream (fOut);
            	obj_out.writeObject(allPackets); //ArrayList is already serializable. Just push the actual object to disk. Woo!
            	
            	
            	
//            	for(byte[] packet:allPackets) 
//				{
//            		fOut.write(packet); //loop through packet.length and create a string with ','. Also, change the fileoutputstream
//				}
            	fOut.close();
            	obj_out.close();
            	
			}catch (FileNotFoundException e){
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public int savePacketsToTextFile(ArrayList<byte[]> allPackets, String filename, int pckcntr)
		{
			int numPackets = 0;
         	for(byte[] packet:allPackets) 
			{
	            try
	            {
	    			//File file = new File(Constants.GOOD_PICS_DIRECTORY, "/" + filename + "." +pckcntr + ".txt");
	    			File file = new File(Constants.PACKET_PICS_DIRECTORY, "/" + pckcntr + "_" + filename + ".txt");
	            	FileOutputStream fOut = new FileOutputStream(file);
	            	ObjectOutputStream obj_out = new ObjectOutputStream (fOut);	            	
	           		fOut.write(packet); 					
		          	fOut.close();
		          	obj_out.close();
		        	pckcntr++;
		        	numPackets++;

				}catch (FileNotFoundException e){
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
         	return numPackets;
 		}
		
		public void testRead(String filename){
			
			FileInputStream f_in;
			try {
				f_in = new FileInputStream(Constants.GOOD_PICS_DIRECTORY + "/" + filename);
				ObjectInputStream obj_in = new ObjectInputStream (f_in);

				ArrayList<byte[]>arrayList_fetched = 
					new ArrayList<byte[]>((ArrayList<byte[]>)obj_in.readObject());
					
					for(int i=0;i<arrayList_fetched.size();i++){
						System.out.println(new String(arrayList_fetched.get(i)));
					}
				
				
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
		
		
		public Bitmap builtBitmapFromBytes(byte[][] result, RGBTriple[] palette, int bitmapWidth, int bitmapHeight)
		{
			Bitmap finalImage = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
            for(int a = 0;a < finalImage.getWidth();a++)
            {
            	for (int b = 0;b < finalImage.getHeight();b++)
            	{
            		int index = result[a][b];
            		if (index < 0)
            		{
            			index += 256;
            		}
            		RGBTriple color = palette[index]; 
            		int redish = color.channels[0]; 
            		if (redish<0)
            			redish+=256;
            		int greenish = color.channels[1];
            		if (greenish<0)
            			greenish+=256;
            		int bluish = color.channels[2];
            		if (bluish<0)
            			bluish+=256;
            		finalImage.setPixel(a, b, Color.rgb(redish, greenish, bluish));
            		//System.out.println(a + ", " + b + ", " + redish + ", " + greenish + ", " + bluish);
            	}
            }
            return finalImage;
		}
		
    	public void run()
    	{
    		Bitmap bitmapOrg = loadImage();
        	System.out.println("finished loading: " + imageFile);
        	
        	
        	
        	Bitmap resizedBitmap16x12 = scaleImage(bitmapOrg, 16, 12);
        	String filename0 = "imageResize16x12";
            saveImage(resizedBitmap16x12, filename0);
        	Bitmap resizedBitmap160x120 = scaleImage(bitmapOrg, 160, 120);
        	String filename1 = "imageResize160x120";
            saveImage(resizedBitmap160x120, filename1);
        	Bitmap resizedBitmap640x480 = scaleImage(bitmapOrg, 640, 480);
        	String filename2 = "imageResize640x480";
            saveImage(resizedBitmap640x480, filename2);
        	System.out.println("finished resizing");
        	
            bitmapOrg.recycle();
        	
            RGBTriple[][] image0 = buildImage(resizedBitmap16x12, 16, 12);
            resizedBitmap16x12.recycle();
            RGBTriple[][] image1 = buildImage(resizedBitmap160x120, 160, 120);
            resizedBitmap160x120.recycle();
            RGBTriple[][] image2 = buildImage(resizedBitmap640x480, 640, 480);
            resizedBitmap640x480.recycle();
        	System.out.println("finished building images");
        	
            //We don't need to do this each time we run the image compression algorithm,
            //it can be constant defined elsewhere.
            RGBTriple[] palette = createPalette(); 
            
            //Takes the longest
            byte[][] result0 = FloydSteinbergDither.floydSteinbergDither(image0, palette);
            byte[][] result1 = FloydSteinbergDither.floydSteinbergDither(image1, palette);
            byte[][] result2 = FloydSteinbergDither.floydSteinbergDither(image2, palette);
            System.out.println("finished dithering");
            
            //int tileNumber=0;
            ArrayList<byte[]> allPackets16x12 = buildPacketArrayChris(result0, 16, 12, 0);
            if (Constants.EPIC_LOGCATS) Log.e("ImageProcessor","Done packet array 16x12");
            ArrayList<byte[]> allPackets160x120 = buildPacketArrayChris(result1, 160, 120, 1);
            if (Constants.EPIC_LOGCATS) Log.e("ImageProcessor","Done packet array 160x120");
            ArrayList<byte[]> allPackets640x480 = buildPacketArrayChris(result2, 640, 480, 101);
            if (Constants.EPIC_LOGCATS) Log.e("ImageProcessor","Done packet array 640x480");
            System.out.println("finished packetizing");

            /*
            savePacketsToFile(allPackets16x12, "packets16x12");
            if (Constants.EPIC_LOGCATS) Log.e("ImageProcessor","Done save packet array 16x12");
            savePacketsToFile(allPackets160x120, "packets160x120");
            if (Constants.EPIC_LOGCATS) Log.e("ImageProcessor","Done save packet array 160x120");
            savePacketsToFile(allPackets640x480, "packets640x480");
            if (Constants.EPIC_LOGCATS) Log.e("ImageProcessor","Done save packet array 640x480");
            */
            
            //useful for inspecting packet contents...
            int numPackets = 0;
            numPackets = savePacketsToTextFile(allPackets16x12, "packets16x12", 0);
            if (Constants.EPIC_LOGCATS) Log.e("ImageProcessor","Done save packet array text 16x12");
            numPackets += savePacketsToTextFile(allPackets160x120, "packets160x120", 1);
            if (Constants.EPIC_LOGCATS) Log.e("ImageProcessor","Done save packet array text 160x120");
            numPackets += savePacketsToTextFile (allPackets640x480, "packets640x480", 101);
            if (Constants.EPIC_LOGCATS) Log.e("ImageProcessor","Done save packet array text 640x480");

            System.out.println("finished writing file");
            
            File status = null;
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
}


// http://en.literateprograms.org/Floyd-Steinberg_dithering_(Java)?oldid=12476
class RGBTriple
{
    public final byte[] channels;
    												
    public RGBTriple()
    {
    	channels = new byte[3];
    }
    
    public RGBTriple(int R, int G, int B)
    {
    	channels = new byte[]
    	{(byte)R, (byte)G, (byte)B};
    }
}

class FloydSteinbergDither
{
	private static byte plus_truncate_uchar(byte a, int b)
    {
        if ((a & 0xff) + b < 0)
            return 0;
        else if ((a & 0xff) + b > 255)
            return (byte)255;
        else
            return (byte)(a + b);
    }

    private static byte findNearestColor(RGBTriple color, RGBTriple[] palette)
    {
        int minDistanceSquared = 255*255 + 255*255 + 255*255 + 1;
        byte bestIndex = 0;
        for (int i = 32; i < palette.length; i++) //start at 32 instead of zero to avoid control characters in pallete indices.
        {
            int Rdiff = (color.channels[0] & 0xff) - (palette[i].channels[0] & 0xff);
            //System.out.println(palette[i]);
            int Gdiff = (color.channels[1] & 0xff) - (palette[i].channels[1] & 0xff);
            int Bdiff = (color.channels[2] & 0xff) - (palette[i].channels[2] & 0xff);
            int distanceSquared = Rdiff*Rdiff + Gdiff*Gdiff + Bdiff*Bdiff;
            if (distanceSquared < minDistanceSquared)
            {
                minDistanceSquared = distanceSquared;
                bestIndex = (byte)i;
            }
        }
        
        return bestIndex;
    }
    
    public static byte[][] scrubImage(byte[][] image)
    {
    	byte[][] scrub = new byte[image.length][image[0].length];
    	int scrubCount = 0;
    	
    	for(int y = 0; y < image.length; y++) {
    		for(int x = 0; x < image[0].length; x++) {
    			byte index = image[y][x];
    			
    			int indexi = index;
				if (indexi < 0)
				{
					indexi += 256;
				}
    			
    			if(indexi < 32) {
    				indexi = 32;
    				++scrubCount;
    			}
    			else if(indexi >= 128 && indexi <= 160) {
    				indexi = 32;
    				++scrubCount;
    			}
    			else if(indexi == 255) {
    				indexi = 32;
    				++scrubCount;
    			}
    			
    			scrub[y][x] = (byte)indexi;
    			
    		}
    	}
    	Log.e("************", "scrubCount = " + scrubCount);
    	return scrub;
    }


    public static byte[][] floydSteinbergDither(RGBTriple[][] image, RGBTriple[] palette)
    {
        byte[][] result = new byte[image.length][image[0].length];

		for (int y = 0; y < image.length; y++)
		{
			for (int x = 0; x < image[y].length; x++)
			{
				RGBTriple currentPixel = image[y][x];
				byte index = findNearestColor(currentPixel, palette);
				int indexi = index;
				if (indexi < 0)
				{
					indexi += 256;
				}
				result[y][x] = index;
				
				for (int i = 0; i < 3; i++)
				{
					int error = (currentPixel.channels[i] & 0xff) - (palette[indexi].channels[i] & 0xff);
					if (x + 1 < image[0].length)
					{
						image[y+0][x+1].channels[i] = plus_truncate_uchar(image[y+0][x+1].channels[i], (error*7) >> 4);
					}
					if (y + 1 < image.length)
					{
						if (x - 1 > 0)
						{
							image[y+1][x-1].channels[i] = plus_truncate_uchar(image[y+1][x-1].channels[i], (error*3) >> 4);
						}
						image[y+1][x+0].channels[i] = plus_truncate_uchar(image[y+1][x+0].channels[i], (error*5) >> 4);
						if (x + 1 < image[0].length)
						{
							image[y+1][x+1].channels[i] = plus_truncate_uchar(image[y+1][x+1].channels[i], (error*1) >> 4);
						}
					}
				}
			
			}
		}

		return scrubImage(result);
		
    }
}
