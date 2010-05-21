package emblcmci;

/*use 3D object counter to adjust threshold level of 3D stack. 
 * 
 * 
 */
import java.util.ArrayList;
import java.util.Vector;
import ij.plugin.Duplicator;
import ij.*;
import Utilities.Counter3D;
import Utilities.Object3D;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.*;
import ij.process.ImageProcessor;

public class AutoThresholdAdjuster3D_ implements PlugIn {

	private static boolean createComposite = true;
	
	int thr, minSize, maxSize, dotSize, fontSize;
	boolean excludeOnEdges, showObj, showSurf, showCentro, showCOM, showNb, whiteNb, newRT, showStat, showMaskedImg, closeImg, showSummary, redirect;

	int maxspotvoxels = 300000000;
	int minspotvoxels = 3;
	int maxloops =50;	// maximum loop for optimum threshold searching
	
	public void run(String arg) {
		//ImagePlus imp;
		//imp = ij.WindowManager.getCurrentImage();

		//copied and modifed from image - color merge... (RGBStackMerge.java)
		int[] wList = WindowManager.getIDList();
		if (wList==null) {
			IJ.error("bory dot analysis", "No images are open.");
			return;
		}

		String[] titles = new String[wList.length+1];
		for (int i=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			titles[i] = imp!=null?imp.getTitle():"";
		}
		String none = "*None*";
		titles[wList.length] = none;

		GenericDialog gd = new GenericDialog("Bory Dot Analysis");
		gd.addChoice("Ch0:", titles, titles[0]);
		gd.addChoice("Ch1:", titles, titles[1]);
		//String title3 = titles.length>2&&!IJ.macroRunning()?titles[2]:none;
		gd.addCheckbox("Create Merged Binary", createComposite);
		//gd.addCheckbox("Keep Source Images", false);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		int[] index = new int[2];
		index[0] = gd.getNextChoiceIndex();
		index[1] = gd.getNextChoiceIndex();
		createComposite = gd.getNextBoolean();
		ImagePlus imp0 = WindowManager.getImage(wList[index[0]]);
		ImagePlus imp1 = WindowManager.getImage(wList[index[1]]);		
		//cal.set
		if (imp0 == null) return;
		if (imp0.getStackSize() == 1) return;		
		if (imp1.getStackSize() == 1) return;
		segAndMeasure( imp0, imp1);
	}
	
	public void segAndMeasure(ImagePlus imp0, ImagePlus imp1){
		ImagePlus binimp0 = segmentaitonByObjectSize(imp0);
		ImagePlus binimp1 = segmentaitonByObjectSize(imp1);
		//binimp0.show();
		//binimp1.show();
		
		if (createComposite) {
			ImagePlus ch0proj=null;
			ImagePlus ch1proj=null;
			ch0proj = createZprojTimeSeries(binimp0, imp0.getNSlices(), imp0.getNFrames());
			ch1proj = createZprojTimeSeries(binimp1, imp1.getNSlices(), imp1.getNFrames());
			ImageStack dummy = null;
			RGBStackMerge rgbm = new RGBStackMerge();
			ImageStack rgbstack = rgbm.mergeStacks(ch0proj.getWidth(), ch0proj.getHeight(), ch0proj.getStackSize(), ch0proj.getStack(), ch1proj.getStack(), dummy, true);
			ImagePlus rgbbin = new ImagePlus("binProjMerged", rgbstack);
			rgbbin.show();
			
		}
		measureDots(binimp0);
		measureDots(binimp1);		
	}

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
		ImagePlus projimp = new ImagePlus();
		projimp.setStack(zprostack);
		
		return projimp;				
	}
	
	// processes each time point separated. 
	public ImagePlus segmentaitonByObjectSize(ImagePlus imp){

		Duplicator bin = new Duplicator();	//this duplication may not be necessary
		ImagePlus binimp = bin.run(imp);		
		int nSlices = imp.getImageStackSize();
		int zframes = imp.getNSlices();
		int tframes = nSlices/zframes;
		double minth =0.0;
		int adjth =0;
		Duplicator dup = new Duplicator();	//this duplication may not be necessary
		ImagePlus impcopy = null;
		int maxth = (int) Math.pow(2,imp.getBitDepth());
		for(int i =0; i<tframes; i++){
			impcopy = dup.run(imp, (i*zframes+1), (i+1)*zframes);
			minth = initializeThresholdLevel(impcopy);
			IJ.log(Integer.toString(i)+": initial threshold set to "+Double.toString(minth));
			adjth = (int) ThresholdAdjusterBy3Dobj(imp, (int)minth);
			IJ.log("... ... Adjusted to "+Integer.toString(adjth));
			
			for (int j=0; j<zframes; j++)
				binimp.getStack().getProcessor(i*zframes+1+j).threshold(adjth);
		}	
		return binimp;
	}
	
	// this basically initializes the threshold value using Shanbhag autothreshold
	public int initializeThresholdLevel(ImagePlus imp){
		ZProjector zpimp = new ZProjector(imp);
		zpimp.setMethod(1); //1 is max intensity projection
			//setStartSlice(int slice);
			//setStopSlice(int slice);
		zpimp.doProjection();
			//zpimp.getProjection().show();
			//IJ.setAutoThreshold(zpimp.getProjection(), "Shanbhag dark");
		//IJ.setAutoThreshold(zpimp.getProjection(), "Minimum dark");
		//double minth = zpimp.getProjection().getProcessor().getMinThreshold();
		int[] hist = zpimp.getProjection().getProcessor().getHistogram();	//simpler strategy
		int sumpixels =0;
		int i = hist.length-1;
		while (sumpixels < 25){
			sumpixels += hist[i--];
		}
		return i;
	}
	
	public double ThresholdAdjusterBy3Dobj(ImagePlus imp, int initTh){
		// somehow this part causes error in osx
//		IJ.run("3D OC Options", "volume surface nb_of_obj._voxels nb_of_surf._voxels " +
//				"integrated_density mean_gray_value std_dev_gray_value median_gray_value " +
//				"minimum_gray_value maximum_gray_value centroid mean_distance_to_surface " +
//				"std_dev_distance_to_surface median_distance_to_surface centre_of_mass " +
//				"bounding_box dots_size=5 font_size=10 redirect_to=none");
		int localthres =0;
		Duplicator dup = new Duplicator();	//this duplication may not be necessary
		ImagePlus impcopy = dup.run(imp);
		// check initial condition
		excludeOnEdges = false;
		redirect = false; 
		Counter3D OC = new Counter3D(impcopy, initTh, minspotvoxels, (int) maxspotvoxels*2, excludeOnEdges, redirect);
		Vector<Object3D> obj = OC.getObjectsList();
		int nobj = obj.size();
		IJ.log("Before Adjust\n...Number of Objects: "+Integer.toString(nobj));
		int volumesum=0; 
		for (int i=0; i<nobj; i++){
			 Object3D currObj=obj.get(i);
			 volumesum += currObj.size;
		}
		IJ.log("...Total Volume: "+Integer.toString(volumesum));
		localthres = initTh;
		int volmin = 5;
		int volmax = 80;
		int loopcount =0;
//		while ( (((nobj != 2) && (nobj !=4))|| ((volumesum > maxspotvoxels*nobj) 
//				|| (volumesum < minspotvoxels*nobj))) && (loopcount>maxloops)) {
		while ( ((nobj<1)|| (nobj>4) || (volumesum > volmax
					|| (volumesum <volmin))) && (loopcount<maxloops)) {

			if ((nobj<1) || (volumesum < volmin)) localthres--;
			if ((nobj>4) || (volumesum > volmax)) localthres++;			
			OC = new Counter3D(impcopy, localthres, minspotvoxels, (int) (maxspotvoxels*1.5), excludeOnEdges, redirect);
			obj = OC.getObjectsList();
			nobj = obj.size();
			volumesum=0;
			for (int i=0; i<nobj; i++){
				 Object3D currObj=obj.get(i);
				 volumesum += currObj.size;
			}
			loopcount++;
		}
		IJ.log("Loop exit by "+Integer.toString(loopcount)+ "Obj No."+Integer.toString(nobj)+"- VolumeSum"+Integer.toString(volumesum));
		
		return localthres;
	}
	
	public void measureDots(ImagePlus imp) {	
		
		int nSlices = imp.getStackSize();
		if (nSlices ==1) return;
		int zframes =8; // TODO
		int tframes = nSlices/zframes;
		Calibration cal = imp.getCalibration();
		Calibration calkeep = cal.copy();		
		//IJ.log(Integer.toString(imp.getHeight()));
		ImagePlus imps = null;
		Duplicator singletime = new Duplicator();
		ArrayList<Integer> objindex = new ArrayList<Integer>();
		ArrayList<Float> coords = new ArrayList<Float>();
		ArrayList<Integer> vols = new ArrayList<Integer>();
		ArrayList<Float> intdens = new ArrayList<Float>();
		
		for (int j=0; j<tframes; j++){
			//coords.clear();
			//vols.clear();
			IJ.log("====frame "+Integer.toString(j)+" ==========");
			imps = singletime.run(imp, j*zframes+1, j*zframes+zframes); 
			thr = 128;
			minSize = 3;
			maxSize = 1000;
			excludeOnEdges = false;
			redirect = false;
			Counter3D OC=new Counter3D(imps, thr, minSize, maxSize, excludeOnEdges, redirect);
			newRT = true;
			 //OC.showStatistics(newRT);
			 //if (!Counter3D.getObjects) Counter3DgetObjects();
			float[][] centroidList=OC.getCentroidList();
			for (int i=0; i<centroidList.length; i++) {
				 float cx = centroidList[i][0];
				 float cy = centroidList[i][1];
				 float cz = centroidList[i][2];
				 //IJ.log(Float.toString(cx)+", "+Float.toString(cy)+", "+Float.toString(cz)+", ");
			 }
			 Vector<Object3D> obj = OC.getObjectsList();
			 int nobj = obj.size();
			 IJ.log(Integer.toString(nobj));
			 int volume; 
			 float intden, meanint;
			 String opt ="";
			 String Cent ="";
			 String CentM ="";		 
			 for (int i=0; i<nobj; i++){
				 opt ="";
				 Object3D currObj=obj.get(i);
				 volume = currObj.size;
				 intden = currObj.int_dens;
				 meanint = currObj.mean_gray;
				 float[] tmpArrayC=currObj.centroid;
				 float[] tmpArrayM=currObj.c_mass;
				 Cent = "("+Float.toString(tmpArrayC[0])+","+Float.toString(tmpArrayC[1])+","+Float.toString(tmpArrayC[2])+")";
				 opt = "Object"+Integer.toString(i)+" vol="+Integer.toString(volume)+ "\t "+Cent+" : IntDen"+Float.toString(intden);
				 IJ.log(opt);
				 for (int k = 0; k<3; k++) coords.add(tmpArrayC[k]);
				 vols.add(volume);
				 intdens.add(intden);
				 objindex.add(j);
			 }
			 
		} 
		int[][] intA = new int[objindex.size()][2];
		float[][] floatA = new float[objindex.size()][7];		
		
		for (int i=0; i<objindex.size(); i++){
			intA[i][0] = objindex.get(i);
			intA[i][1] = vols.get(i);
			floatA[i][0] = coords.get(i*3);
			floatA[i][1] = coords.get(i*3+1);
			floatA[i][2] = coords.get(i*3+2);
			floatA[i][6] = intdens.get(i);			
		}
		showStatistics("ch0", intA, floatA);
	}
	
	   public void showStatistics(String chnum, int[][] intA, float[][] floatA){
	        ResultsTable rt;        
	        rt=new ResultsTable();	        
	        for (int i=0; i<intA.length; i++){
	            rt.incrementCounter();
	            rt.setValue("frame", i, intA[i][0]);
	            rt.setValue("Volume", i, intA[i][1]);
	            //rt.setValue("IntDen", i, intA[i][1]);
	            //rt.setValue("meanint", i, intA[i][2]);
	            rt.setValue("x", i, floatA[i][0]);
	            rt.setValue("y", i, floatA[i][1]);
	            rt.setValue("z", i, floatA[i][2]);
	            //rt.setValue("cx", i, floatA[i][3]);
	            //rt.setValue("cy", i, floatA[i][4]);
	            //rt.setValue("cz", i, floatA[i][5]);
	            rt.setValue("Intden", i, floatA[i][6]);
	        }
	       
	        rt.show("Statistics"+chnum);
	        
/*
 * 		int[][] rankA = new int[distA.length][distA[0].length];
		ArrayList<Double> list = new ArrayList<Double>();
		ArrayList<Integer> rank = new ArrayList<Integer>();
		for(int j = 0; j<distA.length; j++){
			list.clear();
			for(int i =0; i<distA[0].length; i++) {
				list.add(distA[j][i]);
				//System.out.println("from distA to list:" + Double.toString(distA[j][i]));
			}	        
 */
	        

	    }

}

