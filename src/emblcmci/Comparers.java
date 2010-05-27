package emblcmci;

import java.util.Comparator;

import Utilities.Object3D;

public class Comparers {
	public class ComparerBysize implements Comparator<Object4D> {
		
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
	public Comparator<Object4D> getComparerBysize(){
        return new ComparerBysize();
    }
	public class ComparerBysize3D implements Comparator<Object3D> {
		
        public int compare(Object3D o1, Object3D o2) {
            Object3D obj3d1 = (Object3D) o1;
            Object3D obj3d2 = (Object3D) o2;
            int i = 0;
            if (obj3d1.size > obj3d2.size) 
                i = -1;
            if (obj3d1.size == obj3d2.size)
                i = 0;
            if (obj3d1.size < obj3d2.size)
                i = 1;
            return i;
        }
    }
	public Comparator<Object3D> getComparerBysize3D(){
        return new ComparerBysize3D();
    }	
}
