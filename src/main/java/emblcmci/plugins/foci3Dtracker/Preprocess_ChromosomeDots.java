package emblcmci.plugins.foci3Dtracker;

/**For prerpocessing yeast gene FOCI dots (4D)
 * Kota Miura (miura@embl.de) for Bory's project
 * 
 */

import emblcmci.foci3Dtracker.PreprocessChromosomeDots;
import ij.plugin.PlugIn;


public class Preprocess_ChromosomeDots implements PlugIn {

	@Override
	public void run(String arg) {
		PreprocessChromosomeDots ppc = new PreprocessChromosomeDots();
		if (arg.equals("FFTparaSetter")) {
			ppc.fftparaSetter();
		} else { 
			ppc.run();
		}
	}

}
