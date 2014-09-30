package emblcmci.foci3Dtracker;

/** Segmentation of fission yeast chromosome Foci 
 *	use 3D object counter to adjust threshold level of 3D stack. 
 *	3D object counter must be installed in ImageJ
 *	100614 main functions now in separate classes
 * @author Kota Miura  
 * @ cmci, embl miura@embl.de
 */

import java.awt.Rectangle;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.plugin.Duplicator;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.process.StackProcessor;

public class AutoThresholdAdjuster3D { // there should be a constructor with
										// respective MaxSpotVoxels,
										// MinSpotVoxels, MaxLoops?
	private static boolean createComposite = true;
	boolean silent = false;
	boolean showMaskedImg = false;

	ParamSetter para = new ParamSetter();

	int segMethod = para.getSegMethod();

	String fullpathtoTrainedData0 = para.getTrainedDataFullPath0();

	String fullpathtoTrainedData1 = para.getTrainedDataFullPath1();

	/**
	 * extended class of Object3D object3D + timepoint, channel, dotID
	 * */
	Object4D obj4d; // Object3D added with time point and channel number fields.

	/** Vector for storing detected dots in channel 0 */
	ArrayList<Object4D> obj4Dch0 = new ArrayList<Object4D>();

	/** Vector for storing detected dots in channel 1 */
	ArrayList<Object4D> obj4Dch1 = new ArrayList<Object4D>();

	/** Array for linked 4D objects, field variable to store the results of */
	Object4D[][] linkedArray;

	Calibration cal, calkeep;

	/**
	 * Factor to multiply for depth, to correct for xy pixel scale =1
	 * 
	 */
	double zfactor;

	public void run() {
		// ** get a list of opened windows.
		// copied and modified from image - color merge... (RGBStackMerge.java)
		int[] wList = WindowManager.getIDList();
		if (wList == null) {
			IJ.error("bory dot analysis", "No images are open.");
			return;
		}

		String[] titles = new String[wList.length + 1];
		for (int i = 0; i < wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			titles[i] = imp != null ? imp.getTitle() : "";
		}
		String none = "*None*";
		titles[wList.length] = none;

		// ** dialog for selecting image stack for each channel
		GenericDialog gd = new GenericDialog("Bory Dot Analysis");
		gd.addChoice("Ch0:", titles, titles[0]);
		gd.addChoice("Ch1:", titles, titles[1]);
		// String title3 = titles.length>2&&!IJ.macroRunning()?titles[2]:none;
		gd.addCheckbox("Create Merged Binary", createComposite);
		// gd.addCheckbox("Keep Source Images", false);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		int[] index = new int[2];
		index[0] = gd.getNextChoiceIndex();
		index[1] = gd.getNextChoiceIndex();
		createComposite = gd.getNextBoolean();
		ImagePlus imp0 = WindowManager.getImage(wList[index[0]]);
		ImagePlus imp1 = WindowManager.getImage(wList[index[1]]);
		// cal.set

		// ** check if selected windows are stack
		if (imp0 == null)
			return;
		if (imp0.getStackSize() == 1) {
			IJ.error("Channel 0 is not a stack");
			return;
		}
		if (imp1.getStackSize() == 1) {
			IJ.error("Channel 1 is not a stack");
			return;
		}
		// @TODO 20140930 setting scale should be associated with measurement class
		if (!setScale(imp0)) {
			IJ.error("Voxel Depth(z)is not defined correctly: check [Image -> properties]");
			return;
		}


		Roi r0 = imp0.getRoi();
		Roi r1 = imp1.getRoi();
		Roi r = null;

		// ** works both with and without ROI.
		// in case of ROI selected, that portion is cropped.
		// ... then start of segmentation and measurements
		if ((r0 == null) && (r1 == null)) {
			segAndMeasure(imp0, imp1);
			GUIoutputs out = new GUIoutputs();
			out.drawResultImages(linkedArray, imp0, imp1, obj4Dch0, obj4Dch1);

		} else {
			ImagePlus imp0roi = null;
			ImagePlus imp1roi = null;
			IJ.log("... ROI found ... ");
			if (r0 == null)
				r = r1;
			else
				// if (r1 == null)
				r = r0;
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
			tempstackproc = new StackProcessor(tempdup.getStack(), tempdup
					.getStack().getProcessor(1));
			cropstack = tempstackproc.crop(rb.x, rb.y, rb.width, rb.height);
			imp0roi = new ImagePlus("croppedCh0", cropstack);
			imp0roi.setCalibration(cal);
			imp0roi.setDimensions(imp0.getNChannels(), imp0.getNSlices(),
					imp0.getNFrames());
			if (silent == false) {
				imp0roi.show();
			}

			tempdup = new Duplicator().run(imp1);
			tempstackproc = new StackProcessor(tempdup.getStack(), tempdup
					.getStack().getProcessor(1));
			cropstack = tempstackproc.crop(rb.x, rb.y, rb.width, rb.height);
			imp1roi = new ImagePlus("croppedCh1", cropstack);
			imp1roi.setCalibration(cal);
			imp1roi.setDimensions(imp1.getNChannels(), imp1.getNSlices(),
					imp1.getNFrames());
			if (silent == false) {
				imp1roi.show();
			}

			segAndMeasure(imp0roi, imp1roi);
			GUIoutputs out = new GUIoutputs();
			out.drawResultImages(linkedArray, imp0, imp1, obj4Dch0, obj4Dch1);
		}
	}

	/**
	 * Three different methods for segmentation of dots, then measure dot-dot
	 * distance time course. <br>
	 * currently,
	 * <ul>
	 * 1. automatic threshold adjustment coupled with 3Dobject counter is used.
	 * <br>
	 * 2. using trained data and trainable segmentation <br>
	 * 3. using segmentation module of particle tracker 3D (not implemented
	 * yet).
	 * </ul>
	 * 
	 * @param imp0
	 *            channel 1 4D stack
	 * @param imp1
	 *            channel 2 4D stack
	 * @return
	 */
	public boolean segAndMeasure(ImagePlus imp0, ImagePlus imp1) {

		Segmentation seg;
		// auto adjusted threshold segmentation
		if (segMethod == 0) {
			// binimp0 = segmentaitonByObjectSize(imp0);
			// binimp1 = segmentaitonByObjectSize(imp1);
			seg = new SegmentatonByThresholdAdjust();
			seg.setComponents(imp0, imp1, this.obj4Dch0, this.obj4Dch1);

		} else {
			return false;
		}

		
		/*
		 * in case of particle 3D, no binary images are produced so no composite
		 * image.
		 */
		/* ImagePlus rgbbin = null;
		 * temporarily out, 20140930 
		 * if ((segMethod != 2) && (createComposite)){ 
		 * ImagePlus ch0proj=null; 
		 * ImagePlus ch1proj=null; 
		 * ch0proj = createZprojTimeSeries(seg.binimp0, imp0.getNSlices(),imp0.getNFrames()); 
		 * ch1proj = createZprojTimeSeries(seg.binimp1, imp1.getNSlices(), imp1.getNFrames()); 
		 * ImageStack dummy = null;
		 * RGBStackMerge rgbm = new RGBStackMerge(); 
		 * ImageStack rgbstack = rgbm.mergeStacks(ch0proj.getWidth(), ch0proj.getHeight(),
		 * ch0proj.getStackSize(), ch0proj.getStack(), ch1proj.getStack(),
		 * dummy, true); 
		 * rgbbin = new ImagePlus("binProjMerged", rgbstack);
		 * rgbbin.show();
		 * 
		 * }
		 */

		// linkedArray = dotLinker(obj4Dch0, obj4Dch1, imp0.getNFrames());
		this.linkedArray = seg.doSegmentation();

		if (silent == false) {
			GUIoutputs out = new GUIoutputs();
			out.showStatistics(obj4Dch0);
			out.showStatistics(obj4Dch1);
			out.showDistances(linkedArray);
		}

		return true;
	}

	// method added by Christoph
	public void setSilent(boolean z) {
		silent = z;
	}

	public void setParameters() {

	}

	// public Vector<Object4D> getStatistics(){ // would be nicer to have
	// argument int channel here.
	// return []
	// }
	public Object4D[][] getLinkedArray() {
		return linkedArray;
	}

	public boolean setScale(ImagePlus imp) {
		boolean gotScale = false;
		cal = imp.getCalibration();
		if (Double.isNaN(cal.pixelDepth))
			return gotScale;
		calkeep = cal.copy();
		zfactor = cal.pixelDepth / cal.pixelWidth;
		return true;
	}

	public double getZfactor() {
		return zfactor;
	}

	public double getXYscale() {
		return cal.pixelWidth;
	}

	public double getZscale() {
		return cal.pixelDepth;
	}

	public static void main(String[] args) {

		ImagePlus imp0 = IJ
				.openImage("/Users/miura/Dropbox/people/ChristophSchiklenk/tt/c1pcd.tif");
		ImagePlus imp1 = IJ
				.openImage("/Users/miura/Dropbox/people/ChristophSchiklenk/tt/c2pcd.tif");
		AutoThresholdAdjuster3D ata = new AutoThresholdAdjuster3D();
		// @TODO setting scale should be associated with measurement class
		if (!ata.setScale(imp0)) {
			IJ.error("Voxel Depth(z)is not defined correctly: check [Image -> properties]");
			return;
		}
		ata.segAndMeasure(imp0, imp1);

	}
}


