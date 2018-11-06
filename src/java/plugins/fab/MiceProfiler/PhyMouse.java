/**
 *  Copyright Murex S.A.S., 2003-2018. All Rights Reserved.
 *
 *  This software program is proprietary and confidential to Murex S.A.S and its affiliates ("Murex") and, without limiting the generality of the foregoing reservation of rights, shall not be accessed, used, reproduced or distributed without the
 *  express prior written consent of Murex and subject to the applicable Murex licensing terms. Any modification or removal of this copyright notice is expressly prohibited.
 */
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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.DataBuffer;

import java.io.File;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;

import icy.gui.util.GuiUtil;

import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;

import icy.main.Icy;

import icy.roi.ROI2D;

import icy.sequence.Sequence;

import icy.type.DataType;

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
 */
public class PhyMouse implements ActionListener, ChangeListener {

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Static fields/initializers
    //~ ----------------------------------------------------------------------------------------------------------------

    private static final int SEUIL_EDGE_MAP = 32;

    private static final float BINARY_ENERGY_MULTIPLICATOR = 10000;
    private static final float GRADIENT_ENERGY_MULTIPLICATOR = 10000;

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Instance fields
    //~ ----------------------------------------------------------------------------------------------------------------

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

    //Maps for ?
    private IcyBufferedImage binaryMap;
    private IcyBufferedImage edgeMap;

    //Physics for ?
    private final List<Body> bodyList = Lists.newArrayList();
    private final List<SlideJoint> slideJointList = Lists.newArrayList();
    private final List<DistanceJoint> distanceJointList = Lists.newArrayList();

    //Mouse detail
    private final List<Mouse> mouseList = Lists.newArrayList();
    private final Map<Integer, MouseInfoRecord> mouse1Records = Maps.newHashMap();
    private final Map<Integer, MouseInfoRecord> mouse2Records = Maps.newHashMap();

    /** used by painter */
    private boolean motion_prediction_state = false;

    //??
    private boolean reverseThresholdBoolean = false;

    //??
    private final MAnchor2D[] headForcedPosition = new MAnchor2D[2];

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Constructors
    //~ ----------------------------------------------------------------------------------------------------------------

    public PhyMouse(Sequence sequence) {
        this.sequence = sequence;
        this.world = new World(new Vector2f(0, 0), 10, new QuadSpaceStrategy(20, 5));
        this.world.clear();
        this.world.setGravity(0, 0);

        this.panel = GuiUtil.generatePanel();
        fillWindowPanel();
    }

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Methods
    //~ ----------------------------------------------------------------------------------------------------------------

    public void generateMouse(float x, float y, float alpha) {
        mouseScaleModel = Float.parseFloat(scaleTextField.getText());
        mouseList.add(new Mouse(world, x, y, alpha, bodyList, this));
    }

    public void computeForcesMap(IcyBufferedImage imageSource) {
        //Retrieve ROI if added (get last one, but only one expected)
        ROI2D clipROI = null;
        Sequence activeSequence = Icy.getMainInterface().getFocusedSequence();
        if (activeSequence != null) {
            if (!activeSequence.getROIs().isEmpty())
                clipROI = (ROI2D) activeSequence.getROIs().get(activeSequence.getROIs().size() - 1);
        }

        //Create or update binaryMap
        int imageSourceWidth = imageSource.getWidth();
        int imageSourceHeight = imageSource.getHeight();
        if (binaryMap == null) {
            binaryMap = new IcyBufferedImage(imageSourceWidth, imageSourceHeight, 1, DataType.UBYTE);
        } else if ((binaryMap.getWidth() != imageSourceWidth) || (binaryMap.getHeight() != imageSourceHeight)) {
            binaryMap = new IcyBufferedImage(imageSourceWidth, imageSourceHeight, 1, DataType.UBYTE);
        }

        //Apply Threshold to data (only inside ROI if provided). Take into account reverseThresholdBoolean
        byte[] binaryMapDataBuffer = binaryMap.getDataXYAsByte(0);
        byte[] imageSourceDataBuffer = imageSource.getDataXYAsByte(1);
        for (int x = 0; x < imageSourceWidth; x++) {
            for (int y = 0; y < imageSourceHeight; y++) {
                int val = imageSourceDataBuffer[x + (y * imageSourceWidth)] & 0xFF;

                if (clipROI != null) {
                    if (!clipROI.contains(x, y))
                        val = 0;
                } else {
                    if (val < thresholdBinaryMap)
                        val = (!reverseThresholdBoolean) ? 255 : 0;
                    else
                        val = (!reverseThresholdBoolean) ? 0 : 255;
                }

                binaryMapDataBuffer[x + (y * imageSourceWidth)] = (byte) val;
            }
        }

        //Create or update edgeMap
        if (edgeMap == null) {
            edgeMap = new IcyBufferedImage(imageSourceWidth, imageSourceHeight, 1, DataType.UBYTE);
        } else if ((edgeMap.getWidth() != imageSourceWidth) || (edgeMap.getHeight() != imageSourceHeight)) {
            edgeMap = new IcyBufferedImage(imageSourceWidth, imageSourceHeight, 1, DataType.UBYTE);
        }

        int maxWidth = binaryMap.getWidth() - 1;
        int maxHeight = binaryMap.getHeight() - 1;
        byte[] edgeMapDataBuffer = edgeMap.getDataXYAsByte(0);
        for (int x = 1; x < maxWidth; x++) {
            for (int y = 1; y < maxHeight; y++) {
                int val1 = binaryMapDataBuffer[x + (y * imageSourceWidth)] & 0xFF;
                int val2 = binaryMapDataBuffer[x + 1 + (y * imageSourceWidth)] & 0xFF;

                int val4 = binaryMapDataBuffer[x + ((y + 1) * imageSourceWidth)] & 0xFF;

                int val = Math.abs(val1 - val2) + Math.abs(val1 - val4);

                if (val > SEUIL_EDGE_MAP)
                    val = 255;
                else
                    val = 0;

                edgeMapDataBuffer[x + (y * imageSourceWidth)] = (byte) val;
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

    public Map<Integer, MouseInfoRecord> getMouse1Records() {
        return mouse1Records;
    }

    public Map<Integer, MouseInfoRecord> getMouse2Records() {
        return mouse2Records;
    }

    public void computeForces() {
        // force location of the head
        if (headForcedPosition[0] != null) {
            MAnchor2D pos = headForcedPosition[0];
            mouseList.get(0).getHead().setPosition((float) pos.getX(), (float) pos.getY());
        }
        if (headForcedPosition[1] != null) {
            MAnchor2D pos = headForcedPosition[1];
            mouseList.get(1).getHead().setPosition((float) pos.getX(), (float) pos.getY());
        }

        // Create mask
        IcyBufferedImage maskMap = new IcyBufferedImage(binaryMap.getWidth(), binaryMap.getHeight(), 1, DataType.UBYTE);
        byte[] maskMapData = maskMap.getDataXYAsByte(0);
        int maskMapWidth = maskMap.getWidth();

        // apply energy.
        for (final Body body : bodyList) {
            int length = maskMapData.length;
            for (int i = 0; i < length; i++) {
                maskMapData[i] = 0;
            }

            EnergyInfo energyInfo = (EnergyInfo) body.getUserData();

            drawCircleInMaskMap((int) body.getLastPosition().getX(), (int) body.getLastPosition().getY(), (int) energyInfo.getRay(), maskMap, true);

            /*for (final Body body2 : bodyList) {
             *  if (body != body2) {
             *      EnergyInfo energyInfo2 = (EnergyInfo) body2.getUserData();
             *      if (energyInfo.getEnergyMap() == energyInfo2.getEnergyMap()) {
             *          /*TODO: if (energyInfo2.excludeFromAttractiveMapOwner) {
             *  drawCircleInMaskMap((int) body2.getLastPosition().getX(), (int)
             * body2.getLastPosition().getY(), (int) energyInfo2.getRay(), maskMap, false);
             *}
             *      }
             *  }
             *}*/

            if (energyInfo.getEnergyMap() == EnergyMap.BINARY_MOUSE) {
                float vx = 0;
                float vy = 0;
                float count = 0;

                int maxX = (int) (body.getLastPosition().getX() + energyInfo.getRay());
                int maxY = (int) (body.getLastPosition().getY() + energyInfo.getRay());

                int imageWidth = binaryMap.getWidth();
                int imageHeight = binaryMap.getHeight();
                byte[] binaryMapDataBuffer = binaryMap.getDataXYAsByte(0);

                for (int x = (int) (body.getLastPosition().getX() - energyInfo.getRay()); x < maxX; x++) {
                    for (int y = (int) (body.getLastPosition().getY() - energyInfo.getRay()); y < maxY; y++) {
                        if (x >= imageWidth)
                            continue;
                        if (y >= imageHeight)
                            continue;
                        if (x < 0)
                            continue;
                        if (y < 0)
                            continue;

                        float factor = 0.5f;
                        if (maskMapData[x + (y * maskMapWidth)] != 0) {
                            factor = 1f;
                        }

                        if (maskMapData[x + (y * maskMapWidth)] != 0) {
                            if ((binaryMapDataBuffer[x + (y * imageWidth)] & 0xFF) == 255) {
                                vx += (x - body.getLastPosition().getX()) * factor;
                                vy += (y - body.getLastPosition().getY()) * factor;
                            }
                            count++;
                        }
                    }
                }

                if (count > 0) {
                    vx /= count;
                    vy /= count;
                }

                vx *= BINARY_ENERGY_MULTIPLICATOR;
                vy *= BINARY_ENERGY_MULTIPLICATOR;

                energyInfo.vx = vx;
                energyInfo.vx = vy;

                body.setForce(vx, vy);
            } else if (energyInfo.getEnergyMap() == EnergyMap.GRADIENT_MAP) {
                float vx = 0;
                float vy = 0;
                float count = 0;

                int imageWidth = edgeMap.getWidth();
                int imageHeight = edgeMap.getHeight();

                byte[] edgeMapDataBuffer = edgeMap.getDataXYAsByte(0);

                int maxX = (int) (body.getLastPosition().getX() + energyInfo.getRay());
                int maxY = (int) (body.getLastPosition().getY() + energyInfo.getRay());

                for (int x = (int) (body.getLastPosition().getX() - energyInfo.getRay()); x < maxX; x++) {
                    for (int y = (int) (body.getLastPosition().getY() - energyInfo.getRay()); y < maxY; y++) {
                        // if ( mask.contains( x ,y ) )

                        if (x >= imageWidth)
                            continue;
                        if (y >= imageHeight)
                            continue;
                        if (x < 0)
                            continue;
                        if (y < 0)
                            continue;

                        if (maskMapData[x + (y * maskMapWidth)] != 0) {
                            if ((edgeMapDataBuffer[x + (y * imageWidth)] & 0xFF) != 0) {
                                vx += x - body.getLastPosition().getX();
                                vy += y - body.getLastPosition().getY();
                            }
                            count++;
                        }
                    }
                }

                if (count > 0) {
                    vx /= count;
                    vy /= count;
                }

                vx *= GRADIENT_ENERGY_MULTIPLICATOR; // *50
                vy *= GRADIENT_ENERGY_MULTIPLICATOR;

                energyInfo.vx = vx;
                energyInfo.vy = vy;

                body.setForce(vx, vy);
            }
        }

        // remove all forced positions.
        headForcedPosition[0] = null;
        headForcedPosition[1] = null;
    }

    public void swapIdentityRecordFromTToTheEnd(int time) {
        // look for maximum time recorded
        int maxT = 0;
        for (int i : mouse1Records.keySet()) {
            if (i > maxT)
                maxT = i;
        }
        for (int i : mouse2Records.keySet()) {
            if (i > maxT)
                maxT = i;
        }

        // swap data
        for (int t = time; t <= maxT; t++) {
            MouseInfoRecord recA = mouse1Records.get(t);
            MouseInfoRecord recB = mouse2Records.get(t);

            mouse1Records.put(t, recB);
            mouse2Records.put(t, recA);
        }

    }

    public void worldStep(int currentFrame) {
        motion_prediction_state = false;

        world.step();
        //nbTotalIteration++;

        /*while (pauseTrackAllBox.isSelected()) {
         *  try {
         *      Thread.sleep(100);
         *  } catch (final InterruptedException e) {
         *      e.printStackTrace();
         *  }
         *}*/

        recordMousePosition(currentFrame);

        /*if (displayStepBox.isSelected()) {
         *  sequence.painterChanged(null);
         *}*/
    }

    /**
     * Applique la motion prediction et passe l'inertie a 0.
     */
    public void applyMotionPrediction() {
        if (!useMotionPredictionCheckBox.isSelected())
            return;

        motion_prediction_state = true;

        // record and motion prediction.
        for (Body body : bodyList) {
            // record current position.
            EnergyInfo energyInfo = (EnergyInfo) body.getUserData();
            // copy the object.
            ROVector2f vCopy = new Vector2f(body.getLastPosition().getX(), body.getLastPosition().getY());

            body.setForce(0, 0);

            energyInfo.previousPositionList.add(vCopy);

            // compute la prediction (t) - (t-1)
            if (energyInfo.previousPositionList.size() > 1) { // no prediction for first frame.
                Vector2f newVelocity = new Vector2f(10f * (energyInfo.previousPositionList.get(1).getX() - energyInfo.previousPositionList.get(0).getX()),
                    10f * (energyInfo.previousPositionList.get(1).getY() - energyInfo.previousPositionList.get(0).getY()));

                body.adjustVelocity(newVelocity);
                energyInfo.previousPositionList.remove(energyInfo.previousPositionList.get(0));
            }
        }

        for (int i = 0; i < 6; i++) {
            /*while (pauseTrackAllBox.isSelected()) {
             *  try {
             *      Thread.sleep(100);
             *  } catch (final InterruptedException e) {
             *      e.printStackTrace();
             *  }
             *}*/

            world.step();
        }

        for (final Body body : bodyList) {
            body.adjustVelocity(new Vector2f(0, 0));
        }

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
                Line2D line = new Line2D.Float(slideJoint.getBody1().getLastPosition().getX(), slideJoint.getBody1().getLastPosition().getY(), slideJoint.getBody2().getLastPosition().getX(),
                        slideJoint.getBody2().getLastPosition().getY());
                g2.draw(line);
            }
        }

        // paint DistanceJoint
        if (displayDistanceJointCheckBox.isSelected()) {
            g2.setColor(Color.YELLOW);
            for (DistanceJoint distanceJoint : distanceJointList) {
                Line2D line = new Line2D.Float(distanceJoint.getBody1().getLastPosition().getX(), distanceJoint.getBody1().getLastPosition().getY(), distanceJoint.getBody2().getLastPosition().getX(),
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
                        for (int i = 0; i < (vert.length - 1); i++) {
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
                    Ellipse2D ellipse = new Ellipse2D.Float(body.getLastPosition().getX() - energyInfo.getRay(), body.getLastPosition().getY() - energyInfo.getRay(), (energyInfo.getRay() * 2) + 1,
                            (energyInfo.getRay() * 2) + 1);
                    g2.draw(ellipse);
                }
            }

            boolean displayForce = displayForceCheckBox.isSelected();
            if (displayBinaryMapCheckBox.isSelected() && (energyInfo.getEnergyMap() != EnergyMap.BINARY_MOUSE))
                displayForce = false;
            if (displayGradientMapCheckBox.isSelected() && (energyInfo.getEnergyMap() != EnergyMap.GRADIENT_MAP))
                displayForce = false;
            if (displayForce) {
                Line2D energyVector = new Line2D.Float(body.getLastPosition().getX(), body.getLastPosition().getY(), body.getLastPosition().getX() + energyInfo.vx, body.getLastPosition().getY() + energyInfo.vy);
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
            MouseInfoRecord mouseAInfo = mouse1Records.get(currentFrame);
            if (mouseAInfo != null) {
                g2.setColor(Color.RED);

                Ellipse2D ellipseHead = new Ellipse2D.Double(mouseAInfo.getHeadPosition().getX() - (22 * mouseScaleModel), mouseAInfo.getHeadPosition().getY() - (22 * mouseScaleModel), 45 * mouseScaleModel,
                        45 * mouseScaleModel);
                Ellipse2D ellipseBody = new Ellipse2D.Double(mouseAInfo.getBodyPosition().getX() - (45 * mouseScaleModel), mouseAInfo.getBodyPosition().getY() - (45 * mouseScaleModel), 90 * mouseScaleModel,
                        90 * mouseScaleModel);
                Ellipse2D ellipseTail = new Ellipse2D.Double(mouseAInfo.getTailPosition().getX() - (10 * mouseScaleModel), mouseAInfo.getTailPosition().getY() - (10 * mouseScaleModel), 20 * mouseScaleModel,
                        20 * mouseScaleModel);

                g2.draw(ellipseHead);
                g2.draw(ellipseBody);
                g2.draw(ellipseTail);

            }

            // Mouse B
            MouseInfoRecord mouseBInfo = mouse2Records.get(currentFrame);
            if (mouseBInfo != null) {
                g2.setColor(Color.GREEN);

                Ellipse2D ellipseHead = new Ellipse2D.Double((mouseBInfo.getHeadPosition().getX() - (22 * mouseScaleModel)), (mouseBInfo.getHeadPosition().getY() - (22 * mouseScaleModel)), (45 * mouseScaleModel),
                    (45 * mouseScaleModel));
                Ellipse2D ellipseBody = new Ellipse2D.Double((mouseBInfo.getBodyPosition().getX() - (45 * mouseScaleModel)), (mouseBInfo.getBodyPosition().getY() - (45 * mouseScaleModel)), (90 * mouseScaleModel),
                    (90 * mouseScaleModel));
                Ellipse2D ellipseTail = new Ellipse2D.Double((mouseBInfo.getTailPosition().getX() - (10 * mouseScaleModel)), (mouseBInfo.getTailPosition().getY() - (10 * mouseScaleModel)), (20 * mouseScaleModel),
                    (20 * mouseScaleModel));

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

    public void actionPerformed(ActionEvent e) {
        if ((e.getSource() == displayForceCheckBox) || (e.getSource() == displayEnergyAreaCheckBox) || (e.getSource() == displayBodyCenterCheckBox) || (e.getSource() == displayBodyShapeCheckBox) ||
            (e.getSource() == displayGlobalSplineCheckBox) || (e.getSource() == displaySlideJointCheckBox) || (e.getSource() == displayBinaryMapCheckBox) || (e.getSource() == displayGradientMapCheckBox)) {
            sequence.painterChanged(null);
        }

        /*if (e.getSource() == computeATestAnchorVectorMapButton) {
         *  Sequence sequence = Icy.getMainInterface().getFocusedSequence();
         *  List<Ancre2> ancreListOut = new ArrayList<Ancre2>();
         *  List<Ancre2> ancreListIn = new ArrayList<Ancre2>();
         *
         *  Ancre2 a1 = new Ancre2(Icy.getMainInterface().getFocusedSequence().getWidth(),
         * Icy.getMainInterface().getFocusedSequence().getHeight(), 200, 160, 140 /*ray = 140./);
         *  //a1.setColor(Color.yellow);
         *
         *  Ancre2 a2 = new Ancre2(Icy.getMainInterface().getFocusedSequence().getWidth(),
         * Icy.getMainInterface().getFocusedSequence().getHeight(), 300, 160, 140 /*ray = 140./);
         *  //a2.setColor(Color.orange);
         *
         *  Ancre2 a3 = new Ancre2(Icy.getMainInterface().getFocusedSequence().getWidth(),
         * Icy.getMainInterface().getFocusedSequence().getHeight(), 250, 250, 100 /*ray = 140./);
         *  //a3.setColor(Color.blue);
         *
         *  Ancre2 aConflict = new Ancre2(Icy.getMainInterface().getFocusedSequence().getWidth(),
         * Icy.getMainInterface().getFocusedSequence().getHeight(), 250, 160, 140 /*ray = 140./);
         *  //aConflict.setColor(Color.pink);
         *
         *  byte[] aConflictdata = aConflict.carteAncre.getDataXYAsByte(0);
         *  byte[] a1data = a1.carteAncre.getDataXYAsByte(0);
         *  byte[] a2data = a2.carteAncre.getDataXYAsByte(0);
         *  byte[] a3data = a3.carteAncre.getDataXYAsByte(0);
         *
         *  for (int i = 0; i < aConflictdata.length; i++) {
         *      aConflictdata[i] = 0;
         *      if (a1data[i] != 0)
         *          aConflictdata[i]++;
         *      if (a2data[i] != 0)
         *          aConflictdata[i]++;
         *      if (a3data[i] != 0)
         *          aConflictdata[i]++;
         *  }
         *
         *  ancreListIn.add(a1);
         *  ancreListIn.add(a2);
         *  ancreListIn.add(a3);
         *
         *  // creation des zones ici nomme ancres
         *  for (final Ancre2 ancre : ancreListIn) {
         *      // cherche le max dans le masque de l ancre avec la map de
         *      // conflit
         *      int max = 0;
         *      byte[] mapAncre = ancre.carteAncre.getDataXYAsByte(0);
         *      for (int i = 0; i < aConflictdata.length; i++) {
         *          if (mapAncre[i] != 0) {
         *              if (aConflictdata[i] > max)
         *                  max = aConflictdata[i];
         *          }
         *      }
         *
         *      System.out.println("max = " + max);
         *
         *      // creation des ancres pour chaque indice de conflit
         *      for (int cumulIndex = max; cumulIndex > 0; cumulIndex--) {
         *          Ancre2 newAncre = new Ancre2(sequence.getWidth(), sequence.getHeight(), 10, 10, 0);
         *          byte[] ancreData = newAncre.carteAncre.getDataXYAsByte(0);
         *          for (int i = 0; i < aConflictdata.length; i++) {
         *              if (mapAncre[i] == (byte) 255) {
         *                  if (aConflictdata[i] == cumulIndex) {
         *                      ancreData[i] = (byte) 255;
         *                  }
         *              }
         *
         *          }
         *          System.out.println("computing.");
         *          computeATestAnchorVectorMap(true, newAncre);
         *          //newAncre.setColor(new Color((float) Math.random(), (float) Math.random(), (float) Math.random()));
         *          sequence.addPainter(newAncre);
         *      }
         *  }
         *}*/

        // generaliser a toutes les carte binaire et gradient
        /*if (e.getSource() == displayScaleBinaryButton) {
         *  binaryScaleMap = new ArrayList<Scale>();
         *  binaryScaleMap.clear();
         *
         *  // construction de la carte binaire.
         *
         *  // Echelle 1 = copie originale de la carte binarized
         *  Scale binaryScale0 = new Scale(Icy.getMainInterface().getFocusedSequence().getWidth(),
         * Icy.getMainInterface().getFocusedSequence().getHeight(), 0);
         *
         *  byte[] dataIn = Icy.getMainInterface().getFocusedSequence().getDataXYAsByte(0, 0, 0);
         *  float[] dataOut = binaryScale0.value;
         *  {
         *      int maxWidth = Icy.getMainInterface().getFocusedSequence().getWidth();
         *      int maxHeight = Icy.getMainInterface().getFocusedSequence().getHeight();
         *      int offset = 0;
         *
         *      for (int y = 0; y < maxHeight; y++) {
         *          for (int x = 0; x < maxWidth; x++) {
         *              dataOut[offset] = dataIn[offset] & 0xFF;
         *
         *              binaryScale0.barycenterX[offset] = x;
         *              binaryScale0.barycenterY[offset] = y;
         *              offset++;
         *          }
         *      }
         *  }
         *  binaryScale0.sendToDisplay();
         *
         *  binaryScaleMap.add(binaryScale0);
         *
         *  int MAX_SCALE_TO_BUILD = 9;
         *
         *  // construction des echelles version 2
         *  for (int scale = 1; scale < MAX_SCALE_TO_BUILD; scale++) {
         *      final Scale previousScale = binaryScaleMap.get(scale - 1);
         *      Scale currentScale = new Scale(previousScale.width / 2, previousScale.height / 2, scale);
         *
         *      float[] in = previousScale.value;
         *      float[] out = currentScale.value;
         *
         *      int maxHeight = previousScale.height / 2;
         *      int maxWidth = previousScale.width / 2;
         *
         *      for (int y = 0; y < maxHeight; y++) { // balayage dans la nouvelle echelle
         *          for (int x = 0; x < maxWidth; x++) { // balayage dans la nouvelle echelle
         *              // calcul de la valeur coefficient
         *
         *              boolean LOG = false;
         *
         *              if ((x == 20) && (y == 21) && (scale == 2)) {
         *                  System.out.println("log true x: 20 y:21 s:2");
         *                  LOG = true;
         *              }
         *
         *              int xx = x * 2;
         *              int yy = y * 2;
         *
         *              if (LOG) {
         *                  System.out.println("recherche pour X s= " + x);
         *                  System.out.println("recherche pour Y s= " + y);
         *
         *                  System.out.println("recherche pour X s-1= " + xx);
         *                  System.out.println("recherche pour X s-1= " + (xx + 1));
         *                  System.out.println("recherche pour Y s-1= " + yy);
         *                  System.out.println("recherche pour Y s-1= " + (yy + 1));
         *              }
         *
         *              out[x + (y * currentScale.width)] = in[xx + (yy * previousScale.width)] + in[xx + 1 + (yy *
         * previousScale.width)] + in[xx + ((yy + 1) * previousScale.width)] +
         *                  in[xx + 1 + ((yy + 1) * previousScale.width)];
         *
         *              if (LOG) {
         *                  System.out.println("previous val 1: " + in[xx + (yy * previousScale.width)]);
         *                  System.out.println("previous val 2: " + in[xx + 1 + (yy * previousScale.width)]);
         *                  System.out.println("previous val 3: " + in[xx + ((yy + 1) * previousScale.width)]);
         *                  System.out.println("previous val 4: " + in[xx + 1 + ((yy + 1) * previousScale.width)]);
         *                  System.out.println("OUT destination value = " + out[x + (y * currentScale.width)]);
         *              }
         *
         *              // calculation of propagated barycentres
         *              if (out[x + (y * currentScale.width)] != 0) {
         *                  if (LOG) {
         *                      System.out.println("Entree dans barycentre.");
         *                  }
         *
         *                  // X calculation for barycentre
         *                  currentScale.barycenterX[x + (y * currentScale.width)] =
         *                      ((float) ((previousScale.value[xx + (yy * previousScale.width)] *
         * previousScale.barycenterX[xx + (yy * previousScale.width)]) +
         *                              (previousScale.value[xx + 1 + (yy * previousScale.width)] *
         * previousScale.barycenterX[xx + 1 + (yy * previousScale.width)]) +
         *                              (previousScale.value[xx + ((yy + 1) * previousScale.width)] *
         * previousScale.barycenterX[xx + ((yy + 1) * previousScale.width)]) +
         *                              (previousScale.value[xx + 1 + ((yy + 1) * previousScale.width)] *
         * previousScale.barycenterX[xx + 1 + ((yy + 1) * previousScale.width)])) / (float) out[x + (y *
         * currentScale.width)]);
         *
         *                  if (LOG) {
         *                      System.out.println("Calcul de by: ");
         *                      System.out.println("p1: " + previousScale.barycenterY[xx + (yy * previousScale.width)]);
         *                      System.out.println("p2: " + previousScale.barycenterY[xx + 1 + (yy *
         * previousScale.width)]);
         *                      System.out.println("p3: " + previousScale.barycenterY[xx + ((yy + 1) *
         * previousScale.width)]);
         *                      System.out.println("p4: " + previousScale.barycenterY[xx + 1 + ((yy + 1) *
         * previousScale.width)]);
         *                  }
         *
         *                  currentScale.barycenterY[x + (y * currentScale.width)] =
         *
         *                      ((float) ((previousScale.value[xx + (yy * previousScale.width)] *
         * previousScale.barycenterY[xx + (yy * previousScale.width)]) +
         *                              (previousScale.value[xx + 1 + (yy * previousScale.width)] *
         * previousScale.barycenterY[xx + 1 + (yy * previousScale.width)]) +
         *                              (previousScale.value[xx + ((yy + 1) * previousScale.width)] *
         * previousScale.barycenterY[xx + ((yy + 1) * previousScale.width)]) +
         *                              (previousScale.value[xx + 1 + ((yy + 1) * previousScale.width)] *
         * previousScale.barycenterY[xx + 1 + ((yy + 1) * previousScale.width)])) / (float) out[x + (y *
         * currentScale.width)]);
         *
         *                  if (LOG) {
         *                      System.out.println("Bary x:" + currentScale.barycenterX[x + (y * currentScale.width)]);
         *                      System.out.println("Bary y:" + currentScale.barycenterY[x + (y * currentScale.width)]);
         *                  }
         *
         *              }
         *          }
         *      }
         *
         *      currentScale.sendToDisplay();
         *
         *      System.out.println("adding scale : " + scale);
         *      binaryScaleMap.add(currentScale);
         *  }
         *}*/

        /*if (e.getSource() == resultButton) {
         *  writeXMLResult();
         *}*/

//              if ( e.getSource() == applyNewScaleButton )
//              {
//                      //applyNewScaleButton
//                      SCALE = Float.parseFloat(scaleTextField.getText());
//
//                      for ( Mouse mouse : mouseList )
//                      {
//                              // set mouse again
//
////                            sdf
//                      }
//              }
    }


    public boolean isStable() {
        return !(world.getTotalEnergy() > 1000d);
    }

    public void loadXML(File currentFile) {
        // LOAD DOCUMENT
        mouse1Records.clear();
        mouse2Records.clear();

        File XMLFile = new File(currentFile.getAbsoluteFile() + ".xml");
        if (!XMLFile.exists()) {
            System.out.println("No XML linked to video provided.s");
            return;
        }

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
                        neckX = Float.parseFloat(detNode.getAttribute("neckx"));
                        neckY = Float.parseFloat(detNode.getAttribute("necky"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Point2D neckPosition = new Point2D.Float(neckX, neckY);
                    MouseInfoRecord mouseInfoRecord = new MouseInfoRecord(headPosition, tailPosition, bodyPosition, neckPosition);

                    mouse1Records.put(Integer.parseInt(detNode.getAttribute("t")), mouseInfoRecord);

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
                    try {
                        neckX = Float.parseFloat(detNode.getAttribute("neckx"));
                        neckY = Float.parseFloat(detNode.getAttribute("necky"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Point2D neckPosition = new Point2D.Float(neckX, neckY);
                    MouseInfoRecord mouseInfoRecord = new MouseInfoRecord(headPosition, tailPosition, bodyPosition, neckPosition);

                    mouse2Records.put(Integer.parseInt(detNode.getAttribute("t")), mouseInfoRecord);
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
            Set<Integer> integerKey = mouse1Records.keySet();
            Iterator<Integer> it = integerKey.iterator();
            while (it.hasNext()) {
                Integer t = it.next();
                if (t > maxT)
                    maxT = t;
            }
        }

        // MOUSE A
        for (int t = 0; t <= maxT; t++) {
            MouseInfoRecord mouseAInfo = mouse1Records.get(t);
            if (mouseAInfo == null)
                continue;

            Element newResultElement = XMLDocument.createElement("DET");
            newResultElement.setAttribute("t", "" + t);

            newResultElement.setAttribute("headx", "" + mouseAInfo.getHeadPosition().getX());
            newResultElement.setAttribute("heady", "" + mouseAInfo.getHeadPosition().getY());

            newResultElement.setAttribute("bodyx", "" + mouseAInfo.getBodyPosition().getX());
            newResultElement.setAttribute("bodyy", "" + mouseAInfo.getBodyPosition().getY());

            newResultElement.setAttribute("tailx", "" + mouseAInfo.getTailPosition().getX());
            newResultElement.setAttribute("taily", "" + mouseAInfo.getTailPosition().getY());

            newResultElement.setAttribute("neckx", "" + mouseAInfo.getNeckPosition().getX());
            newResultElement.setAttribute("necky", "" + mouseAInfo.getNeckPosition().getY());

            resultMouseA.appendChild(newResultElement);
        }

        // MOUSE B

        if (mouseList.size() > 1) { // check if two mice are presents
            for (int t = 0; t <= maxT; t++) {
                MouseInfoRecord mouseBInfo = mouse2Records.get(t);
                if (mouseBInfo == null)
                    continue;

                Element newResultElement = XMLDocument.createElement("DET");
                newResultElement.setAttribute("t", "" + t);

                newResultElement.setAttribute("headx", "" + mouseBInfo.getHeadPosition().getX());
                newResultElement.setAttribute("heady", "" + mouseBInfo.getHeadPosition().getY());

                newResultElement.setAttribute("bodyx", "" + mouseBInfo.getBodyPosition().getX());
                newResultElement.setAttribute("bodyy", "" + mouseBInfo.getBodyPosition().getY());

                newResultElement.setAttribute("tailx", "" + mouseBInfo.getTailPosition().getX());
                newResultElement.setAttribute("taily", "" + mouseBInfo.getTailPosition().getY());

                newResultElement.setAttribute("neckx", "" + mouseBInfo.getNeckPosition().getX());
                newResultElement.setAttribute("necky", "" + mouseBInfo.getNeckPosition().getY());

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

    public void setReverseThreshold(boolean reverseThresholdBoolean) {
        this.reverseThresholdBoolean = reverseThresholdBoolean;
    }

    /**
     * force head location for the next compute force frame (1 frame only)
     */
    public void setHeadLocation(int mouseNumber, MAnchor2D controlPoint) {
        headForcedPosition[mouseNumber] = controlPoint;
    }

    public void generateSlideJoint(Body bodyA, Body bodyB, float min, float max) {
        SlideJoint slideJoint = new SlideJoint(bodyA, bodyB, new Vector2f(0, 0), new Vector2f(0, 0f), min, max, 0);
        slideJointList.add(slideJoint);
        world.add(slideJoint);
    }

    public void recordMousePosition(int currentFrame) {
        mouse1Records.put(currentFrame, getMouseInfoRecord(0));
        mouse2Records.put(currentFrame, getMouseInfoRecord(1));
    }

    private void fillWindowPanel() {
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
    }

    private MouseInfoRecord getMouseInfoRecord(int i) {
        Point2D headPosition = new Point2D.Float(mouseList.get(i).getHead().getPosition().getX(), mouseList.get(i).getHead().getPosition().getY());
        Point2D tailPosition = new Point2D.Float(mouseList.get(i).getTail().getPosition().getX(), mouseList.get(i).getTail().getPosition().getY());
        Point2D bodyPosition = new Point2D.Float(mouseList.get(i).getTommyBody().getPosition().getX(), mouseList.get(i).getTommyBody().getPosition().getY());
        Point2D neckPosition = new Point2D.Float(mouseList.get(i).getNeckAttachBody().getPosition().getX(), mouseList.get(i).getNeckAttachBody().getPosition().getY());
        return new MouseInfoRecord(headPosition, tailPosition, bodyPosition, neckPosition);
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
                if (((x * x) + (y * y)) <= raySquare) {
                    int xx = centreX + x;
                    int yy = centreY + y;

                    if (xx >= 0) {
                        if (yy >= 0) {
                            if (xx < width) {
                                if (yy < height) {
                                    maskMapData[xx + (yy * width)] = val;
                                }
                            }
                        }
                    }

                }
            }
        }
    }
}
