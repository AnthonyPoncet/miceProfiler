/*
 * Copyright 2011, 2012 Institut Pasteur.
 * 
 * This file is part of MiceProfiler.
 * 
 * MiceProfiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MiceProfiler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MiceProfiler. If not, see <http://www.gnu.org/licenses/>.
 */
package plugins.fab.MiceProfiler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;
import icy.gui.util.GuiUtil;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.main.Icy;
import icy.painter.Painter;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.DataType;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.DataBuffer;
import java.io.File;
import java.util.*;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.phys2d.math.ROVector2f;
import net.phys2d.math.Vector2f;
import net.phys2d.raw.Body;
import net.phys2d.raw.DistanceJoint;
import net.phys2d.raw.SlideJoint;
import net.phys2d.raw.World;
import net.phys2d.raw.shapes.Box;
import net.phys2d.raw.shapes.Circle;
import net.phys2d.raw.shapes.Polygon;
import net.phys2d.raw.shapes.Shape;
import net.phys2d.raw.strategies.QuadSpaceStrategy;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Physics model of the 2d mouse
 * 
 */
public class PhyMouse implements ActionListener, ChangeListener {
	private final Sequence sequence;
	private final World world;

	//Panel for Mice Profiler window
	private final JPanel panel;
	private final JCheckBox displayBinaryMapCheckBox = new JCheckBox("Binary Map", false);
	private final JCheckBox displayGradientMapCheckBox = new JCheckBox("Gradient Map", false);
	private final JCheckBox displayForceCheckBox = new JCheckBox("Forces", false);
	private final JCheckBox displayEnergyAreaCheckBox = new JCheckBox("Energy Area", false);
	private final JCheckBox displayBodyCenterCheckBox = new JCheckBox("Body Center", false);
	private final JCheckBox displayBodyShapeCheckBox = new JCheckBox("Body Shape", true);
	private final JCheckBox displayGlobalSplineCheckBox = new JCheckBox("Global Spline", true);
	private final JCheckBox displaySlideJointCheckBox = new JCheckBox("Slide Joint", true);
	private final JCheckBox displayDistanceJointCheckBox = new JCheckBox("Distance Joint", true);
	private final JCheckBox displayMemoryCheckBox = new JCheckBox("Track Memory", true);
	private final JCheckBox useMotionPredictionCheckBox = new JCheckBox("Use motion prediction", false);
	private int thresholdBinaryMap = 30;
	private final JSpinner binaryThresholdSpinner = new JSpinner(new SpinnerNumberModel(thresholdBinaryMap, 0, 255, 10));
	private float mouseScaleModel = 0.22f;
	private final JTextField scaleTextField = new JTextField(Float.toString(mouseScaleModel));



    private IcyBufferedImage binaryMap;
    private IcyBufferedImage edgeMap;


    private final List<Body> bodyList = Lists.newArrayList();
    private final List<SlideJoint> slideJointList = Lists.newArrayList();
    private final List<DistanceJoint> distanceJointList = Lists.newArrayList();


    private final List<Mouse> mouseList = Lists.newArrayList();
    private final Map<Integer, MouseInfoRecord> mouseARecord = Maps.newHashMap();
    private final Map<Integer, MouseInfoRecord> mouseBRecord = Maps.newHashMap();

    /** used by painter */
	private boolean motion_prediction_state = false;

	/*

	private final static float SCALERAY = 1f;
	private final static float GRADIENT_ENERGY_MULTIPLICATOR = 10000;
	private final static float BINARY_ENERGY_MULTIPLICATOR = 10000;



	private final JCheckBox pauseTrackAllBox = new JCheckBox("pause track all", false);
	private final JCheckBox displayStepBox = new JCheckBox("display step", false);

	private final static int SEUIL_EDGE_MAP = 32;



	private final JButton resultButton = new JButton("give me the results");

	private final Map<Integer, ArrayList<Body>> bodyHash = new HashMap<Integer, ArrayList<Body>>();

	private final JButton displayScaleBinaryButton = new JButton("display binary scales");
	private final JButton computeATestAnchorVectorMapButton = new JButton("compute test anchor");


    private final Map<Integer, MouseView> mouse1view = new HashMap<Integer, MouseView>();
    private final Map<Integer, MouseView> mouse2view = new HashMap<Integer, MouseView>();


    private ArrayList<Scale> binaryScaleMap = null;




    private boolean reverseThresholdBoolean = false;

    private final MAnchor2D[] headForcedPosition = new MAnchor2D[2];*/

    public PhyMouse(Sequence sequence) {
		this.sequence = sequence;
		this.world = new World(new Vector2f(0, 0), 10, new QuadSpaceStrategy(20, 5));
		this.world.clear();
		this.world.setGravity(0, 0);

		//Create Panel for Mice Profiler window
		this.panel = GuiUtil.generatePanel();
		this.panel.add(GuiUtil.besidesPanel(displayBinaryMapCheckBox, displayGradientMapCheckBox));
		this.panel.add(GuiUtil.besidesPanel(displayForceCheckBox, displayEnergyAreaCheckBox));
		this.panel.add(GuiUtil.besidesPanel(displayBodyCenterCheckBox, displayBodyShapeCheckBox));
		this.panel.add(GuiUtil.besidesPanel(displayGlobalSplineCheckBox, displaySlideJointCheckBox));
		this.panel.add(GuiUtil.besidesPanel(displayDistanceJointCheckBox, displayMemoryCheckBox));
		this.panel.add(GuiUtil.besidesPanel(useMotionPredictionCheckBox));
		this.panel.add(GuiUtil.besidesPanel(new JLabel("Binary Threshold:"), binaryThresholdSpinner));
		JLabel mouseModelScaleLabel = new JLabel("Mouse Model Scale:");
		mouseModelScaleLabel.setToolTipText("Scale of the model.");
		this.panel.add(GuiUtil.besidesPanel(mouseModelScaleLabel, scaleTextField)); // applyNewScaleButton
		this.displayBinaryMapCheckBox.addActionListener(this);
		this.displayGradientMapCheckBox.addActionListener(this);
		this.displayForceCheckBox.addActionListener(this);
		this.displayEnergyAreaCheckBox.addActionListener(this);
		this.displayBodyCenterCheckBox.addActionListener(this);
		this.displayBodyShapeCheckBox.addActionListener(this);
		this.displayGlobalSplineCheckBox.addActionListener(this);
		this.displaySlideJointCheckBox.addActionListener(this);
		this.displayDistanceJointCheckBox.addActionListener(this);
		this.binaryThresholdSpinner.addChangeListener(this);

		/*this.resultButton.addActionListener(this);
		this.computeATestAnchorVectorMapButton.addActionListener(this);
		this.displayScaleBinaryButton.addActionListener(this);*/
	}

	public void generateMouse(float x, float y, float alpha) {
		mouseScaleModel = Float.parseFloat(scaleTextField.getText());
		mouseList.add(new Mouse(this, x, y, alpha));
	}

	private Body copyBody(Body source) {
		Body target = null;
		if (source.getShape() instanceof Box) {
			final Box box = (Box) source.getShape();
			target = new Body("", new Box(box.getSize().getX(), box.getSize()
					.getY()), source.getMass());
		}

		if (source.getShape() instanceof Circle) {
			final Circle circle = (Circle) source.getShape();
			target = new Body("", new Circle(circle.getRadius()),
					source.getMass());
		}

		target.setDamping(source.getDamping());
		target.setPosition(source.getPosition().getX(), source.getPosition()
				.getY());
		target.setGravityEffected(false);
		target.setRotation(source.getRotation());
		target.adjustVelocity(new Vector2f(source.getVelocity().getX(), source
				.getVelocity().getY()));

		final EnergyInfo energyInfo = (EnergyInfo) source.getUserData();
		target.setUserData(energyInfo);

		return target;
	}

    private class MouseView {
        private final Point2D headPosition;

        private final Point2D bodyPosition;

        private final float headAngle;

        public MouseView(Point2D headPosition, Point2D bodyPosition, float headAngle) {
            this.headPosition = headPosition;
            this.bodyPosition = bodyPosition;
            this.headAngle = headAngle;
        }

    }

	public void computeForcesMap(IcyBufferedImage imageSource) {
		//System.out.println( "refresh boolean: " + reverseThresholdBoolean );
		ROI2D clipROI = null;
		Sequence activeSequence = Icy.getMainInterface().getFocusedSequence();
		if (activeSequence != null) {
            for (ROI roi : activeSequence.getROIs()) {
                clipROI = (ROI2D) roi;
            }
        }

		int imageSourceWidth = imageSource.getWidth();
		int imageSourceHeight = imageSource.getHeight();

		if (binaryMap == null) {
			binaryMap = new IcyBufferedImage(imageSourceWidth, imageSourceHeight, 1, DataType.UBYTE);
		}
		if ((binaryMap.getWidth() != imageSourceWidth) || (binaryMap.getHeight() != imageSourceHeight)) {
			binaryMap = new IcyBufferedImage(imageSourceWidth, imageSourceHeight, 1, DataType.UBYTE);
		}

		byte[] binaryMapDataBuffer = binaryMap.getDataXYAsByte(0);
		byte[] imageSourceDataBuffer = imageSource.getDataXYAsByte(1);

		for (int x = 0; x < imageSourceWidth; x++) {
            for (int y = 0; y < imageSourceHeight; y++) {
                int val = imageSourceDataBuffer[x + y * imageSourceWidth] & 0xFF;

                if (val < thresholdBinaryMap)
                    val = 255;
                else
                    val = 0;

                if (reverseThresholdBoolean) {
                    if (val == 255) {
                        val = 0;
                    } else {
                        val = 255;
                    }
                }

                if (clipROI != null) {
                    if (!clipROI.contains(x, y))
                        val = 0;
                }

                binaryMapDataBuffer[x + y * imageSourceWidth] = (byte) val;
            }
        }

		// compute Edge Object EnergyMap
		if (edgeMap == null) {
			edgeMap = new IcyBufferedImage(imageSourceWidth, imageSourceHeight, 1, DataType.UBYTE);
		}

		int maxWidth = binaryMap.getWidth() - 1;
		int maxHeight = binaryMap.getHeight() - 1;

		byte[] edgeMapDataBuffer = edgeMap.getDataXYAsByte(0);

		for (int x = 1; x < maxWidth; x++) {
            for (int y = 1; y < maxHeight; y++) {
                int val1 = binaryMapDataBuffer[x + y * imageSourceWidth] & 0xFF;
                int val2 = binaryMapDataBuffer[x + 1 + y * imageSourceWidth] & 0xFF;

                int val4 = binaryMapDataBuffer[x + (y + 1) * imageSourceWidth] & 0xFF;

                int val = Math.abs(val1 - val2) + Math.abs(val1 - val4);

                if (val > SEUIL_EDGE_MAP) // avant 32 // avant 16 encore
                    val = 255;
                else
                    val = 0;

                edgeMapDataBuffer[x + y * imageSourceWidth] = (byte) val;
            }
        }
	}

	public void clearMapForROI() {
        mouseList.clear();
        bodyList.clear();
        distanceJointList.clear();
        slideJointList.clear();
        world.clear();
    }

    public List<Mouse> getMouseList() {
        return mouseList;
    }

    /**
	 * True: ajoute False: remove
	 * 
	 * @param set255
	 */
	private void drawCircleInMaskMap(int centreX, int centreY, int ray, IcyBufferedImage maskImage, boolean set255) {
		byte val = 0;
		if (set255) {
			val = (byte) 255;
		}

		byte[] maskMapData = maskImage.getDataXYAsByte(0);

		int raySquare = ray * ray;
		int width = maskImage.getWidth();
		int height = maskImage.getHeight();

		for (int y = -ray; y <= ray; y++) {
            for (int x = -ray; x <= ray; x++) {
                if (x * x + y * y <= raySquare) {
                    int xx = centreX + x;
                    int yy = centreY + y;

                    if (xx >= 0) {
                        if (yy >= 0) {
                            if (xx < width) {
                                if (yy < height) {
                                    maskMapData[xx + yy * width] = val;
                                }
                            }
                        }
                    }

                }
            }
        }
	}

	public void computeForces() {
		// force location of the head
		if (headForcedPosition[0] != null){
			MAnchor2D pos = headForcedPosition[0];
			mouseList.get( 0 ).headBody.setPosition( (float) pos.getX() , (float)pos.getY() );
		}
		if ( headForcedPosition[1] != null ){
			MAnchor2D pos = headForcedPosition[1];
			mouseList.get( 1 ).headBody.setPosition( (float) pos.getX() , (float)pos.getY() );
		}

		// compute forces
		IcyBufferedImage maskMap = new IcyBufferedImage(binaryMap.getWidth(), binaryMap.getHeight(), 1, DataBuffer.TYPE_BYTE);
		byte[] maskMapData = maskMap.getDataXYAsByte(0);
		int maskMapWidth = maskMap.getWidth();

		// apply energy.
		for (final Body body : bodyList) {
            int length = maskMapData.length;
            for (int i = 0; i < length; i++) {
                maskMapData[i] = 0;
            }

			EnergyInfo energyInfo = (EnergyInfo) body.getUserData();

			drawCircleInMaskMap((int) body.getLastPosition().getX(), (int) body.getLastPosition().getY(), (int) energyInfo.ray, maskMap, true);

			for (final Body body2 : bodyList) {
                if (body != body2) {
                    EnergyInfo energyInfo2 = (EnergyInfo) body2.getUserData();
                    if (energyInfo.energyMap == energyInfo2.energyMap) {
                        if (energyInfo2.excludeFromAttractiveMapOwner) {
                            drawCircleInMaskMap((int) body2.getLastPosition().getX(), (int) body2.getLastPosition().getY(), (int) energyInfo2.ray, maskMap, false);
                        }
                    }
                }
            }

			if (energyInfo.energyMap == EnergyMap.BINARY_MOUSE) {
				float vx = 0;
				float vy = 0;
				float count = 0;

				int maxX = (int) (body.getLastPosition().getX() + energyInfo.ray);
				int maxY = (int) (body.getLastPosition().getY() + energyInfo.ray);

				int imageWidth = binaryMap.getWidth();
				int imageHeight = binaryMap.getHeight();
				byte[] binaryMapDataBuffer = binaryMap.getDataXYAsByte(0);

				for (int x = (int) (body.getLastPosition().getX() - energyInfo.ray); x < maxX; x++) {
                    for (int y = (int) (body.getLastPosition().getY() - energyInfo.ray); y < maxY; y++) {
                        if (x >= imageWidth)
                            continue;
                        if (y >= imageHeight)
                            continue;
                        if (x < 0)
                            continue;
                        if (y < 0)
                            continue;

                        float factor = 0.5f;
                        if (maskMapData[x + y * maskMapWidth] != 0) {
                            factor = 1f;
                        }

                        if (maskMapData[x + y * maskMapWidth] != 0) {
                            if ((binaryMapDataBuffer[x + y * imageWidth] & 0xFF) == 255) {
                                vx += (x - body.getLastPosition().getX()) * factor;
                                vy += (y - body.getLastPosition().getY()) * factor;
                            }
                            count++;
                        }
                    }
                }

				if ( count > 0 ) {
					vx /=count;
					vy /=count;
				}

				vx *= BINARY_ENERGY_MULTIPLICATOR;
				vy *= BINARY_ENERGY_MULTIPLICATOR;

				energyInfo.setVx(vx);
				energyInfo.setVy(vy);

				body.setForce(vx, vy);
			}

			if (energyInfo.energyMap == EnergyMap.GRADIENT_MAP) {
				float vx = 0;
				float vy = 0;
				float count = 0;

				int imageWidth = edgeMap.getWidth();
				int imageHeight = edgeMap.getHeight();

				byte[] edgeMapDataBuffer = edgeMap.getDataXYAsByte(0);

				int maxX = (int) (body.getLastPosition().getX() + energyInfo.ray);
				int maxY = (int) (body.getLastPosition().getY() + energyInfo.ray);

				for (int x = (int) (body.getLastPosition().getX() - energyInfo.ray); x < maxX; x++) {
                    for (int y = (int) (body.getLastPosition().getY() - energyInfo.ray); y < maxY; y++) {
                        // if ( mask.contains( x ,y ) )

                        if (x >= imageWidth)
                            continue;
                        if (y >= imageHeight)
                            continue;
                        if (x < 0)
                            continue;
                        if (y < 0)
                            continue;

                        if (maskMapData[x + y * maskMapWidth] != 0) {
                            if ((edgeMapDataBuffer[x + y * imageWidth] & 0xFF) != 0) {
                                vx += x - body.getLastPosition().getX();
                                vy += y - body.getLastPosition().getY();
                            }
                            count++;
                        }
                    }
                }

				if (count > 0) {
					vx /=count;
					vy /=count;
				}
				
				vx *= GRADIENT_ENERGY_MULTIPLICATOR; // *50
				vy *= GRADIENT_ENERGY_MULTIPLICATOR;

				energyInfo.setVx(vx);
				energyInfo.setVy(vy);

				body.setForce(vx, vy);
			}
		}

		// remove all forced positions.
		headForcedPosition[0] = null;
		headForcedPosition[1] = null;
	}

	private class MouseInfoRecord {
		private final Point2D headPosition;
        private final Point2D tailPosition;
        private final Point2D bodyPosition;
        private final Point2D neckPosition; // body that links to the head to compute head angle.

        public MouseInfoRecord(Point2D headPosition, Point2D tailPosition, Point2D bodyPosition, Point2D neckPosition) {
            this.headPosition = headPosition;
            this.tailPosition = tailPosition;
            this.bodyPosition = bodyPosition;
            this.neckPosition = neckPosition;
        }
    }

	public void swapIdentityRecordFromTToTheEnd(int time) {
		// look for maximum time recorded
		int maxT = 0;
		for ( int i : mouseARecord.keySet() ) {
			if ( i > maxT ) maxT = i;
		}
		for ( int i : mouseBRecord.keySet() ) {
			if ( i > maxT ) maxT = i;
		}
		
		// swap data
		for ( int t = time ; t <= maxT ; t++ ) {
			MouseInfoRecord recA = mouseARecord.get( t );
			MouseInfoRecord recB = mouseBRecord.get( t );
			
			mouseARecord.put( t , recB );
			mouseBRecord.put( t , recA );
		}
		
	}

	public void recordMousePosition(int currentFrame) {
		// record mouse A
		{
            Point2D headPosition = new Point2D.Float(mouseList.get(0).headBody.getPosition().getX(), mouseList.get(0).headBody.getPosition().getY());
            Point2D tailPosition = new Point2D.Float(mouseList.get(0).tail.getPosition().getX(), mouseList.get(0).tail.getPosition().getY());
            Point2D bodyPosition = new Point2D.Float(mouseList.get(0).tommyBody.getPosition().getX(), mouseList.get(0).tommyBody.getPosition().getY());
            Point2D neckPosition = new Point2D.Float(mouseList.get(0).neckAttachBody.getPosition().getX(), mouseList.get(0).neckAttachBody.getPosition().getY());
            MouseInfoRecord mouseAInfo = new MouseInfoRecord(headPosition, tailPosition, bodyPosition, neckPosition);
            mouseARecord.put(currentFrame, mouseAInfo);
		}
		// record mouse B
		if (mouseList.size() > 1) // is there 2 mice ?
		{
            Point2D headPosition = new Point2D.Float(mouseList.get(1).headBody.getPosition().getX(), mouseList.get(1).headBody.getPosition().getY());
            Point2D tailPosition = new Point2D.Float(mouseList.get(1).tail.getPosition().getX(), mouseList.get(1).tail.getPosition().getY());
            Point2D bodyPosition = new Point2D.Float(mouseList.get(1).tommyBody.getPosition().getX(), mouseList.get(1).tommyBody.getPosition().getY());
            Point2D neckPosition = new Point2D.Float(mouseList.get(1).neckAttachBody.getPosition().getX(), mouseList.get(1).neckAttachBody.getPosition().getY());
            MouseInfoRecord mouseBInfo = new MouseInfoRecord(headPosition, tailPosition, bodyPosition, neckPosition);
            mouseBRecord.put(currentFrame, mouseBInfo);
		}
	}

	public void worldStep(int currentFrame) {
		motion_prediction_state = false;

		world.step();
		nbTotalIteration++;

		while (pauseTrackAllBox.isSelected()) {
			try {
				Thread.sleep(100);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}

		recordMousePosition(currentFrame);

		if (displayStepBox.isSelected()) {
			sequence.painterChanged(null);
		}
	}

	/**
	 * Applique la motion prediction et passe l'inertie a 0.
	 */
	public void applyMotionPrediction() {
		if (!useMotionPredictionCheckBox.isSelected())
			return;

		motion_prediction_state = true;

		// record and motion prediction.
		for (Mouse mouse : mouseList) {
			for (Body body : mouse.bodyList) {
				// record current position.
				EnergyInfo eInfo = (EnergyInfo) body.getUserData();
				// copy the object.
				ROVector2f vCopy = new Vector2f(body.getLastPosition().getX(), body.getLastPosition().getY());

				body.setForce(0, 0);

				eInfo.previousPositionList.add(vCopy);

				// compute la prediction (t) - (t-1)
				if (eInfo.previousPositionList.size() > 1) { // no prediction for first frame.
					Vector2f newVelocity = new Vector2f(
					        10f * (eInfo.previousPositionList.get(1).getX() - eInfo.previousPositionList.get(0).getX()),
							10f * (eInfo.previousPositionList.get(1).getY() - eInfo.previousPositionList.get(0).getY()));

					body.adjustVelocity(newVelocity);
					eInfo.previousPositionList.remove(eInfo.previousPositionList.get(0));
				}
			}
		}

		for (int i = 0; i < 6; i++) {
			while (pauseTrackAllBox.isSelected()) {
				try {
					Thread.sleep(100);
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}

			world.step();

			if (displayStepBox.isSelected()) {
				// sequence.refreshPainter();
				// try {
				// Thread.sleep( 200 );
				// } catch (InterruptedException e) {
				//
				// e.printStackTrace();
				// }

				// if ( i == 5 )
				// try {
				// Thread.sleep( 1000 );
				// } catch (InterruptedException e) {
				//
				// e.printStackTrace();
				// }
			}
		}

		for (final Body body : bodyList) {
			body.adjustVelocity(new Vector2f(0, 0));
		}

	}

	private SlideJoint generateSlideJoint(Body bodyA, Body bodyB, float min, float max) {
		SlideJoint slideJoint = new SlideJoint(bodyA, bodyB, new Vector2f(0, 0), new Vector2f(0, 0f), min, max, 0);
		slideJointList.add(slideJoint);
		world.add(slideJoint);
		return slideJoint;
	}

	public void paint(Graphics g, IcyCanvas canvas) {
		if (!(canvas instanceof IcyCanvas2D))
			return;

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setStroke(new BasicStroke(0.3f));
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
		if (displayBinaryMapCheckBox.isSelected()) {
			if (binaryMap != null)
				g2.drawImage(IcyBufferedImageUtil.toBufferedImage(binaryMap, null), null, 0, 0);
		}
		if (displayGradientMapCheckBox.isSelected()) {
			if (edgeMap != null)
				g2.drawImage(IcyBufferedImageUtil.toBufferedImage(edgeMap, null), null, 0, 0);
		}
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

		// paint SlideJoint
		if (displaySlideJointCheckBox.isSelected()) {
			g2.setColor(Color.ORANGE);
			for (SlideJoint slideJoint : slideJointList) {
				Line2D line = new Line2D.Float(
				        slideJoint.getBody1().getLastPosition().getX(),
                        slideJoint.getBody1().getLastPosition().getY(),
                        slideJoint.getBody2().getLastPosition().getX(),
                        slideJoint.getBody2().getLastPosition().getY());
				g2.draw(line);
			}
		}

		// paint DistanceJoint
		if (displayDistanceJointCheckBox.isSelected()) {
			g2.setColor(Color.YELLOW);
			for (DistanceJoint distanceJoint : distanceJointList) {
				Line2D line = new Line2D.Float(
				        distanceJoint.getBody1().getLastPosition().getX(),
                        distanceJoint.getBody1().getLastPosition().getY(),
                        distanceJoint.getBody2().getLastPosition().getX(),
                        distanceJoint.getBody2().getLastPosition().getY());
				g2.draw(line);
			}
		}

		// paint Bodie's center
		if (displayBodyCenterCheckBox.isSelected()) {
			g2.setColor(Color.BLUE);
			for (Body body : bodyList) {
				Ellipse2D ellipse = new Ellipse2D.Float(body.getLastPosition().getX() - 1.5f, body.getLastPosition().getY() - 1.5f, 3, 3);
				g2.draw(ellipse);
			}
		}

		// paint Bodie's shape (if any)
		if (displayBodyShapeCheckBox.isSelected()) {
			g2.setColor(Color.WHITE);
			for (Body body : bodyList) {
				Shape shape = body.getShape();
				if (shape != null) {
                    if (shape instanceof Polygon) {
                        ROVector2f[] vert = ((Polygon) shape).getVertices();
                        for (int i = 0; i < vert.length - 1; i++) {
                            AffineTransform transform = g2.getTransform();
                            g2.translate(body.getLastPosition().getX(), body.getLastPosition().getY());
                            g2.rotate(body.getRotation());

                            Line2D line2D = new Line2D.Float(vert[i].getX(), vert[i].getY(), vert[i + 1].getX(), vert[i + 1].getY());
                            g2.draw(line2D);

                            g2.setTransform(transform);
                        }
                        AffineTransform transform = g2.getTransform();
                        g2.translate(body.getLastPosition().getX(), body.getLastPosition().getY());
                        g2.rotate(body.getRotation());
                        Line2D line2D = new Line2D.Float(vert[0].getX(), vert[0].getY(), vert[vert.length - 1].getX(), vert[vert.length - 1].getY());
                        g2.draw(line2D);
                        g2.setTransform(transform);

                    }
                    if (shape instanceof Box) {
                        Box maBox = (Box) shape;

                        AffineTransform transform = g2.getTransform();
                        g2.translate(body.getLastPosition().getX(), body.getLastPosition().getY());
                        g2.rotate(body.getRotation());
                        Rectangle2D rect2D = new Rectangle2D.Float(-maBox.getSize().getX() / 2f, -maBox.getSize().getY() / 2f, maBox.getSize().getX(), maBox.getSize().getY());
                        g2.draw(rect2D);
                        g2.setTransform(transform);
                    }
                    if (shape instanceof Circle) {
                        Circle maCircle = (Circle) shape;

                        AffineTransform transform = g2.getTransform();
                        g2.translate(body.getLastPosition().getX(), body.getLastPosition().getY());
                        g2.rotate(body.getRotation());
                        final Ellipse2D ellipse2D = new Ellipse2D.Double(-maCircle.getRadius(), -maCircle.getRadius(), maCircle.getRadius() * 2, maCircle.getRadius() * 2);
                        g2.draw(ellipse2D);
                        g2.setTransform(transform);
                    }
                }
			}
		}

		// paint Energy area
        for (Body body : bodyList) {
            g2.setStroke(new BasicStroke(0.5f));
            EnergyInfo energyInfo = (EnergyInfo) body.getUserData();
            switch (energyInfo.getEnergyMap()) {
                case NO_ENERGY:
                    g2.setColor(Color.BLACK);
                    break;
                case GRADIENT_MAP:
                    g2.setColor(Color.PINK);
                    break;
                case BINARY_MOUSE:
                    g2.setColor(Color.GREEN);
                    break;
                case BINARY_EAR:
                    g2.setColor(Color.RED);
                    break;
            }

            if (displayEnergyAreaCheckBox.isSelected()) {
                if (energyInfo.getEnergyMap() != EnergyMap.NO_ENERGY) {
                    Ellipse2D ellipse = new Ellipse2D.Float(
                            body.getLastPosition().getX() - energyInfo.getRay(),
                            body.getLastPosition().getY() - energyInfo.getRay(),
                            energyInfo.getRay() * 2 + 1,
                            energyInfo.getRay() * 2 + 1);
                    g2.draw(ellipse);
                }
            }

            boolean displayForce = displayForceCheckBox.isSelected();
            if (displayBinaryMapCheckBox.isSelected() && energyInfo.getEnergyMap() != EnergyMap.BINARY_MOUSE)
                displayForce = false;
            if (displayGradientMapCheckBox.isSelected() && energyInfo.getEnergyMap() != EnergyMap.GRADIENT_MAP)
                displayForce = false;
            if (displayForce) {
                Line2D energyVector = new Line2D.Float(
                        body.getLastPosition().getX(),
                        body.getLastPosition().getY(),
                        body.getLastPosition().getX() + energyInfo.getVx(),
                        body.getLastPosition().getY() + energyInfo.getVy());
                g2.draw(energyVector);
            }
        }


		g2.setColor(Color.BLUE);

		if (motion_prediction_state) {
			g2.setColor(Color.WHITE);
			g2.drawString("Motion prediction (movie slowed)", 20, 200);
		}

		g2.setColor(Color.WHITE);

		// Display at t (passe egalement) of mouseA and mouseB
		int currentFrame = 0;
        if (displayMemoryCheckBox.isSelected()) {
            // Mouse A
            MouseInfoRecord mouseAInfo = mouseARecord.get(currentFrame);
		    if (mouseAInfo != null) {
		        g2.setColor(Color.RED);

		        Ellipse2D ellipseHead = new Ellipse2D.Double(mouseAInfo.headPosition.getX() - 22 * mouseScaleModel, mouseAInfo.headPosition.getY() - 22 * mouseScaleModel, 45 * mouseScaleModel, 45 * mouseScaleModel);
		        Ellipse2D ellipseBody = new Ellipse2D.Double(mouseAInfo.bodyPosition.getX() - 45 * mouseScaleModel, (mouseAInfo.bodyPosition.getY() - 45 * mouseScaleModel), (90 * mouseScaleModel), (90 * mouseScaleModel));
		        Ellipse2D ellipseTail = new Ellipse2D.Double(mouseAInfo.tailPosition.getX() - 10 * mouseScaleModel, (mouseAInfo.tailPosition.getY() - 10 * mouseScaleModel), (20 * mouseScaleModel), (20 * mouseScaleModel));

		        g2.draw(ellipseHead);
		        g2.draw(ellipseBody);
		        g2.draw(ellipseTail);

		    }

			// Mouse B
			MouseInfoRecord mouseBInfo = mouseBRecord.get(currentFrame);
            if (mouseBInfo != null) {
                g2.setColor(Color.GREEN);

                Ellipse2D ellipseHead = new Ellipse2D.Double((mouseBInfo.headPosition.getX() - 22 * mouseScaleModel), (mouseBInfo.headPosition.getY() - 22 * mouseScaleModel), (45 * mouseScaleModel), (45 * mouseScaleModel));
                Ellipse2D ellipseBody = new Ellipse2D.Double((mouseBInfo.bodyPosition.getX() - 45 * mouseScaleModel), (mouseBInfo.bodyPosition.getY() - 45 * mouseScaleModel), (90 * mouseScaleModel), (90 * mouseScaleModel));
                Ellipse2D ellipseTail = new Ellipse2D.Double((mouseBInfo.tailPosition.getX() - 10 * mouseScaleModel), (mouseBInfo.tailPosition.getY() - 10 * mouseScaleModel), (20 * mouseScaleModel), (20 * mouseScaleModel));

                g2.draw(ellipseHead);
                g2.draw(ellipseBody);
                g2.draw(ellipseTail);
            }
		}
    }

	public List<Body> getBodyList() {
		return bodyList;
	}

	public JPanel getPanel() {
		return panel;
	}

	private double convertScaleX(double x) {
		x = x - 150;
		x *= (35. / 100.);
		return x;
	}

	private double convertScaleY(double y) {
		y = y - 50;
		y *= (50. / 200.);
		return y;
	}

	private void writeXMLResult() {
		// Should save
		// head position
		// tommy position
		// tail position

		System.out.println();
		System.out.println("Body Mouse X 1:");
		for (int i = 0; i < sequence.getLength(); i++) {
			MouseView mouseView = mouse1view.get(i);
			System.out.print(convertScaleX(mouseView.bodyPosition.getX()) + ", ");
			if (i % 10 == 0)
				System.out.println();
		}
		System.out.println();
		System.out.println("Body Mouse Y 1:");
		for (int i = 0; i < sequence.getLength(); i++) {
			MouseView mouseView = mouse1view.get(i);
			System.out.print(convertScaleY(mouseView.bodyPosition.getY()) + ", ");
			if (i % 10 == 0)
				System.out.println();
		}

		System.out.println();
		System.out.println("head Mouse X 1:");
		for (int i = 0; i < sequence.getLength(); i++) {
			MouseView mouseView = mouse1view.get(i);
			System.out.print(convertScaleX(mouseView.headPosition.getX()) + ", ");
			if (i % 10 == 0)
				System.out.println();
		}
		System.out.println();
		System.out.println("head Mouse Y 1:");
		for (int i = 0; i < sequence.getLength(); i++) {
			MouseView mouseView = mouse1view.get(i);
			System.out.print(convertScaleY(mouseView.headPosition.getY()) + ", ");
			if (i % 10 == 0)
				System.out.println();
		}

		System.out.println();
		System.out.println("Head Mouse Angle 1:");
		for (int i = 0; i < sequence.getLength(); i++) {
			MouseView mouseView = mouse1view.get(i);
			System.out.print((int) (180 * mouseView.headAngle / 3.14f) + ", ");
			if (i % 10 == 0)
				System.out.println();
		}

		// souris 2
		System.out.println();
		System.out.println("Body Mouse X 2:");
		for (int i = 0; i < sequence.getLength(); i++) {
			MouseView mouseView = mouse2view.get(i);
			System.out.print(convertScaleX(mouseView.bodyPosition.getX()) + ", ");
			if (i % 10 == 0)
				System.out.println();
		}
		System.out.println();
		System.out.println("Body Mouse Y 2:");
		for (int i = 0; i < sequence.getLength(); i++) {
			MouseView mouseView = mouse2view.get(i);
			System.out.print(convertScaleY(mouseView.bodyPosition.getY()) + ", ");
			if (i % 10 == 0)
				System.out.println();
		}

		System.out.println();
		System.out.println("head Mouse X 1:");
		for (int i = 0; i < sequence.getLength(); i++) {
			MouseView mouseView = mouse2view.get(i);
			System.out.print(convertScaleX(mouseView.headPosition.getX()) + ", ");
			if (i % 10 == 0)
				System.out.println();
		}
		System.out.println();
		System.out.println("head Mouse Y 1:");
		for (int i = 0; i < sequence.getLength(); i++) {
			MouseView mouseView = mouse2view.get(i);
			System.out.print(convertScaleY(mouseView.headPosition.getY()) + ", ");
			if (i % 10 == 0)
				System.out.println();
		}

		System.out.println();
		System.out.println("Head Mouse Angle 2:");
		for (int i = 0; i < sequence.getLength(); i++) {
			MouseView mouseView = mouse2view.get(i);
			System.out.print((int) (180 * mouseView.headAngle / 3.14f) + ", ");
			if (i % 10 == 0)
				System.out.println();
		}

	}

	private class Scale implements Painter {
		private final int height;
        private final int width;
        private final float value[];
        private final float barycenterX[];
        private final float barycenterY[];
        private final int scale;

		public Scale(int width, int height, int scale) {
			this.scale = scale;
			this.width = width;
			this.height = height;
			value = new float[width * height];
			barycenterX = new float[width * height];
			barycenterY = new float[width * height];
		}

		float getScaleFactor() {
			float scaleFactor = 1;
			for (int i = 0; i < scale; i++) {
				scaleFactor *= 2;
			}

			return scaleFactor;
		}

		void sendToDisplay() {
			IcyBufferedImage image = new IcyBufferedImage(width, height, 1, DataBuffer.TYPE_FLOAT);
			float[] data = image.getDataXYAsFloat(0);

			for (int i = 0; i < value.length; i++) {
				data[i] = value[i];
			}

			Sequence sequence = new Sequence( image);
			sequence.setName("Scale " + scale + " resol div par " + getScaleFactor());
			sequence.addPainter(this);
			Icy.addSequence(sequence);
		}

		@Override
		public void keyPressed(KeyEvent e, Point2D imagePoint, IcyCanvas canvas) {
		}

		@Override
		public void mouseClick(MouseEvent e, Point2D p, IcyCanvas canvas) {
			final int x = (int) p.getX();
			final int y = (int) p.getY();
			System.out.println("Point : x:" + x + " y:" + y + " bx:"
							+ barycenterX[x + y * width] + " by:"
							+ barycenterY[x + y * width] + " v:"
							+ value[x + y * width]);
		}

		@Override
		public void mouseDrag(MouseEvent e, Point2D p, IcyCanvas canvas) {
		}

		@Override
		public void mouseMove(MouseEvent e, Point2D p, IcyCanvas canvas) {
		}

		@Override
		public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
			g.setStroke(new BasicStroke(0.1f));
			Line2D line = new Line2D.Float();
			g.setColor(Color.red);

			float scaleDivider = getScaleFactor();

			for (int x = 0; x < width; x += 1) {
                for (int y = 0; y < height; y += 1) {
                    float xx = barycenterX[x + y * width] / scaleDivider;
                    float yy = barycenterY[x + y * width] / scaleDivider;

                    if (value[x + y * width] != 0) {
                        g.setColor(Color.yellow);
                        line.setLine(0.5 + x + 0.25, 0.5 + y + 0.25, 0.5 + x - 0.25, 0.5 + y - 0.25);
                        g.draw(line);
                        line.setLine(0.5 + x + 0.25, 0.5 + y - 0.25, 0.5 + x - 0.25, 0.5 + y + 0.25);
                        g.draw(line);

                        g.setColor(Color.red);

                        line.setLine(0.5 + x, 0.5 + y, xx + 0.25, yy + 0.25);
                        g.draw(line);
                    }
                }
            }
		}

		@Override
		public void keyReleased(KeyEvent e, Point2D imagePoint, IcyCanvas canvas) {
		}

		@Override
		public void mousePressed(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {
		}

		@Override
		public void mouseReleased(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {
		}
	}

	private class Ancre2 implements Painter {
		private final int mapWidth;
        private final int mapHeight;
        private final float centerX;
        private final float centerY;
        private final float ray;
        private final IcyBufferedImage carteAncre;
        private final int minX;
        private final int maxX;
        private final int minY;
        private final int maxY;
        private final List<Rectangle2D> listRect = new ArrayList<Rectangle2D>();

        private Sequence sequence;

		public Ancre2(int mapWidth, int mapHeight, float centerX, float centerY, float ray) {
			this.mapWidth = mapWidth;
			this.mapHeight = mapHeight;
			this.centerX = centerX;
			this.centerY = centerY;
			this.ray = ray;

			minX = (int) (centerX - ray);
			maxX = (int) (centerX + ray);
			minY = (int) (centerY - ray);
			maxY = (int) (centerY + ray);

			carteAncre = new IcyBufferedImage(mapWidth, mapHeight, 1, DataBuffer.TYPE_BYTE);

			// construction de la carte encre.
			byte[] data = carteAncre.getDataXYAsByte(0);

			for (int x = minX; x < maxX; x++) {
                for (int y = minY; y < maxY; y++) {
                    float dis = (x - centerX) * (x - centerX) + (y - centerY) * (y - centerY);
                    if (dis < ray * ray) {
                        data[x + y * mapWidth] = (byte) 255;
                    }
                }
            }
		}

		public void displayAsSequence() {
			sequence = new Sequence(carteAncre);
			sequence.setName("Map Ancre");
			sequence.addPainter(this);
			Icy.addSequence(sequence);
		}

		public void refreshDisplay() {
			sequence.painterChanged(null);
		}

		@Override
		public void keyPressed(KeyEvent e, Point2D imagePoint, IcyCanvas canvas) {
		}

		@Override
		public void mouseClick(MouseEvent e, Point2D p, IcyCanvas canvas) {
		}

		@Override
		public void mouseDrag(MouseEvent e, Point2D p, IcyCanvas canvas) {
		}

		@Override
		public void mouseMove(MouseEvent e, Point2D p, IcyCanvas canvas) {
		}

		@Override
		public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
			g.setStroke(new BasicStroke(1f));

			g.setColor(new Color((float) Math.random(), (float) Math.random(), (float) Math.random(), (float) 0.5f));
			for (final Rectangle2D rect : listRect) {
				g.draw(rect);
			}
		}

		@Override
		public void keyReleased(KeyEvent e, Point2D imagePoint, IcyCanvas canvas) {
		}

		@Override
		public void mousePressed(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {
		}

		@Override
		public void mouseReleased(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {
		}

	}

	private void computeATestAnchorVectorMap(boolean DISPLAY, Ancre2 ancre) {
		Sequence sequenceFocused = Icy.getMainInterface().getFocusedSequence();

		// creation d'une ancre avec sa carte
		Ancre2 a1 = ancre;

		if (DISPLAY) {
			a1.displayAsSequence();
			sequenceFocused.addPainter(a1);
		}

		// calcul du vecteur moyen avec methode lente et basique
		{
			int count = 0;
			float vx = 0;
			float vy = 0;
			final byte[] dataAncre = a1.carteAncre.getDataXYAsByte(0);
			final byte[] dataCarte = sequenceFocused.getDataXYAsByte(0, 0, 0);

			for (int x = a1.minX; x < a1.maxX; x++) {
                for (int y = a1.minY; y < a1.maxY; y++) {
                    if (dataAncre[x + y * a1.mapWidth] != 0) {
                        float vectX = (x - a1.centerX) * (dataCarte[x + y * a1.mapWidth] & 0xFF);
                        float vectY = (y - a1.centerY) * (dataCarte[x + y * a1.mapWidth] & 0xFF);
                        vx += vectX;
                        vy += vectY;
                        count++;
                    }
                }
            }

			System.out.println("nb iteration simple : " + count);
			System.out.println("Calcul du vecteur via methode simple : vx = " + vx + " vy = " + vy);
		}

		// calcul du vecteur moyen avec methode scale et quadtrees
		{
			// Iteration sur les echelles ( plus grande vers + petite )
			float vx = 0;
			float vy = 0;
			int count = 0;

			for (int scaleNumber = binaryScaleMap.size() - 1; scaleNumber >= 0; scaleNumber--) {
				// System.out.println("scale : " + scaleNumber);
				// rajouter le clipping sur le parcours des map d'attraction en
				// echelle sur la ROI.

				Scale currentScale = binaryScaleMap.get(scaleNumber);
				int scaleWidth = currentScale.width;
				int scaleHeight = currentScale.height;
				int scaleMulti = (int) currentScale.getScaleFactor();
				byte[] dataAncre = a1.carteAncre.getDataXYAsByte(0);
				int widthDataAncre = a1.carteAncre.getWidth();

				for (int x = 0; x < scaleWidth; x++) {
                    for (int y = 0; y < scaleHeight; y++) {
                        // first point to test:
                        int p1x = x * scaleMulti;
                        int p1y = y * scaleMulti;
                        // second point to test:
                        int p2x = (x + 1) * scaleMulti - 1;
                        int p2y = (y + 1) * scaleMulti - 1;

                        // test d'inclusion de l'echelle
                        if ((dataAncre[p1x + p1y * widthDataAncre] != 0) // haut gauche
                                && (dataAncre[p2x + p2y * widthDataAncre] != 0) // bas droite
                                && (dataAncre[p1x + p2y * widthDataAncre] != 0)
                                && (dataAncre[p2x + p1y * widthDataAncre] != 0)) {
                            // add a quad to watch
                            // System.out.print("x");
                            a1.listRect.add(new Rectangle2D.Float(p1x, p1y, p2x - p1x + 1, p2y - p1y + 1));

                            // remove pixels from anchor
                            for (int xx = p1x; xx <= p2x; xx++) {
                                for (int yy = p1y; yy <= p2y; yy++) {
                                    dataAncre[xx + yy * widthDataAncre] = 0; // 0
                                }
                            }

                            // calcul du vecteur
                            final float vectX = (currentScale.barycenterX[x + y * scaleWidth] - a1.centerX) * currentScale.value[x + y * scaleWidth];
                            final float vectY = (currentScale.barycenterY[x + y * scaleWidth] - a1.centerY) * currentScale.value[x + y * scaleWidth];
                            vx += vectX;
                            vy += vectY;

                            count++;
                        }
                    }
                }

				if (DISPLAY) {
					a1.refreshDisplay();
				}

			}
			System.out.println("nb iteration quad : " + count);
			System.out.println("Calcul du vecteur via methode quad : vx = " + vx + " vy = " + vy);
		}
	}

	public void actionPerformed(ActionEvent e) {
		if ((e.getSource() == displayForceCheckBox)
				|| (e.getSource() == displayEnergyAreaCheckBox)
				|| (e.getSource() == displayBodyCenterCheckBox)
				|| (e.getSource() == displayBodyShapeCheckBox)
				|| (e.getSource() == displayGlobalSplineCheckBox)
				|| (e.getSource() == displaySlideJointCheckBox)
				|| (e.getSource() == displayBinaryMapCheckBox)
				|| (e.getSource() == displayGradientMapCheckBox)) {
			sequence.painterChanged(null);
		}

		if (e.getSource() == computeATestAnchorVectorMapButton) {
			Sequence sequence = Icy.getMainInterface().getFocusedSequence();
			List<Ancre2> ancreListOut = new ArrayList<Ancre2>();
			List<Ancre2> ancreListIn = new ArrayList<Ancre2>();

			Ancre2 a1 = new Ancre2(Icy.getMainInterface().getFocusedSequence().getWidth(), Icy.getMainInterface().getFocusedSequence().getHeight(), 200, 160, 140 /*ray = 140.*/);
			//a1.setColor(Color.yellow);

			Ancre2 a2 = new Ancre2(Icy.getMainInterface().getFocusedSequence().getWidth(), Icy.getMainInterface().getFocusedSequence().getHeight(), 300, 160, 140 /*ray = 140.*/);
			//a2.setColor(Color.orange);

			Ancre2 a3 = new Ancre2(Icy.getMainInterface().getFocusedSequence().getWidth(), Icy.getMainInterface().getFocusedSequence().getHeight(), 250, 250, 100 /*ray = 140.*/);
			//a3.setColor(Color.blue);

			Ancre2 aConflict = new Ancre2(Icy.getMainInterface().getFocusedSequence().getWidth(), Icy.getMainInterface().getFocusedSequence().getHeight(), 250, 160, 140 /*ray = 140.*/);
			//aConflict.setColor(Color.pink);

			byte[] aConflictdata = aConflict.carteAncre.getDataXYAsByte(0);
			byte[] a1data = a1.carteAncre.getDataXYAsByte(0);
			byte[] a2data = a2.carteAncre.getDataXYAsByte(0);
			byte[] a3data = a3.carteAncre.getDataXYAsByte(0);

			for (int i = 0; i < aConflictdata.length; i++) {
				aConflictdata[i] = 0;
				if (a1data[i] != 0)
					aConflictdata[i]++;
				if (a2data[i] != 0)
					aConflictdata[i]++;
				if (a3data[i] != 0)
					aConflictdata[i]++;
			}

			ancreListIn.add(a1);
			ancreListIn.add(a2);
			ancreListIn.add(a3);

			// creation des zones ici nomme ancres
			for (final Ancre2 ancre : ancreListIn) {
				// cherche le max dans le masque de l ancre avec la map de
				// conflit
				int max = 0;
				byte[] mapAncre = ancre.carteAncre.getDataXYAsByte(0);
				for (int i = 0; i < aConflictdata.length; i++) {
					if (mapAncre[i] != 0) {
						if (aConflictdata[i] > max)
							max = aConflictdata[i];
					}
				}

				System.out.println("max = " + max);

				// creation des ancres pour chaque indice de conflit
				for (int cumulIndex = max; cumulIndex > 0; cumulIndex--) {
					Ancre2 newAncre = new Ancre2(sequence.getWidth(), sequence.getHeight(), 10, 10, 0);
					byte[] ancreData = newAncre.carteAncre.getDataXYAsByte(0);
					for (int i = 0; i < aConflictdata.length; i++) {
						if (mapAncre[i] == (byte) 255) {
							if (aConflictdata[i] == cumulIndex) {
								ancreData[i] = (byte) 255;
							}
						}

					}
					System.out.println("computing.");
					computeATestAnchorVectorMap(true, newAncre);
					//newAncre.setColor(new Color((float) Math.random(), (float) Math.random(), (float) Math.random()));
					sequence.addPainter(newAncre);
				}
			}
		}

		// generaliser a toutes les carte binaire et gradient
		if (e.getSource() == displayScaleBinaryButton) {
			binaryScaleMap = new ArrayList<Scale>();
			binaryScaleMap.clear();

			// construction de la carte binaire.

			// Echelle 1 = copie originale de la carte binarized
			Scale binaryScale0 = new Scale(Icy.getMainInterface().getFocusedSequence().getWidth(), Icy.getMainInterface().getFocusedSequence().getHeight(), 0);

			byte[] dataIn = Icy.getMainInterface().getFocusedSequence().getDataXYAsByte(0, 0, 0);
			float[] dataOut = binaryScale0.value;
			{
				int maxWidth = Icy.getMainInterface().getFocusedSequence().getWidth();
				int maxHeight = Icy.getMainInterface().getFocusedSequence().getHeight();
				int offset = 0;

				for (int y = 0; y < maxHeight; y++) {
					for (int x = 0; x < maxWidth; x++) {
						dataOut[offset] = dataIn[offset] & 0xFF;

						binaryScale0.barycenterX[offset] = x;
						binaryScale0.barycenterY[offset] = y;
						offset++;
					}
				}
			}
			binaryScale0.sendToDisplay();

			binaryScaleMap.add(binaryScale0);

			int MAX_SCALE_TO_BUILD = 9;

			// construction des echelles version 2
			for (int scale = 1; scale < MAX_SCALE_TO_BUILD; scale++) {
				final Scale previousScale = binaryScaleMap.get(scale - 1);
				Scale currentScale = new Scale(previousScale.width / 2, previousScale.height / 2, scale);

				float[] in = previousScale.value;
				float[] out = currentScale.value;

				int maxHeight = previousScale.height / 2;
				int maxWidth = previousScale.width / 2;

				for (int y = 0; y < maxHeight; y++) { // balayage dans la nouvelle echelle
					for (int x = 0; x < maxWidth; x++) {// balayage dans la nouvelle echelle
						// calcul de la valeur coefficient

						boolean LOG = false;

						if (x == 20 && y == 21 && scale == 2) {
							System.out.println("log true x: 20 y:21 s:2");
							LOG = true;
						}

						int xx = x * 2;
						int yy = y * 2;

						if (LOG) {
							System.out.println("recherche pour X s= " + x);
							System.out.println("recherche pour Y s= " + y);

							System.out.println("recherche pour X s-1= " + xx);
							System.out.println("recherche pour X s-1= " + (xx + 1));
							System.out.println("recherche pour Y s-1= " + yy);
							System.out.println("recherche pour Y s-1= " + (yy + 1));
						}

						out[x + y * currentScale.width] = in[xx + yy * previousScale.width] + in[xx + 1 + yy * previousScale.width] + in[xx + (yy + 1) * previousScale.width] + in[xx + 1 + (yy + 1) * previousScale.width];

						if (LOG) {
							System.out.println("previous val 1: " + in[xx + yy * previousScale.width]);
							System.out.println("previous val 2: " + in[xx + 1 + yy * previousScale.width]);
							System.out.println("previous val 3: " + in[xx + (yy + 1) * previousScale.width]);
							System.out.println("previous val 4: " + in[xx + 1 + (yy + 1)
											* previousScale.width]);
							System.out.println("OUT destination value = " + out[x + y * currentScale.width]);
						}

						// calculation of propagated barycentres
						if (out[x + y * currentScale.width] != 0) {
							if (LOG) {
								System.out.println("Entree dans barycentre.");
							}

							// X calculation for barycentre
							currentScale.barycenterX[x + y * currentScale.width] =
							((float) (previousScale.value[xx + yy * previousScale.width]
									* previousScale.barycenterX[xx + yy * previousScale.width]
									+ previousScale.value[xx + 1 + yy * previousScale.width]
									* previousScale.barycenterX[xx + 1 + yy * previousScale.width]
									+ previousScale.value[xx + (yy + 1) * previousScale.width]
									* previousScale.barycenterX[xx + (yy + 1) * previousScale.width]
                                    + previousScale.value[xx + 1 + (yy + 1) * previousScale.width]
									* previousScale.barycenterX[xx + 1 + (yy + 1) * previousScale.width]) / (float) out[x + y * currentScale.width]);

							if (LOG) {
								System.out.println("Calcul de by: ");
								System.out.println("p1: " + previousScale.barycenterY[xx + yy * previousScale.width]);
								System.out.println("p2: " + previousScale.barycenterY[xx + 1 + yy * previousScale.width]);
								System.out.println("p3: " + previousScale.barycenterY[xx + (yy + 1) * previousScale.width]);
								System.out.println("p4: " + previousScale.barycenterY[xx + 1 + (yy + 1) * previousScale.width]);
							}

							currentScale.barycenterY[x + y * currentScale.width] =

							((float) (previousScale.value[xx + yy * previousScale.width]
									* previousScale.barycenterY[xx + yy * previousScale.width]
									+ previousScale.value[xx + 1 + yy * previousScale.width]
									* previousScale.barycenterY[xx + 1 + yy * previousScale.width]
									+ previousScale.value[xx + (yy + 1) * previousScale.width]
									* previousScale.barycenterY[xx + (yy + 1) * previousScale.width]
                                    + previousScale.value[xx + 1 + (yy + 1) * previousScale.width]
									* previousScale.barycenterY[xx + 1 + (yy + 1) * previousScale.width]) / (float) out[x + y * currentScale.width]);

							if (LOG) {
								System.out.println("Bary x:" + currentScale.barycenterX[x + y * currentScale.width]);
								System.out.println("Bary y:" + currentScale.barycenterY[x + y * currentScale.width]);
							}

						}
					}
				}

				currentScale.sendToDisplay();

				System.out.println("adding scale : " + scale);
				binaryScaleMap.add(currentScale);
			}
		}

		if (e.getSource() == resultButton) {
			writeXMLResult();
		}

//		if ( e.getSource() == applyNewScaleButton )
//		{
//			//applyNewScaleButton
//			SCALE = Float.parseFloat(scaleTextField.getText()); 
//			
//			for ( Mouse mouse : mouseList )
//			{
//				// set mouse again
//				
////				sdf
//			}
//		}
	}

	public boolean isStable() {
		double seuil = 1000d;

		if (world.getTotalEnergy() > seuil)
			return false;

		return true;
	}

	public void loadXML(File currentFile) {
		// LOAD DOCUMENT
		mouseARecord.clear();
		mouseBRecord.clear();

		File XMLFile = new File(currentFile.getAbsoluteFile() + ".xml");
		if (!XMLFile.exists())
			return;

		Document XMLDocument = XMLTools.loadDocument(XMLFile);

		XPath xpath = XPathFactory.newInstance().newXPath();
		{
			String expression = "//MOUSEA/DET";
			NodeList nodes;
			try {
				nodes = (NodeList) xpath.evaluate(expression, XMLDocument, XPathConstants.NODESET);

				System.out.println("node size : " + nodes.getLength());

				for (int i = 0; i < nodes.getLength(); i++) {
					Element detNode = (Element) nodes.item(i);

                    Point2D bodyPosition = new Point2D.Float(Float.parseFloat(detNode.getAttribute("bodyx")), Float.parseFloat(detNode.getAttribute("bodyy")));
                    Point2D headPosition = new Point2D.Float(Float.parseFloat(detNode.getAttribute("headx")), Float.parseFloat(detNode.getAttribute("heady")));
                    Point2D tailPosition = new Point2D.Float(Float.parseFloat(detNode.getAttribute("tailx")), Float.parseFloat(detNode.getAttribute("taily")));

                    float neckX = 0;
                    float neckY = 0;
                    try {
                        neckX = Float.parseFloat( detNode.getAttribute("neckx") );
                        neckY = Float.parseFloat( detNode.getAttribute("necky") );
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                    Point2D neckPosition = new Point2D.Float( neckX , neckY );
                    MouseInfoRecord mouseInfoRecord = new MouseInfoRecord(headPosition, tailPosition, bodyPosition, neckPosition);

                    mouseARecord.put(Integer.parseInt(detNode.getAttribute("t")), mouseInfoRecord);

				}
			} catch (final XPathExpressionException e) {
				e.printStackTrace();
			}
		}

		// MOUSE B LOAD
		{
			String expression = "//MOUSEB/DET";
			NodeList nodes;
			try {
				nodes = (NodeList) xpath.evaluate(expression, XMLDocument, XPathConstants.NODESET);

				System.out.println("node size : " + nodes.getLength());

				for (int i = 0; i < nodes.getLength(); i++) {
					Element detNode = (Element) nodes.item(i);

                    Point2D bodyPosition = new Point2D.Float(Float.parseFloat(detNode.getAttribute("bodyx")), Float.parseFloat(detNode.getAttribute("bodyy")));
                    Point2D headPosition = new Point2D.Float(Float.parseFloat(detNode.getAttribute("headx")), Float.parseFloat(detNode.getAttribute("heady")));
                    Point2D tailPosition = new Point2D.Float(Float.parseFloat(detNode.getAttribute("tailx")), Float.parseFloat(detNode.getAttribute("taily")));

                    float neckX = 0;
                    float neckY = 0;
                    try{
                        neckX = Float.parseFloat( detNode.getAttribute("neckx") );
                        neckY = Float.parseFloat( detNode.getAttribute("necky") );
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    Point2D neckPosition = new Point2D.Float( neckX , neckY );
                    MouseInfoRecord mouseInfoRecord = new MouseInfoRecord(headPosition, tailPosition, bodyPosition, neckPosition);

					mouseBRecord.put(Integer.parseInt(detNode.getAttribute("t")), mouseInfoRecord);
				}
			} catch (final XPathExpressionException e) {
				e.printStackTrace();
			}
		}
	}

	public void saveXML(File currentFile) {
		// CREATE DOCUMENT
		File XMLFile = new File(currentFile.getAbsoluteFile() + ".xml");

		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = dbfac.newDocumentBuilder();
		} catch (final ParserConfigurationException e) {
			e.printStackTrace();
		}
		Document XMLDocument = docBuilder.newDocument();

		Element child = XMLDocument.createElement("MOUSETRACK002Puppy");
		Element child1 = XMLDocument.createElement("FILENAME");
		child1.setTextContent(currentFile.getAbsolutePath());
		Element child2 = XMLDocument.createElement("PARAMETERS");
		Element resultChild = XMLDocument.createElement("RESULT");

		XMLDocument.appendChild(child);
		child.appendChild(child1);
		child.appendChild(child2);
		child.appendChild(resultChild);

		Element resultMouseA = XMLDocument.createElement("MOUSEA");
		Element resultMouseB = XMLDocument.createElement("MOUSEB");

		resultChild.appendChild(resultMouseA);
		resultChild.appendChild(resultMouseB);

		// FILL DATA
		int maxT = 0;
		{
			Set<Integer> integerKey = mouseARecord.keySet();
			Iterator<Integer> it = integerKey.iterator();
			while (it.hasNext()) {
				Integer t = it.next();
				if (t > maxT)
					maxT = t;
			}
		}

		// MOUSE A
		for (int t = 0; t <= maxT; t++) {
			MouseInfoRecord mouseAInfo = mouseARecord.get(t);
			if (mouseAInfo == null)
				continue;

			Element newResultElement = XMLDocument.createElement("DET");
			newResultElement.setAttribute("t", "" + t);

			newResultElement.setAttribute("headx", "" + mouseAInfo.headPosition.getX());
			newResultElement.setAttribute("heady", "" + mouseAInfo.headPosition.getY());

			newResultElement.setAttribute("bodyx", "" + mouseAInfo.bodyPosition.getX());
			newResultElement.setAttribute("bodyy", "" + mouseAInfo.bodyPosition.getY());

			newResultElement.setAttribute("tailx", "" + mouseAInfo.tailPosition.getX());
			newResultElement.setAttribute("taily", "" + mouseAInfo.tailPosition.getY());

			newResultElement.setAttribute("neckx", "" + mouseAInfo.neckPosition.getX());
			newResultElement.setAttribute("necky", "" + mouseAInfo.neckPosition.getY());

			resultMouseA.appendChild(newResultElement);
		}

		// MOUSE B

		if (mouseList.size() > 1) { // check if two mice are presents
			for (int t = 0; t <= maxT; t++) {
				MouseInfoRecord mouseBInfo = mouseBRecord.get(t);
				if (mouseBInfo == null)
					continue;

				Element newResultElement = XMLDocument.createElement("DET");
				newResultElement.setAttribute("t", "" + t);

				newResultElement.setAttribute("headx", "" + mouseBInfo.headPosition.getX());
				newResultElement.setAttribute("heady", "" + mouseBInfo.headPosition.getY());

				newResultElement.setAttribute("bodyx", "" + mouseBInfo.bodyPosition.getX());
				newResultElement.setAttribute("bodyy", "" + mouseBInfo.bodyPosition.getY());

				newResultElement.setAttribute("tailx", "" + mouseBInfo.tailPosition.getX());
				newResultElement.setAttribute("taily", "" + mouseBInfo.tailPosition.getY());

				newResultElement.setAttribute("neckx", "" + mouseBInfo.neckPosition.getX());
				newResultElement.setAttribute("necky", "" + mouseBInfo.neckPosition.getY());

				resultMouseB.appendChild(newResultElement);
			}
		}

		// SAVE DOC
		XMLTools.saveDocument(XMLDocument, XMLFile);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == binaryThresholdSpinner) {
			thresholdBinaryMap = Integer.parseInt(binaryThresholdSpinner.getValue().toString());
		}
	}

	public void divideTimePer2() {
		int max = 0;

		Set<Integer> integerKey = mouseBRecord.keySet();
		Iterator<Integer> it = integerKey.iterator();
		while (it.hasNext()) {
			final Integer t = it.next();
			if (t > max)
				max = t;
		}
		System.out.println("Max =" + max);
		for (int i = 0; i <= max; i += 2) {
			if (mouseARecord.containsKey(i)) {
				MouseInfoRecord mouseRecordA = mouseARecord.get(i);
				mouseARecord.remove(i);
				mouseARecord.put(i / 2, mouseRecordA);
			}

			if (mouseBRecord.containsKey(i)) {
				MouseInfoRecord mouseRecordB = mouseBRecord.get(i);
				mouseBRecord.remove(i);
				mouseBRecord.put(i / 2, mouseRecordB);
			}
		}

		for (int i = max / 2; i <= max; i++) {
			mouseARecord.remove(i);
			mouseBRecord.remove(i);
		}

		System.out.println("Split done.");
	}
	
	public void setReverseThreshold(boolean reverseThresholdBoolean) {
		this.reverseThresholdBoolean = reverseThresholdBoolean;
	}

	/** force head location for the next compute force frame (1 frame only)*/
	public void setHeadLocation(int mouseNumber, MAnchor2D controlPoint) {
		headForcedPosition[mouseNumber] = controlPoint;		
	}

}
