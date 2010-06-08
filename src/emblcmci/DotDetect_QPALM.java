package emblcmci;


import ij.IJ;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import QuickPALM.MyDialogs;
import QuickPALM.MyFunctions;

public class DotDetect_QPALM implements PlugIn {

	@Override
	public void run(String arg) {
		// TODO Auto-generated method stub
		
	}

}


class DetectDotsQP extends MyFunctions {
	void detectParticles(ImageProcessor ip, int nframe)
	{
		int i, j;
		int width = ip.getWidth();
		int height = ip.getHeight();
		int s = 0; // signal from ip
		
		boolean mask [][] = new boolean [width][height];
		
		int xmin = 0;
		int ymin = 0;
		int smin = 99999;	
		double saturation = ip.getMax();

		for (i=0;i<width;i++)
			for (j=0;j<height;j++)
			{
				s=ip.get(i,j);
				if (s<smin)
				{
					smin=s;
					xmin=i;
					ymin=j;
				}
				if (s!=saturation) mask[i][j]=false;
				else
				{
					mask[i][j]=true;
					ip.set(i,j,0);
				}
			}
			
		ImageProcessor spip=ip.duplicate(); // short-pass version of ip
		ImageProcessor lpip=ip.duplicate(); // low-pass version of ip
		getGblur().blur(spip, 0.5);
		//getGblur().blur(lpip, dg.fwhm*2);	comout
		double dotdia = 5;
		getGblur().blur(lpip, dotdia);
				
		// build new frequency gatted image		
		for (i=0;i<width;i++)
			for (j=0;j<height;j++)
			{
				s = spip.get(i,j)-lpip.get(i,j);	
				ip.set(i, j, (s>0)?s:0);
			}
		
		// lets calculate the noise level
		int xstart = xmin-6;
		if (xstart<0) xstart=0;
		int xend = xmin+7;
		if (xend>width) xend = width;
		int ystart = ymin-6;
		if (ystart<0) ystart=0;
		int yend = ymin+7;
		if (yend>height) yend = height;
		
		double noise=0;
		int npixels = 0; // total non-zero pixels
		for (i=xstart;i<xend;i++)
			for (j=ystart;j<yend;j++)
			{
				s = ip.get(i, j);
				if (s>0)
				{
					noise+=ip.get(i, j);
					npixels++;
				}
			}
		noise /= npixels;
		
		// set minimum thresh
		double snrthresh = noise*dg.snr;
		
		// start detecting particles
		int [] maxs;
		int ok_nparticles = 0;
		int notok_nparticles = 0;
		int last_ok_nparticles = 0;
		int smartcounter = 0;

		for (int n=0;n<=dg.maxpart;n++)
		{
			maxs = getMaxPositions(ip);
			if (ip.get(maxs[1], maxs[2])<snrthresh) break;
			else if (getParticle(ip, mask, maxs, dg, ptable, nframe))
				ok_nparticles++;
			else notok_nparticles++;
			if (dg.smartsnr)
			{
				if (last_ok_nparticles!=ok_nparticles)
				{
					last_ok_nparticles=ok_nparticles;
					smartcounter=0;
				}
				else if (ok_nparticles>1 && smartcounter>dg.maxpart*0.1) break;
				else smartcounter++;
			}
		}
		IJ.log("'OK'/'Not OK' Particles= "+ok_nparticles+"/"+notok_nparticles);
	}	
}