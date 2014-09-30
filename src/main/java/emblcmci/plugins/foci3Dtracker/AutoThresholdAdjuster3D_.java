package emblcmci.plugins.foci3Dtracker;

import emblcmci.foci3Dtracker.AutoThrAdj3D_rewrite;
import emblcmci.foci3Dtracker.AutoThresholdAdjuster3D;
import ij.plugin.PlugIn;

public class AutoThresholdAdjuster3D_ implements PlugIn {

	@Override
	public void run(String arg) {
//		AutoThrAdj3D_rewrite at = new AutoThrAdj3D_rewrite();
		AutoThresholdAdjuster3D at = new AutoThresholdAdjuster3D();
		at.run();
	}

}
