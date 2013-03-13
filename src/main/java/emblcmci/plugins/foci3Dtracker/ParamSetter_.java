package emblcmci.plugins.foci3Dtracker;

import emblcmci.foci3Dtracker.ParamSetter;
import ij.plugin.PlugIn;


public class ParamSetter_ implements PlugIn {

	@Override
	public void run(String arg) {
		ParamSetter para = new ParamSetter();
		para.showDialog();
	}

}
