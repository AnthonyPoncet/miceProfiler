package plugins.fab.MiceProfiler.controller;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;

import icy.image.IcyBufferedImageUtil;

import icy.painter.Overlay;

import icy.sequence.Sequence;

import net.phys2d.math.ROVector2f;
import net.phys2d.raw.Body;
import net.phys2d.raw.DistanceJoint;
import net.phys2d.raw.SlideJoint;
import net.phys2d.raw.shapes.Box;
import net.phys2d.raw.shapes.Circle;

import plugins.fab.MiceProfiler.MouseInfoRecord;
import plugins.fab.MiceProfiler.model.EnergyInfo;
import plugins.fab.MiceProfiler.model.EnergyMap;


public class MiceProfilerOverlay extends Overlay {

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Constructors
    //~ ----------------------------------------------------------------------------------------------------------------

    public MiceProfilerOverlay() {
        super("MiceProfilerOverlay");
    }

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Methods
    //~ ----------------------------------------------------------------------------------------------------------------

    @Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
        if (!(canvas instanceof IcyCanvas2D)) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setStroke(new BasicStroke(0.3f));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        if (displayBinaryMapCheckBox.isSelected()) {
            if (binaryMap != null) {
                g2.drawImage(IcyBufferedImageUtil.toBufferedImage(binaryMap, null), null, 0, 0);
            }
        }
        if (displayGradientMapCheckBox.isSelected()) {
            if (edgeMap != null) {
                g2.drawImage(IcyBufferedImageUtil.toBufferedImage(edgeMap, null), null, 0, 0);
            }
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
                net.phys2d.raw.shapes.Shape shape = body.getShape();
                if (shape != null) {
                    if (shape instanceof net.phys2d.raw.shapes.Polygon) {
                        ROVector2f[] vert = ((net.phys2d.raw.shapes.Polygon) shape).getVertices();
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
                    if (shape instanceof net.phys2d.raw.shapes.Box) {
                        net.phys2d.raw.shapes.Box maBox = (Box) shape;

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
            if (displayBinaryMapCheckBox.isSelected() && (energyInfo.getEnergyMap() != EnergyMap.BINARY_MOUSE)) {
                displayForce = false;
            }
            if (displayGradientMapCheckBox.isSelected() && (energyInfo.getEnergyMap() != EnergyMap.GRADIENT_MAP)) {
                displayForce = false;
            }
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
}
