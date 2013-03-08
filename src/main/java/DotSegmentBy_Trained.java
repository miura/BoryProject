import emblcmci.DotSegmentByTrained;
import ij.plugin.PlugIn;


public class DotSegmentBy_Trained implements PlugIn {

	@Override
	public void run(String arg) {
		// TODO Auto-generated method stub
		DotSegmentByTrained dbt = new DotSegmentByTrained();
		dbt.run();
	}

}
