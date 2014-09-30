package emblcmci.foci3Dtracker;

/*Christoph (schiklen@embl.de) rewrites custom version of AutothresholdAdjuster3D from Kota Miura (CMCI, miura@embl.de)
 *
 */

// Java imports
import java.util.Vector;

// ImageJ imports
import ij.*;
import ij.measure.Calibration;

public class AutoThrAdj3D_rewrite {

	// Segmentation parameters
	ParamSetter para = new ParamSetter();

	boolean silent = false;

	/** Object4D extends Object3D by timepoint, channel, dotID */
	Object4D obj4d;

	/** Vector for storing detected dots in channel 0 */
	Vector<Object4D> obj4Dch0 = new Vector<Object4D>();

	/** Vector for storing detected dots in channel 1 */
	Vector<Object4D> obj4Dch1 = new Vector<Object4D>();

	/**
	 * Array for linked 4D objects, field variable to store the results of
	 * linking process
	 */
	Object4D[][] linkedArray;

	/** z-Projection image of detected dots */
	ImagePlus linkedImage;

	Calibration cal, calkeep;
	int segMethod = para.getSegMethod();
	private double zfactor;

	Segmentation segmenter;

	/* Constructors */
	// Empty constructor
	public AutoThrAdj3D_rewrite() {
	} // do I really need or can I construct without arguments anyway?!?

	// Constructor with segmentation parameters as arguments
	public AutoThrAdj3D_rewrite(int maxXYPixels, int maxspotvoxels,
			int minspotvoxels, int minspotvoxels_measure, int maxloops,
			int thadj_volmin, int thadj_volmax, int thadj_nummin,
			int thadj_nummax) {
		para.setMaxspotvoxels(maxspotvoxels);
		para.setMinspotvoxels(minspotvoxels);
		para.setMinspotvoxels_measure(minspotvoxels_measure);
		para.setMaxloops(maxloops);
		para.setThadj_volmin(thadj_volmin);
		para.setThadj_volmax(thadj_volmax);
		para.setThadj_nummin(thadj_nummin);
		para.setThadj_nummax(thadj_nummax);
		
	}
	/* Methods */

	public void setSilent(boolean z) {
		this.silent = z;
	}
/* this should be rewritten
	// Just a convenient method to later get the segmentation parameters with
	// one line.
	public int[] getParameters() {
		int[] parameters = { this.maxspotvoxels, this.minspotvoxels,
				this.minspotvoxels_measure, this.maxloops, this.thadj_volmin,
				this.thadj_volmax, this.thadj_nummin, this.thadj_nummax };
		return parameters;
	}
*/
	
	public Object4D[][] getLinkedArray() {
		return linkedArray;
	}

	public ImagePlus getLinkedImp() {
		return this.linkedImage;
	}

	// - - - - - - - - - - S E G M E T H O D S - - - - - - - - - - - - - -
	/*
	 * Methods that do the actual segmentation
	 */
	public boolean segAndMeasure(ImagePlus imp0, ImagePlus imp1) {
		// this.linkedArray = dotLinker(obj4Dch0, obj4Dch1, imp0.getNFrames());

		Segmentation seg = new SegmentatonByThresholdAdjust();
		seg.setComponents(imp0, imp1, obj4Dch0, obj4Dch1);
		this.linkedArray = seg.doSegmentation();
		GUIoutputs out = new GUIoutputs();
		this.linkedImage = out.drawlinksGrayscale(this.linkedArray, imp0, imp1);

		if (silent == false) {
			out.showStatistics(obj4Dch0);
			out.showStatistics(obj4Dch1);
			out.showDistances(linkedArray);
			this.linkedImage.show();
		}

		// out.drawlinksGrayscale(linkedArray, imp0, imp1);
		// out.plotDetectedDots(obj4Dch0, imp0, Color.yellow);
		// out.plotDetectedDots(obj4Dch1, imp1, Color.red);
		return true;
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

	/**
	 * For debugging
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		ImagePlus imp0 = IJ
				.openImage("/Users/miura/Dropbox/people/ChristophSchiklenk/tt/c1pcd.tif");
		ImagePlus imp1 = IJ
				.openImage("/Users/miura/Dropbox/people/ChristophSchiklenk/tt/c2pcd.tif");
		AutoThrAdj3D_rewrite ata = new AutoThrAdj3D_rewrite();
		// @TODO setting scale should be associated with measurement class
		if (!ata.setScale(imp0)) {
			IJ.error("Voxel Depth(z)is not defined correctly: check [Image -> properties]");
			return;
		}
		ata.segAndMeasure(imp0, imp1);

	}

}
