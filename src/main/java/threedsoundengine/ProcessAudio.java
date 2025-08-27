package threedsoundengine;

import org.apache.commons.geometry.euclidean.threed.SphericalCoordinates;

public class ProcessAudio {

	public ThreeDEngine threeDEngine;
	private Main.ThreadData data;
	private SphericalCoordinates sphericalCoordsSS;
	private int channels;
	private int sampleRate;
	private int bufferSize;
	public float delayRD1, delayRD2, delayRD3, delayRD4;
	public float delayVD1, delayVD2, delayVD3, delayVD4;
	public float gainR, gainV;
	private double[][] threeDOut;
	
	public ProcessAudio(Main.ThreadData tDataIn, int channelsIn, int sampleRateIn, int frameSizeIn, int bufferSizeIn) {
		
		data = tDataIn;
		channels = channelsIn;
		sampleRate = sampleRateIn;
		bufferSize = bufferSizeIn;
		
		threeDEngine = new ThreeDEngine(data, channels, sampleRate, bufferSize, 5000.0, 5.0);
		threeDOut = new double [channels][bufferSize];
	}
	
	public double[][] processData(double[][] inputBuffer) {
		
		//Convert sound source position to spherical coordinates, swapping the y and z axes
		sphericalCoordsSS = SphericalCoordinates.fromCartesian(data.soundSourcePos.getX(), data.soundSourcePos.getZ(), data.soundSourcePos.getY());
		threeDOut = threeDEngine.processDirection(inputBuffer, sphericalCoordsSS);
		

		return threeDOut;
	}
}
