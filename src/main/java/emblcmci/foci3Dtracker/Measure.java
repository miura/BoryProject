package emblcmci.foci3Dtracker;

import ij.measure.Calibration;

// For caluclating distances from Object4D
// moved from main classes on 20140930
// Christoph & Kota
public class Measure {

	//This method calculates the distance between the dots in pixel
	// Christoph
	public double returnDistance(Object4D obj1, Object4D obj2){
		double dist = -1.0;
		if ((obj1.centroid.length > 1) && (obj2.centroid.length > 1)) {
			double sqd = (Math.pow( (obj1.centroid[0] - obj2.centroid[0]), 2.0)    // changed 2 to 2.0
					+ Math.pow( (obj1.centroid[1] - obj2.centroid[1]), 2.0) 
					+ Math.pow( (obj1.centroid[2] - obj2.centroid[2]), 2.0)
					);
			dist = Math.pow(sqd, 0.5);
		}	
		return dist;
	}

	//This method calculates the distance between the dots in microns
	// Christoph
	public double returnDistanceMicrons(Object4D obj1, Object4D obj2, Calibration cal){
		double dist = -1.0;

		if ((obj1.centroid.length > 1) && (obj2.centroid.length > 1)) {
			double sqd = (Math.pow( cal.getX(obj1.centroid[0] - obj2.centroid[0]), 2.0)    // changed 2 to 2.0
					+ Math.pow( cal.getY(obj1.centroid[1] - obj2.centroid[1]), 2.0) 
					+ Math.pow( cal.getZ(obj1.centroid[2] - obj2.centroid[2]), 2.0)
					);
			dist = Math.pow(sqd, 0.5);
		}	
		return dist;
	}
	
	//for calculating distance in pixels, z distance scaled to XY pixels. 
	// this method was used in the plugin. 
	public double returnDistanceZfact(Object4D obj1, Object4D obj2, double zfactor){
		   double dist = -1.0;
		   if ((obj1.centroid.length > 1) && (obj2.centroid.length > 1)) {
			   double sqd = (
					   Math.pow(obj1.centroid[0] - obj2.centroid[0], 2.0)    // changed 2 to 2.0
					   + Math.pow(obj1.centroid[1] - obj2.centroid[1], 2.0) 
					   + Math.pow((obj1.centroid[2] - obj2.centroid[2])*zfactor, 2.0)
					);
			   dist = Math.pow(sqd, 0.5);
		   }	
		   return dist;
		}	
	
}
