package emblcmci;

import ij.*;
import Utilities.Counter3D;
import ij.plugin.*;

public class AutoThresholdAdjuster3D_ implements PlugIn {

	int thr, minSize, maxSize, dotSize, fontSize;
	boolean excludeOnEdges, showObj, showSurf, showCentro, showCOM, showNb, whiteNb, newRT, showStat, showMaskedImg, closeImg, showSummary, redirect;

	
	public void run(String arg) {
		IJ.showMessage("My_Plugin","Hello world!");
		ImagePlus imp;
		imp = ij.WindowManager.getCurrentImage();
		IJ.log(Integer.toString(imp.getHeight()));
		 thr = 128;
		 minSize = 3;
		 maxSize = 1000;
		 excludeOnEdges = false;
		 redirect = false;
		 Counter3D OC=new Counter3D(imp, thr, minSize, maxSize, excludeOnEdges, redirect);
		 newRT = true;
		 OC.showStatistics(newRT);
	}
//	ImagePlus imp;
	 	
//	public void run(String arg) {
//		 imp = ij.WindowManager.getCurrentImage();

//		 Counter3D OC=new Counter3D(imp, thr, minSize, maxSize, excludeOnEdges, redirect);
//		 newRT = true;
//		 OC.showStatistics(newRT);
//		//IJ.showMessage("test3D_Plugin","Hello world 3!");
//	}
}

