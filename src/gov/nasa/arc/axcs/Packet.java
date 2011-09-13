package gov.nasa.arc.axcs;

public class Packet {
	public enum packetSize{SUPER_RES, HIGH_RES, MEDIUM_RES, LOW_RES};
	public String filename;
	public packetSize pSize;
	public double variance;
	public double probabilitySent;
	public int index, startx, starty;
	public Packet(String f, packetSize p, double v, int i, int sx, int sy){
		filename = f;
		pSize = p;
		variance = v;
		index = i;
		probabilitySent = -1;
		startx = sx;
		starty = sy;
	}
	
	public void chooseProbability(double totalLevelVariance){
		switch (this.pSize){
			case LOW_RES: 
				this.probabilitySent = Constants.lowResWeight1;
				break;
			case MEDIUM_RES:
				this.probabilitySent = Constants.medResWeight1*this.variance/totalLevelVariance;
				break;
			case HIGH_RES:
				this.probabilitySent = Constants.highResWeight1*this.variance/totalLevelVariance;
				break;
			case SUPER_RES: 
				this.probabilitySent = Constants.superResWeight1;
				break;
		}
	}
	
	public void updateProbability(){
		switch (this.pSize){
			case LOW_RES: 
				this.probabilitySent *= Constants.lowResWeight2/Constants.lowResWeight1;
				break;
			case MEDIUM_RES:
				this.probabilitySent *= Constants.medResWeight2/Constants.medResWeight1;
				break;
			case HIGH_RES:
				this.probabilitySent *= Constants.highResWeight2/Constants.highResWeight1;
				break;
			case SUPER_RES: 
				this.probabilitySent = Constants.superResWeight2;
				break;
		}
	}
}
