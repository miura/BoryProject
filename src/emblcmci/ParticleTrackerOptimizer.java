package emblcmci;

import java.util.Vector;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.measure.ResultsTable;

/** 
 * 
 * @author Miura
 *
 */
public class ParticleTrackerOptimizer {
	
	Vector<Object4D> obj4Dv = null;
	double radiusMin = 1;
	double radiusMax = 5;
	double radiusSteps = 0.5;
	float percentileMin = 0.00001F;
	float percentileMax = 1.0F;
	float percentileSteps = 0.0001F;
	Vector<Object4D> refobj4D; // this will be the reference Object4D, reading from textfile. 
	
	boolean setParameterRanges(){
		GenericDialog gd = new GenericDialog("particleTracker3D Optimizer");
		gd.addNumericField("Radius Minumum :", this.radiusMin, 1);
		gd.addNumericField("\t Maximum :", this.radiusMax, 1);
		gd.addNumericField("\t steps :", this.radiusSteps, 1);		
		gd.addNumericField("Percentile Minumum :", this.percentileMin, 5);
		gd.addNumericField("percentile Maximum :", this.percentileMax, 5);
		gd.addNumericField("percentile Steps :", this.percentileSteps, 5);
		gd.showDialog();
		if (gd.wasCanceled()) 
			return false;
		this.radiusMin = gd.getNextNumber();
		this.radiusMax = gd.getNextNumber();
		this.radiusSteps = gd.getNextNumber();
		this.percentileMin = ((float) gd.getNextNumber())/100;
		this.percentileMax = ((float) gd.getNextNumber())/100;
		this.percentileSteps = ((float) gd.getNextNumber())/100;
		
		return true;
	}
	boolean iterateSpace(ImagePlus imp){
		int iterx = (int) Math.round((this.radiusMax - this.radiusMin)/this.radiusSteps);
		int itery = (int) Math.round((this.percentileMax - this.percentileMin)/this.percentileSteps);
		setResults2Obj4dV();	//currently opened dot list in Results Table becomes the reference. 
		for (int j = 0; j < itery; j++){
			for (int i = 0; i < iterx; i++){
				doParticleTrack(imp, radiusMin + i * radiusSteps, 0, percentileMin + j * percentileSteps);
			}
		}
		return true;
	}
	
	public void doParticleTrack(ImagePlus imp, double radius, double cutoff, float percentile){ 
		obj4Dv.clear();
		DotSegmentByParticletracker3D dpt3D = new DotSegmentByParticletracker3D(radius, cutoff, percentile);
		dpt3D.setup("", imp);
		printParamIJlog(dpt3D);
		String particles = dpt3D.DetectDots3D(imp);
		AutoThresholdAdjuster3D ata = new AutoThresholdAdjuster3D();
		ata.storeParticleInfoInObj4D(particles, obj4Dv, "ch0");
		IJ.log(particles);
	}
	
	public void printParamIJlog(DotSegmentByParticletracker3D dpt3D){
		IJ.log("Radius:" + Double.toString(dpt3D.radius));
		IJ.log("Cutoff:" + Double.toString(dpt3D.cutoff));
		IJ.log("Percentile:" + Double.toString(dpt3D.percentile));	
	}
	
	/** load current Results table to Obj4D as reference.
	 * 
	 * expect that the text file is saved from results table in ImageJ,
	 * then this text file is imported by "Import -> Results. "
	 * @return
	 */
	
	@SuppressWarnings({ "null", "static-access" })
	public boolean setResults2Obj4dV(){
		Vector<Object4D> refobj4d = null;
		ResultsTable rt = null;
		rt.getResultsTable();
		if (rt == null) return false;
		String chnum = "ch0"; // dummy
		int size = 1;	//dummy
		float m0 = 0.0F;
		float m1 = 0.0F;
		float m2 = 0.0F;
		float m3 = 0.0F;
		float m4 = 0.0F;
		float score = 0.0F;
		int timepoint, dotID;
		float intden;
		float[] centroid = {0, 0, 0};
		if (rt.getCounter()<1) return false;
		
		for(int i = 0; i < rt.getCounter(); i++){
			timepoint = (int) rt.getValueAsDouble(rt.getColumnIndex("frame"), i);
			dotID = (int) rt.getValueAsDouble(rt.getColumnIndex("dotID"), i);
        	centroid[0] = (float) rt.getValueAsDouble(rt.getColumnIndex("x"), i);
        	centroid[1] = (float) rt.getValueAsDouble(rt.getColumnIndex("y"), i);
        	centroid[2] = (float) rt.getValueAsDouble(rt.getColumnIndex("z"), i);
        	intden =  (float) rt.getValueAsDouble(rt.getColumnIndex("Intden"), i);
        	Object4D obj4d = new Object4D(size, timepoint, chnum, dotID, centroid, m0, m1, m2, m3, m4, score);
        	obj4d.int_dens = intden;
        	refobj4d.add(obj4d);
		}
		this.refobj4D = refobj4d;
		return true;
	}

}
