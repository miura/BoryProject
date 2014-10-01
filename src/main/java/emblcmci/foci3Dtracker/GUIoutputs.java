package emblcmci.foci3Dtracker;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.GroupedZProjector;
import ij.plugin.RGBStackMerge;
import ij.plugin.StackCombiner;
import ij.plugin.ZProjector;
import ij.process.ImageProcessor;
import ij.process.StackConverter;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Vector;

import Utilities.Object3D;

/**Methods for graphical output 
 *   moved from main classes
 * */

public class GUIoutputs {

	// added on 20140926
	public void drawResultImages(ArrayList<FociPair> linkedArray, 
			ImagePlus imp0,
			ImagePlus imp1, 
			ArrayList<Object4D> obj4Dch0,
			ArrayList<Object4D> obj4Dch1) {
		ImagePlus linkplotstack = drawlinksGrayscale(linkedArray, imp0, imp1);
		linkplotstack.show();
		//plotDetectedDots(obj4Dch0, imp0, Color.yellow);
		//plotDetectedDots(obj4Dch1, imp1, Color.red);

	}
	/**plotting linked lines, but with original gray scale image (will be converted to RGB).
	 * 
	 * @param linked
	 * @param imp0
	 * @param imp1
	 * @author Kota Miura
	 * modified by Christoph Schiklenk
	 * @return 
	 */
	public ImagePlus drawlinksGrayscale(ArrayList<FociPair> linked,
			ImagePlus imp0, ImagePlus imp1) {
		ImagePlus ch0proj = null;
		ImagePlus ch1proj = null;

		GroupedZProjector gzp = new GroupedZProjector();
		ch0proj = gzp.groupZProject(imp0, 1, imp0.getNSlices());
		ch1proj = gzp.groupZProject(imp1, 1, imp1.getNSlices());

		new StackConverter(ch0proj).convertToRGB();
		new StackConverter(ch1proj).convertToRGB();

		int offset = 0;
		int ch0x, ch0y, ch1x, ch1y;
		for (FociPair pair : linked) {
			if (pair != null) {
				ch0x = Math.round(pair.ch0dot.centroid[0] - offset);
				ch0y = Math.round(pair.ch0dot.centroid[1] - offset);
				ch1x = Math.round(pair.ch1dot.centroid[0] - offset);
				ch1y = Math.round(pair.ch1dot.centroid[1] - offset);
				ImageProcessor ip0 = ch0proj.getStack().getProcessor(
						pair.timepoint + 1);
				ip0.setColor(Color.blue);
				ip0.drawLine(ch0x, ch0y, ch1x, ch1y);
				ip0.setColor(Color.yellow);
				ip0.drawPixel(ch0x, ch0y);
				ip0.setColor(Color.red);
				ip0.drawPixel(ch1x, ch1y);
				ImageProcessor ip1 = ch1proj.getStack().getProcessor(
						pair.timepoint + 1);
				ip1.setColor(Color.blue);
				ip1.drawLine(ch0x, ch0y, ch1x, ch1y);
				ip1.setColor(Color.yellow);
				ip1.drawPixel(ch0x, ch0y);
				ip1.setColor(Color.red);
				ip1.drawPixel(ch1x, ch1y);
			}
		}
		ImageStack combined = new StackCombiner().combineHorizontally(
				ch0proj.getStack(), ch1proj.getStack());
		ImagePlus combimp = new ImagePlus("DetectedDots", combined);
		return combimp;
	}

	public void showCompositeBinary(SegmentatonByThresholdAdjust seg){
		ImagePlus binimp0 = seg.binimp0;
		ImagePlus binimp1 = seg.binimp1;
		ImagePlus ch0proj = createZprojTimeSeries(binimp0, binimp0.getNSlices(),binimp0.getNFrames());
		ImagePlus ch1proj = createZprojTimeSeries(binimp1, binimp1.getNSlices(), binimp1.getNFrames());
		RGBStackMerge rgbm = new RGBStackMerge();
		ImageStack dummy = null;
		ImageStack rgbstack = rgbm.mergeStacks(
				ch0proj.getWidth(), 
				ch0proj.getHeight(),
				ch0proj.getStackSize(), 
				ch0proj.getStack(), 
				ch1proj.getStack(),
				dummy, true);
		ImagePlus rgbbin = new ImagePlus("binProjMerged", rgbstack);
		rgbbin.show();
	}
	
	/**
	 * Show Object4D vector in Results window. 
	 * @param obj4Dv Vector<Object4D>
	 */
	public void showStatistics(ArrayList<Object4D> obj4Dv){
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

	public void showDistances(ArrayList<FociPair> linked){
		ResultsTable rt;        
		rt = new ResultsTable();
		int ct = 0;
		double ch0ch1dist = -1;
        Measure m = new Measure();
		for (FociPair pair : linked) {

			if ((pair.ch0dot != null) && (pair.ch1dot != null)) {
				rt.incrementCounter();
				// ch0ch1dist = m.returnDistance(linked[i][j], linked[i][j+1]);
				ch0ch1dist = pair.distance;
				rt.setValue("frame", ct, pair.timepoint);
				rt.setValue("ch0-ch1_dist", ct, ch0ch1dist);
				//if (pair.distanceToOtherPairsch0.size() > 0){
				for (Double dist : pair.distanceToOtherPairsch0)
					rt.setValue("ch0-ch0_dist", ct, dist);
				for (Double dist : pair.distanceToOtherPairsch1)
					rt.setValue("ch1-ch1_dist", ct, dist);					
				//}
				rt.setValue("ch0vol", ct, pair.ch0dot.size);
				rt.setValue("ch1vol", ct, pair.ch1dot.size);
				ct++;
			}
		}

		rt.show("Statistics_Distance");  
	}

	/* for plotting Object4Ds detected by segmentation. 
	 * Creates a new RGB 
	 * imp  grayscale image
	 * Very primitive, so not much used anymore as of 201409
	 */
	public void plotDetectedDots(ArrayList<Object4D> obj4dv, ImagePlus imp, Color color){
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

	
// Form here, old guys, maybe delete. 
	
	/**
	 * Show Object4D vector in Results window. 
	 * Old
	 * @param obj4Dv Vector<Object4D>
	 */
	   public void showStatisticsKota(Vector<Object4D> obj4Dv){
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
	   
	   //old
	   public void showDistancesKota(Object4D[][] linked){
	        ResultsTable rt;        
	        rt=new ResultsTable();
	        int ct = 0;
	        double ch0ch1dist = -1;
	        Measure m = new Measure();
	        for (int i=0; i<linked.length; i++){
	        	for (int j = 0; j < linked[0].length; j+=2){
		        	if ((linked[i][j] != null) && (linked[i][j+1] != null)){
		        		rt.incrementCounter();
		        		ch0ch1dist = m.returnDistance(linked[i][j], linked[i][j+1]);
		        		rt.setValue("frame", ct, linked[i][j].timepoint);
			            rt.setValue("ch0-ch1_dist", ct, ch0ch1dist);
			            double ch0dist = 0;
			            double ch1dist = 0;
						if (linked[i][3] != null) {
			            	ch0dist = m.returnDistance(linked[i][0], linked[i][2]);
							ch1dist = m.returnDistance(linked[i][1], linked[i][3]);
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
		/**plotting linked lines, but with original gray scale image (will be converted to RGB).
		 * Old, Obsolete
		 * 
		 * @param linked
		 * @param imp0
		 * @param imp1
		 * @author Kota Miura
		 */
		/*
		public void drawlinksGrayscaleKota(Object4D[][] linked, ImagePlus imp0, ImagePlus imp1){
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
	*/	   
}
