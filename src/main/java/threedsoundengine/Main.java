/*
 * Copyright (c) 2025 James G. Stanier
 *
 * This file is part of ThreeDSoundEngine.
 *
 * This software is dual-licensed under:
 *   1. The GNU General Public License v3.0 (GPLv3)
 *   2. A commercial license (contact j.stanier766(at)gmail.com for details)
 *
 * You may use this file under the terms of the GPLv3 as published by
 * the Free Software Foundation. For proprietary/commercial use,
 * please see the LICENSE-COMMERCIAL file or contact the copyright holder.
 */

package threedsoundengine;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.commons.geometry.euclidean.oned.Vector1D;
import org.apache.commons.geometry.euclidean.threed.AffineTransformMatrix3D;
import org.apache.commons.geometry.euclidean.threed.Plane;
import org.apache.commons.geometry.euclidean.threed.Planes;
import org.apache.commons.geometry.euclidean.threed.SphericalCoordinates;
import org.apache.commons.geometry.euclidean.threed.Triangle3D;
import org.apache.commons.geometry.euclidean.threed.Vector3D;
import org.apache.commons.geometry.euclidean.threed.mesh.TriangleMesh;
import org.apache.commons.geometry.euclidean.threed.rotation.QuaternionRotation;
import org.apache.commons.geometry.euclidean.threed.shape.Sphere;
import org.apache.commons.geometry.euclidean.twod.Vector2D;
import org.apache.commons.numbers.core.Precision;

public class Main {

	class ThreadData {
		public Sphere soundSourceSphere;
		public volatile Vector3D soundSourcePos, listenerPos;
		public String fileName = "";
		public boolean bLoop = false;
		public boolean bAudioStop = false;		
		public boolean bExit = false;		
		public double listenerBoxSize;
		public double vGain;
		public double incrementAzimuth, incrementPolar;
		public double panDivision,upperLimitFrequency, lowerLimitFrequency;
	}

	private JFrame frame;
	private ThreadData tData;
	private double x = 0;
	private double y = 0;
	private int width = 800;
	private int height = 600;
	private double zoom = 2.0;
	private Precision.DoubleEquivalence precision = Precision.doubleEquivalenceOfEpsilon(1e-6);
	private Plane planeX = Planes.fromNormal(Vector3D.of(1, 0, 0), precision);
	private Plane planeY = Planes.fromNormal(Vector3D.of(0, 1, 0), precision);
	private Plane plane2X, plane2Y;
	private TriangleMesh soundSourceMesh;
	private QuaternionRotation yRotation, xRotation;
	private AffineTransformMatrix3D scale, maty, matx;
	private BufferedImage bimg;
	private double[] zBuffer;
	private boolean bSwap = false;
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Main window = new Main();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public Main() throws Exception {
		displayGraphics();
	}
	
	public void displayGraphics() throws Exception {
		
		tData = new ThreadData();
		
		//Set up defaults
		setDefaults();
			
        frame = new JFrame();
        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

		new ControlDialog(tData, frame);
		

		// panel to display render results
        JPanel renderPanel = new JPanel() {
            public void paintComponent(Graphics g) {
            	
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());
                
                width = getWidth();
                height = getHeight();
                
                plane2X = planeX.translate(Vector3D.of(-width/2, 0, 0));
                plane2Y = planeY.translate(Vector3D.of(0, -height/2, 0));

                //Create a sound source shape, mesh and triangle list
                Vector3D boxSizeV = Vector3D.of(tData.listenerBoxSize, tData.listenerBoxSize, tData.listenerBoxSize);
                tData.soundSourceSphere = Sphere.from(tData.soundSourcePos, 0.2, precision);
                soundSourceMesh = tData.soundSourceSphere.toTriangleMesh(2);
                List<Triangle3D> soundSourceMeshList = soundSourceMesh.triangleStream().toList();
               
                double yaw = Math.toRadians(x);
                double pitch = Math.toRadians(y);
                
                scale = AffineTransformMatrix3D.createScale(20, 20, 20);               

                yRotation = QuaternionRotation.fromAxisAngle(plane2Y.getNormal(), -yaw + 33 * Math.PI / 32);
                xRotation = QuaternionRotation.fromAxisAngle(plane2X.getNormal(), -Math.PI / 32);
                
                maty = AffineTransformMatrix3D.createRotation(plane2Y.getOrigin(), yRotation);
                matx = AffineTransformMatrix3D.createRotation(plane2X.getOrigin(), xRotation);

                bimg = new BufferedImage(width ,height, BufferedImage.TYPE_INT_RGB);

                //Initialise zBuffer
                zBuffer = new double[height * width];
                for (int q = 0; q < zBuffer.length; q++) {
                    zBuffer[q] = Double.NEGATIVE_INFINITY;
                }

                //Draw sound source sphere
                drawTriangles(soundSourceMeshList, Color.YELLOW);
                
                //Draw axes
                drawLinezB(scale.apply(maty.apply(matx.apply(Vector3D.of(5, 0, 0)))), scale.apply(maty.apply(matx.apply(Vector3D.of(-5, 0, 0)))), Color.WHITE);
                drawLinezB(scale.apply(maty.apply(matx.apply(Vector3D.of(5, 0.2, 0)))), scale.apply(maty.apply(matx.apply(Vector3D.of(5, -0.2, 0)))), Color.WHITE);
                drawLinezB(scale.apply(maty.apply(matx.apply(Vector3D.of(5, 0, 0.2)))), scale.apply(maty.apply(matx.apply(Vector3D.of(5, 0, -0.2)))), Color.WHITE);
                drawLinezB(scale.apply(maty.apply(matx.apply(Vector3D.of(-5, 0.2, 0)))), scale.apply(maty.apply(matx.apply(Vector3D.of(-5, -0.2, 0)))), Color.WHITE);
                drawLinezB(scale.apply(maty.apply(matx.apply(Vector3D.of(-5, 0, 0.2)))), scale.apply(maty.apply(matx.apply(Vector3D.of(-5, 0, -0.2)))), Color.WHITE);
                drawLinezB(scale.apply(maty.apply(matx.apply(Vector3D.of(0, 5, 0)))), scale.apply(maty.apply(matx.apply(Vector3D.of(0, -5, 0)))), Color.WHITE);
                drawLinezB(scale.apply(maty.apply(matx.apply(Vector3D.of(0.2, 5, 0)))), scale.apply(maty.apply(matx.apply(Vector3D.of(-0.2, 5, 0)))), Color.WHITE);
                drawLinezB(scale.apply(maty.apply(matx.apply(Vector3D.of(0, 5, 0.2)))), scale.apply(maty.apply(matx.apply(Vector3D.of(0, 5, -0.2)))), Color.WHITE);
                drawLinezB(scale.apply(maty.apply(matx.apply(Vector3D.of(0.2, -5, 0)))), scale.apply(maty.apply(matx.apply(Vector3D.of(-0.2, -5, 0)))), Color.WHITE);
                drawLinezB(scale.apply(maty.apply(matx.apply(Vector3D.of(0, -5, 0.2)))), scale.apply(maty.apply(matx.apply(Vector3D.of(0, -5, -0.2)))), Color.WHITE);
                drawLinezB(scale.apply(maty.apply(matx.apply(Vector3D.of(0, 0, 5)))), scale.apply(maty.apply(matx.apply(Vector3D.of(0, 0, -5)))), Color.WHITE);
                drawLinezB(scale.apply(maty.apply(matx.apply(Vector3D.of(0.2, 0, 5)))), scale.apply(maty.apply(matx.apply(Vector3D.of(-0.2, 0, 5)))), Color.WHITE);
                drawLinezB(scale.apply(maty.apply(matx.apply(Vector3D.of(0, 0.2, 5)))), scale.apply(maty.apply(matx.apply(Vector3D.of(0, -0.2, 5)))), Color.WHITE);
                drawLinezB(scale.apply(maty.apply(matx.apply(Vector3D.of(0.2, 0, -5)))), scale.apply(maty.apply(matx.apply(Vector3D.of(-0.2, 0, -5)))), Color.WHITE);
                drawLinezB(scale.apply(maty.apply(matx.apply(Vector3D.of(0, 0.2, -5)))), scale.apply(maty.apply(matx.apply(Vector3D.of(0, -0.2, -5)))), Color.WHITE);
                
                g2.drawImage(bimg, 0, 0, null);
                
            }
        };

		//set up timer to move sound source
		Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                try {
					updatePosition();
					renderPanel.repaint();
				} catch (Exception e) {
					e.printStackTrace();
				}
            }
        }, 10, 10);

       renderPanel.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                double yi = 180.0 / renderPanel.getHeight();
                double xi = 180.0 / renderPanel.getWidth();
                x = (int) (e.getX() * xi) + (renderPanel.getWidth()/3);
                y = -(int) (e.getY() * yi);
                renderPanel.repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {

            }
            
        });
        
        renderPanel.addMouseWheelListener(new MouseWheelListener() {
        	 public void mouseWheelMoved(MouseWheelEvent e) {
        		 zoom += e.getWheelRotation()/5.0;
        		 renderPanel.repaint();
        	 }
        });
        
        renderPanel.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
            	int key = e.getKeyCode();
            	
            	if (key == KeyEvent.VK_RIGHT) {
            	}
            	if (key == KeyEvent.VK_LEFT) {
             	}
            	if (key == KeyEvent.VK_DOWN) {
             	}
            	if (key == KeyEvent.VK_UP) {
            	}
            	if (key == KeyEvent.VK_D) {
            	}
            	
            	renderPanel.repaint();
            }
        });
        
        renderPanel.setFocusable(true);
        renderPanel.requestFocusInWindow();

        pane.add(renderPanel, BorderLayout.CENTER);

        frame.setSize(width, height);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
	}
	
	private void drawTriangles(List<Triangle3D> tris, Color color) {
		
        for (Triangle3D t : tris) {
            Vector3D v1 = t.getPoint1();
            Vector3D v2 = t.getPoint2();
            Vector3D v3 = t.getPoint3();
            
            Vector3D s1 = scale.apply(v1);
            Vector3D s2 = scale.apply(v2);
            Vector3D s3 = scale.apply(v3);
            
            Vector3D r1 = maty.apply(matx.apply(s1));
            Vector3D r2 = maty.apply(matx.apply(s2));
            Vector3D r3 = maty.apply(matx.apply(s3));

            drawLinezB(r1, r2, color);
            drawLinezB(r2, r3, color);
            drawLinezB(r3, r1, color);
            
        }
	}
	private void drawLinezB(Vector3D p1, Vector3D p2, Color color) {
		
        Vector3D p1p = Vector3D.of(perspective(p1).getX(), perspective(p1).getY(), p1.getZ());
        Vector3D p2p = Vector3D.of(perspective(p2).getX(), perspective(p2).getY(), p2.getZ());
        //Find distance between the two
        double distanceXY = Vector2D.of(p1p.getX(), p1p.getY()).distance(Vector2D.of(p2p.getX(), p2p.getY()));
        
        //Scan the line over this distance
        double intervalWidth = 1 / distanceXY;
        for (double i = 0; i < 1; i += intervalWidth) {
        	//Calculate linear interpolation between the two X and Y points
        	Vector2D lerp2D = Vector2D.of(p1p.getX(), p1p.getY()).lerp(Vector2D.of(p2p.getX(), p2p.getY()), i);
        	//Calculate the depth over this distance
        	Vector1D lerp1D = Vector1D.of(p1p.getZ()).lerp(Vector1D.of(p2p.getZ()), i);
        	double depth = lerp1D.getX();
        	
        	//Store depth in buffer, making sure the screen limits are respected
        	if (lerp2D.getX() > 0.0 && lerp2D.getX() < width && lerp2D.getY() > 0.0 && lerp2D.getY() < height) {
        		//If depth is greater than the previous one stored
            	if (depth > zBuffer[((int)(lerp2D.getX()) * height) + (int)lerp2D.getY()]) {
            		//pixel can be drawn
            		bimg.setRGB((int)lerp2D.getX(), (int)lerp2D.getY(), color.getRGB());
            		//New pixel depth is stored
            		zBuffer[((int)(lerp2D.getX()) * height) + (int)lerp2D.getY()] = depth;
            	}
        	}
        }
	}
	
	public void drawPoints(Graphics2D g2, Vector3D start, Vector3D end) {
				
	    g2.drawLine((int)perspective(start).getX(), (int)perspective(start).getY(), 
	    		(int)perspective(end).getX(), (int)perspective(end).getY());


	}
	
	private Vector2D perspective(Vector3D vector) {
		double zDistance = 400 * zoom / 5.0;
		double halfWidth = width/2;
		double halfHeight = height/2;
		
		Vector2D point2D = Vector2D.of(halfHeight * (plane2X.offset(vector)-halfWidth)/(vector.getZ()-zDistance) + halfWidth, 
									   halfHeight * (plane2Y.offset(vector)-halfHeight)/(vector.getZ()-zDistance) + halfHeight);

		return point2D;
	}
		
	private void setDefaults() {
		tData.soundSourcePos = Vector3D.of(3.0, 1.0, 3.0);
		tData.listenerPos = Vector3D.of(2.5, 1.5, 2.5);
        tData.listenerBoxSize = 0.3;
        tData.vGain = 0.0;
        tData.incrementAzimuth = 0.01;
        tData.incrementPolar = 0.004;
        tData.panDivision = 4.0;
        tData.upperLimitFrequency = 40000.0;
        tData.lowerLimitFrequency = 300.0;
        
	}
	
	private void updatePosition() {
		
		//Convert to spherical coordinates whilst swapping the y and z axes
		SphericalCoordinates spherical = SphericalCoordinates.fromCartesian(Vector3D.of(tData.soundSourcePos.getX(), tData.soundSourcePos.getZ(), tData.soundSourcePos.getY()));
		
		//Update Azimuth angle
		double newPosAzimuth = spherical.getAzimuth() + tData.incrementAzimuth * Math.PI;
		//Limit angle
		newPosAzimuth %= 2.0 * Math.PI;
		
		//Update polar angle
		double newPosPolar = spherical.getPolar();
		if (newPosPolar >= 1*Math.PI - tData.incrementPolar * Math.PI) {
			bSwap = false;
		}
		if (newPosPolar <= 0*Math.PI + tData.incrementPolar * Math.PI) {
			bSwap = true;
		}
		if (bSwap) {
			newPosPolar += tData.incrementPolar * Math.PI;
		}
		else {
			newPosPolar -= tData.incrementPolar * Math.PI;
		}
		
		//Update position and convert back to Cartesian coordinates
		tData.soundSourcePos = SphericalCoordinates.toCartesian(spherical.getRadius(), newPosAzimuth, newPosPolar);
		
		//Swap the y and z coordinates back
		tData.soundSourcePos = Vector3D.of(tData.soundSourcePos.getX(), tData.soundSourcePos.getZ(), tData.soundSourcePos.getY());
		
	}
}
