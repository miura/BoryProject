package emblcmci.foci3Dtracker;

import ij.ImagePlus;
import java.util.Vector;

public abstract class Segmentation {

	abstract Segmentation setComponents(ImagePlus imp0, ImagePlus imp1, Vector<Object4D> obj4Dch0, Vector<Object4D> obj4Dch1);
	
	abstract void setParameters();

	abstract public Object4D[][] doSegmentation();
}
