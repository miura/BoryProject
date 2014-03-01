package emblcmci.foci3Dtracker;

/*Christoph rewrites custom version of AutothresholdAdjuster3D from Kota
 * schiklen@embl.de
 */

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import ij.plugin.Duplicator;
import ij.*;
import Utilities.Counter3D;
import Utilities.Object3D;
import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.*;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import ij.process.StackProcessor;

public class AutoThrAdj3D_rewrite {

	// Segmentation parameters
	int maxspotvoxels, minspotvoxels;
	int minspotvoxels_measure;
	int maxloops;
	int thadj_volmin, thadj_volmax;
	int thadj_nummin, thadj_nummax;

	
	private static boolean createComposite = true;
	int thr, minSize, maxSize, dotSize, fontSize;
	boolean excludeOnEdges, showObj, showSurf, showCentro, showCOM, showNb, whiteNb, newRT, showStat, closeImg, showSummary, redirect;
	boolean silent = false;
	boolean showMaskedImg = false;
	
	/*Constructors*/
	//Empty constructor
	public AutoThrAdj3D_rewrite(){} // do I really need  or can I construct without arguments anyway?!?
	
	//Constructor with segmentation parameters as arguments
	public AutoThrAdj3D_rewrite(int maxspotvoxels, 
								int minspotvoxels,
								int minspotvoxels_measure,
								int maxloops,
								int thadj_volmin,
								int thadj_volmax,
								int thadj_nummin,
								int thadj_nummax){
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
	public void setParameters(){}
	
	// Just a convenient method to later retrieve the parameter fields with one line.
	public int[] getParameters(){
		int[] parameters = { 
				this.maxspotvoxels, this.minspotvoxels, this.minspotvoxels_measure,
				this.maxloops,
				this.thadj_volmin, this.thadj_volmax,
				this.thadj_nummin, this.thadj_nummax
			};
		return parameters;
		}


}