import emblcmci.DotSegmentByParticletracker3D;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import eth.track3D.ParticleTracker3D;

public class ParticleDetection_3D implements PlugIn {

	@Override
	public void run(String arg) {
		DotSegmentByParticletracker3D dpt3D = new DotSegmentByParticletracker3D();
		ImagePlus imp = WindowManager.getCurrentImage();
		dpt3D.setup("", imp);
		if (!dpt3D.parameterDialog()) return;
		String particles = dpt3D.DetectDots3D(imp);
		IJ.log(particles);
	}

}
