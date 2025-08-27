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

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ControlDialog {

	private Main.ThreadData data;
	public JDialog ThreeDEngine;
	static private String message;
	private JCheckBox checkBox;
	private StreamAudio audio;
	private JFrame frame;
	private JSlider sliderVGain, sliderIncrementAzimuth, sliderIncrementPolar;
	private JLabel lblVGain_value, lblIncrementAzimuth_value, lblIncrementPolar_value;
	private JSlider sliderPanDivision, sliderUpperLimitFrequency, sliderLowerLimitFrequency;
	private JLabel lblPanDivision_value, lblUpperLimitFrequency_value, lblLowerLimitFrequency_value;
	
	public ControlDialog(Main.ThreadData tDataIn, JFrame frameIn) throws Exception {
		data = tDataIn;
		frame = frameIn;
		
		setup();
	}

	private void setup() throws Exception {
		ThreeDEngine = new JDialog(frame);
		ThreeDEngine.setTitle("3D Sound Engine Dialog");
		ThreeDEngine.setBounds(100, 100, 800, 500);
		ThreeDEngine.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		ThreeDEngine.getContentPane().setLayout(null);
		ThreeDEngine.setLocation(800, 100);
		

		JButton btnPlay = new JButton("Play Audio");
		btnPlay.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					if (data.fileName == "") {
						System.out.println("No file selected!");
					}
					else {
						audio = new StreamAudio(data);
						new Thread(audio).start();
						System.out.println("Audio thread has been started");

						data.bLoop = checkBox.isSelected();
						data.bAudioStop = false;
						data.bExit = false;
					}
				}
				catch(OutOfMemoryError oome) {
					message = "File size too large.";
					oome.printStackTrace();
					JOptionPane.showMessageDialog(ThreeDEngine, message);
				}
				catch(IllegalArgumentException iae) {
					message = "Error loading audio on system.";
					JOptionPane.showMessageDialog(ThreeDEngine, message);
				}
				catch(Exception ex) {
					message = "Application stopped working. Please restart application.";
					ex.printStackTrace();
					JOptionPane.showMessageDialog(ThreeDEngine, message);
				}
			}
		});
		
		btnPlay.setBounds(400, 400, 97, 25);
		ThreeDEngine.getContentPane().add(btnPlay);

	    checkBox = new JCheckBox("Loop");    
	    checkBox.addItemListener(new ItemListener() {    
            public void itemStateChanged(ItemEvent e) {                 
               data.bLoop = (e.getStateChange()==1?true:false);    
            }    
         });    
	    checkBox.setBounds(330, 400, 70, 25);
	    ThreeDEngine.add(checkBox);

		JButton btnStop = new JButton("Stop");
		btnStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				data.bAudioStop = true;
				data.bExit = true;
			}
		});
		btnStop.setBounds(500, 400, 97, 25);
		ThreeDEngine.getContentPane().add(btnStop);

		//Create a file chooser
		JFileChooser fc = new JFileChooser();
		
		// This is for the 'Browse file' button. It will pop up the file chooser window and copy the path of selected file into the fileName variable
		JButton btnBrowse = new JButton("Browse...");
		fc.addChoosableFileFilter(new FileNameExtensionFilter("Wave Files", "wav"));
		fc.setAcceptAllFileFilterUsed(false);
		btnBrowse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
				if (arg0.getSource() == btnBrowse) {
			        int returnVal = fc.showOpenDialog(null);

			        if (returnVal == JFileChooser.APPROVE_OPTION) {
			            File file = fc.getSelectedFile();
			            //This is where the file path gets copied.
			            	data.fileName = file.getPath();
			        } else {
			        }
				}
			}
		});
		btnBrowse.setBounds(200, 400, 97, 25);
		ThreeDEngine.getContentPane().add(btnBrowse);
	
		JLabel lblTitle = new JLabel("3D Sound Engine 1");
		lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
		Font font = new Font("Courier", Font.BOLD,14);
		lblTitle.setFont(font);
		lblTitle.setBounds(200, 20, 400, 16);
		ThreeDEngine.getContentPane().add(lblTitle);

		
		//This slider is for the vertical gain parameter
		JLabel lblVGain = new JLabel("Vertical Gain:");
		lblVGain.setHorizontalAlignment(SwingConstants.LEFT);
		lblVGain.setBounds(350, 50, 150, 16);
		ThreeDEngine.getContentPane().add(lblVGain);

		sliderVGain = new JSlider(JSlider.HORIZONTAL, 0, 100, (int)(data.vGain*100));
		sliderVGain.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				
				data.vGain = (sliderVGain.getValue()/100.0);
				lblVGain_value.setText(Double.toString(sliderVGain.getValue()/100.0));
			}
		});
		sliderVGain.setBounds(520, 50, 200, 25);
		ThreeDEngine.getContentPane().add(sliderVGain);
	
		lblVGain_value = new JLabel(Integer.toString(sliderVGain.getValue()));
		lblVGain_value.setHorizontalAlignment(SwingConstants.CENTER);
		lblVGain_value.setForeground(Color.GRAY);
		lblVGain_value.setBounds(720, 50, 56, 16);
		ThreeDEngine.getContentPane().add(lblVGain_value);
		
		//This slider is for the azimuth increments parameter
		JLabel lblIncrementAzimuth = new JLabel("Azimuth Angular Rate/10ms:");
		lblIncrementAzimuth.setHorizontalAlignment(SwingConstants.LEFT);
		lblIncrementAzimuth.setBounds(350, 100, 150, 16);
		ThreeDEngine.getContentPane().add(lblIncrementAzimuth);

		sliderIncrementAzimuth = new JSlider(JSlider.HORIZONTAL, 0, 40, (int)(data.incrementAzimuth*1000.0));
		sliderIncrementAzimuth.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				
				data.incrementAzimuth = (sliderIncrementAzimuth.getValue()/1000.0);
				lblIncrementAzimuth_value.setText(Double.toString(sliderIncrementAzimuth.getValue()/1000.0));
			}
		});
		sliderIncrementAzimuth.setBounds(520, 100, 200, 25);
		ThreeDEngine.getContentPane().add(sliderIncrementAzimuth);
	
		lblIncrementAzimuth_value = new JLabel(Double.toString(sliderIncrementAzimuth.getValue()/1000.0));
		lblIncrementAzimuth_value.setHorizontalAlignment(SwingConstants.CENTER);
		lblIncrementAzimuth_value.setForeground(Color.GRAY);
		lblIncrementAzimuth_value.setBounds(720, 100, 56, 16);
		ThreeDEngine.getContentPane().add(lblIncrementAzimuth_value);
		
		//This slider is for the polar increments parameter
		JLabel lblIncrementPolar = new JLabel("Polar Angular Rate/10ms:");
		lblIncrementPolar.setHorizontalAlignment(SwingConstants.LEFT);
		lblIncrementPolar.setBounds(350, 150, 150, 16);
		ThreeDEngine.getContentPane().add(lblIncrementPolar);

		sliderIncrementPolar = new JSlider(JSlider.HORIZONTAL, 0, 20, (int)(data.incrementPolar*1000.0));
		sliderIncrementPolar.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				
				data.incrementPolar = (sliderIncrementPolar.getValue()/1000.0);
				lblIncrementPolar_value.setText(Double.toString(sliderIncrementPolar.getValue()/1000.0));
			}
		});
		sliderIncrementPolar.setBounds(520, 150, 200, 25);
		ThreeDEngine.getContentPane().add(sliderIncrementPolar);
	
		lblIncrementPolar_value = new JLabel(Double.toString(sliderIncrementPolar.getValue()/1000.0));
		lblIncrementPolar_value.setHorizontalAlignment(SwingConstants.CENTER);
		lblIncrementPolar_value.setForeground(Color.GRAY);
		lblIncrementPolar_value.setBounds(720, 150, 56, 16);
		ThreeDEngine.getContentPane().add(lblIncrementPolar_value);
		
		//This slider is for the filter pan division parameter
		JLabel lblPanDivision = new JLabel("Pan Divisions:");
		lblPanDivision.setHorizontalAlignment(SwingConstants.LEFT);
		lblPanDivision.setBounds(350, 200, 150, 16);
		ThreeDEngine.getContentPane().add(lblPanDivision);

		sliderPanDivision = new JSlider(JSlider.HORIZONTAL, 1, 10, (int)(data.panDivision));
		sliderPanDivision.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				
				data.panDivision = (sliderPanDivision.getValue());
				lblPanDivision_value.setText(Integer.toString(sliderPanDivision.getValue()));
			}
		});
		sliderPanDivision.setBounds(520, 200, 200, 25);
		ThreeDEngine.getContentPane().add(sliderPanDivision);
	
		lblPanDivision_value = new JLabel(Integer.toString(sliderPanDivision.getValue()));
		lblPanDivision_value.setHorizontalAlignment(SwingConstants.CENTER);
		lblPanDivision_value.setForeground(Color.GRAY);
		lblPanDivision_value.setBounds(720, 200, 56, 16);
		ThreeDEngine.getContentPane().add(lblPanDivision_value);
		
		//This slider is for the filter upper cut-off frequency parameter
		JLabel lblUpperLimitFrequency = new JLabel("Upper Cut-off Frequency:");
		lblUpperLimitFrequency.setHorizontalAlignment(SwingConstants.LEFT);
		lblUpperLimitFrequency.setBounds(350, 250, 150, 16);
		ThreeDEngine.getContentPane().add(lblUpperLimitFrequency);

		sliderUpperLimitFrequency = new JSlider(JSlider.HORIZONTAL, 0, 100, (int)(data.upperLimitFrequency/1000.0));
		sliderUpperLimitFrequency.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				
				data.upperLimitFrequency = (sliderUpperLimitFrequency.getValue()*1000.0);
				lblUpperLimitFrequency_value.setText(Double.toString(sliderUpperLimitFrequency.getValue()*1000.0));
			}
		});
		sliderUpperLimitFrequency.setBounds(520, 250, 200, 25);
		ThreeDEngine.getContentPane().add(sliderUpperLimitFrequency);
	
		lblUpperLimitFrequency_value = new JLabel(Double.toString(sliderUpperLimitFrequency.getValue()*1000.0));
		lblUpperLimitFrequency_value.setHorizontalAlignment(SwingConstants.CENTER);
		lblUpperLimitFrequency_value.setForeground(Color.GRAY);
		lblUpperLimitFrequency_value.setBounds(720, 250, 56, 16);
		ThreeDEngine.getContentPane().add(lblUpperLimitFrequency_value);
		
		//This slider is for the lower limit frequency parameter
		JLabel lblLowerLimitFrequency = new JLabel("Lower Cut-off Frequency:");
		lblLowerLimitFrequency.setHorizontalAlignment(SwingConstants.LEFT);
		lblLowerLimitFrequency.setBounds(350, 300, 150, 16);
		ThreeDEngine.getContentPane().add(lblLowerLimitFrequency);

		sliderLowerLimitFrequency = new JSlider(JSlider.HORIZONTAL, 0, 100, (int)(data.lowerLimitFrequency/10.0));
		sliderLowerLimitFrequency.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				
				data.lowerLimitFrequency = (sliderLowerLimitFrequency.getValue()*10.0);
				lblLowerLimitFrequency_value.setText(Double.toString(sliderLowerLimitFrequency.getValue()*10.0));
			}
		});
		sliderLowerLimitFrequency.setBounds(520, 300, 200, 25);
		ThreeDEngine.getContentPane().add(sliderLowerLimitFrequency);
	
		lblLowerLimitFrequency_value = new JLabel(Double.toString(sliderLowerLimitFrequency.getValue()*10.0));
		lblLowerLimitFrequency_value.setHorizontalAlignment(SwingConstants.CENTER);
		lblLowerLimitFrequency_value.setForeground(Color.GRAY);
		lblLowerLimitFrequency_value.setBounds(720, 300, 56, 16);
		ThreeDEngine.getContentPane().add(lblLowerLimitFrequency_value);
		
		JLabel lblCredits = new JLabel("<html>Created by James G Stanier 2025<html>");
		lblCredits.setHorizontalAlignment(SwingConstants.CENTER);
		lblCredits.setBounds(50, 200, 200, 32);
		ThreeDEngine.getContentPane().add(lblCredits);

		ThreeDEngine.setVisible(true);
	}
}
