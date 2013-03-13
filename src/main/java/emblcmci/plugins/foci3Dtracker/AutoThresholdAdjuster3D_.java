package emblcmci.plugins.foci3Dtracker;

import emblcmci.foci3Dtracker.AutoThresholdAdjuster3D;
import ij.plugin.PlugIn;

public class AutoThresholdAdjuster3D_ implements PlugIn {

	@Override
	public void run(String arg) {
		// TODO Auto-generated method stub
		AutoThresholdAdjuster3D at = new AutoThresholdAdjuster3D();
		at.run();
	}

}
