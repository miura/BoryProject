package emblcmci.foci3Dtracker;

/** Segmentation of fission yeast chromosome Foci 
 *	use 3D object counter to adjust threshold level of 3D stack. 
 *	3D object counter must be installed in ImageJ
 *	100614 main functions now in separate classes
 * @author Kota Miura  
 * @ cmci, embl miura@embl.de
 */

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import ij.plugin.Duplicator;
import ij.*;
import Utilities.Counter3D;
import Utilities.Object3D;
import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.*;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import ij.process.StackProcessor;

public class AutoThresholdAdjuster3D {   // there should be a constructor with respective MaxSpotVoxels, MinSpotVoxels, MaxLoops?
	private static boolean createComposite = true;
	int thr, minSize, maxSize, dotSize, fontSize;
	boolean excludeOnEdges, showObj, showSurf, showCentro, showCOM, showNb, whiteNb, newRT, showStat, closeImg, showSummary, redirect;
	boolean silent = false;
	boolean showMaskedImg = false;
	
	ParamSetter para = new ParamSetter();
	int maxspotvoxels = para.getMaxspotvoxels();
	
	/**Object volume minimum for volume-based segmentation*/
	int minspotvoxels = para.getMinspotvoxels();
	
	/**object volume minimum for measurement (measurement meaning determination of position!????)
	 *  (maybe 7 is too small)*/
	int minspotvoxels_measure = para.getMinspotvoxels_measure();
	
	/** maximum loop for exiting optimum threshold searching	 */
	int maxloops = para.getMaxloops();
	
	int thadj_volmin = para.getThadj_volmin();
	
	int thadj_volmax = para.getThadj_volmax();
	
	int thadj_nummin = para.getThadj_nummin();
	
	int thadj_nummax = para.getThadj_nummax();
	
	int segMethod = para.getSegMethod();

	String fullpathtoTrainedData0 = para.getTrainedDataFullPath0();
	
	String fullpathtoTrainedData1 = para.getTrainedDataFullPath1();
	
	
	/** extended class of Object3D 
	 * object3D + timepoint, channel, dotID
	 * */
	Object4D obj4d;	//Object3D added with time point and channel number fields. 
	
	/** Vector for storing detected dots in channel 0	 */
	Vector<Object4D> obj4Dch0; 

	/** Vector for storing detected dots in channel 1	 */
	Vector<Object4D> obj4Dch1; 
	
	/** Array for linked 4D objects, field variable to store the results of */
	Object4D[][] linkedArray; 
	
	Calibration cal, calkeep;
	
	/**Factor to multiply for depth, to correct for xy pixel scale =1
	 * 
	 */
	double zfactor;
	
	
	public void setParam(ParamSetter p){
		maxspotvoxels = p.getMaxspotvoxels();
		minspotvoxels = p.getMinspotvoxels();
		minspotvoxels_measure = p.getMinspotvoxels_measure();
		maxloops = p.getMaxloops();
		thadj_volmin = p.getThadj_volmin();
		thadj_volmax = p.getThadj_volmax();
		thadj_nummin = p.getThadj_nummin();
		thadj_nummax = p.getThadj_nummax();
		segMethod = p.getSegMethod();
	}


	public void run() {
		// ** get a list of opened windows. 
		//copied and modified from image - color merge... (RGBStackMerge.java)
		int[] wList = WindowManager.getIDList();
		if (wList==null) {
			IJ.error("bory dot analysis", "No images are open.");
			return;
		}

		String[] titles = new String[wList.length+1];
		for (int i=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			titles[i] = imp!=null?imp.getTitle():"";
		}
		String none = "*None*";
		titles[wList.length] = none;
		
		// ** dialog for selecting image stack for each channel 
		GenericDialog gd = new GenericDialog("Bory Dot Analysis");
		gd.addChoice("Ch0:", titles, titles[0]);
		gd.addChoice("Ch1:", titles, titles[1]);
		//String title3 = titles.length>2&&!IJ.macroRunning()?titles[2]:none;
		gd.addCheckbox("Create Merged Binary", createComposite);
		//gd.addCheckbox("Keep Source Images", false);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		int[] index = new int[2];
		index[0] = gd.getNextChoiceIndex();
		index[1] = gd.getNextChoiceIndex();
		createComposite = gd.getNextBoolean();
		ImagePlus imp0 = WindowManager.getImage(wList[index[0]]);
		ImagePlus imp1 = WindowManager.getImage(wList[index[1]]);		
		//cal.set
		
		// ** check if selected windows are stack
		if (imp0 == null) return;
		if (imp0.getStackSize() == 1) {
			IJ.error("Channel 0 is not a stack");
			return;		
		}
		if (imp1.getStackSize() == 1) {
			IJ.error("Channel 1 is not a stack");
			return;
		}
		if (!setScale(imp0)){
			IJ.error("Voxel Depth(z)is not defined correctly: check [Image -> properties]");
			return;
		}

		IJ.log("min vox segment: " + Integer.toString(minspotvoxels) + 
				"\n min vox measure : " + Integer.toString(minspotvoxels_measure));
	
		Roi r0 = imp0.getRoi();
		Roi r1 = imp1.getRoi();
		Roi r = null;
		
		// ** works both with and without ROI. 
		//    in case of ROI selected, that portion is cropped. 
		//    ... then start of segmentation and measurements
		if ((r0 == null) && (r1 == null))				
			segAndMeasure( imp0, imp1);
		else {
			ImagePlus imp0roi = null;
			ImagePlus imp1roi = null;
			IJ.log("... ROI found ... ");			
			if (r0 == null) 
				r =r1;
			else
				//if (r1 == null)
				r =r0;
			if (r == null) {
					IJ.error("need a rectangular ROI");
					return;
			}
			if (!r.isArea()) {
				IJ.error("need a rectangular ROI");
				return;
			}
			Rectangle rb = r.getBounds();
			imp0.killRoi();
			imp1.killRoi();			

			ImagePlus tempdup = null;
			StackProcessor tempstackproc = null;
			ImageStack cropstack = null;

			tempdup = new Duplicator().run(imp0);
			tempstackproc  = new StackProcessor(tempdup.getStack(),tempdup.getStack().getProcessor(1));
			cropstack = tempstackproc.crop(rb.x, rb.y, rb.width, rb.height);
			imp0roi = new ImagePlus("croppedCh0", cropstack);
			imp0roi.setCalibration(cal);
			imp0roi.setDimensions(imp0.getNChannels(), imp0.getNSlices(), imp0.getNFrames());
			if (silent == false){
				imp0roi.show();
			}
			
			tempdup = new Duplicator().run(imp1);
			tempstackproc  = new StackProcessor(tempdup.getStack(),tempdup.getStack().getProcessor(1));
			cropstack = tempstackproc.crop(rb.x, rb.y, rb.width, rb.height);
			imp1roi = new ImagePlus("croppedCh1", cropstack);
			imp1roi.setCalibration(cal);
			imp1roi.setDimensions(imp1.getNChannels(), imp1.getNSlices(), imp1.getNFrames());
			if (silent == false){
				imp1roi.show();
			}
						
			segAndMeasure(imp0roi, imp1roi);
		}
	}
	
	/**Three different methods for segmentation of dots, then measure dot-dot distance time course. 
	 * <br>currently, 
	 * <ul>
	 * 	1. automatic threshold adjustment coupled with 3Dobject counter is used. 
	 *  <br>2. using trained data and trainable segmentation
	 *  <br>3. using segmentation module of particle tracker 3D (not implemented yet). 
	 * </ul> 
	 * @param imp0 channel 1 4D stack
	 * @param imp1 channel 2 4D stack
	 * @return
	 */
	public boolean segAndMeasure(ImagePlus imp0, ImagePlus imp1){
		ImagePlus binimp0, binimp1;
		obj4Dch0 = new Vector<Object4D>();
		obj4Dch1 = new Vector<Object4D>();
		//auto adjusted threshold segmentation
		if (segMethod == 0) {		
			binimp0 = segmentaitonByObjectSize(imp0);
			binimp1 = segmentaitonByObjectSize(imp1);
		}
		else {
			return false;	
		}
		
		ImagePlus rgbbin=null;
		/* in case of particle 3D, no binary images are produced so no composite image. */
		if ((segMethod != 2) && (createComposite)) {
			ImagePlus ch0proj=null;
			ImagePlus ch1proj=null;
			ch0proj = createZprojTimeSeries(binimp0, imp0.getNSlices(), imp0.getNFrames());
			ch1proj = createZprojTimeSeries(binimp1, imp1.getNSlices(), imp1.getNFrames());
			ImageStack dummy = null;
			RGBStackMerge rgbm = new RGBStackMerge();
			ImageStack rgbstack = rgbm.mergeStacks(ch0proj.getWidth(), ch0proj.getHeight(), ch0proj.getStackSize(), ch0proj.getStack(), ch1proj.getStack(), dummy, true);
			rgbbin = new ImagePlus("binProjMerged", rgbstack);
			rgbbin.show();
			
		}
		//3D object measurement part
		int ch0objnum = 0; 
		int ch1objnum = 0; 
		ch0objnum = measureDots(binimp0, "Ch0", obj4Dch0, imp0.getNSlices());
		ch1objnum = measureDots(binimp1, "Ch1", obj4Dch1, imp1.getNSlices());		
		linkedArray = dotLinker(obj4Dch0,  obj4Dch1, imp0.getNFrames());
		
		if (silent == false) {
			showStatistics(obj4Dch0);
			showStatistics(obj4Dch1);
			showDistances(linkedArray);
			}
		
		drawlinksGrayscale(linkedArray, imp0, imp1);
		plotDetectedDots(obj4Dch0, imp0, Color.yellow);
		plotDetectedDots(obj4Dch1, imp1, Color.red);
		return true; 
	}
	
	
	/** Stores particle parameters 
	 * <ul>
	 * <li>centroid<li>coordinates<li>moments<li>scores
	 * </ul>
	 * output from particle3D plugin (type of String) are stored in Object4D array.
	 * <br>Object4D is an extended class of Object3D with time point fields and methods.  
	 * 
	 * @param particles: String variable exported from particle3D plugin
	 * @param obj4dv Vector<Object4D> to store all detected particles
	 * @param chnum String indicating the name of channel
	 */
	public void storeParticleInfoInObj4D(String particles, Vector<Object4D> obj4dv, String chnum){
		String[] lines;
		String line;
		String[] frame_number_info;
		lines = particles.split("\n");
		int currentframe = 0;
		int dotID = 1;
		int dummysize = 1;
		for (int i = 0; i < lines.length; i++){
			if (lines[i] == null) break;
			line = lines[i].trim();
	        frame_number_info = line.split("\\s+");
	        int framenum = Integer.parseInt(frame_number_info[0]);
	        if (framenum != currentframe) {
	        	dotID = 1;
	        	currentframe = framenum;
	        }
	        
	        float[] centroid = {0, 0, 0};
	        centroid[0]= Float.parseFloat(frame_number_info[2]); //xy order is opposite
	        centroid[1]= Float.parseFloat(frame_number_info[1]);
	        centroid[2]= Float.parseFloat(frame_number_info[3]);
	        float m0 = Float.parseFloat(frame_number_info[4]);
	        float m1 = Float.parseFloat(frame_number_info[5]);
	        float m2 = Float.parseFloat(frame_number_info[6]);
	        float m3 = Float.parseFloat(frame_number_info[7]);
	        float m4 = Float.parseFloat(frame_number_info[8]);
	        float score = Float.parseFloat(frame_number_info[9]);
	        	        
	        Object4D obj4d = new Object4D(dummysize, framenum, chnum, dotID, centroid, m0, m1, m2, m3, m4, score);
	        obj4dv.add(obj4d);
	        dotID++;
		}
		sortbyScore(obj4dv);
	}
	
	void sortbyScore(Vector<Object4D> obj4dv){
		int currentframe = 0;
		int counter = 0;
		Vector<Object4D> obj4dVpertime = new Vector<Object4D>();
		for (int i = 0; i < obj4dv.size(); i++){
			if ((i == 0) || (obj4dv.get(i).timepoint != currentframe)){
				currentframe = obj4dv.get(i).timepoint;
				for (int j = 0; j < obj4dv.size(); j++){
					if (obj4dv.get(j).timepoint == currentframe) {
						obj4dVpertime.add(obj4dv.get(j));
					}
				}
				Collections.sort(obj4dVpertime,  new ComparerByscore4D(ComparerByscore4D.DESC));
				for (int j = 0; j < obj4dVpertime.size(); j++){
					obj4dv.setElementAt(obj4dVpertime.get(j), counter);
					counter++;
				}
				obj4dVpertime.clear();
			}
		}	
	}
	
	// to print out linked dots and infromation in log window. 
	void linkresultsPrinter(Object4D[][] linkedArray){
		 for (int j = 0; j < linkedArray.length; j++){
			 IJ.log("tframe = "+Integer.toString(j));
			 for (int i = 0; i< linkedArray[0].length; i++){
				 if (linkedArray[j][i] == null){
					 IJ.log("...");					 
				 } else {
				 IJ.log("... ID = " + Integer.toString(linkedArray[j][i].dotID)
						 + " ... " +  linkedArray[j][i].chnum
						 + " ...Volume = " + Integer.toString(linkedArray[j][i].size));
				 }
			 }
		 }
	}
	/** not used anymore
	 * 
	 * @param linked
	 * @param imp
	 */
	public void drawlinks(Object4D[][] linked, ImagePlus imp){
		IJ.run("Colors...", "foreground=white background=white selection=yellow");
		for(int i = 0;  i < linked.length; i++) {
			for(int j = 0;  j < linked[0].length; j += 2) {
				if (linked[i][j] != null){
					imp.setSlice(linked[i][j].timepoint + 1);
					imp.setRoi(new Line(linked[i][j].centroid[0], linked[i][j].centroid[1], linked[i][j+1].centroid[0], linked[i][j+1].centroid[1]));
				}	IJ.run(imp, "Draw", "slice");
			}
		}
		imp.updateAndDraw();
	}

	/**plotting linked lines, but with original gray scale image (will be converted to RGB).
	 * 
	 * @param linked
	 * @param imp0
	 * @param imp1
	 * @author Kota Miura
	 */
	public void drawlinksGrayscale(Object4D[][] linked, ImagePlus imp0, ImagePlus imp1){
		ImagePlus ch0proj = null;
		ImagePlus ch1proj = null;
		ch0proj = createZprojTimeSeries(imp0, imp0.getNSlices(), imp0.getNFrames());
		ch1proj = createZprojTimeSeries(imp1, imp1.getNSlices(), imp1.getNFrames());
		new StackConverter(ch0proj).convertToRGB();
		new StackConverter(ch1proj).convertToRGB();
		
		int offset = 0;
		int ch0x, ch0y, ch1x, ch1y;
		for(int i = 0;  i < linked.length; i++) {
			for(int j = 0;  j < linked[0].length; j += 2) {
				if (linked[i][j] != null){
					ch0x = Math.round(linked[i][j].centroid[0] - offset);
					ch0y = Math.round(linked[i][j].centroid[1] - offset);
					ch1x = Math.round(linked[i][j + 1].centroid[0] - offset);
					ch1y = Math.round(linked[i][j + 1].centroid[1] - offset);

					ImageProcessor ip0 = ch0proj.getStack().getProcessor(linked[i][j].timepoint + 1);
					ip0.setColor(Color.blue);
					ip0.drawLine(ch0x, ch0y, ch1x, ch1y);
					ip0.setColor(Color.yellow);
					ip0.drawPixel(ch0x, ch0y);
					ip0.setColor(Color.red);
					ip0.drawPixel(ch1x, ch1y);					

					ImageProcessor ip1 = ch1proj.getStack().getProcessor(linked[i][j].timepoint + 1);
					ip1.setColor(Color.blue);
					ip1.drawLine(ch0x, ch0y, ch1x, ch1y);
					ip1.setColor(Color.yellow);
					ip1.drawPixel(ch0x, ch0y);
					ip1.setColor(Color.red);
					ip1.drawPixel(ch1x, ch1y);
					
				}	
			}
		}
		ImageStack combined = new StackCombiner().combineHorizontally(ch0proj.getStack(), ch1proj.getStack());
		ImagePlus combimp = new ImagePlus("DetectedDots", combined);
		
		combimp.show();
	}
	/* for plotting Object4Ds detected by segmentation. 
	 * Creates a new RGB 
	 * imp  grayscale image
	 */
	public void plotDetectedDots(Vector<Object4D> obj4dv, ImagePlus imp, Color color){
		Duplicator dup = new Duplicator();
		ImagePlus dupimp = dup.run(imp);
		new StackConverter(dupimp).convertToRGB();
		float x, y, z;
		int timepoint;
		int nSlices = imp.getNSlices();
		int nFrames = imp.getNFrames();
		if (nFrames <= 1) return;
		ImageProcessor ip = null;
		for (int i = 0; i < obj4dv.size(); i++) {
			x = obj4dv.get(i).centroid[0];
			y = obj4dv.get(i).centroid[1];
			z = obj4dv.get(i).centroid[2];
			timepoint = obj4dv.get(i).timepoint;
			ip = dupimp.getStack().getProcessor(timepoint * nSlices + Math.round(z)); //TODO
			ip.setColor(color);
			ip.drawPixel(Math.round(x), Math.round(y));
		}
		dupimp.show();
		
	}

	/** Z projection of 4D stack, each time point projected to 2D.<br> 
	 *this might not be usefule these days as native z-projection supports 4D. 
	 * @param imp: 4D stack ImagePlus
	 * @param zframes: number of z slices
	 * @param tframes: number of time points.
	 * @return
	 */
	public ImagePlus createZprojTimeSeries(ImagePlus imp, int zframes, int tframes){
		ImageStack zprostack = new ImageStack();
		zprostack = imp.createEmptyStack();
		ZProjector zpimp = new ZProjector(imp);
		zpimp.setMethod(1); //1 is max intensity projection	
		for (int i=0; i<tframes;i++){
			zpimp.setStartSlice(i*zframes+1);
			zpimp.setStopSlice((i+1)*zframes);
			zpimp.doProjection();
			zprostack.addSlice("t="+Integer.toString(i+1), zpimp.getProjection().getProcessor());
		}
		ImagePlus projimp = new ImagePlus("proj" + imp.getTitle(), zprostack);
		//projimp.setStack(zprostack);
		
		return projimp;				
	}
	
	/** Segmentation of Dots using automatic threshold level coupled with 3D Object Counter
	 * processes 3D stack from each time point separately.<br>
	 *   
	 * @param imp: gray scale 4D stack
	 * @return ImagePlus: duplicated and then processed ImagePlus (binary image)
	 * 
	 */
	public ImagePlus segmentaitonByObjectSize(ImagePlus imp){

		Duplicator bin = new Duplicator();	//this duplication may not be necessary
		ImagePlus binimp = bin.run(imp);		
		int nSlices = imp.getImageStackSize();
		int zframes = imp.getNSlices();
		int tframes = nSlices/zframes;
		double minth = 0.0;        // initializing minimal threshold
		int adjth =0;
		Duplicator dup = new Duplicator();	//this duplication may not be necessary
		ImagePlus impcopy = null;
		int maxth = (int) Math.pow(2,imp.getBitDepth());
		for(int i =0; i<tframes; i++){
			impcopy = dup.run(imp, (i*zframes+1), (i+1)*zframes);
			minth = initializeThresholdLevel(impcopy, 25); //second argument is cutoff pixel area in histogram upper part. (make mym dependent?)
			IJ.log(Integer.toString(i)+": initial threshold set to "+Double.toString(minth));
			adjth = (int) ThresholdAdjusterBy3Dobj(imp, (int)minth, this.thadj_volmin, this.thadj_volmax, this.thadj_nummin, this.thadj_nummax);
			IJ.log("... ... Adjusted to "+Integer.toString(adjth));
			
			for (int j=0; j<zframes; j++)
				binimp.getStack().getProcessor(i*zframes+1+j).threshold(adjth);
		}	
		return binimp;
	}
	
	/**Initializes the threshold value of 3D stack with dark background 
	 * <ul>
	 * <li>1. z-projection by maximum intensity
	 * <li>2. get histogram of z-projection
	 * <li>3. then find a pixel value that the upper area of histogram becomes larger than setting value
	 * </ul>
	 * <br>tried using Shanbhag autothreshold but later suppressed. 
	 * <br> 
	 * @param imp	grayscale 3D stack
	 * @param cutoff_upperArea pixel area of upper part of histogram 
	 * @return
	 */
	public int initializeThresholdLevel(ImagePlus imp, int cutoff_upperArea){
		ZProjector zpimp = new ZProjector(imp);
		zpimp.setMethod(1); //1 is max intensity projection
		zpimp.doProjection();
			//zpimp.getProjection().show();
			//IJ.setAutoThreshold(zpimp.getProjection(), "Shanbhag dark");
			//IJ.setAutoThreshold(zpimp.getProjection(), "Minimum dark");
			//double minth = zpimp.getProjection().getProcessor().getMinThreshold();
		int[] hist = zpimp.getProjection().getProcessor().getHistogram();	//simpler strategy
		int sumpixels =0;
		int i = hist.length-1;
		while (sumpixels < cutoff_upperArea){
			sumpixels += hist[i--];
		}
		return i;
		// here: if there is no "upper area": no dot present!?
	}
	/** Explore different threshold levels to find out an optimum threshold level for segmenting 
	 * 3D dots.<br><br>
	 * <b>updates</b>
	 * <ul>
	 * <li>20101117 added a line to suppress error messages from Counter3D
	 * when no 3D objects were found. 
	 * </ul>
	 * 
	 * @param imp	gray scale 3D stack
	 * @param initTh	initial level of threshold to start with exploration
	 * @param thadj_volmin
	 * @param thadj_volmax
	 * @param thadj_nummin
	 * @param thadj_nummax
	 * @return optimized threshold level for segmentation of 3D dots. 
	 */
	public double ThresholdAdjusterBy3Dobj(ImagePlus imp, 
			int initTh, 
			int thadj_volmin, 
			int thadj_volmax, 
			int thadj_nummin,
			int thadj_nummax){
		// somehow this part causes error in osx
		//TODO implement initial setting more directly
//		IJ.run("3D OC Options", "volume surface nb_of_obj._voxels nb_of_surf._voxels " +
//				"integrated_density mean_gray_value std_dev_gray_value median_gray_value " +
//				"minimum_gray_value maximum_gray_value centroid mean_distance_to_surface " +
//				"std_dev_distance_to_surface median_distance_to_surface centre_of_mass " +
//				"bounding_box dots_size=5 font_size=10 redirect_to=none");
		int localthres = 0;
		Duplicator dup = new Duplicator();	//this duplication may not be necessary
		ImagePlus impcopy = dup.run(imp);
		// check initial condition
		excludeOnEdges = false;
		redirect = false; // this is the option to suppress the showing of masked images??
		Counter3D OC = new Counter3D(impcopy, initTh, minspotvoxels, (int) maxspotvoxels*2, excludeOnEdges, redirect);
		Vector<Object3D> obj = OC.getObjectsList();
		int nobj = obj.size();
		int volumesum=0;
		for (int i=0; i<nobj; i++){
			 Object3D currObj=obj.get(i);
			 volumesum += currObj.size;
		}
		IJ.log("Threshold Adjuster initial th: "+ Integer.toString(initTh) +" ObjNum: "+Integer.toString(nobj)+"Volume Sum: "+Integer.toString(volumesum));
		localthres = initTh;
		int loopcount =0;
//		while ( (((nobj != 2) && (nobj !=4))|| ((volumesum > maxspotvoxels*nobj) 
//				|| (volumesum < minspotvoxels*nobj))) && (loopcount>maxloops)) {
		while ( 
				(nobj < thadj_nummin || nobj > thadj_nummax || volumesum > thadj_volmax || volumesum <thadj_volmin) 
				&& 
				(loopcount<maxloops)
				) {

			if ((nobj<thadj_nummin) && (volumesum < thadj_volmin)) localthres--;
			if ((nobj<thadj_nummin) && (volumesum > thadj_volmax)) localthres++;			
			if ((nobj>thadj_nummax) && (volumesum > thadj_volmax)) localthres--;
			if ((nobj>thadj_nummax) && (volumesum < thadj_volmin)) localthres++;
			if ((nobj >= thadj_nummin) && (nobj <= thadj_nummax)){
				if (volumesum < thadj_volmin) localthres--;
				else localthres++;
			}
			// this part is a bit not clear
			if ((volumesum >= thadj_volmin) && (volumesum <= thadj_volmax)){
				if (nobj < thadj_nummin) localthres++;
				else localthres--;
			}			
			IJ.redirectErrorMessages(true);	//20101117
			OC = new Counter3D(impcopy, localthres, minspotvoxels, (int) (maxspotvoxels*1.5), excludeOnEdges, redirect);
			obj = OC.getObjectsList();
			nobj = obj.size();
			volumesum=0;
			for (int i=0; i<nobj; i++){
				 Object3D currObj=obj.get(i);
				 volumesum += currObj.size;
			}
			loopcount++;
		}
		if (loopcount>0) IJ.log("... New Th="+ Integer.toString(localthres)+" Iter="+Integer.toString(loopcount)+ " ObjNo:"+Integer.toString(nobj)+"Volume Sum:"+Integer.toString(volumesum));
		
		return localthres;
	}
	
	public int measureDots(ImagePlus imp, String chnum, // 
			Vector<Object4D> obj4dv, int zframes) {
		
		int nSlices = imp.getStackSize();
		if (nSlices ==1) return -1;
		//int zframes =8; // TODO
		int tframes = nSlices/zframes;
	
		//IJ.log(Integer.toString(imp.getHeight()));
		ImagePlus imps = null;
		Duplicator singletime = new Duplicator();
		
		thr = 128;
		minSize = minspotvoxels_measure;
		maxSize = 1000;
		excludeOnEdges = false;
		redirect = false;
		
		for (int j=0; j<tframes; j++){
			imps = singletime.run(imp, j*zframes+1, j*zframes+zframes); 
			Counter3D OC=new Counter3D(imps, thr, minSize, maxSize, excludeOnEdges, redirect);
			newRT = true;
			Vector<Object3D> obj = OC.getObjectsList();
			int nobj = obj.size();
			Collections.sort(obj,  new ComparerBysize3D(ComparerBysize3D.DESC));
			for (int i=0; i<nobj; i++){			 
				 Object3D cObj=obj.get(i);
				 obj4d = new Object4D(cObj.size);
				 obj4d.CopyObj3Dto4D(cObj, j, chnum, i+1); //adds additional 4d parameters, timepoint, channel & dotID 
				 obj4dv.add(obj4d);
			 } 
		} 
		return obj4dv.size();
	}

	String LogObject3D(Object3D cObj, int i){
		String opt ="";
		String Cent ="";
		Cent = "("
			+Float.toString(cObj.centroid[0])+","
			+Float.toString(cObj.centroid[1])+","
			+Float.toString(cObj.centroid[2])
			+")";
		opt = "Object"+Integer.toString(i)
		+" vol="+Integer.toString(cObj.size) 
		+ "\t "+Cent
		+" : IntDen"+Float.toString(cObj.int_dens);
		return opt;
	}
	
	
	// method added by Christoph
	public void setSilent(boolean z){
		silent = z;
	}
	
	public void setParameters(){
		
	}
	
	//public Vector<Object4D> getStatistics(){ // would be nicer to have argument int channel here.
	//	return []
	//}
		public Object4D[][] getLinkedArray(){
		return linkedArray;
	}
	
	
	/**
	 * Show Object4D vector in Results window. 
	 * 
	 * @param obj4Dv Vector<Object4D>
	 */
	   public void showStatistics(Vector<Object4D> obj4Dv){
	        ResultsTable rt;        
	        rt=new ResultsTable();	        
	        for (int i=0; i<obj4Dv.size(); i++){
	            if (obj4Dv.get(i).centroid.length > 1){
	            	rt.incrementCounter();
	            	rt.setValue("frame", i, obj4Dv.get(i).timepoint);
	            	rt.setValue("dotID", i, obj4Dv.get(i).dotID);
	            	rt.setValue("Volume", i, obj4Dv.get(i).size);
	            	rt.setValue("x", i, obj4Dv.get(i).centroid[0]);
		            rt.setValue("y", i, obj4Dv.get(i).centroid[1]);
		            rt.setValue("z", i, obj4Dv.get(i).centroid[2]);
		            rt.setValue("Intden", i, obj4Dv.get(i).int_dens);
	            }
	        }
	       
	        rt.show("Statistics_"+obj4Dv.get(0).chnum);     
	    }
	   
	   public void showDistances(Object4D[][] linked){
	        ResultsTable rt;        
	        rt=new ResultsTable();
	        int ct = 0;
	        double ch0ch1dist = -1;
	        for (int i=0; i<linked.length; i++){
	        	for (int j = 0; j < linked[0].length; j+=2){
		        	if ((linked[i][j] != null) && (linked[i][j+1] != null)){
		        		rt.incrementCounter();
		        		ch0ch1dist = returnDistance(linked[i][j], linked[i][j+1]);
		        		rt.setValue("frame", ct, linked[i][j].timepoint);
			            rt.setValue("ch0-ch1_dist", ct, ch0ch1dist);
			            double ch0dist = 0;
			            double ch1dist = 0;
						if (linked[i][3] != null) {
			            	ch0dist = returnDistance(linked[i][0], linked[i][2]);
							ch1dist = returnDistance(linked[i][1], linked[i][3]);
						}
			            rt.setValue("ch0-ch0_dist", ct, ch0dist);
			            rt.setValue("ch1-ch1_dist", ct, ch1dist);
			            rt.setValue("ch0vol", ct, linked[i][j].size);
			            rt.setValue("ch1vol", ct, linked[i][j+1].size);
			            
			            ct++;
		        	}
	        	}
	        }
	       
	        rt.show("Statistics_Distance");     
	    }
	   //for calculating distance from index
	   public double returnDistance(Object4D obj1, Object4D obj2){
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

	   // returns number of dots at single time point
	   int returnDotNumber(Vector<Object4D> obj4D, int timepoint){
		   int counter =0;
		   for (int i=0; i<obj4D.size(); i++){
			   if (obj4D.get(i).timepoint == timepoint) counter++;
		   }
		   return counter;
	   }
	   // dotID could only be 1 or 2 (0 does not exist)
	   Object4D returnObj4D(Vector<Object4D> obj4Dv, int tpoint, int dotID){
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
		   int ch0dots = returnDotNumber(obj4Dch0, tpoint);
		   int ch1dots = returnDotNumber(obj4Dch1, tpoint);
		   Object4D ch0id1  = returnObj4D(obj4Dch0, tpoint, 1);
		   Object4D ch1id1  = returnObj4D(obj4Dch1, tpoint, 1);
		   if (ch0dots == 1) {
			   Object4D ch1id2  = returnObj4D(obj4Dch1, tpoint, 2);
			   double dist1 =returnDistance(ch0id1, ch1id1);
			   double dist2 =returnDistance(ch0id1, ch1id2);
			   if (dist1 < dist2) flag = 1;
			   else flag = 2;
		   } else {
			   Object4D ch0id2  = returnObj4D(obj4Dch0, tpoint, 2);
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
		   Object4D ch0id1  = returnObj4D(obj4Dch0, tpoint, 1);
		   Object4D ch0id2  = returnObj4D(obj4Dch0, tpoint, 2);
		   Object4D ch1id1  = returnObj4D(obj4Dch1, tpoint, 1);
		   Object4D ch1id2  = returnObj4D(obj4Dch1, tpoint, 2);
		   double dist1 = returnDistance(ch0id1, ch1id1) + returnDistance(ch0id2, ch1id2);
		   double dist2 = returnDistance(ch0id1, ch1id2) + returnDistance(ch0id2, ch1id1);
		   if (dist1 < dist2) flag = 1;
		   else flag = 2;		   
		   return flag;
	   }
	   /** Link objects in two channels<br>
	    * <br>
	    * assumes that <b>there is only one or two pairs</b>.<br>
	    * Picks up largest and/or nearest particle first.  
	    * <br>
	    * TODO in one case, dots in different daughter cells were linked. This should be avoided. 
	    */
	   public Object4D[][] dotLinker(Vector<Object4D> obj4Dch0,  Vector<Object4D> obj4Dch1, int tframes){
		   Object4D[][] linked = new Object4D[tframes][4]; 
		   Object4D obj4Dch0id1, obj4Dch1id1;
		   Object4D obj4Dch0id2, obj4Dch1id2;		   
		   int flag = 0;
		   
		   for (int i = 0; i < tframes; i++){
			   obj4Dch0id1 = returnObj4D(obj4Dch0, i, 1);	
			   obj4Dch1id1 = returnObj4D(obj4Dch1, i, 1);
			   obj4Dch0id2 = returnObj4D(obj4Dch0, i, 2);	
			   obj4Dch1id2 = returnObj4D(obj4Dch1, i, 2);
			   if ((obj4Dch0id1 != null) && (obj4Dch1id1 != null)) {
				   
				   // 1x1 case
				   if ((obj4Dch0id2 == null) && (obj4Dch1id2 == null)) { 
					   linked[i][0] = obj4Dch0id1;
					   linked[i][1] = obj4Dch1id1;				   
				   } else {
					   if ((obj4Dch0id2 != null) && (obj4Dch1id2 != null)) { // 2x2 both channels contain multiple dots
						   flag = compare2x2(i);
						   if (flag == 1) {
							   linked[i][0] = obj4Dch0id1;
							   linked[i][1] = obj4Dch1id1;
							   linked[i][2] = obj4Dch0id2;
							   linked[i][3] = obj4Dch1id2;							   
						   } else {
							   linked[i][0] = obj4Dch0id1;
							   linked[i][1] = obj4Dch1id2;
							   linked[i][2] = obj4Dch0id2;
							   linked[i][3] = obj4Dch1id1;
						   }
					   } else {	//2x1 one channel contains only one dots
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
	   
	   public boolean setScale(ImagePlus imp){
		   boolean gotScale = false;
			cal = imp.getCalibration();
			if (Double.isNaN(cal.pixelDepth)) 
				return gotScale;
			calkeep = cal.copy();
			zfactor = cal.pixelDepth / cal.pixelWidth;
			return true;
	   }
	   
	   public double getZfactor(){
		   return zfactor;
	   }
	   public double getXYscale(){
		   return cal.pixelWidth;
	   }
	   public double getZscale(){
		   return cal.pixelDepth;
	   }
}

/** 
* for sorting Object3D Vector, descending order by size (volume)
*/
class ComparerBysize3D implements Comparator<Object3D> {
	public static final int ASC = 1;
	public static final int DESC = -1;
	private int sort = ASC;
	
	public ComparerBysize3D(){
	}
	public ComparerBysize3D(int sort){
		this.sort = sort;
	}	
    public int compare(Object3D o1, Object3D o2) {
    	
        Object3D obj3d1 = (Object3D) o1;
        Object3D obj3d2 = (Object3D) o2;
        int i = 0;
        if (obj3d1.size < obj3d2.size) 
            i = -1*sort;
        if (obj3d1.size == obj3d2.size)
            i = 0;
        if (obj3d1.size > obj3d2.size)
            i = 1*sort;
        return i;
    }
}

/**for sorting Object4D, descending order by score (of none-particle criteria)
 * 
 * @author Miura
 *
 */
class ComparerByscore4D implements Comparator<Object4D> {
	public static final int ASC = 1;
	public static final int DESC = -1;
	private int sort = ASC;
	
	public ComparerByscore4D(){
	}
	
	public ComparerByscore4D(int sort){
		this.sort = sort;
	}	
	
    public int compare(Object4D o1, Object4D o2) {
        Object4D obj4d1 = (Object4D) o1;
        Object4D obj4d2 = (Object4D) o2;
        int i = 0;
        if (obj4d1.score < obj4d2.score) 
            i = -1 * sort;
        if (obj4d1.score == obj4d2.score)
            i = 0;
        if (obj4d1.score > obj4d2.score)
            i = 1 * sort;
        return i;
    }
}


