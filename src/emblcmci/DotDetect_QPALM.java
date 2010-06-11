package emblcmci;


import ij.IJ;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.process.ImageProcessor;
//import QuickPALM.MyDialogs;
import QuickPALM.MyFunctions;

public class DotDetect_QPALM implements PlugIn {

	@Override
	public void run(String arg) {
		// TODO Auto-generated method stub
		
	}

}

/**
 * 
 * @author miura
 * @param ImageProcessor ip
 *  @param frame index
 *  @param dotdiameter expected
 *  @param maxpart maximum number of iteration per frame
 *  @param snr signal to noise ratio, default is 2
 */
class DetectDotsQP extends MyFunctions {
	ResultsTable ptable = Analyzer.getResultsTable(); // Particle table
	double dotdiameter = 2;
	boolean smartsnr = true;
	
	void detectParticles(ImageProcessor ip, int nframe,  int maxpart, int snr)
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
		//getGblur().blur(lpip, dg.fwhm*2);	comout, instead below
		getGblur().blur(lpip, dotdiameter);	//dotdiameter is the argument
				
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
		//double snrthresh = noise*dg.snr;
		double snrthresh = noise*snr;		
		// start detecting particles
		int [] maxs;
		int ok_nparticles = 0;
		int notok_nparticles = 0;
		int last_ok_nparticles = 0;
		int smartcounter = 0;

		//for (int n=0;n<=dg.maxpart;n++)
		for (int n=0;n<=maxpart;n++)
		{
			maxs = getMaxPositions(ip);
			if (ip.get(maxs[1], maxs[2])<snrthresh) break;
			//else if (getParticle(ip, mask, maxs, dg, ptable, nframe))
			else if (getParticle(ip, mask, maxs, ptable, nframe))				
				ok_nparticles++;
			else notok_nparticles++;
			//if (dg.smartsnr)
			if (smartsnr)

			{
				if (last_ok_nparticles!=ok_nparticles)
				{
					last_ok_nparticles=ok_nparticles;
					smartcounter=0;
				}
				//else if (ok_nparticles>1 && smartcounter>dg.maxpart*0.1) break;
				else if (ok_nparticles>1 && smartcounter>maxpart*0.1) break;				
				else smartcounter++;
			}
		}
		IJ.log("'OK'/'Not OK' Particles= "+ok_nparticles+"/"+notok_nparticles);
	}

	private boolean getParticle(ImageProcessor ip, boolean[][] mask,
			int[] maxs, ResultsTable ptable2, int nframe) {
//		int roirad = (int) Math.round(dg.fwhm);
		int roirad = (int) Math.round(dotdiameter);

		int xmax = maxs[1];
		int ymax = maxs[2];
		int smax = ip.get(xmax, ymax);
		int xstart = xmax-roirad;
		int xend = xmax+1+roirad;
		int ystart = ymax-roirad;
		int yend = ymax+1+roirad;
		int width = ip.getWidth();
		int height = ip.getHeight();
		double thrsh = smax*dg.pthrsh;
		
		int i, j;
		
		// skip perifery particles
		if (xstart<0 || xend>=width || ystart<0 || yend>=height) 
		{
			IJ.log("fail on perifery");
			xstart = (int) (xmax-roirad/2);
			ystart = (int) (ymax-roirad/2);
			xend = (int) (xmax+1+roirad/2);
			yend = (int) (ymax+1+roirad/2);
			
			xstart = (xstart<0)?0:xstart;
			ystart = (ystart<0)?0:ystart;
			xend = (xend>=width)?width-1:xend;
			yend= (yend>=height)?height-1:yend;
			clearRegion(thrsh, ip, mask, xstart, xend, ystart, yend);
			return false;
		}

		// already analysed region
		for (i=xstart;i<=xend;i++)
			for (j=ystart;j<=yend;j++)
				if (mask[i][j])
				{
					IJ.log("fail on already analysed");
					xstart = (int) (xmax-roirad/2);
					ystart = (int) (ymax-roirad/2);
					xend = (int) (xmax+1+roirad/2);
					yend= (int) (ymax+1+roirad/2);
					clearRegion(thrsh, ip, mask, xstart, xend, ystart, yend);
					return false;
				}
		
		int npixels=0;
		int s = 0;
		int sSum = 0;
	
		double xm = 0;
		double ym = 0;
		double xstd = 0; // stddev x
		double ystd = 0; // stddev y
		double xlstd = 0; // stddev left x
		double xrstd = 0; // stddev right x
		double ylstd = 0; // stddev left y
		double yrstd = 0; // stddev right y
		int xlsum = 0; // left pixel sum
		int xrsum = 0; // right pixel sum
		int ylsum = 0; // left pixel sum
		int yrsum = 0; // right pixel sum
				
		for (i=xstart;i<=xend;i++)
			for (j=ystart;j<=yend;j++)
			{
				s=ip.get(i, j);	
				if (s>thrsh)
				{	
					xm+=i*s;
					ym+=j*s;
					sSum+=s;
					npixels++;
				}
			}
		xm/=sSum;
		ym/=sSum;
		
		double sxdev = 0;
		double sydev = 0;
		// get the axial std	
		for (i=xstart;i<=xend;i++)
		{
			for (j=ystart;j<=yend;j++)
			{
				s=ip.get(i, j);	
				if (s>thrsh)
				{
					sxdev = (i-xm)*s;
					sydev = (j-ym)*s;
					if (sxdev<0)
					{
						xlstd+=-sxdev;
						xlsum+=s;
					}
					else
					{
						xrstd+=sxdev;
						xrsum+=s;
					}
					if (sydev<0)
					{
						ylstd+=-sydev;
						ylsum+=s;
					}
					else
					{
						yrstd+=sydev;
						yrsum+=s;
					}
					xstd+=Math.abs(sxdev);
					ystd+=Math.abs(sydev);
				}
			}
		}
		xstd/=sSum;
		ystd/=sSum;
		xlstd/=xlsum;
		xrstd/=xrsum;
		ylstd/=ylsum;
		yrstd/=yrsum;
				
		// redimentionalize ROI based on axix std
		int xstart_ = (int) Math.round(xm-xlstd*1.177) - 1;
		int xend_ = (int) Math.round(xm+xrstd*1.177) + 1;
		int ystart_ =  (int) Math.round(ym-ylstd*1.177) - 1;
		int yend_ = (int) Math.round(ym+yrstd*1.177) + 1;
		if (xstart_>xstart) xstart = xstart_;
		if (ystart_>ystart) ystart = ystart_;
		if (xend_<xend) xend=xend_;
		if (yend_<yend) yend=yend_;
				
		double wmh = ((xlstd+xrstd)-(ylstd+yrstd))*1.177; // width minus height
		double z = 0;
		
		// area filter
		if (npixels<5 || ((xlstd+xrstd)*1.177>dg.fwhm) || ((ylstd+yrstd)*1.177>dg.fwhm))
		{
			log("fail on size");
			clearRegion(thrsh, ip, mask, xstart, xend, ystart, yend);
			return false;
		}
		
		// symmetricity
		double xsym = 1-Math.abs((xlstd-xrstd)/(xlstd+xrstd));
		double ysym = 1-Math.abs((ylstd-yrstd)/(ylstd+yrstd));
		double sym = (xsym<ysym)?xsym:ysym;
		
		// if 2D
		if (!dg.is3d)
		{
			if (sym < dg.symmetry)
			{
				log("fail on 2D symmetry");
				clearRegion(thrsh, ip, mask, xstart, xend, ystart, yend);
				return false;
			}
		}
		// if 3D
		else
		{
			if (xsym<dg.symmetry || ysym<dg.symmetry)
			{
				log("fail on 3D symmetry");
				clearRegion(thrsh, ip, mask, xstart, xend, ystart, yend);
				return false;
			}
			z = getZ(wmh);
			if (z==9999)
			{
				clearRegion(thrsh, ip, mask, xstart, xend, ystart, yend);
				return false;
			}
		}
		
		double s_ = sSum/npixels;
		double xm_=xm*dg.pixelsize;
		double ym_=ym*dg.pixelsize;
		double xlstd_=xlstd*1.177;
		double xrstd_=xrstd*1.177;
		double ylstd_=ylstd*1.177;
		double yrstd_=yrstd*1.177;
		double frame_=nframe+1;

		ptable_lock.lock();
		ptable.incrementCounter();
		ptable.addValue("Intensity", s_);
		ptable.addValue("X (px)", xm);
		ptable.addValue("Y (px)", ym);
		ptable.addValue("X (nm)", xm_);
		ptable.addValue("Y (nm)", ym_);
		ptable.addValue("Z (nm)", z);
		ptable.addValue("Left-Width (px)", xlstd_);
		ptable.addValue("Right-Width (px)", xrstd_);
		ptable.addValue("Up-Height (px)", ylstd_);
		ptable.addValue("Down-Height (px)", yrstd_);			
		ptable.addValue("X Symmetry (%)", xsym);
		ptable.addValue("Y Symmetry (%)", ysym);
		ptable.addValue("Width minus Height (px)", wmh);
		ptable.addValue("Frame Number", frame_);
		ptable_lock.unlock();
		if (psave!=null)
		{
			psave.saveParticle(s_, xm, ym, xm_, ym_, z, xlstd_, xrstd_, ylstd_, yrstd_, xsym, ysym, wmh, frame_);
		}
		
		clearRegion(thrsh, ip, mask, xstart, xend, ystart, yend);
		return true;
	}	
}