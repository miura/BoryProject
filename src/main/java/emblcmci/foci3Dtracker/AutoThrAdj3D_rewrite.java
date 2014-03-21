package emblcmci.foci3Dtracker;

/*Christoph (schiklen@embl.de) rewrites custom version of AutothresholdAdjuster3D from Kota Miura (CMCI, miura@embl.de)
 *
 */

// Java imports
import java.awt.Color;
import java.util.Collections;
import java.util.Vector;

// ImageJ imports
import ij.plugin.Duplicator;
//import ij.plugin.RGBStackMerge;
import ij.plugin.StackCombiner;
import ij.plugin.ZProjector;
import ij.plugin.GroupedZProjector;
import ij.*;
import Utilities.Counter3D;
import Utilities.Object3D;
import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
//import ij.plugin.*;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import emblcmci.foci3Dtracker.DotLinker; // outsourced the dot linking methods into new class for more modularity

public class AutoThrAdj3D_rewrite {

	// Segmentation parameters
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

	public void setParameters(){} // paramsetter object?
	
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
		int ch0objnum = 0; 
		int ch1objnum = 0; 
		ch0objnum = measureDots(binimp0, "Ch0", obj4Dch0); //measureDots writes
		ch1objnum = measureDots(binimp1, "Ch1", obj4Dch1);
		
		DotLinker linker = new DotLinker(obj4Dch0,  obj4Dch1, imp0.getNFrames());
		
		this.linkedArray = linker.linkDots();
		//this.linkedArray = dotLinker(obj4Dch0,  obj4Dch1, imp0.getNFrames());
		
		this.linkedImage = drawlinksGrayscale(this.linkedArray, imp0, imp1);
		
		if (silent == false) {
			showStatistics(obj4Dch0);
			showStatistics(obj4Dch1);
			showDistances(linkedArray);
			}
		
		drawlinksGrayscale(linkedArray, imp0, imp1);
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

	
	//This method calculates the distance between the dots in pixel
	public double returnDistance(Object4D obj1, Object4D obj2){
		double dist = -1.0;
		if ((obj1.centroid.length > 1) && (obj2.centroid.length > 1)) {
			double sqd = (Math.pow( (obj1.centroid[0] - obj2.centroid[0]), 2.0)    // changed 2 to 2.0
					   + Math.pow( (obj1.centroid[1] - obj2.centroid[1]), 2.0) 
					   + Math.pow( (obj1.centroid[2] - obj2.centroid[2]), 2.0)
						 );
			dist = Math.pow(sqd, 0.5);
		   }	
		return dist;
	}
	
	//This method calculates the distance between the dots in microns
	public double returnDistanceMicrons(Object4D obj1, Object4D obj2, Calibration cal){
		double dist = -1.0;
		
		if ((obj1.centroid.length > 1) && (obj2.centroid.length > 1)) {
			double sqd = (Math.pow( cal.getX(obj1.centroid[0] - obj2.centroid[0]), 2.0)    // changed 2 to 2.0
					    + Math.pow( cal.getY(obj1.centroid[1] - obj2.centroid[1]), 2.0) 
					    + Math.pow( cal.getZ(obj1.centroid[2] - obj2.centroid[2]), 2.0)
						 );
			dist = Math.pow(sqd, 0.5);
		   }	
		return dist;
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

	   
	   /**Methods for graphical output */
	   
	   /**plotting linked lines, but with original gray scale image (will be converted to RGB).
		 * 
		 * @param linked
		 * @param imp0
		 * @param imp1
		 * @author Kota Miura
		 * modified by Christoph Schiklenk
		 * @return 
		 */
		public ImagePlus drawlinksGrayscale(Object4D[][] linked, ImagePlus imp0, ImagePlus imp1){
			ImagePlus ch0proj = null;
			ImagePlus ch1proj = null;
			
			GroupedZProjector gzp = new GroupedZProjector();
			ch0proj = gzp.groupZProject(imp0, 1, imp0.getNSlices());
			ch1proj = gzp.groupZProject(imp1, 1, imp1.getNSlices());
			
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
			return combimp;
		}
		
		/**
		 * Show Object4D vector in Results window. 
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
		        rt = new ResultsTable();
		        int ct = 0;
		        double ch0ch1dist = -1;
		        for (int i=0; i<linked.length; i++){
		        	for (int j = 0; j < linked[0].length; j += 2){
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
		   
}
