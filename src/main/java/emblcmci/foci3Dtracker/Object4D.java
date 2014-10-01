package emblcmci.foci3Dtracker;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

/*
 * @author Kota Miura
 * @author CMCI, EMBL
 * extending Fabrices' Object3D.java
 * http://fiji.sc/javadoc/Utilities/Object3D.html
 */
import Utilities.Object3D;

public class Object4D extends Object3D{
	
	// adds time point field to Object3D
	int timepoint;
	// adds channel name field to Object3D
	String chnum;
	// ID of dots within a time frame. Starts from 1 (0 will be the initial value)
	int dotID;
	
	// for storing image moments from the results of ParticleTracker3D
	float m0, m1, m2, m3, m4, score;
	
	public Object4D(int size) {
		super(size);
		this.timepoint = 0;
		this.chnum = "ch";
		this.dotID = 0;
	}
	public Object4D(int size, int timepoint) {
		super(size);
		this.timepoint = timepoint;
		this.chnum = "ch";
		this.dotID = 0;
	}
	
	public Object4D(int size, int timepoint, String chnum) {
		super(size);
		this.timepoint = timepoint;
		this.chnum = chnum;
		this.dotID = 0;
	}
	
	public Object4D(int size, int timepoint, String chnum, int dotID) {
		super(size);
		this.timepoint = timepoint;
		this.chnum = chnum;
		this.dotID = dotID;
	}
	
	public Object4D(int size, int timepoint, String chnum, int dotID, float[] centroid, float m0, float m1, float m2, float m3, float m4, float score){
		super(size);
		this.timepoint = timepoint;
		this.chnum = chnum;
		this.dotID = dotID;
		super.centroid = centroid;
		this.m0 = m0;
		this.m1 = m1;
		this.m2 = m2;
		this.m3 = m3;
		this.m4 = m4;
		this.score = score;
	}
	
	public void CopyObj3Dto4D(Object3D obj3D){
			this.obj_voxels = obj3D.obj_voxels;
			this.isSurfVoxels = obj3D.isSurfVoxels;
			this.surf_voxelsCoord = obj3D.surf_voxelsCoord;
			this.mean_gray = obj3D.mean_gray;
			this.median = obj3D.median;
			this.SD = obj3D.SD;
			this.min = obj3D.min;
			this.max = obj3D.max;
			this.int_dens = obj3D.int_dens;
			this.mean_dist2surf = obj3D.mean_dist2surf;
			this.median_dist2surf = obj3D.median_dist2surf;
			this.SD_dist2surf = obj3D.SD_dist2surf;
			this.size = obj3D.size;
			this.surf_size = obj3D.surf_size;
			this.surf_cal = obj3D.surf_cal;
			this.centroid = obj3D.centroid;
			this.c_mass = obj3D.c_mass;
			this.bound_cube_TL = obj3D.bound_cube_TL;
			this.bound_cube_BR = obj3D.bound_cube_BR;
			this.bound_cube_width = obj3D.bound_cube_width;
			this.bound_cube_height = obj3D.bound_cube_height;
			this.bound_cube_depth = obj3D.bound_cube_depth;
			this.cal = obj3D.cal;
	}
	
	public void CopyObj3Dto4D(Object3D obj3D, int timepoint){
			CopyObj3Dto4D(obj3D);
			this.timepoint = timepoint;	//makes the object 4D
	}
	public void CopyObj3Dto4D(Object3D obj3D, int timepoint, String chnum){
		CopyObj3Dto4D(obj3D, timepoint);
		this.chnum = chnum;
	}
	public void CopyObj3Dto4D(Object3D obj3D, int timepoint, String chnum, int dotID){
		CopyObj3Dto4D(obj3D, timepoint, chnum);
		this.dotID = dotID;
	}
	public void SetDotID(int dotID){
		this.dotID = dotID;
	}
	public void setCentroid(float[] centroid){
		this.centroid = centroid;
	}
	
	public int getTimepoint(){
		return this.timepoint;
	}
	
	/** Stores particle parameters 
	 * <ul>
	 * <li>centroid<li>coordinates<li>moments<li>scores
	 * </ul>
	 * output from particle3D plugin (type of String) are stored in Object4D array.
	 * <br>Object4D is an extended class of Object3D with time point fields and methods.  
	 * 
	 * @param particles: String variable exported from particle3D plugin
	 * @param obj4dv Vector<Object4D> to store all detected particles
	 * @param chnum String indicating the name of channel
	 */
	public void storeParticleInfoInObj4D(String particles, Vector<Object4D> obj4dv, String chnum){
		String[] lines;
		String line;
		String[] frame_number_info;
		lines = particles.split("\n");
		int currentframe = 0;
		int dotID = 1;
		int dummysize = 1;
		for (int i = 0; i < lines.length; i++){
			if (lines[i] == null) break;
			line = lines[i].trim();
	        frame_number_info = line.split("\\s+");
	        int framenum = Integer.parseInt(frame_number_info[0]);
	        if (framenum != currentframe) {
	        	dotID = 1;
	        	currentframe = framenum;
	        }
	        
	        float[] centroid = {0, 0, 0};
	        centroid[0]= Float.parseFloat(frame_number_info[2]); //xy order is opposite
	        centroid[1]= Float.parseFloat(frame_number_info[1]);
	        centroid[2]= Float.parseFloat(frame_number_info[3]);
	        float m0 = Float.parseFloat(frame_number_info[4]);
	        float m1 = Float.parseFloat(frame_number_info[5]);
	        float m2 = Float.parseFloat(frame_number_info[6]);
	        float m3 = Float.parseFloat(frame_number_info[7]);
	        float m4 = Float.parseFloat(frame_number_info[8]);
	        float score = Float.parseFloat(frame_number_info[9]);
	        	        
	        Object4D obj4d = new Object4D(dummysize, framenum, chnum, dotID, centroid, m0, m1, m2, m3, m4, score);
	        obj4dv.add(obj4d);
	        dotID++;
		}
		sortbyScore(obj4dv);
	}
	
	void sortbyScore(Vector<Object4D> obj4dv){
		int currentframe = 0;
		int counter = 0;
		Vector<Object4D> obj4dVpertime = new Vector<Object4D>();
		for (int i = 0; i < obj4dv.size(); i++){
			if ((i == 0) || (obj4dv.get(i).timepoint != currentframe)){
				currentframe = obj4dv.get(i).timepoint;
				for (int j = 0; j < obj4dv.size(); j++){
					if (obj4dv.get(j).timepoint == currentframe) {
						obj4dVpertime.add(obj4dv.get(j));
					}
				}
				Collections.sort(obj4dVpertime,  new ComparerByscore4D(ComparerByscore4D.DESC));
				for (int j = 0; j < obj4dVpertime.size(); j++){
					obj4dv.setElementAt(obj4dVpertime.get(j), counter);
					counter++;
				}
				obj4dVpertime.clear();
			}
		}	
	}
	
}

/**
 * for sorting Object3D Vector, descending order by size (volume)
 */
class ComparerBysize3D implements Comparator<Object3D> {
	public static final int ASC = 1;
	public static final int DESC = -1;
	private int sort = ASC;

	public ComparerBysize3D() {
	}

	public ComparerBysize3D(int sort) {
		this.sort = sort;
	}

	public int compare(Object3D o1, Object3D o2) {

		Object3D obj3d1 = (Object3D) o1;
		Object3D obj3d2 = (Object3D) o2;
		int i = 0;
		if (obj3d1.size < obj3d2.size)
			i = -1 * sort;
		if (obj3d1.size == obj3d2.size)
			i = 0;
		if (obj3d1.size > obj3d2.size)
			i = 1 * sort;
		return i;
	}
}

/**
 * for sorting Object4D, descending order by score (of none-particle criteria)
 * 
 * @author Miura
 * 
 */
class ComparerByscore4D implements Comparator<Object4D> {
	public static final int ASC = 1;
	public static final int DESC = -1;
	private int sort = ASC;

	public ComparerByscore4D() {
	}

	public ComparerByscore4D(int sort) {
		this.sort = sort;
	}

	public int compare(Object4D o1, Object4D o2) {
		Object4D obj4d1 = (Object4D) o1;
		Object4D obj4d2 = (Object4D) o2;
		int i = 0;
		if (obj4d1.score < obj4d2.score)
			i = -1 * sort;
		if (obj4d1.score == obj4d2.score)
			i = 0;
		if (obj4d1.score > obj4d2.score)
			i = 1 * sort;
		return i;
	}
}
