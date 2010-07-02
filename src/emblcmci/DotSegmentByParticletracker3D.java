package emblcmci;

import java.awt.Choice;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.process.StackConverter;
import ij.process.StackStatistics;
import eth.track3D.ParticleTracker3D;
import eth.track3D.ParticleTracker3D.MyFrame;

/* Kota Miura (CMCI. EMBL)
 * Extends Particletracker3D to be usable just for particle detections
 * 
 * 
 */
public class DotSegmentByParticletracker3D extends ParticleTracker3D{
	//	getUserDefinedPreviewParams(); <- something simlar to this to set parameters are required
	/* user defined parameters */
	static double cutoff = 3.0; 		// default
	static float percentile = 0.001F; 	// default (user input/100)
	static int absIntensityThreshold = 0; //user input 
	static int radius = 3; 				// default
	static int linkrange = 2; 			// default
	static double displacement = 10.0; 	// default
	static int preprocessing_mode = 3;	//"none", "box-car avg.", "BG Subtraction", "Laplace Operation"}, "box-car avg.";
	
	public DotSegmentByParticletracker3D() {
		super();
		// TODO Auto-generated constructor stub
	}

	public DotSegmentByParticletracker3D(double raidus, double cutoff, float percentile) {
		super();
		InitiUserDefinedPara();
		this.radius = radius;
		this.cutoff = cutoff;
		this.percentile = percentile;
		
		// TODO Auto-generated constructor stub
	}

	public void InitiUserDefinedPara(){
		super.cutoff = cutoff; 		// default
		super.percentile = percentile; 	// default (user input/100)
		super.absIntensityThreshold = absIntensityThreshold; //user input 
		super.radius = radius; 				// default
		super.linkrange = linkrange; 			// default
		super.displacement = displacement; 	// default
		super.preprocessing_mode = preprocessing_mode;
	}
	public boolean parameterDialog(){
		gd = new GenericDialog("Particle Tracker...", IJ.getInstance());
		gd.addMessage("Particle Detection:");			
		// These 3 params are only relevant for non text_files_mode
        gd.addNumericField("Radius", radius, 0);
        gd.addNumericField("Cutoff", cutoff, 1);
        gd.addChoice("Threshold mode", new String[]{"Absolute Threshold","Percentile"}, "Percentile");
        ((Choice)gd.getChoices().firstElement()).addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e) {
				// TODO Auto-generated method stub
				int mode = 0;
				if(e.getItem().toString().equals("Absolute Threshold")) {
					mode = ABS_THRESHOLD_MODE;						
				}
				if(e.getItem().toString().equals("Percentile")) {
					mode = PERCENTILE_MODE;						
				}
				thresholdModeChanged(mode);
			}});
//        gd.addNumericField("Percentile", 0.001, 5);
        gd.addNumericField("Percentile / Abs.Threshold", 0.1, 5, 6, " % / Intensity");
        
//        gd.addPanel(makeThresholdPanel(), GridBagConstraints.CENTER, new Insets(0, 0, 0, 0));
        gd.addChoice("Preprocessing mode", new String[]{"none", "box-car avg.", "BG Subtraction", "Laplace Operation"}, "Laplace Operation");	        
        gd.showDialog();
    	radius = (int)gd.getNextNumber();
    	cutoff = gd.getNextNumber(); 
    	percentile = ((float)gd.getNextNumber())/100;
    	absIntensityThreshold = (int)(percentile*100+0.5);
    	int thsmode = gd.getNextChoiceIndex();
    	preprocessing_mode = gd.getNextChoiceIndex();
    	super.radius = radius;
    	super.cutoff = cutoff;
    	super.percentile = percentile;
    	super.absIntensityThreshold = absIntensityThreshold;
    	super.preprocessing_mode = preprocessing_mode;
    	setThresholdMode(thsmode); 
    	return true;
	}
	
	public String DetectDots3D(ImagePlus imp){
		//setup("", imp);
		//InitiUserDefinedPara();
		//if (!parameterDialog()) return "-1";
		
		//momentum_from_text = false;
		boolean convert = false;		
        // initialize ImageStack stack
		stack = original_imp.getStack();
		IJ.log(Integer.toString(original_imp.getWidth()));
		this.title = original_imp.getTitle();
		
		// get global minimum and maximum
		StackStatistics stack_stats = new StackStatistics(original_imp);
		global_max = (float)stack_stats.max;
		global_min = (float)stack_stats.min;
		frames_number = original_imp.getNFrames();
		slices_number = original_imp.getNSlices();
		
        // check if the original images are not GRAY8, 16 or 32
        if (this.original_imp.getType() != ImagePlus.GRAY8 &&
        		this.original_imp.getType() != ImagePlus.GRAY16 &&
        		this.original_imp.getType() != ImagePlus.GRAY32) {
        	//gd.addCheckbox("Convert to Gray8 (recommended)", true);
        	convert = true;
        }  
        generateMask(this.radius);
        // if user choose to convert reset stack, title, frames number and global min, max
    	if (convert) {
    		sc = new StackConverter(original_imp);
    		sc.convertToGray8();
			stack = original_imp.getStack();
			this.title = original_imp.getTitle();
			StackStatistics stack_stats2 = new StackStatistics(original_imp);
			global_max = (float)stack_stats2.max;
			global_min = (float)stack_stats2.min;
			frames_number = stack.getSize();
        }        
    	

		/* detect particles and store as string*/
    	String particles = "";
		if (this.processFrames()) { // process the frames
			// for each frame - save the detected particles
			for (int i = 0; i<this.frames.length; i++) {
				//IJ.log(this.frames[i].toString());
				//IJ.log(this.frames[i].getFullFrameInfo().toString());
				int particlenum = this.frames[i].getParticles().size();
				//IJ.log("frame" + Integer.toString(i));
				for (int j = 0; j < particlenum; j++) {
					//IJ.log("\t" + this.frames[i].getParticles().elementAt(j).toString());
					particles = particles + this.frames[i].getParticles().elementAt(j).toString();//+"\n";
				}
//				if (!write2File(sd.getDirectory(), sd.getFileName() + "_" + i, 
//						this.frames[i].frameDetectedParticlesForSave(true).toString())) {
//					// upon any problam savingto file - return
//					return;
			}
			//IJ.log(particles);
		}
		//ImagePlus dot_imp = new ImagePlus("From text files", createStackFromTextFiles());
		//dot_imp.show(); tried this but does not work since field value "max_coord" could not be set. 
		return particles;
	}
}


	
	

