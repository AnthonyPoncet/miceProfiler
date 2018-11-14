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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.google.common.collect.Lists;

import icy.canvas.IcyCanvas;

import icy.gui.viewer.Viewer;

import icy.main.Icy;

import icy.plugin.abstract_.PluginActionable;

import icy.sequence.Sequence;

import plugins.fab.MiceProfiler.controller.MiceProfilerActionListener;
import plugins.fab.MiceProfiler.controller.SequenceViewerListener;
import plugins.fab.MiceProfiler.view.MiceProfilerWindow;
import plugins.fab.MiceProfiler.view.SequenceWindow;


/**
 * @author Fabrice de Chaumont
 */
public class MiceProfilerTracker extends PluginActionable implements ChangeListener {

    private MiceProfilerWindow miceProfilerWindow;
    private SequenceWindow sequenceWindow;

    //interface PluginActionable
    @Override
    public void run() {
        miceProfilerWindow = new MiceProfilerWindow();
        sequenceWindow = new SequenceWindow();
        MiceProfilerActionListener miceProfilerActionListener = new MiceProfilerActionListener(miceProfilerWindow, sequenceWindow);
        SequenceViewerListener sequenceViewerListener = new SequenceViewerListener(miceProfilerWindow);

        miceProfilerWindow.initialize(miceProfilerActionListener, this);
        sequenceWindow.initialize(this, miceProfilerWindow, sequenceViewerListener);

        // Start Physics Engines
        System.out.println("----------");
        System.out.println("Mice Profiler / Fab / Version 7sssss");
        System.out.println("Red mice: occupante /// Green: visiteur");
    }


    @Override
    public void keyPressed(KeyEvent e, Point2D imagePoint, IcyCanvas quiaCanvas) {
        // shortcuts:
        switch (e.getKeyCode()) {

        case KeyEvent.VK_LEFT:
            displayRelativeFrame(-1);
            e.consume();
            break;

        case KeyEvent.VK_RIGHT:
            displayRelativeFrame(1);
            e.consume();
            break;

        case KeyEvent.VK_DOWN:
            displayRelativeFrame(-10);
            e.consume();
            break;

        case KeyEvent.VK_UP:
            displayRelativeFrame(10);
            e.consume();
            break;

        case KeyEvent.VK_SPACE:
            if (trackAllThread == null) {
                startTrackAll();
            } else {
                if (!trackAllThread.isAlive()) {
                    startTrackAll();
                } else {
                    stopTrackAll();
                }
            }
            e.consume();
            break;
        }

        if (e.getKeyChar() == 'r') {
            readPositionFromROI();
            displayRelativeFrame(1);
            readPositionFromROI();
            e.consume();
        }
    }

    @Override
    public void keyReleased(KeyEvent e, Point2D imagePoint, IcyCanvas canvas) {
    }

    //interface ChangeListener
    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == sliderTime) {
            createOrUpdateImageBufferThread();

            Viewer v = Icy.getMainInterface().getFirstViewer(sequence);

            if (v != null) {
                v.setPositionT(sliderTime.getValue());
            }

            displayImageAt(sliderTime.getValue());

            if (trackAllButton.isEnabled()) { // should be something better...
                // set mouseA
                {
                    MouseInfoRecord record = phyMouse.getMouse1Records().get(sliderTime.getValue());
                    if (record != null) {
                        mouseGuidePainter.getM1h().setPosition(record.getHeadPosition());
                        mouseGuidePainter.getM1b().setPosition(record.getTailPosition());
                    }
                }

                // set mouseB
                {
                    MouseInfoRecord record = phyMouse.getMouse2Records().get(sliderTime.getValue());
                    if (record != null) {
                        mouseGuidePainter.getM2h().setPosition(record.getHeadPosition());
                        mouseGuidePainter.getM2b().setPosition(record.getTailPosition());
                    }
                }
            }
        }
    }

    private void readPositionFromROI() {
        synchronized (phyMouse) {
            phyMouse.clearMapForROI();

            //System.out.println("debug: Read position from roi step 1");

            // mouse 1
            {
                float alpha = (float) Math.atan2(mouseGuidePainter.getM1h().getY() - mouseGuidePainter.getM1b().getY(), mouseGuidePainter.getM1h().getX() - mouseGuidePainter.getM1b().getX());

                phyMouse.generateMouse((float) mouseGuidePainter.getM1h().getX(), (float) mouseGuidePainter.getM1h().getY(), alpha + ((float) Math.PI / 2f));
            }
            // mouse 2
            {
                // System.out.println("READ POSITION FROM ROI");

                final float alpha = (float) Math.atan2(mouseGuidePainter.getM2h().getY() - mouseGuidePainter.getM2b().getY(), mouseGuidePainter.getM2h().getX() - mouseGuidePainter.getM2b().getX());

                phyMouse.generateMouse((float) mouseGuidePainter.getM2h().getX(), (float) mouseGuidePainter.getM2h().getY(), alpha + ((float) Math.PI / 2f));
            }

            phyMouse.recordMousePosition(sliderTime.getValue());

            //System.out.println("nb souris dans mouse model : " + phyMouse.mouseList.size());

        }
        sequence.painterChanged(null);
    }

    private void startTrackAll() {
        readPositionFromROI();
        trackAllThread = new TrackAllThread(this, sequence, phyMouse, mouseGuidePainter, getCurrentFrame(), getTotalNumberOfFrame());
        trackAllThread.start();
        stopTrackAllButton.setEnabled(true);
        trackAllButton.setEnabled(false);
        startThreadStepButton.setEnabled(false);
        readPositionFromROIButton.setEnabled(false);
    }

    private void stopTrackAll() {
        trackAllThread.interrupt();
    }
}
