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

import icy.canvas.Canvas2D;
import icy.canvas.IcyCanvas;
import icy.file.FileUtil;
import icy.gui.dialog.MessageDialog;
import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerEvent.ViewerEventType;
import icy.gui.viewer.ViewerListener;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.painter.Painter;
import icy.plugin.abstract_.Plugin;
import icy.plugin.interface_.PluginImageAnalysis;
import icy.sequence.DimensionId;
import icy.sequence.Sequence;
import icy.system.thread.ThreadUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Date;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.Timer;

/**
 * @author Fabrice de Chaumont
 */
public class MiceProfilerTracker extends Plugin implements Painter, PluginImageAnalysis, ActionListener, ChangeListener, ViewerListener {

    //Mice Profiler window (fill by compute() function)
    private final IcyFrame mainFrame = new IcyFrame("Mice Profiler", true, true, true, true);
    private final JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();

    //Video Settings
    private final JButton setVideoSourceButton = new JButton("Click to set/change video source");
    private final JSlider sliderTime = new JSlider();
    private final JCheckBox useImageBufferOptimisation = new JCheckBox("Use image load optimisation", true);
    private final JTextField numberOfImageForBufferTextField = new JTextField("200");
    private final JLabel currentBufferLabel = new JLabel("Current buffer:");
    private final JLabel bufferValue = new JLabel("0%");
    private final JButton previousFrame = new JButton("Previous Frame");
    private final JButton nextFrame = new JButton("Next Frame");
    private final JButton previous10Frame = new JButton("Previous 10 Frame");
    private final JButton next10Frame = new JButton("Next 10 Frame");
    private final JCheckBox updatePhysicsGuidesCheckBox = new JCheckBox("update phys. guides", true);
    private final JLabel currentTimeLabel = new JLabel("current time");
    //Other
    private final JButton trackAllButton = new JButton("<html><br><b>Track All Start</b><br><br></html>");
    private final JButton stopTrackAllButton = new JButton("<html><br><b>Track All Stop</b><br><br></html>");
    private final JButton readPositionFromROIButton = new JButton("Read starting position from Line ROI.");
    private final JButton saveXMLButton = new JButton("Save XML Data");
    private final JCheckBox limitTrackingSpeedCheckBox = new JCheckBox("Limit tracking speed to 15fps");
    private final JComboBox mouseColorComboBox = new JComboBox(new String[] { "Track black mice", "Track white mice" });
    private final JButton reverseTrackFromTButton = new JButton("Reverse Identity (from now to end of sequence)");
    private final JButton startThreadStepButton = new JButton("Start Step Anim");
    private final JButton stopThreadStepButton = new JButton("Stop Step Anim");
    private final JLabel lastFramePhysicTime = new JLabel("lastFramePhysicTime");
    private final JLabel lastFrameLoadTime = new JLabel("lastFrameLoadTime");
    private final JLabel lastFrameForceMapTime = new JLabel("lastFrameForceMapTime");
    private final JLabel totalImageTime = new JLabel("time last image");


    //Video display window
    private final Sequence sequenceOut = new Sequence();


    private final PhyMouse phyMouse = new PhyMouse(sequenceOut);
    private MouseGuidePainter mouseGuidePainter;

    private TrackAllThread trackAllThread;


    private XugglerAviFile aviFile;
    private File currentFile;
    private final Timer checkBufferTimer = new Timer(1000, this);
    private ImageBufferThread bufferThread; //video reading thread


    private StepThread stepThread;

    /*private final JButton startButton = new JButton("Start");
    private final JButton start2Button = new JButton("Start (step)");
    private Point mousePoint = new Point(100, 100);
    private final Animator animator = new Animator();




    private final JButton divPer2Button = new JButton("Div per 2 fps");

    private final JCheckBox useTotalSystemEnergyStopConditionBox = new JCheckBox("use total system energy stop condition", true);



    private final JButton loadXMLButton = new JButton("Load XML Data");

    private static final int ITERATION = 50;









    */

    public void setSliderTimeValue(int value) {
        sliderTime.setValue(value);
    }

    public int getCurrentFrame() {
        return sliderTime.getValue();
    }

    public int getTotalNumberOfFrame() {
        return sliderTime.getMaximum();
    }

    public int getNumberOfImageForBuffer() {
        return Integer.parseInt(numberOfImageForBufferTextField.getText());
    }

    public IcyBufferedImage getImageAt(int time) {
        return sequenceOut.getImage(time, 0);
    }

    public void deactivateTrackAll() {
        stopTrackAllButton.setEnabled(false);
        trackAllButton.setEnabled(true);
        startThreadStepButton.setEnabled(true);
        readPositionFromROIButton.setEnabled(true);
    }


    //interface PluginImageAnalysis
    @Override
    public void compute() {
        //Initialize video display window
        BufferedImage bImage = new BufferedImage(400, 400, BufferedImage.TYPE_3BYTE_BGR);
        sequenceOut.setImage(0, 0, bImage);
        addSequence(sequenceOut);
        sequenceOut.removeAllImages();
        sequenceOut.addPainter(this);

        // Start Physics Engines
        System.out.println("----------");
        System.out.println("Mice Profiler / Fab / Version 7");
        //System.out.println("Red mice: occupante /// Green: visiteur");

        //Initialize Mice Profiler window
        mainFrame.setLayout(new BorderLayout());
        mainFrame.add(mainPanel, BorderLayout.CENTER);
        mainPanel.add(GuiUtil.besidesPanel(createVideoPanel()));
        mainPanel.add(GuiUtil.besidesPanel(trackAllButton, stopTrackAllButton));
        mainPanel.add(GuiUtil.besidesPanel(readPositionFromROIButton));
        mainPanel.add(GuiUtil.besidesPanel(saveXMLButton));
        mainPanel.add(GuiUtil.besidesPanel(limitTrackingSpeedCheckBox));
        mainPanel.add(GuiUtil.besidesPanel(mouseColorComboBox));
        mainPanel.add(GuiUtil.besidesPanel(reverseTrackFromTButton));
        mainPanel.add(GuiUtil.besidesPanel(phyMouse.getPanel()));
        mainPanel.add(GuiUtil.besidesPanel(startThreadStepButton, stopThreadStepButton));
        mainPanel.add(GuiUtil.besidesPanel(lastFramePhysicTime, lastFrameLoadTime));
        mainPanel.add(GuiUtil.besidesPanel(lastFrameForceMapTime, totalImageTime));
        stopTrackAllButton.addActionListener(this);
        stopTrackAllButton.setEnabled(false);
        readPositionFromROIButton.addActionListener(this);
        saveXMLButton.addActionListener(this);
        mouseColorComboBox.addActionListener( this );
        reverseTrackFromTButton.addActionListener( this );
        startThreadStepButton.addActionListener(this);
        stopThreadStepButton.addActionListener(this);
        stopThreadStepButton.setEnabled(false);

        //loadXMLButton.setEnabled(false);
        //startButton.addActionListener(this);
        //start2Button.addActionListener(this);
        //trackAllButton.addActionListener(this);

        mainFrame.pack();
        mainFrame.center();
        mainFrame.setVisible(true);
        mainFrame.addToDesktopPane();


        ThreadUtil.invokeLater(() -> {
            Viewer v = Icy.getMainInterface().getFirstViewer(sequenceOut);

            if (v != null)
                v.addListener(MiceProfilerTracker.this);
            else
                throw new IllegalStateException("Mice Profiler Tracker Runnable failed because viewer is null.");
        });

        checkBufferTimer.start();
    }

    private JPanel createVideoPanel() {
        JPanel videoPanel = GuiUtil.generatePanel("Video Settings");
        videoPanel.add(GuiUtil.besidesPanel(setVideoSourceButton));
        videoPanel.add(GuiUtil.besidesPanel(sliderTime));
        videoPanel.add(GuiUtil.besidesPanel(useImageBufferOptimisation, numberOfImageForBufferTextField));
        videoPanel.add(GuiUtil.besidesPanel(currentBufferLabel, bufferValue));
        videoPanel.add(GuiUtil.besidesPanel(previousFrame, nextFrame));
        videoPanel.add(GuiUtil.besidesPanel(previous10Frame, next10Frame));
        videoPanel.add(GuiUtil.besidesPanel(updatePhysicsGuidesCheckBox));
        videoPanel.add(GuiUtil.besidesPanel(currentTimeLabel));
        setVideoSourceButton.addActionListener(this);
        sliderTime.setMajorTickSpacing(1000);
        sliderTime.setPaintTicks(true);
        sliderTime.setPaintTrack(true);
        sliderTime.addChangeListener(this);
        previousFrame.addActionListener(this);
        nextFrame.addActionListener(this);
        previous10Frame.addActionListener(this);
        next10Frame.addActionListener(this);
        return videoPanel;
    }

    private int getCurrentBufferLoadPercent() {
        int currentBufferPercent;
        int frameStart = getCurrentFrame() - 10;
        int frameEnd = getCurrentFrame() + getNumberOfImageForBuffer();
        float nbImage = 0;
        float nbImageLoaded = 0;

        for (int t = frameStart; t < frameEnd; t++) {
            if (t >= 0 && t < getTotalNumberOfFrame()) {
                nbImage++;
                if (sequenceOut.getImage(t, 0) != null)
                    nbImageLoaded++;
            }
        }

        currentBufferPercent = (int) (nbImageLoaded * 100f / nbImage);

        return currentBufferPercent;
    }

    //interface ActionListener
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == checkBufferTimer) {
            bufferValue.setText(getCurrentBufferLoadPercent() + " %");
        }

        if (e.getSource() == setVideoSourceButton) {
            // load last preferences for loader.
            JFileChooser fileChooser = new JFileChooser();
            Preferences preferences = Preferences.userRoot().node("plugins/PhysicTracker/browser");
            String path = preferences.get("path", "");
            fileChooser.setCurrentDirectory(new File(path));
            int x = preferences.getInt("x", 0);
            int y = preferences.getInt("y", 0);
            int width = preferences.getInt("width", 400);
            int height = preferences.getInt("height", 400);
            fileChooser.setLocation(x, y);
            fileChooser.setPreferredSize(new Dimension(width, height));

            if (fileChooser.showDialog(null, "Load") == JFileChooser.APPROVE_OPTION) {
                preferences.put("path", fileChooser.getCurrentDirectory().getAbsolutePath());
                preferences.putInt("x", fileChooser.getX());
                preferences.putInt("y", fileChooser.getY());
                preferences.putInt("width", fileChooser.getWidth());
                preferences.putInt("height", fileChooser.getHeight());

                try {
                    aviFile = new XugglerAviFile(fileChooser.getSelectedFile().getAbsolutePath(), true);
                } catch (Exception exc) {
                    MessageDialog.showDialog( "File type or video-codec not supported.", MessageDialog.ERROR_MESSAGE);
                    aviFile = null;
                    return;
                }

                sequenceOut.removeAllImages();
                sequenceOut.setName(fileChooser.getSelectedFile().getName());
                displayImageAt(0);

                sliderTime.setMaximum((int) aviFile.getTotalNumberOfFrame());
                sliderTime.setValue(0);

                setVideoSourceButton.setText(fileChooser.getSelectedFile().getName());

                currentFile = fileChooser.getSelectedFile();
                phyMouse.loadXML(currentFile);

                if (bufferThread != null) {
                    bufferThread.setPleaseStop(true);
                    try {
                        bufferThread.join();
                    } catch (final InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
                bufferThread = new ImageBufferThread(this);
                bufferThread.setName("Buffer Thread");
                bufferThread.setPriority(Thread.NORM_PRIORITY);
                bufferThread.start();

                useImageBufferOptimisation.setEnabled(false);
                numberOfImageForBufferTextField.setEnabled(false);

                synchronized (phyMouse) {
                    phyMouse.generateMouse(246, 48, 0); // Mouse 1
                    phyMouse.generateMouse(238, 121, (float) (Math.PI)); // Mouse 2
                }

                ManualHelper manualHelperA = new ManualHelper("Manual Helper", Color.RED, 1);
                ManualHelper manualHelperB = new ManualHelper("Manual Helper", Color.GREEN, 2);
                sequenceOut.addListener(manualHelperA);
                sequenceOut.addListener(manualHelperB);
                sequenceOut.addOverlay(manualHelperA);
                sequenceOut.addOverlay(manualHelperB);
                sequenceOut.addOverlay(new LockScrollHelperOverlay());

                mouseGuidePainter = new MouseGuidePainter(sequenceOut, false);
            }
        }

        if (e.getSource() == previousFrame) {
        	displayRelativeFrame( -1);
        }
        if (e.getSource() == nextFrame) {
        	displayRelativeFrame( 1);
        }
        if (e.getSource() == previous10Frame) {
            displayRelativeFrame( -10 );
        }
        if (e.getSource() == next10Frame) {
            displayRelativeFrame( 10 );
        }

        if (e.getSource() == saveXMLButton) {
            saveXML();
        }


        /*if (e.getSource() == startButton) {
            sliderTime.setValue(1 + sliderTime.getValue());

            IcyBufferedImage imageSourceR = sequenceOut.getImage(0, 0); // .getAsRenderedImage(
            IcyBufferedImage imageSource = imageSourceR;

            synchronized (phyMouse01A) {
                phyMouse01A.computeForcesMap(imageSource);
            }

            for (int i = 0; i < ITERATION; i++) {
                synchronized (phyMouse01A) {
                    phyMouse01A.computeForces();
                    phyMouse01A.worldStep(currentFrame);
                }
            }

            sequenceOut.painterChanged(null);
        }*/

        /*if (e.getSource() == divPer2Button) {
            phyMouse01A.divideTimePer2();
        }*/



        if (e.getSource() == trackAllButton) {
        	startTrackAll();
        }

        if (e.getSource() == stopTrackAllButton) {
        	stopTrackAll();
        }

        if (e.getSource() == startThreadStepButton) {
            startThreadStepButton.setEnabled(false);
            trackAllButton.setEnabled(false);
            stopThreadStepButton.setEnabled(true);
            readPositionFromROIButton.setEnabled(false);
            stepThread = new StepThread(this);
            stepThread.start();
        }

        if (e.getSource() == stopThreadStepButton) {
            stepThread.shouldRun = false;
            startThreadStepButton.setEnabled(true);
            trackAllButton.setEnabled( true );
            stopThreadStepButton.setEnabled(false);
            readPositionFromROIButton.setEnabled( true );
        }

        if (e.getSource() == mouseColorComboBox) {
            System.out.println("Setting mouse color: " + mouseColorComboBox.getSelectedIndex());
            phyMouse.setReverseThreshold(mouseColorComboBox.getSelectedIndex() == 1);
        }
        
        if (e.getSource() == readPositionFromROIButton) {
            readPositionFromROI();
        }

        /*if (e.getSource() == start2Button) {
            IcyBufferedImage imageSourceR = sequenceOut.getImage(currentFrame, 0);
            IcyBufferedImage imageSource = imageSourceR;

            phyMouse01A.computeForcesMap(imageSource);
            phyMouse01A.computeForces();
            phyMouse01A.worldStep(currentFrame);

            sequenceOut.painterChanged(null);
        }*/
        
        if (e.getSource() == reverseTrackFromTButton) {
        	phyMouse.swapIdentityRecordFromTToTheEnd(sliderTime.getValue());
        	sequenceOut.painterChanged(null);
        }
    }

    //interface Painter
    @Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
        synchronized (phyMouse) {
            phyMouse.paint(g, canvas);
        }

    }

    @Override
    public void mousePressed(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {
    }

    @Override
    public void mouseReleased(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {
    }

    @Override
    public void mouseClick(MouseEvent e, Point2D p, IcyCanvas quiaCanvas) {
    }

    @Override
    public void mouseMove(MouseEvent e, Point2D p, IcyCanvas quiaCanvas) {
    }

    @Override
    public void mouseDrag(MouseEvent e, Point2D p, IcyCanvas quiaCanvas) {
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
                    if (!trackAllThread.shouldRun) {
                        startTrackAll();
                    } else {
                        stopTrackAll();
                    }
                }
                e.consume();
                break;
        }

    	if (e.getKeyChar()=='r') {
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
            Viewer v = Icy.getMainInterface().getFirstViewer(sequenceOut);

            if (v != null)
                v.setT(sliderTime.getValue());

            displayImageAt(sliderTime.getValue());

            if ( trackAllButton.isEnabled() ) { // should be something better...
            	// set mouseA
            	{
            		MouseInfoRecord record = phyMouse.mouseARecord.get(sliderTime.getValue());
            		if ( record != null ) {
            			mouseGuidePainter.M1h.setPosition( record.headPosition );
            			mouseGuidePainter.M1b.setPosition( record.tailPosition );
            		}
            	}

            	// set mouseB
            	{
            		MouseInfoRecord record = phyMouse.mouseBRecord.get( sliderTime.getValue() );
            		if ( record != null ) {
            			mouseGuidePainter.M2h.setPosition( record.headPosition );
            			mouseGuidePainter.M2b.setPosition( record.tailPosition );
            		}
            	}
            }
        }
    }

    //interface ViewerListener
    @Override
    public void viewerChanged(ViewerEvent event) {
        if ((event.getType() == ViewerEventType.POSITION_CHANGED) && (event.getDim() == DimensionId.T))        
            sliderTime.setValue(event.getSource().getPositionT());
    }

    @Override
    public void viewerClosed(Viewer viewer) {
        // ignore
    }



    private void readPositionFromROI() {
        synchronized (phyMouse) {
            phyMouse.clearMapForROI();

            //System.out.println("debug: Read position from roi step 1");

            // mouse 1
            {
                float alpha = (float) Math.atan2(mouseGuidePainter.getM1h().getY() - mouseGuidePainter.getM1b().getY(),
                        mouseGuidePainter.getM1h().getX() - mouseGuidePainter.getM1b().getX());

                phyMouse01A.generateMouse((float) mouseGuidePainter.getM1h().getX(), (float) mouseGuidePainter.getM1h().getY(),
                        alpha + (float) Math.PI / 2f);
            }
            // mouse 2
            {
                // System.out.println("READ POSITION FROM ROI");

                final float alpha = (float) Math.atan2(mouseGuidePainter.getM2h().getY() - mouseGuidePainter.getM2b().getY(),
                        mouseGuidePainter.getM2h().getX() - mouseGuidePainter.getM2b().getX());

                phyMouse01A.generateMouse((float) mouseGuidePainter.getM2h().getX(), (float) mouseGuidePainter.getM2h().getY(),
                        alpha + (float) Math.PI / 2f);
            }

            phyMouse01A.recordMousePosition(sliderTime.getValue());

            //System.out.println("nb souris dans mouse model : " + phyMouse01A.mouseList.size());

        }
        sequenceOut.painterChanged(null);
    }

    private void saveXML() {
        // save a backup in the backup directory
        // Check if the folder exists
        String directory = FileUtil.getDirectory(currentFile.getAbsolutePath());
        directory += FileUtil.separator + "xml tracking backup";
        FileUtil.createDir(directory);

        String backupFileName = FileUtil.getFileName(currentFile.getAbsolutePath(), false);
        backupFileName += " " + new Date().toString();

        backupFileName = backupFileName.replace(":", "_"); // remove time : incompatible with // fileName.

        String backupFullName = directory + FileUtil.separator + backupFileName;

        phyMouse01A.saveXML(new File(backupFullName));

        // update the file
        phyMouse01A.saveXML(currentFile);
    }

    private void displayImageAt(int frameNumber) {
        if (aviFile != null) {
            currentFrame = frameNumber;
            phyMouse.currentFrame = frameNumber;

            if (sequenceOut.getImage(frameNumber, 0) == null) {
                boolean wasEmpty = sequenceOut.getNumImage() == 0;

                sequenceOut.setImage(frameNumber, 0, aviFile.getImage(frameNumber));

                if (wasEmpty) {
                    for (Viewer viewer : sequenceOut.getViewers()) {
                        if (viewer.getCanvas() instanceof Canvas2D) {
                            ((Canvas2D) viewer.getCanvas()).centerImage();
                        }
                    }
                }
            }

            String timeString = "";
            timeString += "(#frame): " + frameNumber + "/" + aviFile.getTotalNumberOfFrame() + " "  + aviFile.getTimeForFrame(frameNumber);

            currentTimeLabel.setText(timeString);
        }
    }

    private void displayRelativeFrame(int nbFrame) {
        sliderTime.setValue(sliderTime.getValue() + nbFrame );
    }

    private void startTrackAll() {
        readPositionFromROI();
        trackAllThread = new TrackAllThread(this);
        trackAllThread.start();
        stopTrackAllButton.setEnabled(true);
        trackAllButton.setEnabled(false);
        startThreadStepButton.setEnabled(false);
        readPositionFromROIButton.setEnabled(false);
    }

    private void stopTrackAll() {
        trackAllThread.shouldRun = false;
    }
}