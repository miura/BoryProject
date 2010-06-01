package emblcmci;

//import sun.java2d.loops.FillPath;
import trainableSegmentation.Trainable_Segmentation;
import ij.*;
import ij.process.*;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;


public class DotSegmentBy_Trained implements PlugIn {

	private static String fullpathdata = "ttt";

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
	public DotSegmentBy_Trained(){}
	
	
	//for calling from macro
	public static void setFFTparameters(int fl, int fs, int tol, String sups){
		filterlarge = fl;
		filtersmall = fs;
		tolerance = tol;
		suppress = sups;
	}
	
	public void Setfullpathdata(String fullpathdata){
		this.fullpathdata = fullpathdata;
	}
	
	//interactively sets the trained data. dialog pops up.
	public String setDatapath(){
		OpenDialog od = new OpenDialog("Choose data file","");
		if (od.getFileName()==null)
			return null;
		fullpathdata = od.getDirectory() + od.getFileName();
		IJ.log("Data will be laoded from " + fullpathdata + "...");
		return fullpathdata;
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
		if (null == currentimp) return;
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
			//Ignacio's update
			Trainable_Segmentation seg = new Trainable_Segmentation(new ImagePlus("temp"+i, ipc));
			seg.loadTrainingData(fullpath);


//				try{
//					trainer.trainClassifier();
//				}catch(Exception e){
//					e.printStackTrace();
//				}
			seg.trainClassifier();
			IJ.log("trained segmentation of slice="+Integer.toString(i)+" finished");
			binstack.addSlice("n="+Integer.toString(i), seg.getClassifiedImage().getProcessor().convertToByte(true).duplicate(), i);
		    System.gc(); 	
		}	
			//trainer.showClassificationImage2();
		
		ImagePlus resultImage = new ImagePlus("classification result", binstack);
		return resultImage;
		
	}
	public static String testmacrocall(String logtext){
		return logtext;
	}

}
