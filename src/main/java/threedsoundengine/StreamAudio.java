/*
 * Copyright (c) 2025 James G. Stanier
 *
 * This file is part of ThreeDSoundEngine.
 *
 * This software is dual-licensed under:
 *   1. The GNU General Public License v3.0 (GPLv3)
 *   2. A commercial license (contact j.stanier766@gmail.com for details)
 *
 * You may use this file under the terms of the GPLv3 as published by
 * the Free Software Foundation. For proprietary/commercial use,
 * please see the LICENSE-COMMERCIAL file or contact the copyright holder.
 */

package threedsoundengine;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class StreamAudio implements Runnable {
		
	private Main.ThreadData data;
	private File f, fOut;
	private AudioInputStream stream;
	private AudioFormat format;
	private FileOutputStream fos;
	private BufferedOutputStream bos = null;
	private DataOutputStream out;
	private static int BUFFER_SIZE = 2048; //for mono
	private int frameSize;
	private int sampleRate;
	private int bitsPerSample;
	private int channels;
	public ProcessAudio processAudio;
	//private long stopTime, startTime;
	

	public StreamAudio(Main.ThreadData tDataIn) throws UnsupportedAudioFileException, IOException {
		data = tDataIn;
		
		setup(data.fileName);
	}

	@Override
	public void run() {
		try {
			playAudio();
			System.out.println("Audio thread exiting...");
		} catch (LineUnavailableException | IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void setup(String fileName) throws UnsupportedAudioFileException, IOException {
		
		f = new File(fileName);
		fOut = new File("output.wav");
		fos = new FileOutputStream(fOut);
		bos = new BufferedOutputStream(fos);
		out = new DataOutputStream(bos);
				
	    stream = AudioSystem.getAudioInputStream(f);
	    format = stream.getFormat();
	    
	    channels = 2; //Output in stereo, even with a mono signal. If mono chosen, modify line out and processing for stereo
        //If mono file input, create a line out with required number of channels, otherwise, pass through
        if (stream.getFormat().getChannels() != channels) {
        	format = new AudioFormat(format.getEncoding(), format.getSampleRate(), format.getSampleSizeInBits(), channels, format.getFrameSize() * channels, format.getFrameRate(), format.isBigEndian());
        }
	    frameSize = format.getFrameSize();
	    sampleRate = (int)format.getSampleRate();
	    bitsPerSample = format.getSampleSizeInBits();
	    
	    //impulseArrayPre = new double[10000][2];

        out.writeBytes("RIFF");// 0-4 ChunkId always RIFF
        out.writeInt(Integer.reverseBytes((int)(stream.getFrameLength() * format.getFrameSize()) + 44));// 5-8 ChunkSize always audio-length +header-length(44)
        out.writeBytes("WAVE");// 9-12 Format always WAVE
        out.writeBytes("fmt ");// 13-16 Subchunk1 ID always "fmt " with trailing whitespace
        out.writeInt(Integer.reverseBytes(16)); // 17-20 Subchunk1 Size always 16
        out.writeShort(Short.reverseBytes((short) 1));// 21-22 Audio-Format 1 for PCM PulseAudio
        out.writeShort(Short.reverseBytes((short)format.getChannels()));// 23-24 Num-Channels 1 for mono, 2 for stereo
        out.writeInt(Integer.reverseBytes((int)format.getSampleRate()));// 25-28 Sample-Rate
        out.writeInt(Integer.reverseBytes((int)(format.getSampleRate() * format.getFrameSize())));// 29-32 Byte Rate
        out.writeShort(Short.reverseBytes((short)format.getFrameSize()));// 33-34 Block Align
        out.writeShort(Short.reverseBytes((short)format.getSampleSizeInBits()));// 35-36 Bits-Per-Sample
        out.writeBytes("data");// 37-40 Subchunk2 ID always data
        out.writeInt(Integer.reverseBytes((int)(5 * stream.getFrameLength() * format.getFrameSize())));// 41-44 Subchunk 2 Size audio-length
        
		processAudio = new ProcessAudio(data, channels, sampleRate, frameSize, BUFFER_SIZE / stream.getFormat().getFrameSize());
		

	}
	
	public void playAudio() throws LineUnavailableException, IOException, InterruptedException {
		
		SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, format, ((int) stream.getFrameLength() * format.getFrameSize()));
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format, BUFFER_SIZE * format.getChannels());
        line.start();
        stream.mark((int)(stream.getFrameLength() * stream.getFormat().getFrameSize()));
        
        int numRead = 0;
        byte[] buf = new byte[BUFFER_SIZE];
        byte[] bufOut = new byte[line.getBufferSize()];
        double[][] doubleArray, doubleArrayTemp;// = new double[channels][buf.length / frameSize];
        while (true) {
	        while ((numRead = stream.read(buf, 0, buf.length)) >= 0) {
	        	
	        	//Convert from byte to double array
	        	doubleArray = toDoubleArray(buf, stream.getFormat().getFrameSize(), stream.getFormat().getChannels());
	        	
	        	//If mono, stream will give a mono buffer. Modify doubleArray to accommodate
	        	if (stream.getFormat().getChannels() != channels) {
	        		doubleArrayTemp = new double[channels][buf.length / stream.getFormat().getFrameSize()];
	        		doubleArrayTemp[0] = doubleArray[0];
	        		doubleArrayTemp[1] = doubleArray[0];
	        		doubleArray = doubleArrayTemp;
	        	}

	        	
	    		//startTime = System.currentTimeMillis();
	        		        	
	        	doubleArray = processAudio.processData(doubleArray);
	        	
	    		//Apply attenuation to prevent clipping
	        	doubleArray = automaticGainControl(doubleArray, bitsPerSample);

	    		//stopTime = System.currentTimeMillis();
	    		//System.out.println(stopTime - startTime);

	    		//Convert back from double to byte array
	        	bufOut = toByteArray(doubleArray, frameSize, channels);
	        	
	        	int offset = 0;
	        	if (stream.getFormat().getChannels() != channels) numRead *= format.getChannels();
	        	while (offset < numRead) {
	        		offset += line.write(bufOut, offset, numRead - offset);
	        	}
	        	out.write(bufOut);
	        	
	        	if (data.bAudioStop) break;
	        }
	    	if (data.bLoop) stream.reset();
	    	else break;
	    	if (data.bAudioStop) break;
        }
        
        line.drain();
        line.stop();
        out.flush();
        out.close();

	}
	
	private double[][] automaticGainControl(double[][] array, int bitsPerSampleIn) {
		
		//Scan array for maximum value
		double max = 0.0;
		boolean bAttenuate = false;
		double atten = 1.0;
    	for (int i = 0; i < array.length; i++) { //channel
    		for (int j = 0; j < array[0].length; j++) { //data
    			if (Math.abs(array[i][j]) > max) max = Math.abs(array[i][j]);
    			
    		}
    	}
    	
    	//Calculate extent to attenuate
    	double maxValue = Math.pow(2, bitsPerSampleIn-1) - 100;
    	if (max > maxValue) {
    		bAttenuate = true;
        	atten = maxValue / max;
    	}
    	//Attenuate buffer by this extent if necessary
    	if (bAttenuate) {
	    	for (int i = 0; i < array.length; i++) { //channel
	    		for (int j = 0; j < array[0].length; j++) { //data
	    			array[i][j] *= atten;
	    		}
	    	}
    	}
    	
    	return array;
	}
	
	private byte[] toByte(double value) {
	    byte[] bytes = new byte[2];
	    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putShort((short)value);
	    return bytes;
	
	}
	
	private double toDouble(byte[] value) {
		return (double)ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN).getShort();
	}
	
	private byte[] toByteArray(double[][] array, int frameSize, int channel) {
		byte[] byteArray = new byte[array[0].length * frameSize];
		byte[] bytes = new byte[frameSize / channel];
		int index = 0;
		
		for (int j = 0; j < array[0].length; j++) {
			for (int i = 0; i < array.length; i++) {
				bytes = toByte(array[i][j]);
				for (int k = 0; k < frameSize / channel; k++) {
					byteArray[index++] = bytes[k];
				}
			}
		}
		return byteArray;
	}
	
	private double[][] toDoubleArray(byte[] array, int frameSize, int channel) {
		double[][] doubleArray = new double[channel][array.length / frameSize];
		byte[] bytes = new byte[frameSize / channel];
		int index = 0;
		
		for (int j = 0; j < doubleArray[0].length; j++) { //data
			for (int i = 0; i < doubleArray.length; i++) { //channel
				for (int k = 0; k < frameSize / channel; k++) {
					bytes[k] = array[index++];
				}
				doubleArray[i][j] = toDouble(bytes);
			}	
		}
		return doubleArray;
	}
}
