package emblcmci;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.process.ImageProcessor;
//import ij.io.OpenDialog;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;

import trainableSegmentation.FeatureStack;
import trainableSegmentation.Trainable_Segmentation;
import weka.core.Attribute;

public class LoadTrainedEx extends Trainable_Segmentation{

	//public static ImageProcessor ip = WindowManager.getCurrentImage().getProcessor().duplicate()
	// setImagewitout GUI
	public void setCurrentImageNoGUI(ImageProcessor ip){
		setTrainingImage(new ImagePlus("Trainable Segmentation",ip));
		if (Math.max(getTrainingImage().getWidth(), getTrainingImage().getHeight()) > 1024)
			if (!IJ.showMessageWithCancel("Warning", "At least one dimension of the image \n" +
					"is larger than 1024 pixels. \n" +
					"Feature stack creation and classifier training \n" +
					"might take some time depending on your computer.\n" +
					"Proceed?"))
				return;


		getTrainingImage().setProcessor("Trainable Segmentation", getTrainingImage().getProcessor().duplicate().convertToByte(true));
		
	
		// Initialize feature stack (no features yet)
		setFeatureStack(new FeatureStack(getTrainingImage()));
		
		setDisplayImage(new ImagePlus());
		getDisplayImage().setProcessor("Trainable Segmentation", getTrainingImage().getProcessor().duplicate());

		//getDisplayImage().show();
	}
	
	//loads data from specified file without GUI
	public void loadTrainingDataNoGUI(String fullpath)
	{
		/*
		OpenDialog od = new OpenDialog("Choose data file","");
		if (od.getFileName()==null)
			return;
		IJ.log("Loading data from " + od.getDirectory() + od.getFileName() + "...");
		loadedTrainingData = readDataFromARFF(od.getDirectory() + od.getFileName());
		*/
		
		IJ.log("Loading data from " + fullpath);
		setLoadedTrainingData(readDataFromARFF(fullpath));
		
		// Check the features that were used in the loaded data
		Enumeration<Attribute> attributes = getLoadedTrainingData().enumerateAttributes();
		final int numFeatures = FeatureStack.availableFeatures.length;
		boolean[] usedFeatures = new boolean[numFeatures];
		while(attributes.hasMoreElements())
		{
			final Attribute a = attributes.nextElement();
			for(int i = 0 ; i < numFeatures; i++)
				if(a.name().startsWith(FeatureStack.availableFeatures[i]))
					usedFeatures[i] = true;
		}
		
		// Check if classes match
		Attribute classAttribute = getLoadedTrainingData().classAttribute();
		Enumeration<String> classValues  = classAttribute.enumerateValues();
		
		// Update list of names of loaded classes
		setLoadedClassNames(new ArrayList<String>());
		
		int j = 0;
		while(classValues.hasMoreElements())
		{
			final String className = classValues.nextElement().trim();
			getLoadedClassNames().add(className);
			
			IJ.log("Read class name: " + className);
			if( !className.equals(this.getClassLabels()[j]))
			{
				String s = getClassLabels()[0];
				for(int i = 1; i < getNumOfClasses(); i++)
					s = s.concat(", " + getClassLabels()[i]);
				IJ.error("ERROR: Loaded classes and current classes do not match!\nExpected: " + s);
				setLoadedTrainingData(null);
				return;
			}
			j++;
		}
		
		if(j != getNumOfClasses())
		{
			IJ.error("ERROR: Loaded number of classes and current number do not match!");
			setLoadedTrainingData(null);
			return;
		}
		
		IJ.log("Loaded data: " + getLoadedTrainingData().numInstances() + " instances");
		
		boolean featuresChanged = false;
		final boolean[] oldEnableFeatures = this.getFeatureStack().getEnableFeatures();
		// Read checked features and check if any of them changed
		for(int i = 0; i < numFeatures; i++)
		{
			if (usedFeatures[i] != oldEnableFeatures[i])
				featuresChanged = true;
		}
		// Update feature stack if necessary
		IJ.log("featuresChanged =" +  (featuresChanged)); //kota to be deleted
		if(featuresChanged)	
		{					
			//this.setButtonsEnabled(false); kota commented out
			this.getFeatureStack().setEnableFeatures(usedFeatures);
			this.getFeatureStack().updateFeatures();
			//this.setButtonsEnabled(true);  kota commented out
			// Force whole data to be updated
			setUpdateWholeData(true);
		}
		
	}
	public void showClassificationImage2(){
		ImagePlus resultImage = new ImagePlus("classification result", getClassifiedImage().getProcessor().convertToByte(true).duplicate());
		IJ.log(resultImage.getTitle());
		resultImage.show();
	}
	

}
