package emblcmci;

/** Preprocess image stack by specifeied FFT parameters. 
 * 
 * @author Kota Miura
 * @author CMCI EMBL, 2010
 */

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;
import ij.process.ImageConverter;
import ij.process.StackConverter;
import ij.process.StackStatistics;

public class Preprocess_ChromosomeDots implements PlugIn {
	//FFT parameters
	private int filterlarge =10;
	private int filtersmall =2;
	private int tolerance =5;
	private String suppress ="None";
	private String fftargument;
	
	ImagePlus imp;
	@Override
	public void run(String arg) {
		if (null == WindowManager.getCurrentImage()) 
			imp = IJ.openImage(); 
		else 		
			imp = new Duplicator().run(WindowManager.getCurrentImage());
		if (null == imp) return;
		StackStatistics impstat = new StackStatistics(imp);
		double maxint = impstat.max;
		double minint = impstat.min;		
		IJ.log("max int = " + Double.toString(maxint));
		IJ.log("min int = " + Double.toString(minint));
		ImageConverter.setDoScaling(true);
		imp.getProcessor().setMinAndMax(minint, maxint);
		StackConverter sc = new StackConverter(imp);
		sc.convertToGray8();
		//IJ.run(imp, "8-bit", "");
		fftbandPssSpec(imp);
		imp.show();
	}
	public void setFFTparameters(int fl, int fs, int tol, String sups){
		filterlarge = fl;
		filtersmall = fs;
		tolerance = tol;
		suppress = sups;
	}	
	
	public void fftbandPssSpec(ImagePlus imp) {
		fftargument = "filter_large="+Integer.toString(filterlarge)
						+" filter_small="+Integer.toString(filtersmall)
						+" suppress="+suppress
						+" tolerance="+Integer.toString(tolerance)
						+" process";
		IJ.log(fftargument);
		IJ.run(imp, "Bandpass Filter...", fftargument); 		
	}	

}
