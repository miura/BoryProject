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

	int maxspotvoxels;
	int minspotvoxels;
	int minspotvoxels_measure;
	int maxloops;
	int thadj_volmin;
	int thadj_volmax;
	int thadj_nummin;

	
	/*Constructor with segmentation parameters as arguments
	 * 
	 */
	public AutoThrAdj3D_rewrite(int maxspotvoxels, 
								int minspotvoxels,
								int minspotvoxels_measure,
								int maxloops,
								int thadj_volmin,
								int thadj_volmax,
								int thadj_nummin){
		this.maxspotvoxels = maxspotvoxels;
	}
}