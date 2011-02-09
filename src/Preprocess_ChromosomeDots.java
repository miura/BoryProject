import emblcmci.PreprocessChromosomeDots;
import ij.IJ;
import ij.plugin.PlugIn;


public class Preprocess_ChromosomeDots implements PlugIn {

	@Override
	public void run(String arg) {
		// TODO Auto-generated method stub
		PreprocessChromosomeDots ppc = new PreprocessChromosomeDots();
//		if (arg.equals("FFTparaSetter")){
//			ppc.FFTparaSetter();
//		} else { 
			//ppc = new PreprocessChromosomeDots();
			ppc.run();
//		}
	}

}
