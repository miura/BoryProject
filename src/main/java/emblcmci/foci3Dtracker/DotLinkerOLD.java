package emblcmci.foci3Dtracker;

import emblcmci.foci3Dtracker.Object4D;
import ij.measure.Calibration;

import java.util.ArrayList;


/** Link objects in two channels<br>
* <br>
* assumes that <b>there is only one or two pairs</b>.<br>
* Picks up largest and/or nearest particle first.  
* <br>
* TODO in one case, dots in different daughter cells were linked. This should be avoided.
* TODO make more flexible in terms of number of objects, e. g. up to 8 dots in meiosis.
*/
public class DotLinkerOLD{
	ArrayList<Object4D> ch0;
	ArrayList<Object4D> ch1;
	int nFrames;
	// array of arrays to store results in.
	Object4D[][] linkedDots;
	
	
	//empty constructor. maybe even delete! 
	public DotLinkerOLD(){}

	// useful methods for empty construction.
	public void setDots(ArrayList<Object4D> ch0, ArrayList<Object4D> ch1){
		this.ch0 = ch0;
		this.ch1 = ch1;
	}

	public void setNFrames(int nFrames){
		this.nFrames = nFrames;
	}
	
	
	
	// constructor with two Vector<Object4D>s that contain the Object4Ds that are supposed to be linked.
	public DotLinkerOLD(ArrayList<Object4D> ch0, ArrayList<Object4D> ch1, int nFrames){
		this.ch0 = ch0;
		this.ch1 = ch1;
		this.nFrames = nFrames;
	}
	
	
	// actual linking method, marriage algorithm.
	public Object4D[][] linkDots(){
		Object4D[][] linked = new Object4D[this.nFrames][4]; //also make available for Meiosis: 8!
		Object4D obj4Dch0id1, obj4Dch1id1;
		Object4D obj4Dch0id2, obj4Dch1id2;		   
		int flag = 0;
		   
		for (int i = 0; i < this.nFrames; i++){  // iterate through frame numbers.
			obj4Dch0id1 = returnObj4D(this.ch0, i, 1);	// 
			obj4Dch1id1 = returnObj4D(this.ch1, i, 1);
			obj4Dch0id2 = returnObj4D(this.ch0, i, 2);	
			obj4Dch1id2 = returnObj4D(this.ch1, i, 2);
			
			if ((obj4Dch0id1 != null) && (obj4Dch1id1 != null)) {
				// 1x1 case
				if ((obj4Dch0id2 == null) && (obj4Dch1id2 == null)) { 
					linked[i][0] = obj4Dch0id1;
					linked[i][1] = obj4Dch1id1;				   
				}
				else {
				// 2x2 both channels contain multiple dots
					 if ((obj4Dch0id2 != null) && (obj4Dch1id2 != null)) {
						flag = compare2x2(i);
					 	if (flag == 1) {
					 		linked[i][0] = obj4Dch0id1;
					 		linked[i][1] = obj4Dch1id1;
					 		linked[i][2] = obj4Dch0id2;
					 		linked[i][3] = obj4Dch1id2;							   
						 	}
					 	else {
							  linked[i][0] = obj4Dch0id1;
							  linked[i][1] = obj4Dch1id2;
							  linked[i][2] = obj4Dch0id2;
							  linked[i][3] = obj4Dch1id1;
						    }
					   } 
					 else {
					//2x1 one channel contains only one dots
						   flag = compare2x1(i);
						   if (flag == 1) {
							   linked[i][0] = obj4Dch0id1;
							   linked[i][1] = obj4Dch1id1;
						   } else {
							   if (flag == 2){
								   linked[i][0] = obj4Dch0id1;
								   linked[i][1] = obj4Dch1id2;
							   } else {
								   linked[i][0] = obj4Dch0id2;
								   linked[i][1] = obj4Dch1id1;
							   }
						   }
					   }
				   }
			   }
		   }
		   return linked;
	   }	
	
	   // dotID could only be 1 or 2 (0 does not exist)
	   Object4D returnObj4D(ArrayList<Object4D> obj4Dv, int tpoint, int dotID){
		   Object4D retobj4D = null;
		   for (int i=0; i<obj4Dv.size(); i++){
			   if ((obj4Dv.get(i).timepoint == tpoint) 
				  && (obj4Dv.get(i).dotID == dotID)){
				  
				   retobj4D = obj4Dv.get(i);
			   }
		   }		   
		   return retobj4D;
	   }
	   
	   //since there is only one dot in a channel, there could be only one link, with three cases
	   int compare2x1(int tpoint){
		   int flag = 0;
		   int ch0dots = returnDotNumber(this.ch0, tpoint);
		   //int ch1dots = returnDotNumber(obj4Dch1, tpoint);
		   Object4D ch0id1  = returnObj4D(this.ch0, tpoint, 1);
		   Object4D ch1id1  = returnObj4D(this.ch1, tpoint, 1);
		   if (ch0dots == 1) {
			   Object4D ch1id2  = returnObj4D(this.ch1, tpoint, 2);
			   double dist1 =returnDistance(ch0id1, ch1id1);
			   double dist2 =returnDistance(ch0id1, ch1id2);
			   if (dist1 < dist2) flag = 1;
			   else flag = 2;
		   } else {
			   Object4D ch0id2  = returnObj4D(this.ch0, tpoint, 2);
			   double dist1 =returnDistance(ch0id1, ch1id1);
			   double dist2 =returnDistance(ch0id2, ch1id1);
			   if (dist1 < dist2) flag = 1;
			   else flag = 3;			   
		   }
		   return flag;
	   }
	   
	   //only two cases  of combinations
	   int compare2x2(int tpoint){
		   int flag =0;
		   Object4D ch0id1  = returnObj4D(this.ch0, tpoint, 1);
		   Object4D ch0id2  = returnObj4D(this.ch0, tpoint, 2);
		   Object4D ch1id1  = returnObj4D(this.ch1, tpoint, 1);
		   Object4D ch1id2  = returnObj4D(this.ch1, tpoint, 2);
		   double dist1 = returnDistance(ch0id1, ch1id1) + returnDistance(ch0id2, ch1id2);
		   double dist2 = returnDistance(ch0id1, ch1id2) + returnDistance(ch0id2, ch1id1);
		   if (dist1 < dist2) flag = 1;
		   else flag = 2;		   
		   return flag;
	   }
	   
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
		
		// returns number of dots at single time point
		int returnDotNumber(ArrayList<Object4D> obj4D, int timepoint){
			int counter = 0;
			for (int i = 0; i < obj4D.size(); i++){
				if (obj4D.get(i).timepoint == timepoint) counter++;
			}
			return counter;
		}
	
}