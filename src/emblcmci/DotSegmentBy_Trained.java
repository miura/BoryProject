package emblcmci;

//import sun.java2d.loops.FillPath;
import ij.*;
import ij.process.*;
//import ij.gui.*;
import ij.io.OpenDialog;

import ij.plugin.PlugIn;
//import trainableSegmentation.*;	//Fiji plugin
//import trainableSegmentation.Trainable_Segmentation;


public class DotSegmentBy_Trained implements PlugIn {

	public static String fullpathdata = "ttt";

	//FFT parameters
	public static int filterlarge =10;
	public static int filtersmall =2;
	public static int tolerance =5;
	public static String suppress ="None";
	public static String FFTargument;
	
	public void run(String arg) {
		IJ.log("test fiji");
		setDatapath(); 
		ProcessTopImage();
	}
	//** constructor 
	//
	public DotSegmentBy_Trained(){
		FFTargument = "filter_large="+Integer.toString(filterlarge)
		+" filter_small="+Integer.toString(filtersmall)
		+" suppress="+suppress
		+" tolerance="+Integer.toString(tolerance)
		+" process";
		//IJ.log(FFTargument);
	}
	
	//for calling from macro
	public static void setFFTparameters(int fl, int fs, int tol, String sups){
		filterlarge = fl;
		filtersmall = fs;
		tolerance = tol;
		suppress = sups;
	}
	
	
	//interactively sets the trained data. dialog pops up.
	public static void setDatapath(){
		OpenDialog od = new OpenDialog("Choose data file","");
		if (od.getFileName()==null)
			return;
		fullpathdata = od.getDirectory() + od.getFileName();
		IJ.log("Loading data from " + fullpathdata + "...");
	}
	
	// this could be called easily from automated routine in Macro. 
	public static void setDatapath(String fullpath){
		fullpathdata = fullpath;
		IJ.log("Loading data from " + fullpathdata + "...");
	}
	
	public static void ProcessTopImage(){
		ImagePlus currentimp;
		//get current image
		if (null == WindowManager.getCurrentImage()) 
			currentimp = IJ.openImage();
		else 		
			currentimp = DuplicateStack(WindowManager.getCurrentImage());
		//currentimp.show();
		ImagePlus resultImage = Core(currentimp);
		resultImage.show();			
	}
	
	public static void duplicatetest(){
		DuplicateStack(WindowManager.getCurrentImage());
	}
	
	//duplicate and make another instance of imp
	public static ImagePlus DuplicateStack(ImagePlus ims){
		ImageStack stack = ims.getImageStack();
		
		ImageStack dupstack = ims.createEmptyStack();
		//ImageStack dupstack = new ImageStack(stack.getWidth(), stack.getHeight());
		for (int i=0; i<stack.getSize(); i++){
				dupstack.addSlice(Integer.toString(i), stack.getProcessor(i+1).duplicate(), i); 
		}
		ImagePlus dupimp = new ImagePlus("duplicate", dupstack);
		//dupimp.show();
		return dupimp;
	}
	// this as well could be called from macro easily. 
	public static void ProcessImageAt(String fullpathstack){
		//String fullpathstack = "C:\\HDD\\People\\Bory\\testChromosome\\3con170210_6_R3D.dv - C=0.tif";
		//String fullpathstack = "C:\\HDD\\People\\Bory\\100423\\tt.tif";
		ImagePlus imp = IJ.openImage(fullpathstack);//during development, in
		ImagePlus resultImage = Core(imp);
		IJ.log(resultImage.getTitle());
		resultImage.show();	
	}

	public static void FFTbandPssSpec(ImagePlus imp) {
		//FFTprocess(stack); filterlargesmall() cannot be used, so do the higher method as below. 
		 //IJ.run(imp, "Bandpass Filter...", "filter_large=10 filter_small=2 suppress=None tolerance=5 autoscale process");
		FFTargument = "filter_large="+Integer.toString(filterlarge)
						+" filter_small="+Integer.toString(filtersmall)
						+" suppress="+suppress
						+" tolerance="+Integer.toString(tolerance)
						+" process";
		IJ.log(FFTargument);
		IJ.run(imp, "Bandpass Filter...", FFTargument); 		
	}
	
	public static ImagePlus Core(ImagePlus imp){

		ImageStack stack = imp.getStack();

		ImagePlus imptemp = new ImagePlus("working slice", new ByteProcessor(imp.getWidth(), imp.getHeight()));
		ImageProcessor ipc = imptemp.getProcessor();// = imp.getProcessor().duplicate();		
		ImageStack binstack = imp.createEmptyStack();
		String fullpath;
		fullpath = fullpathdata;
		for (int i=0; i<stack.getSize(); i++){
			ipc = stack.getProcessor(i+1).duplicate();
			LoadTrainedEx trainer = new LoadTrainedEx(); //too many instances for large stack?
			trainer.setNoGUI(true);
			trainer.setCurrentImageNoGUI(ipc);
			trainer.loadTrainingDataNoGUI(fullpath); //data100423.arff 

				//ImagePlus testImage = new ImagePlus("test image",ipc);
				//trainer.applyClassifierToTestImage(testImage).show();	
				//testImage.show();

			try{
				trainer.trainClassifier();
			}catch(Exception e){
				e.printStackTrace();
			}
			IJ.log("trained segmentation of slice="+Integer.toString(i)+" finished");
			binstack.addSlice("n="+Integer.toString(i), trainer.getClassifiedImage().getProcessor().convertToByte(true).duplicate(), i);
			// test: new ImagePlus("n"+Integer.toString(i), trainer.getClassifiedImage().getProcessor().convertToByte(true).duplicate()).show();
		    System.gc(); 	
		}	
			//trainer.showClassificationImage2();
		
		ImagePlus resultImage = new ImagePlus("classification result", binstack);
		return resultImage;
		
	}
	public static String testmacrocall(String logtext){
		return logtext;
	}

	//parameters for FFT band pass filter: not used, sinceIJ.run is used
	/*
	public static double filterSmallDia = 2.0;
	public static double filterLargeDia = 10.0;
	private static int choiceIndex = 0;
	public static double toleranceDia = 5.0;
	public static boolean doScalingDia = true;
	*/	
	
	//this method is not used currently 100416. tried to access directly to FFTfilter, but alternatively done by IJ.run 
/*	public void FFTprocess(ImageStack stack){
		//ImageProcessor ip2 = ip;
		//if (ip2 instanceof ColorProcessor) {
		//	//showStatus("Extracting brightness");
		//	ip2 = ((ColorProcessor)ip2).getBrightness();
		//} 
		Rectangle roiRect = stack.getRoi();	// in case of no ROI, full size of the image field
		int maxN = Math.max(roiRect.width, roiRect.height); 
		double sharpness = (100.0 - toleranceDia) / 100.0;
		boolean doScaling = doScalingDia;
		//boolean saturate = saturateDia;
		//IJ.runPlugIn(imp, "ij.plugin.filter.FFTFilter", "");
		//
		//maybe use IJ.runMacro("", parameters)
		//IJ.showProgress(1,20);

		/* 	tile mirrored image to power of 2 size		
			first determine smallest power 2 >= 1.5 * image width/height
		  	factor of 1.5 to avoid wrap-around effects of Fourier Trafo */

/*		int i=2;
		while(i<1.5 * maxN) i *= 2;		
        
        // Calculate the inverse of the 1/e frequencies for large and small structures.
        double filterLarge = 2.0*filterLargeDia / (double)i;
        double filterSmall = 2.0*filterSmallDia / (double)i;
        
		// fit image into power of 2 size 
		Rectangle fitRect = new Rectangle();
		fitRect.x = (int) Math.round( (i - roiRect.width) / 2.0 );
		fitRect.y = (int) Math.round( (i - roiRect.height) / 2.0 );
		fitRect.width = roiRect.width;
		fitRect.height = roiRect.height;
		
		// put image (ROI) into power 2 size image
		// mirroring to avoid wrap around effects
		//showStatus("Pad to "+i+"x"+i); commented out
		
		// follwoing part shoul de done in slice loop?
		
		FFTFilter fftf = new FFTFilter();
		
		for (i=0; i<stack.getSize(); i++) {
			ImageProcessor ip = stack.getProcessor(i);
			ImageProcessor ip2 = ip; //probably need
			
			ip2 = fftf.tileMirror(ip2, i, i, fitRect.x, fitRect.y);
			IJ.showProgress(2,20);
			
			// transform forward
			//showStatus(i+"x"+i+" forward transform");
			FHT fht = new FHT(ip2);
			fht.setShowProgress(false);
			fht.transform();
			//IJ.showProgress(9,20);
			//new ImagePlus("after fht",ip2.crop()).show();	

			// filter out large and small structures
			////showStatus("Filter in frequency domain");
			fftf.filterLargeSmall(fht, filterLarge, filterSmall, choiceIndex, sharpness);		
			//filterLargeSmall(fht, filterLarge, filterSmall, choiceIndex, sharpness);
		}		
	}
	*/
}
