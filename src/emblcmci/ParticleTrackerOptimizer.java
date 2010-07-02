package emblcmci;

import java.util.Vector;

import ij.IJ;
import ij.ImagePlus;

public class ParticleTrackerOptimizer {
	
	Vector<Object4D> obj4Dv = null;
	double radiusMin = 1;
	double radiusMax = 5;
	float percentileMin = 0.00001F;
	float percentileMax = 1.0F;
	
	void setParameterRanges(){
		
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

}
