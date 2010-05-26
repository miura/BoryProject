package emblcmci;
/*
 * @author Kota Miura
 * @author CMCI, EMBL
 * extending Fabrices' Object3D
 */
import Utilities.Object3D;

public class Object4D extends Object3D{
	
	// adds time point field to Object3D
	int timepoint;
	// adds channel name field to Object3D
	String chnum;
	
	public Object4D(int size) {
		super(size);
		timepoint = 0;
		chnum = "ch";
	}
	public Object4D(int size, int timepoint) {
		super(size);
		this.timepoint = timepoint;
		this.chnum = "ch";
	}
	
	public Object4D(int size, int timepoint, String chnum) {
		super(size);
		this.timepoint = timepoint;
		this.chnum = chnum;
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
	
}
