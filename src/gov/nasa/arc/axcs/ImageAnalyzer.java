package gov.nasa.arc.axcs;

import java.util.HashMap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

public class ImageAnalyzer
{
	public enum usefulColor {BLACK, BLUE, WHITE, NONE};
	public enum optimizationScheme {MOST_EDGES, MOST_BLUE, MOST_WHITE, LEAST_BLACK, MOST_BALANCED, LEAST_NONE, BLACK_30};
	public HashMap	<String, Float> allScores = new HashMap<String, Float>();
	
	public ImageAnalyzer()
	{		
	}
	
	public boolean scoreAllImages(String[] fileNames)
	{
		for(int a = 0;a<fileNames.length;a++)
		{
			//Log.e("Analyzer", "Here is a file name: "+fileNames[a]);
			if (allScores.containsKey(fileNames[a]))
			{
				//do nothing, the image has already been scored.
				Log.e("Scoring", "This image: "+fileNames[a]+ " has a score of: " + allScores.get(fileNames[a]));
			}
			else
			{
				//we have to score the image and add it to the hashmap of stored images
				String name = Constants.RAW_PICS_DIRECTORY+"/"+fileNames[a];
				//Log.e("File", "File name: "+name);
				Bitmap tempImage = BitmapFactory.decodeFile(name);
				Log.e("File", "File: "+tempImage);
				float tempScore = totalScore(tempImage);
				allScores.put(fileNames[a], tempScore);
				Log.e("Scoring", "This image: "+fileNames[a]+ " has a score of: " + allScores.get(fileNames[a]));
			}
		}
		
		return true;
	}
	
	public String getBestImage()
	{
		Log.e("Starting", "Starting with "+allScores.size()+" keys");
		String returnKey = null;
		float minScore = 0;
		
		for(String tempKey:allScores.keySet())
		{
			float tempScore = allScores.get(tempKey);
			if (tempScore > minScore)
			{
				minScore = tempScore;
				returnKey = tempKey;
			}
		}
		
		return Constants.RAW_PICS_DIRECTORY+"/"+returnKey; 
	}
	
	public float totalScore(Bitmap tempImage_big){
		//boolean printed = true;
		ImageScore pictureScore = score(tempImage_big);
		switch (Constants.scheme){
			case MOST_EDGES:
				//if (!printed) Log.e("stats","Selected most edges.");
				return pictureScore.edginess;
			case MOST_BLUE:
				//if (!printed) Log.e("stats","Selected most blue.");
				return pictureScore.blueness;
			case MOST_WHITE:
				//if (!printed) Log.e("stats","Selected most white.");
				return pictureScore.whiteness;
			case LEAST_BLACK:
				//if (!printed) Log.e("stats","Selected least black.");
				return 1 - pictureScore.blackness;
			case MOST_BALANCED:
				//if (!printed) Log.e("stats","Selected most balanced.");
				return (1-((1-pictureScore.blackness)*(1-pictureScore.blackness)+(1-pictureScore.whiteness)*(1-pictureScore.whiteness)+(1-pictureScore.blueness)*(1-pictureScore.blueness)));
			case LEAST_NONE:
				//if (!printed) Log.e("stats","Selected least none.");
				return 1 - pictureScore.noneness;
			case BLACK_30:
				//if (!printed) Log.e("stats","Selected black 30.");
				return 1-(pictureScore.blackness-(float).3)*(pictureScore.blackness-(float).3);
			default:
				//if (!printed) Log.e("stats","Invalid optimization scheme.");
				return 0;
		}
	}
	
	public ImageScore score(Bitmap tempImage_big)
	{
		//Log.e("Attempting to score an image!", "Attempting to score an image!: "+tempImage_big);
		
		//first, make the image small.
		Bitmap tempImage = Bitmap.createScaledBitmap(tempImage_big, 128, 96, true);
		
		//then, count how many black and blue pixels there are
		int num_black = 0;
		int num_blue = 0;
		int num_white = 0;
		int num_none = 0;
		int num_edge = 0;
		float num_pixels = tempImage.getHeight() * tempImage.getWidth();
		
		//boolean printed = true;
		
		float[][] hue = new float[tempImage.getWidth()][tempImage.getHeight()];
		float[][] sat = new float[tempImage.getWidth()][tempImage.getHeight()];
		float[][] val = new float[tempImage.getWidth()][tempImage.getHeight()];
		usefulColor[][] colorType = new usefulColor[tempImage.getWidth()][tempImage.getHeight()];
		boolean[][] isEdge = new boolean[tempImage.getWidth()][tempImage.getHeight()];
		
		for(int y = 0;y<tempImage.getHeight();y++)
		{
			for(int x = 0;x<tempImage.getWidth();x++)
			{
				int currentPixel = tempImage.getPixel(x, y);
				float[] hsv = new float[3];
				Color.colorToHSV(currentPixel, hsv);
				hue[x][y] = hsv[0]; //by default, hsv[0] is the hue parameter, measured from 0-360
				sat[x][y] = hsv[1] * 100;//this shifts the saturation parameter from 0-1 up to 0-100
				val[x][y] = hsv[2] * 100;//this shifts the value parameter from 0-1 up to 0-100
				if (val[x][y] < 25)
				{
					//if (!printed) Log.e("Type", "Black, " + x + ", " + y);
					num_black++;
					colorType[x][y] = usefulColor.BLACK;
				}
				else if (sat[x][y] < 10 && val[x][y] > 80)
				{
					//if (!printed) Log.e("Type", "White, " + x + ", " + y);
					num_white++;
					colorType[x][y] = usefulColor.WHITE;
				}
				else if (sat[x][y] > 50 && val[x][y] > 40 && hue[x][y] > 200 && hue[x][y] < 260)
				{
					//if (!printed) Log.e("Type", "Blue, " + x + ", " + y);
					num_blue++;
					colorType[x][y] = usefulColor.BLUE;
				}
				else {
					//if (!printed) Log.e("Type", "None, " + x + ", " + y);
					num_none++;
					colorType[x][y] = usefulColor.NONE;
				}
				if (x > 0 && y > 0){
					if (colorType[x][y] != colorType[x-1][y] || colorType[x][y] != colorType[x][y-1]){
						//if (!printed) Log.e("Type", "Edge, " + x + ", " + y);
						isEdge[x][y] = true;
						num_edge++;
					}
				}
			}
		}
		
		float blackness = num_black/num_pixels;
		float whiteness = num_white/num_pixels;
		float blueness = num_blue/num_pixels;
		float noneness = num_none/num_pixels;
		float edginess = num_edge/num_pixels;
		
		//Log.e("Image Scored", "pblack: "+percent_black + " pblue: "+percent_blue + " pwhite: "+percent_white);
		
		return new ImageScore(blackness, whiteness, blueness, noneness, edginess);
	}
	
	
	public class ImageScore
	{
		float blackness;
		float whiteness;
		float blueness;
		float noneness;
		float edginess;
		
		public ImageScore(float bk, float wh, float bu, float na, float ed)
		{
			blackness = bk;
			whiteness = wh;
			blueness = bu;
			noneness = na;
			edginess = ed;
		}
	}
	
	
}


