import emblcmci.ParamSetter;
import ij.plugin.PlugIn;


public class ParamSetter_ implements PlugIn {

	@Override
	public void run(String arg) {
		ParamSetter para = new ParamSetter();
		para.showDialog();
	}

}
