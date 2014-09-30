package emblcmci.foci3Dtracker;

/*Christoph (schiklen@embl.de) rewrites custom version of AutothresholdAdjuster3D from Kota Miura (CMCI, miura@embl.de)
 *
 */

// Java imports
import java.util.Collections;
import java.util.Vector;

// ImageJ imports
import ij.plugin.Duplicator;
//import ij.plugin.RGBStackMerge;
import ij.plugin.ZProjector;
import ij.*;
import Utilities.Counter3D;
import Utilities.Object3D;
import ij.measure.Calibration;
import emblcmci.foci3Dtracker.DotLinker; // outsourced the dot linking methods into new class for more modularity

public class AutoThrAdj3D_rewrite {

	// Segmentation parameters
	ParamSetter para = new ParamSetter();
	int maxYXPixels = 25;   // maximal area in px of the dot on z-projection, default 25 px
	int maxspotvoxels, minspotvoxels;
	int minspotvoxels_measure;
	int maxloops;
	int thadj_volmin, thadj_volmax;
	int thadj_nummin, thadj_nummax;

	private static boolean createComposite = true;
	int thr, minSize, maxSize, dotSize, fontSize;  // minSize, maxSize redundant to minspotvoxels_measure, maxspotvoxels, minspotvoxels?
	boolean excludeOnEdges, newRT, redirect;
	boolean silent = false;
	boolean showMaskedImg = false;

	/** Object4D extends Object3D by timepoint, channel, dotID */
	Object4D obj4d;

	/** Vector for storing detected dots in channel 0 */
	Vector<Object4D> obj4Dch0; 

	/** Vector for storing detected dots in channel 1 */
	Vector<Object4D> obj4Dch1; 

	/** Array for linked 4D objects, field variable to store the results of linking process */
	Object4D[][] linkedArray; 

	/** z-Projection image of detected dots */
	ImagePlus linkedImage;

	Calibration cal, calkeep;
	int segMethod = para.getSegMethod();
	private double zfactor;

	/*Constructors*/
	//Empty constructor
	public AutoThrAdj3D_rewrite(){} // do I really need  or can I construct without arguments anyway?!?

	//Constructor with segmentation parameters as arguments
	public AutoThrAdj3D_rewrite(int maxXYPixels,
		int maxspotvoxels, 
		int minspotvoxels,
		int minspotvoxels_measure,
		int maxloops,
		int thadj_volmin,
		int thadj_volmax,
		int thadj_nummin,
		int thadj_nummax){
		this.maxYXPixels = maxXYPixels;
		this.maxspotvoxels = maxspotvoxels;
		this.minspotvoxels = minspotvoxels;
		this.minspotvoxels_measure = minspotvoxels_measure;
		this.maxloops = maxloops;
		this.thadj_volmin = thadj_volmin;
		this.thadj_volmax = thadj_volmax;
		this.thadj_nummin = thadj_nummin;
		this.thadj_nummax = thadj_nummax;
	}

	/*Methods*/

	//public void setParameters(){} // paramsetter object?
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

	public void setSilent(boolean z) {
		this.silent = z;
	}

	// Just a convenient method to later get the segmentation parameters with one line.
	public int[] getParameters(){
		int[] parameters = { 
			this.maxspotvoxels, this.minspotvoxels, this.minspotvoxels_measure,
			this.maxloops,
			this.thadj_volmin, this.thadj_volmax,
			this.thadj_nummin, this.thadj_nummax
		};
		return parameters;
	}

	public Object4D[][] getLinkedArray(){
		return linkedArray;
	}

	public ImagePlus getLinkedImp(){
		return this.linkedImage;
	}

	// - - - - - - - - - - S E G M E T H O D S - - - - - - - - - - - - - -
	/* 
	 * Methods that do the actual segmentation
	 */
	public boolean segAndMeasure(ImagePlus imp0, ImagePlus imp1){
		ImagePlus binimp0, binimp1;
		obj4Dch0 = new Vector<Object4D>();
		obj4Dch1 = new Vector<Object4D>();
		//auto adjusted threshold segmentation	
		binimp0 = segmentaitonByObjectSize(imp0);
		binimp1 = segmentaitonByObjectSize(imp1);

		//ImagePlus rgbbin = null;

		//3D object measurement part
		int ch0objnum = measureDots(binimp0, "Ch0", obj4Dch0); //measureDots writes
		int ch1objnum = measureDots(binimp1, "Ch1", obj4Dch1);

		DotLinker linker = new DotLinker(obj4Dch0,  obj4Dch1, imp0.getNFrames());

		this.linkedArray = linker.linkDots();
		//this.linkedArray = dotLinker(obj4Dch0,  obj4Dch1, imp0.getNFrames());

		GUIoutputs out = new GUIoutputs();
		this.linkedImage = out.drawlinksGrayscale(this.linkedArray, imp0, imp1);

		if (silent == false) {
			out.showStatistics(obj4Dch0);
			out.showStatistics(obj4Dch1);
			out.showDistances(linkedArray);
		}

		out.drawlinksGrayscale(linkedArray, imp0, imp1);
		//plotDetectedDots(obj4Dch0, imp0, Color.yellow);
		//plotDetectedDots(obj4Dch1, imp1, Color.red);
		return true; 
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
		int nSlices = imp.getNSlices();
		int tframes = imp.getNFrames();
		double minTh = 0.0; // why is this a double here and later to int?
		int adjth = 0;
		Duplicator dup = new Duplicator();	//this duplication may not be necessary
		ImagePlus impcopy = null;
		for(int i = 0; i<tframes; i++){
			impcopy = dup.run(imp, (i*nSlices+1), (i+1)*nSlices);
			/*on initialize ThresholdLevel: second argument is cutoff pixel area in histogram upper part. 
			  TODO 1. make micron^2 dependent?, 2. Is there some measurement on which the 25 is based?
			  Moved the 25 px to "segmentation parameters" this.maxYXPixels*/
			minTh = estimateThreshold(impcopy, this.maxYXPixels);
			IJ.log(Integer.toString(i) + ": initial threshold set to " + Double.toString(minTh));
			adjth = (int) ThresholdAdjusterBy3Dobj(imp, (int)minTh, this.thadj_volmin, this.thadj_volmax, this.thadj_nummin, this.thadj_nummax);
			IJ.log("... ... Adjusted to " + Integer.toString(adjth));
			for (int j=0; j<nSlices; j++)
				binimp.getStack().getProcessor(i*nSlices+1+j).threshold(adjth);
		}
		return binimp;
	}


	/* Calculate an estimated threshold value for 3D stack with dark background,
	   based on expected XY area (in pixel) of the object */
	public int estimateThreshold(ImagePlus imp, int XYPixelArea){
		ZProjector zpimp = new ZProjector(imp);
		zpimp.setMethod(1); //1 is max intensity projection
		zpimp.doProjection();
		int[] hist = zpimp.getProjection().getProcessor().getHistogram();
		int sumpixels = 0;
		int i = hist.length-1;
		while (sumpixels < XYPixelArea){
			sumpixels += hist[i--];
		}
		return i;
	}

	public double ThresholdAdjusterBy3Dobj(ImagePlus imp, 
			int estimatedTh,
			int thadj_volmin, 
			int thadj_volmax, 
			int thadj_nummin,
			int thadj_nummax){
		int localthres = 0;
		Duplicator dup = new Duplicator();	//this duplication may not be necessary
		ImagePlus impcopy = dup.run(imp);
		// check initial condition
		excludeOnEdges = false;
		redirect = false; // this is the option to suppress the showing of masked images??
		Prefs.set("3D-OC-Options_showMaskedImg.boolean",false); // answer for Christoph: this line.

		Counter3D OC = new Counter3D(imp, estimatedTh, minspotvoxels, (int) maxspotvoxels*2, excludeOnEdges, redirect);
		Vector<Object3D> obj = OC.getObjectsList();
		int nobj = obj.size();
		int volumesum=0;
		for (int i=0; i<nobj; i++){
			Object3D currObj=obj.get(i);
			volumesum += currObj.size;
		}
		IJ.log("Threshold Adjuster initial th: "+ Integer.toString(estimatedTh) +" ObjNum: "+Integer.toString(nobj)+"Volume Sum: "+Integer.toString(volumesum));
		localthres = estimatedTh;
		int loopcount =0;
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
			OC = new Counter3D(imp, localthres, minspotvoxels, (int) (maxspotvoxels*1.5), excludeOnEdges, redirect);
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




	public int measureDots(ImagePlus imp, String chnum, // removed argument zframes, should be in imp format!
			Vector<Object4D> obj4dv) {

		int nSlices = imp.getNSlices();  //nSlices: number of z slices
		if (nSlices ==1) return -1;			
		int nFrames = imp.getNFrames();  //nFrames: number of timeframes

		ImagePlus frameStack = null;
		Duplicator dup = new Duplicator();

		thr = 128; // TODO: where does this number come from? 1/2 8 bit??
		minSize = minspotvoxels_measure;
		maxSize = 1000;  // TODO: where does this number come from?
		excludeOnEdges = false;
		redirect = false;

		for (int j=0; j<nFrames; j++){
			frameStack = dup.run(imp, j*nSlices+1, j*nSlices+nSlices); 
			Counter3D OC = new Counter3D(frameStack, thr, minSize, maxSize, excludeOnEdges, redirect);
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

	   public boolean setScale(ImagePlus imp){
		   boolean gotScale = false;
			cal = imp.getCalibration();
			if (Double.isNaN(cal.pixelDepth)) 
				return gotScale;
			calkeep = cal.copy();
			zfactor = cal.pixelDepth / cal.pixelWidth;
			return true;
	   }

	   /**
	    * For debugging
	    * @param args
	    */
	   public static void main(String[] args){
		   
		   ImagePlus imp0 = IJ.openImage("/Users/miura/Dropbox/people/ChristophSchiklenk/tt/c1pcd.tif");
		   ImagePlus imp1 = IJ.openImage("/Users/miura/Dropbox/people/ChristophSchiklenk/tt/c2pcd.tif");
		   AutoThrAdj3D_rewrite ata = new AutoThrAdj3D_rewrite();
		   // @TODO setting scale should be associated with measurement class
			if (!ata.setScale(imp0)){
				IJ.error("Voxel Depth(z)is not defined correctly: check [Image -> properties]");
				return;
			}
			ata.segAndMeasure(imp0, imp1);
			
	   }	


}
