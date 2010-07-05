package emblcmci;

/** Segmentation of Chromosome dots using trained aiff data 
 * by Trainable_Segmentation without GUI. 
 * TODO check how preferences for feature extraction is set. 
 * When data is loaded, the setting could be default values. 
 * @author Kota Miura
 * @author CMCI EMBL
 */

import trainableSegmentation.Trainable_Segmentation;
import ij.*;
import ij.process.*;
import ij.io.OpenDialog;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;


public class DotSegmentByTrained {

	private static String fullpathdata = "ttt";

	public void run() {
		//IJ.log("test fiji");
		setDatapath(); 
		processTopImage();
	}
	//** constructor 
	//
	public DotSegmentByTrained(){}
	
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
	
	
	/** sets path to the trained data .aiff
	 * 
	 *  this could be called easily from automated routine in Macro. 
	 * @param fullpath
	 */
	public static void setDatapath(String fullpath){
		fullpathdata = fullpath;
		IJ.log("Loading data from " + fullpathdata + "...");
	}
	
	public static void processTopImage(){
		ImagePlus currentimp;
		//get current image
		if (null == WindowManager.getCurrentImage()) 
			currentimp = IJ.openImage(); 
		else 		
			currentimp = new Duplicator().run(WindowManager.getCurrentImage());
		if (null == currentimp) return;
		//currentimp.show();
		ImagePlus resultImage = core(currentimp);
		resultImage.show();			
	}
	
	public static void duplicatetest(){
		duplicateStack(WindowManager.getCurrentImage());
	}
	
	//duplicate and make another instance of imp
	public static ImagePlus duplicateStack(ImagePlus ims){
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
	public static void processImageAt(String fullpathstack){
		//String fullpathstack = "C:\\HDD\\People\\Bory\\testChromosome\\3con170210_6_R3D.dv - C=0.tif";
		//String fullpathstack = "C:\\HDD\\People\\Bory\\100423\\tt.tif";
		ImagePlus imp = IJ.openImage(fullpathstack);//during development, in
		ImagePlus resultImage = core(imp);
		IJ.log(resultImage.getTitle());
		resultImage.show();	
	}
	
	public static ImagePlus core(ImagePlus imp){

		ImageStack stack = imp.getStack();

		ImagePlus imptemp = new ImagePlus("training slice", new ByteProcessor(imp.getWidth(), imp.getHeight()));
		ImageProcessor ipc = imptemp.getProcessor();// = imp.getProcessor().duplicate();		
		String fullpath;
		fullpath = fullpathdata;

		ipc = stack.getProcessor(1).duplicate();
		//using Ignacio's update
		Trainable_Segmentation seg = new Trainable_Segmentation(new ImagePlus("temp", ipc));
		//"Load Data" button
		seg.loadTrainingData(fullpath);
		//"Train classifier" button
		try{
			seg.trainClassifier();
		}catch(Exception e){
			e.printStackTrace();
		}
		//"Apply Classifier" button
		ImagePlus binimp = seg.applyClassifierToTestImage(imp);
				
		return binimp;
		
	}
	public static String testmacrocall(String logtext){
		return logtext;
	}

}
