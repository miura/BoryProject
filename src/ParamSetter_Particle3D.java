import emblcmci.DotSegmentByParticletracker3D;
import ij.plugin.PlugIn;



public class ParamSetter_Particle3D implements PlugIn {

	@Override
	public void run(String arg) {
		DotSegmentByParticletracker3D param = new DotSegmentByParticletracker3D();
		if (!param.parameterDialog()) return;
	}

}
