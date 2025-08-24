import org.apache.commons.geometry.euclidean.oned.Vector1D;
import org.apache.commons.geometry.euclidean.threed.SphericalCoordinates;

public class ThreeDEngine {

	private Main.ThreadData data;
	//private SphericalCoordinates sphericalCoordsSS;
	private int channels;
	private int sampleRate;
	private int writePositionD = 0;
	private int writePositionV = 0;
	private int readPositionD = 0;
	private int readPositionV = 0;
	private int bufferSize;
	private int delayBufferDSize, delayBufferVSize;
	private double[][] delayBufferD, delayBufferV;
	private double[][] inBuffer;
	private double[][] outBuffer, outBufferD, outBufferV, outBufferFiltered;
	private int delayInSamplesD, delayInSamplesV;
	private double[] fOld, hPanOld;

	public ThreeDEngine(Main.ThreadData tDataIn, int channelsIn, int sampleRateIn, int bufferSizeIn, double delayBufferDSizeInMs, double delayBufferVSizeInMs) {
		
		data = tDataIn;
		channels = channelsIn;
		sampleRate = sampleRateIn;
		bufferSize = bufferSizeIn;
		delayBufferDSize = (int)(delayBufferDSizeInMs * sampleRate / 1000.0f);
		delayBufferVSize = (int)(delayBufferVSizeInMs * sampleRate / 1000.0f);
		
		delayBufferD = new double[channels][delayBufferDSize];
		delayBufferV = new double[channels][delayBufferVSize];
		inBuffer = new double[channels][bufferSize];
		outBuffer = new double[channels][bufferSize];
		outBufferD = new double[channels][bufferSize];
		outBufferV = new double[channels][bufferSize];
		outBufferFiltered = new double[channels][bufferSize];
		
		fOld = new double[channels];
		hPanOld = new double[channels];
	}
	
	public double[][] processDirection(double[][] inputBuffer, SphericalCoordinates sphericalCoordsSS) {
		
		//Attenuate input
		inBuffer = attenuateAndCopy(inputBuffer, 0.7f);
		outBufferD = new double[channels][bufferSize];
		outBufferV = new double[channels][bufferSize];
		outBufferFiltered = new double[channels][bufferSize];
		
		//Get spherical coordinates from position (swapping y and z axes)
		//sphericalCoordsSS = SphericalCoordinates.fromCartesian(data.soundSourcePos.getX(), data.soundSourcePos.getZ(), data.soundSourcePos.getY());
		double[] hPan = new double[channels];
		double[] hPanLerp = new double[channels];
		double vPan;
		int panResolution = (inBuffer[0].length/10) + 1;
		int count = panResolution;
		hPan = horizontalPan(sphericalCoordsSS);
		vPan = verticalPan(sphericalCoordsSS);
		
		//Perform distance localisation
		for (int j = 0; j < inBuffer[0].length; j++) { //data
			for (int i = 0; i < inBuffer.length; i++) { //channels	
				//Calculate read position from the delay time in milliseconds
				delayInSamplesD = (int)(0.0 * sampleRate / 1000.0);
				readPositionD = (writePositionD - delayInSamplesD + delayBufferDSize - 1) % delayBufferDSize;
				//smooth panning depending on the panResolution value
				if (count == panResolution) { //interpolate only every 100 samples
					Vector1D lerp1D = Vector1D.of(hPanOld[i]).lerp(Vector1D.of(hPan[i]), (double)j/inBuffer[0].length);
					hPanLerp[i] = lerp1D.getX();
					if (i == inBuffer.length-1) count = 0;
				}
				//Read delayed input to output and input buffers, apply panning
				outBufferD[i][j] = delayBufferD[i][readPositionD] * hPanLerp[i] * 1.0;
				//Copy input buffer to delay buffer at write position and add output buffer as feedback
				delayBufferD[i][writePositionD] = inBuffer[i][j];
			}
			count++;
			writePositionD++;
			writePositionD %= delayBufferDSize;
		}
		
		//Perform vertical localisation
		for (int j = 0; j < outBufferD[0].length; j++) { //data
			for (int i = 0; i < outBufferD.length; i++) { //channels	
				//Calculate read position
				delayInSamplesV = (int)(verticalDelay(sphericalCoordsSS) * sampleRate / 1000.0);
				readPositionV = (writePositionV - delayInSamplesV + delayBufferVSize - 1) % delayBufferVSize;
				//smooth panning depending on the panResolution value
				if (count == panResolution) { //interpolate only every 100 samples
					Vector1D lerp1D = Vector1D.of(hPanOld[i]).lerp(Vector1D.of(hPan[i]), (double)j/outBufferD[0].length);
					hPanLerp[i] = lerp1D.getX();
					if (i == outBufferD.length-1) count = 0;
				}
				//Read delayed input to output and input buffers for the delay-and-add model, apply panning
				outBufferV[i][j] = (delayBufferV[i][readPositionV] + outBufferD[i][j]) * hPanLerp[i];
				//Copy input buffer to delay buffer at write position and add output buffer as feedback
				delayBufferV[i][writePositionV] = outBufferD[i][j] + (outBufferV[i][j] * data.vGain);
			}
			count++;
			writePositionV++;
			writePositionV %= delayBufferVSize;
		}
		
		hPanOld = hPan;
		
		//Sum buffers (assign for now)
		outBuffer = outBufferV;
		
		//Apply low pass filtering to output buffer
		double cOFrequency;
		if (sphericalCoordsSS.getAzimuth() > 4 * Math.PI / 4 && sphericalCoordsSS.getAzimuth() < 8 * Math.PI / 4 && sphericalCoordsSS.getPolar() > 0 && sphericalCoordsSS.getPolar() < Math.PI) {
			cOFrequency = (((((1 - hPan[0] * hPan[1]) + (1 - vPan)) / data.panDivision)) * data.upperLimitFrequency) + data.lowerLimitFrequency; //Smooth the transition to the back by altering the cutoff frequency
		}
		else cOFrequency = 20000.0;
		//System.out.println(cOFrequency);
		double alpha = filter(cOFrequency);
		for (int i = 0; i < outBuffer.length; i++) { //channels	
			outBufferFiltered[i][0] = fOld[i] + (alpha * (outBuffer[i][0] - fOld[i]));
			for (int j = 1; j < outBuffer[0].length; j++) { //data
				outBufferFiltered[i][j] = outBufferFiltered[i][j - 1] + (alpha * (outBuffer[i][j] - outBufferFiltered[i][j - 1]));
			}
		}
		//Save old values for the next time around for the buffer
		for (int i = 0; i < outBufferFiltered.length; i++) { //channels
			fOld[i] = outBufferFiltered[i][outBufferFiltered[0].length - 1];
		}
		
		return outBufferFiltered;
	}
	
	private double[][] attenuateAndCopy(double[][] buffer, float gain) {
		
		double[][] copyBuffer = new double[buffer.length][buffer[0].length];

		for (int i = 0; i < buffer.length; i++) { //channels
			for (int j = 0; j < buffer[0].length; j++) { //data
				copyBuffer[i][j] = buffer[i][j] * gain;
			}
		}
		return copyBuffer;
	}
	
	private double verticalDelay(SphericalCoordinates sCoords) {
		return 0.5 * sCoords.getPolar() / Math.PI;
	}
	
	private double[] horizontalPan(SphericalCoordinates sCoords) {
		double[] panning = new double[channels];
		
		if (channels == 2) {
				if (Math.cos(sCoords.getAzimuth()) < 0) {
				panning[0] = 1.0;
				panning[1] = Math.abs(Math.sin(sCoords.getAzimuth()));
			}
			else {
				panning[0] = Math.abs(Math.sin(sCoords.getAzimuth()));
				panning[1] = 1.0;
			}
		}
		else panning[0] = 1.0;
		
		//System.out.println(panning[0] * panning[1]);
		return panning;
	}
	
	private double verticalPan(SphericalCoordinates sCoords) {
		
		return Math.abs(Math.sin(sCoords.getPolar()));
	}
	
	private double filter(double cutoffFrequency) {
		double RC = 1.0 / (cutoffFrequency * 2.0 * Math.PI);
		double dt = 1.0 / sampleRate;
		double a = dt / (RC + dt);
		
		return a;
	}
	
}
