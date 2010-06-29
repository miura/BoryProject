package emblcmci;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import Utilities.Counter3D;
import Utilities.Object3D;
import ij.*;
import ij.plugin.PlugIn;


public class tryObject3D_ implements PlugIn {

	ImagePlus imp;
	int thr, minSize, maxSize, dotSize, fontSize;
	boolean excludeOnEdges, showObj, showSurf, showCentro, showCOM, showNb, whiteNb, newRT, showStat, showMaskedImg, closeImg, showSummary, redirect;
	Vector<Object4D> obj4Dv;
	
	public void run(String arg) {
		 imp = ij.WindowManager.getCurrentImage();
		 thr = 128;
		 minSize = 3;
		 maxSize = 1000;
		 excludeOnEdges = false;
		 redirect = false;
		 Counter3D OC=new Counter3D(imp, thr, minSize, maxSize, excludeOnEdges, redirect);
		 newRT = true;
		 OC.showStatistics(newRT);
		//IJ.showMessage("test3D_Plugin","Hello world 3!");
		 Vector<Object3D> obj3Dv = OC.getObjectsList();
		 obj4Dv = new Vector<Object4D>();
		 for (int i = 0; i < obj3Dv.size(); i++) {
			 Object4D obj4d = new Object4D(obj3Dv.get(i).size);
			 obj4d.CopyObj3Dto4D(obj3Dv.get(i), 1, "ch1");
			 obj4Dv.add(obj4d);
		 }
		 for(int i = 0; i < obj4Dv.size(); i++){
			 IJ.log(Integer.toString(obj4Dv.get(i).size));
		 }
		 ComparerBysize4D comparers = new ComparerBysize4D();
		 Collections.sort(obj4Dv, comparers);
		 IJ.log("after sorting ==========");
		 for(int i = 0; i < obj4Dv.size(); i++){
			 IJ.log(Integer.toString(obj4Dv.get(i).size));
		 }

		 
	}

}

class ComparerBysize4D implements Comparator<Object4D> {
	
	public ComparerBysize4D(){
	}
	
    public int compare(Object4D o1, Object4D o2) {
        Object4D obj4d1 = (Object4D) o1;
        Object4D obj4d2 = (Object4D) o2;
        int i = 0;
        if (obj4d1.size > obj4d2.size) 
            i = -1;
        if (obj4d1.size == obj4d2.size)
            i = 0;
        if (obj4d1.size < obj4d2.size)
            i = 1;
        return i;
    }
}


