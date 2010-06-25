import emblcmci.DotSegmentByParticletracker3D;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;


public class ParticleDetection_3D implements PlugIn {

	@Override
	public void run(String arg) {
		DotSegmentByParticletracker3D dpt3D = new DotSegmentByParticletracker3D();
		ImagePlus imp = WindowManager.getCurrentImage();
		dpt3D.DetectDots3D(imp);
	}

}
