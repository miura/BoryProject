package emblcmci;

import ij.IJ;
import ij.ImagePlus;
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
//	public double cutoff = 3.0; 		// default
//	public float percentile = 0.001F; 	// default (user input/100)
//	public int absIntensityThreshold = 0; //user input 
//	public int radius = 3; 				// default
//	public int linkrange = 2; 			// default
//	public double displacement = 10.0; 	// default

	
	public DotSegmentByParticletracker3D() {
		super();
		// TODO Auto-generated constructor stub
	}	

	public void InitiUserDefinedPara(){
		super.cutoff = 5.0; 		// default
		super.percentile = 0.0001F; 	// default (user input/100)
		super.absIntensityThreshold = 0; //user input 
		super.radius = 2; 				// default
		super.linkrange = 2; 			// default
		super.displacement = 10.0; 	// default
		super.preprocessing_mode = 3;//"none", "box-car avg.", "BG Subtraction", "Laplace Operation"}, "box-car avg.";
	}
	
	public void DetectDots3D(ImagePlus imp){
		setup("", imp);
		InitiUserDefinedPara();
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
    	

		/* detect particles and save to files*/
		if (this.processFrames()) { // process the frames
			// for each frame - save the detected particles
			for (int i = 0; i<this.frames.length; i++) {
				IJ.log(this.frames[i].toString());
				IJ.log(this.frames[i].getFullFrameInfo().toString());
				
//				if (!write2File(sd.getDirectory(), sd.getFileName() + "_" + i, 
//						this.frames[i].frameDetectedParticlesForSave(true).toString())) {
//					// upon any problam savingto file - return
//					return;
			}
		}
	}
}


	
	

