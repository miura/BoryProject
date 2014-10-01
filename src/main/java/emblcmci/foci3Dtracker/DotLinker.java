package emblcmci.foci3Dtracker;

import emblcmci.foci3Dtracker.Object4D;
import java.util.ArrayList;

/**
 * Link objects in two channels<br>
 * <br>
 * assumes that <b>there is only one or two pairs</b>.<br>
 * Picks up largest and/or nearest particle first. <br>
 * TODO in one case, dots in different daughter cells were linked. This should
 * be avoided. TODO make more flexible in terms of number of objects, e. g. up
 * to 8 dots in meiosis.
 * 
 * 20141001 Upgraded to DotLinkerV2, from DotLinker: uses a new class FociPair
 */
public class DotLinker {
	ArrayList<Object4D> ch0;
	ArrayList<Object4D> ch1;
	int nFrames;
	// array of arrays to store results in.
	ArrayList<FociPair> linkedDots;
	private double zfactor;

	// empty constructor. maybe even delete!
	public DotLinker() {
	}

	// useful methods for empty construction.
	public void setDots(ArrayList<Object4D> ch0, ArrayList<Object4D> ch1) {
		this.ch0 = ch0;
		this.ch1 = ch1;
	}

	public void setNFrames(int nFrames) {
		this.nFrames = nFrames;
	}

	// constructor with two Vector<Object4D>s that contain the Object4Ds that
	// are supposed to be linked.
	public DotLinker(ArrayList<Object4D> ch0, ArrayList<Object4D> ch1,
			int nFrames, double zfactor) {
		this.ch0 = ch0;
		this.ch1 = ch1;
		this.nFrames = nFrames;
		this.zfactor = zfactor;
	}

	// actual linking method, marriage algorithm.
	public ArrayList<FociPair> linkDots() {
		ArrayList<FociPair> linked = new ArrayList<FociPair>(); // also make
																// available for
																// Meiosis: 8!
		Object4D obj4Dch0id1, obj4Dch1id1;
		Object4D obj4Dch0id2, obj4Dch1id2;
		int flag = 0;

		for (int i = 0; i < this.nFrames; i++) { // iterate through frame
													// numbers.
			obj4Dch0id1 = returnObj4D(this.ch0, i, 1); //
			obj4Dch1id1 = returnObj4D(this.ch1, i, 1);
			obj4Dch0id2 = returnObj4D(this.ch0, i, 2);
			obj4Dch1id2 = returnObj4D(this.ch1, i, 2);

			if ((obj4Dch0id1 != null) && (obj4Dch1id1 != null)) {
				// 1x1 case
				if ((obj4Dch0id2 == null) && (obj4Dch1id2 == null)) {
					FociPair apair = new FociPair(obj4Dch0id1, obj4Dch1id1, i);
					linked.add(apair);
				} else {
					double ch0dist, ch1dist;
					// 2x2 both channels contain multiple dots
					if ((obj4Dch0id2 != null) && (obj4Dch1id2 != null)) {
						flag = compare2x2(i);
						ch0dist = Measure.returnDistanceZfact(obj4Dch0id1, obj4Dch0id2, zfactor);
						ch1dist = Measure.returnDistanceZfact(obj4Dch1id1, obj4Dch1id2, zfactor);
						FociPair apair1, apair2;
						if (flag == 1) {
							apair1 = new FociPair(obj4Dch0id1,
									obj4Dch1id1, i);
							apair2 = new FociPair(obj4Dch0id2,
									obj4Dch1id2, i);
						} else {
							apair1 = new FociPair(obj4Dch0id1,
									obj4Dch1id2, i);
							apair2 = new FociPair(obj4Dch0id2,
									obj4Dch1id1, i);
						}
						apair1.addDistanceToOtherPair(ch0dist, ch1dist);
						apair2.addDistanceToOtherPair(ch0dist, ch1dist);
						linked.add(apair1);
						linked.add(apair2);
					} else {
						// 2x1 one channel contains only one dots
						flag = compare2x1(i);
						FociPair apair = null;
						if (flag == 1) {
							apair = new FociPair(obj4Dch0id1, obj4Dch1id1, i);
						} else {
							if (flag == 2) {
								apair = new FociPair(obj4Dch0id1, obj4Dch1id2,
										i);
							} else {
								apair = new FociPair(obj4Dch0id2, obj4Dch1id1,
										i);
							}
						}
						if ((obj4Dch0id1 != null) && (obj4Dch0id2 != null)){
							ch0dist = Measure.returnDistanceZfact(obj4Dch0id1, obj4Dch0id2, zfactor);
							apair.addDistanceToOtherPair(ch0dist, -1);
						}
						if ((obj4Dch1id1 != null) && (obj4Dch1id2 != null)){
							ch1dist = Measure.returnDistanceZfact(obj4Dch1id1, obj4Dch1id2, zfactor);
							apair.addDistanceToOtherPair(-1, ch1dist);
						}
						linked.add(apair);

					}
				}
			}
		}
		for (FociPair apair : linked){
			apair.distance = Measure.returnDistanceZfact(apair.ch0dot, apair.ch1dot, zfactor);
		}
		return linked;
	}

	// dotID could only be 1 or 2 (0 does not exist)
	Object4D returnObj4D(ArrayList<Object4D> obj4Dv, int tpoint, int dotID) {
		Object4D retobj4D = null;
		for (int i = 0; i < obj4Dv.size(); i++) {
			if ((obj4Dv.get(i).timepoint == tpoint)
					&& (obj4Dv.get(i).dotID == dotID)) {

				retobj4D = obj4Dv.get(i);
			}
		}
		return retobj4D;
	}

	// since there is only one dot in a channel, there could be only one link,
	// with three cases
	int compare2x1(int tpoint) {
		int flag = 0;
		int ch0dots = returnDotNumber(this.ch0, tpoint);
		// int ch1dots = returnDotNumber(obj4Dch1, tpoint);
		Object4D ch0id1 = returnObj4D(this.ch0, tpoint, 1);
		Object4D ch1id1 = returnObj4D(this.ch1, tpoint, 1);
		if (ch0dots == 1) {
			Object4D ch1id2 = returnObj4D(this.ch1, tpoint, 2);
			double dist1 = Measure.returnDistanceZfact(ch0id1, ch1id1, zfactor);
			double dist2 = Measure.returnDistanceZfact(ch0id1, ch1id2, zfactor);
			if (dist1 < dist2)
				flag = 1;
			else
				flag = 2;
		} else {
			Object4D ch0id2 = returnObj4D(this.ch0, tpoint, 2);
			double dist1 = Measure.returnDistanceZfact(ch0id1, ch1id1, zfactor);
			double dist2 = Measure.returnDistanceZfact(ch0id2, ch1id1, zfactor);
			if (dist1 < dist2)
				flag = 1;
			else
				flag = 3;
		}
		return flag;
	}

	// only two cases of combinations
	int compare2x2(int tpoint) {
		int flag = 0;
		Object4D ch0id1 = returnObj4D(this.ch0, tpoint, 1);
		Object4D ch0id2 = returnObj4D(this.ch0, tpoint, 2);
		Object4D ch1id1 = returnObj4D(this.ch1, tpoint, 1);
		Object4D ch1id2 = returnObj4D(this.ch1, tpoint, 2);
		double dist1 = Measure.returnDistanceZfact(ch0id1, ch1id1, zfactor)
				+ Measure.returnDistanceZfact(ch0id2, ch1id2, zfactor);
		double dist2 = Measure.returnDistanceZfact(ch0id1, ch1id2, zfactor)
				+ Measure.returnDistanceZfact(ch0id2, ch1id1, zfactor);
		if (dist1 < dist2)
			flag = 1;
		else
			flag = 2;
		return flag;
	}



	// returns number of dots at single time point
	public static int returnDotNumber(ArrayList<Object4D> obj4D, int timepoint) {
		int counter = 0;
		for (Object4D adot : obj4D) {
			if (adot.timepoint == timepoint)
				counter++;
		}
		return counter;
	}

}