package emblcmci.foci3Dtracker;

import ij.ImagePlus;

import java.util.ArrayList;


public abstract class Segmentation {

	public ImagePlus binimp0;
	public ImagePlus binimp1;
	
	abstract Segmentation setComponents(ImagePlus imp0, ImagePlus imp1, ArrayList<Object4D> obj4Dch0, ArrayList<Object4D> obj4Dch1, double zfactor);

	abstract public ArrayList<FociPair> doSegmentation();
	
}
