package emblcmci.foci3Dtracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import Utilities.Counter3D;
import Utilities.Object3D;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.plugin.Duplicator;
import ij.plugin.ZProjector;

/**
 * Implementation of Dot Segmentation for Bory / Christoph's project Moved from
 * main class on 20140930
 * 
 * @author miura
 * 
 */

public class SegmentatonByThresholdAdjust extends Segmentation {
	ImagePlus imp0;
	ImagePlus imp1;
	ArrayList<Object4D> obj4Dch0;
	ArrayList<Object4D> obj4Dch1;

	ParamSetter para = new ParamSetter();
	int maxXYPixels = 25; //@TODO maximal area in px of the dot on z-projection,
							// default 25 px
							// this parameter is not in the Prametersetter yet
	int maxspotvoxels;
	
	/** Object volume minimum for volume-based segmentation */
	int minspotvoxels;
	/**
	 * object volume minimum for measurement (measurement meaning determination
	 * of position!????) (maybe 7 is too small)
	 */
	int minspotvoxels_measure;
	/** maximum loop for exiting optimum threshold searching */
	int maxloops;
	int thadj_volmin, thadj_volmax;
	int thadj_nummin, thadj_nummax;
	int segMethod;

	int thr, minSize, maxSize, dotSize, fontSize; // minSize, maxSize redundant
													// to minspotvoxels_measure,
													// maxspotvoxels,
													// minspotvoxels?
	boolean excludeOnEdges, newRT, redirect;
	boolean showMaskedImg = false;

	public SegmentatonByThresholdAdjust() {
		setStoredParam(para);
	};

	//externally modifying values
	public void setParam(int maxXYPixels, int maxspotvoxels,
			int minspotvoxels, int minspotvoxels_measure, int maxloops,
			int thadj_volmin, int thadj_volmax, int thadj_nummin,
			int thadj_nummax) {
		this.maxXYPixels = maxXYPixels;
		this.maxspotvoxels = maxspotvoxels;
		this.minspotvoxels = minspotvoxels;
		this.minspotvoxels_measure = minspotvoxels_measure;
		this.maxloops = maxloops;
		this.thadj_volmin = thadj_volmin;
		this.thadj_volmax = thadj_volmax;
		this.thadj_nummin = thadj_nummin;
		this.thadj_nummax = thadj_nummax;
		
	}
	
	public void setStoredParam(ParamSetter p) {
		maxXYPixels = p.getMaxXYPixels();
		maxspotvoxels = p.getMaxspotvoxels();
		minspotvoxels = p.getMinspotvoxels();
		minspotvoxels_measure = p.getMinspotvoxels_measure();
		maxloops = p.getMaxloops();
		thadj_volmin = p.getThadj_volmin();
		thadj_volmax = p.getThadj_volmax();
		thadj_nummin = p.getThadj_nummin();
		thadj_nummax = p.getThadj_nummax();
		segMethod = p.getSegMethod();
		IJ.log("min vox segment: " + Integer.toString(minspotvoxels)
				+ "\n min vox measure : "
				+ Integer.toString(minspotvoxels_measure));		
	}

	@Override
	public SegmentatonByThresholdAdjust setComponents(ImagePlus imp0,
			ImagePlus imp1, ArrayList<Object4D> obj4Dch0, ArrayList<Object4D> obj4Dch1) {
		SegmentatonByThresholdAdjust seg = new SegmentatonByThresholdAdjust();
		this.imp0 = imp0;
		this.imp1 = imp1;
		this.obj4Dch0 = obj4Dch0;
		this.obj4Dch1 = obj4Dch1;
		return seg;
	}

//	public void setThresholdAdjParameters(int maxXYPixels, int maxspotvoxels,
//			int minspotvoxels, int minspotvoxels_measure, int maxloops,
//			int thadj_volmin, int thadj_volmax, int thadj_nummin,
//			int thadj_nummax) {
//		this.maxYXPixels = maxXYPixels;
//		this.maxspotvoxels = maxspotvoxels;
//		this.minspotvoxels = minspotvoxels;
//		this.minspotvoxels_measure = minspotvoxels_measure;
//		this.maxloops = maxloops;
//		this.thadj_volmin = thadj_volmin;
//		this.thadj_volmax = thadj_volmax;
//		this.thadj_nummin = thadj_nummin;
//		this.thadj_nummax = thadj_nummax;
//	}

	public Object4D[][] doSegmentation() {
		// auto adjusted threshold segmentation
		this.binimp0 = segmentaitonByObjectSize(this.imp0);
		this.binimp1 = segmentaitonByObjectSize(this.imp1);

		// ImagePlus rgbbin = null;

		// 3D object measurement part
		int ch0objnum = measureDots(this.binimp0, "Ch0", this.obj4Dch0); // measureDots
																			// writes
		int ch1objnum = measureDots(this.binimp1, "Ch1", this.obj4Dch1);

		DotLinker linker = new DotLinker(this.obj4Dch0, this.obj4Dch1,
				this.imp0.getNFrames());

		Object4D[][] linkedArray = linker.linkDots();
		return linkedArray;
	}

	/**
	 * Segmentation of Dots using automatic threshold level coupled with 3D
	 * Object Counter processes 3D stack from each time point separately.<br>
	 * 
	 * @param imp
	 *            : gray scale 4D stack
	 * @return ImagePlus: duplicated and then processed ImagePlus (binary image)
	 * 
	 */
	public ImagePlus segmentaitonByObjectSize(ImagePlus imp) {
		Duplicator bin = new Duplicator(); // this duplication may not be
											// necessary
		ImagePlus binimp = bin.run(imp);
		int nSlices = imp.getNSlices();
		int tframes = imp.getNFrames();
		double minTh = 0.0; // why is this a double here and later to int?
		int adjth = 0;
		Duplicator dup = new Duplicator(); // this duplication may not be
											// necessary
		ImagePlus impcopy = null;
		for (int i = 0; i < tframes; i++) {
			impcopy = dup.run(imp, (i * nSlices + 1), (i + 1) * nSlices);
			/*
			 * on initialize ThresholdLevel: second argument is cutoff pixel
			 * area in histogram upper part. TODO 1. make micron^2 dependent?,
			 * 2. Is there some measurement on which the 25 is based? Moved the
			 * 25 px to "segmentation parameters" this.maxYXPixels
			 */
			minTh = estimateThreshold(impcopy, this.maxXYPixels);
			IJ.log(Integer.toString(i) + ": initial threshold set to "
					+ Double.toString(minTh));
			adjth = (int) ThresholdAdjusterBy3Dobj(imp, (int) minTh,
					this.thadj_volmin, this.thadj_volmax, this.thadj_nummin,
					this.thadj_nummax);
			IJ.log("... ... Adjusted to " + Integer.toString(adjth));
			for (int j = 0; j < nSlices; j++)
				binimp.getStack().getProcessor(i * nSlices + 1 + j)
						.threshold(adjth);
		}
		return binimp;

	}

	/*
	 * Calculate an estimated threshold value for 3D stack with dark background,
	 * based on expected XY area (in pixel) of the object
	 */
	public int estimateThreshold(ImagePlus imp, int XYPixelArea) {
		ZProjector zpimp = new ZProjector(imp);
		zpimp.setMethod(1); // 1 is max intensity projection
		zpimp.doProjection();
		int[] hist = zpimp.getProjection().getProcessor().getHistogram();
		int sumpixels = 0;
		int i = hist.length - 1;
		while (sumpixels < XYPixelArea) {
			sumpixels += hist[i--];
		}
		return i;
	}

	public int measureDots(ImagePlus imp, String chnum, // removed argument
														// zframes, should be in
														// imp format!
			ArrayList<Object4D> obj4dv) {

		int nSlices = imp.getNSlices(); // nSlices: number of z slices
		if (nSlices == 1)
			return -1;
		int nFrames = imp.getNFrames(); // nFrames: number of timeframes

		ImagePlus frameStack = null;
		Duplicator dup = new Duplicator();

		thr = 128; // TODO: where does this number come from? 1/2 8 bit??
		minSize = minspotvoxels_measure;
		maxSize = 1000; // TODO: where does this number come from?
		excludeOnEdges = false;
		redirect = false;

		for (int j = 0; j < nFrames; j++) {
			frameStack = dup.run(imp, j * nSlices + 1, j * nSlices + nSlices);
			Counter3D OC = new Counter3D(frameStack, thr, minSize, maxSize,
					excludeOnEdges, redirect);
			newRT = true;
			Vector<Object3D> obj = OC.getObjectsList();
			int nobj = obj.size();
			Collections.sort(obj, new ComparerBysize3D(ComparerBysize3D.DESC));
			Object4D obj4d;
			for (int i = 0; i < nobj; i++) {
				Object3D cObj = obj.get(i);
				obj4d = new Object4D(cObj.size);
				obj4d.CopyObj3Dto4D(cObj, j, chnum, i + 1); // adds additional
															// 4d parameters,
															// timepoint,
															// channel & dotID
				obj4dv.add(obj4d);
			}
		}
		return obj4dv.size();
	}

	public double ThresholdAdjusterBy3Dobj(ImagePlus imp, int estimatedTh,
			int thadj_volmin, int thadj_volmax, int thadj_nummin,
			int thadj_nummax) {
		int localthres = 0;
		Duplicator dup = new Duplicator(); // this duplication may not be
											// necessary
		ImagePlus impcopy = dup.run(imp);
		// check initial condition
		excludeOnEdges = false;
		redirect = false; // this is the option to suppress the showing of
							// masked images??
		Prefs.set("3D-OC-Options_showMaskedImg.boolean", false); // answer for
																	// Christoph:
																	// this
																	// line.

		Counter3D OC = new Counter3D(imp, estimatedTh, minspotvoxels,
				(int) maxspotvoxels * 2, excludeOnEdges, redirect);
		Vector<Object3D> obj = OC.getObjectsList();
		int nobj = obj.size();
		int volumesum = 0;
		for (int i = 0; i < nobj; i++) {
			Object3D currObj = obj.get(i);
			volumesum += currObj.size;
		}
		IJ.log("Threshold Adjuster initial th: "
				+ Integer.toString(estimatedTh) + " ObjNum: "
				+ Integer.toString(nobj) + "Volume Sum: "
				+ Integer.toString(volumesum));
		localthres = estimatedTh;
		int loopcount = 0;
		while ((nobj < thadj_nummin || nobj > thadj_nummax
				|| volumesum > thadj_volmax || volumesum < thadj_volmin)
				&& (loopcount < maxloops)) {

			if ((nobj < thadj_nummin) && (volumesum < thadj_volmin))
				localthres--;
			if ((nobj < thadj_nummin) && (volumesum > thadj_volmax))
				localthres++;
			if ((nobj > thadj_nummax) && (volumesum > thadj_volmax))
				localthres--;
			if ((nobj > thadj_nummax) && (volumesum < thadj_volmin))
				localthres++;
			if ((nobj >= thadj_nummin) && (nobj <= thadj_nummax)) {
				if (volumesum < thadj_volmin)
					localthres--;
				else
					localthres++;
			}
			// this part is a bit not clear
			if ((volumesum >= thadj_volmin) && (volumesum <= thadj_volmax)) {
				if (nobj < thadj_nummin)
					localthres++;
				else
					localthres--;
			}
			IJ.redirectErrorMessages(true); // 20101117
			OC = new Counter3D(imp, localthres, minspotvoxels,
					(int) (maxspotvoxels * 1.5), excludeOnEdges, redirect);
			obj = OC.getObjectsList();
			nobj = obj.size();
			volumesum = 0;
			for (int i = 0; i < nobj; i++) {
				Object3D currObj = obj.get(i);
				volumesum += currObj.size;
			}
			loopcount++;
		}
		if (loopcount > 0)
			IJ.log("... New Th=" + Integer.toString(localthres) + " Iter="
					+ Integer.toString(loopcount) + " ObjNo:"
					+ Integer.toString(nobj) + "Volume Sum:"
					+ Integer.toString(volumesum));

		return localthres;
	}

}
