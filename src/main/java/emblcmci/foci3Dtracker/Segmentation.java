package emblcmci.foci3Dtracker;

import ij.ImagePlus;
import java.util.Vector;

public abstract class Segmentation {

	public ImagePlus binimp0;
	public ImagePlus binimp1;
	
	abstract Segmentation setComponents(ImagePlus imp0, ImagePlus imp1, Vector<Object4D> obj4Dch0, Vector<Object4D> obj4Dch1);

	abstract public Object4D[][] doSegmentation();
}
