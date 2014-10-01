package emblcmci.foci3Dtracker;

import java.util.ArrayList;

/**
 * A class to hold two Object4Ds, to store linking results. 
 * These guys would then be stored as an ArrayList, to replace the role of Object4D[t][]
 * 20141001
 * @author miura
 *
 */
public class FociPair {
	public Object4D ch0dot;
	public Object4D ch1dot;
	public double distance;
	public int timepoint;
	public ArrayList<Double> distanceToOtherPairsch0 = new ArrayList<Double>();
	public ArrayList<Double> distanceToOtherPairsch1 = new ArrayList<Double>();
	
	public FociPair(Object4D ch0dot, Object4D ch1dot, int timepoint){
		this.ch0dot = ch0dot;
		this.ch1dot = ch1dot;
		this.timepoint = timepoint;
	}
	
	public void addDistanceToOtherPair(double distancech0, double distancech1){
		distanceToOtherPairsch0.add(distancech0);
		distanceToOtherPairsch1.add(distancech1);
	}

}
