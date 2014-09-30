package emblcmci.foci3Dtracker;

/** Segmentation of fission yeast chromosome Foci 
 *	use 3D object counter to adjust threshold level of 3D stack. 
 *	3D object counter must be installed in ImageJ
 *	100614 main functions now in separate classes
 * @author Kota Miura  
 * @ cmci, embl miura@embl.de
 */

import java.awt.Rectangle;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.WindowManager;
import ij.plugin.Duplicator;
import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.*;
import ij.process.StackProcessor;

import Utilities.Counter3D;
import Utilities.Object3D;

public class AutoThresholdAdjuster3D {   // there should be a constructor with respective MaxSpotVoxels, MinSpotVoxels, MaxLoops?
	private static boolean createComposite = true;
	int thr, minSize, maxSize, dotSize, fontSize;
	boolean excludeOnEdges, showObj, showSurf, showCentro, showCOM, showNb, whiteNb, newRT, showStat, closeImg, showSummary, redirect;
	boolean silent = false;
	boolean showMaskedImg = false;
	
	ParamSetter para = new ParamSetter();
	int maxXYPixels = 25; // maximal area in px of the dot on z-projection,
	// default 25 px
	
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
	
	/** Vector for storing detected dots in channel 0 */
	Vector<Object4D> obj4Dch0 = new Vector<Object4D>();

	/** Vector for storing detected dots in channel 1 */
	Vector<Object4D> obj4Dch1 = new Vector<Object4D>();
	
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
		// @TODO setting scale should be associated with measurement class
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
		if ((r0 == null) && (r1 == null)){				
			segAndMeasure( imp0, imp1);
			GUIoutputs out = new GUIoutputs(); 
			out.drawResultImages( linkedArray, imp0, imp1, obj4Dch0, obj4Dch1);

		} else {
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
			GUIoutputs out = new GUIoutputs(); 
			out.drawResultImages( linkedArray, imp0, imp1, obj4Dch0, obj4Dch1);
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

		Segmentation seg;
		//auto adjusted threshold segmentation
		if (segMethod == 0) {		
			//binimp0 = segmentaitonByObjectSize(imp0);
			//binimp1 = segmentaitonByObjectSize(imp1);
			seg = new SegmentatonByThresholdAdjust();
			seg.setComponents(imp0, imp1, this.obj4Dch0, this.obj4Dch1);
			((SegmentatonByThresholdAdjust) seg).setThresholdAdjParameters(
					maxXYPixels, maxspotvoxels, minspotvoxels,
					minspotvoxels_measure, maxloops, thadj_volmin, thadj_volmax,
					thadj_nummin, thadj_nummax);			
		}
		else {
			return false;	
		}
		
		ImagePlus rgbbin=null;
		/* in case of particle 3D, no binary images are produced so no composite image. */
		/* temporarily out, 20140930
		if ((segMethod != 2) && (createComposite)) {
			ImagePlus ch0proj=null;
			ImagePlus ch1proj=null;		
			ch0proj = createZprojTimeSeries(seg.binimp0, imp0.getNSlices(), imp0.getNFrames());
			ch1proj = createZprojTimeSeries(seg.binimp1, imp1.getNSlices(), imp1.getNFrames());
			ImageStack dummy = null;
			RGBStackMerge rgbm = new RGBStackMerge();
			ImageStack rgbstack = rgbm.mergeStacks(ch0proj.getWidth(), ch0proj.getHeight(), ch0proj.getStackSize(), ch0proj.getStack(), ch1proj.getStack(), dummy, true);
			rgbbin = new ImagePlus("binProjMerged", rgbstack);
			rgbbin.show();
			
		}
		*/
	
		linkedArray = dotLinker(obj4Dch0,  obj4Dch1, imp0.getNFrames());
		this.linkedArray = seg.doSegmentation();
		
		if (silent == false) {
			GUIoutputs out = new GUIoutputs();
			out.showStatistics(obj4Dch0);
			out.showStatistics(obj4Dch1);
			out.showDistances(linkedArray);
			}
		
		return true; 
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
		   Measure m = new Measure();
		   if (ch0dots == 1) {
			   Object4D ch1id2  = returnObj4D(obj4Dch1, tpoint, 2);
			   double dist1 =m.returnDistanceZfact(ch0id1, ch1id1, zfactor);
			   double dist2 =m.returnDistanceZfact(ch0id1, ch1id2, zfactor);
			   if (dist1 < dist2) flag = 1;
			   else flag = 2;
		   } else {
			   Object4D ch0id2  = returnObj4D(obj4Dch0, tpoint, 2);
			   double dist1 =m.returnDistanceZfact(ch0id1, ch1id1, zfactor);
			   double dist2 =m.returnDistanceZfact(ch0id2, ch1id1, zfactor);
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
		   Measure m = new Measure();
		   double dist1 = m.returnDistanceZfact(ch0id1, ch1id1, zfactor) + m.returnDistanceZfact(ch0id2, ch1id2, zfactor);
		   double dist2 = m.returnDistanceZfact(ch0id1, ch1id2, zfactor) + m.returnDistanceZfact(ch0id2, ch1id1, zfactor);
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
	   
	   public static void main(String[] args){
		   
		   ImagePlus imp0 = IJ.openImage("/Users/miura/Dropbox/people/ChristophSchiklenk/tt/c1pcd.tif");
		   ImagePlus imp1 = IJ.openImage("/Users/miura/Dropbox/people/ChristophSchiklenk/tt/c2pcd.tif");
		   AutoThresholdAdjuster3D ata = new AutoThresholdAdjuster3D();
		   // @TODO setting scale should be associated with measurement class
			if (!ata.setScale(imp0)){
				IJ.error("Voxel Depth(z)is not defined correctly: check [Image -> properties]");
				return;
			}
			ata.segAndMeasure(imp0, imp1);
			
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


