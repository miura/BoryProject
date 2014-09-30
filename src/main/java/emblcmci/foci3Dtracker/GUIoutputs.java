package emblcmci.foci3Dtracker;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.GroupedZProjector;
import ij.plugin.StackCombiner;
import ij.process.ImageProcessor;
import ij.process.StackConverter;

import java.awt.Color;
import java.util.Vector;

/**Methods for graphical output 
 *   moved from main classes
 * */

public class GUIoutputs {

	// added on 20140926
	public void drawResultImages(
			Object4D[][] linkedArray, 
			ImagePlus imp0, 
			ImagePlus imp1,
			Vector<Object4D> obj4Dch0, 
			Vector<Object4D> obj4Dch1){
		drawlinksGrayscale(linkedArray, imp0, imp1);
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
        Measure m = new Measure();
		for (int i=0; i<linked.length; i++){
			for (int j = 0; j < linked[0].length; j += 2){
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

	/* for plotting Object4Ds detected by segmentation. 
	 * Creates a new RGB 
	 * imp  grayscale image
	 */
	public void plotDetectedDots(Vector<Object4D> obj4dv, ImagePlus imp, Color color){
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