package plugins.fab.MiceProfiler.controller;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import java.util.Date;
import java.util.prefs.Preferences;

import javax.swing.*;

import icy.file.FileUtil;

import icy.gui.dialog.MessageDialog;

import plugins.fab.MiceProfiler.ImageBufferThread;
import plugins.fab.MiceProfiler.PhyMouse;
import plugins.fab.MiceProfiler.StepThread;
import plugins.fab.MiceProfiler.TrackAllThread;
import plugins.fab.MiceProfiler.XugglerAviFile;
import plugins.fab.MiceProfiler.model.Video;
import plugins.fab.MiceProfiler.view.MiceProfilerWindow;
import plugins.fab.MiceProfiler.view.SequenceWindow;


public class MiceProfilerActionListener implements ActionListener {

    private final MiceProfilerWindow window;
    private final SequenceWindow sequenceWindow;

    private Video video;
    private ImageBufferThread bufferThread; //video reading thread
    private final Timer checkBufferTimer = new Timer(1000, this); //Check to update buffer display percentage. TODO: move that to proper implementation

    //Trackall
    private TrackAllThread trackAllThread;
    private PhyMouse phyMouse;

    //??
    private StepThread stepThread;

    public MiceProfilerActionListener(MiceProfilerWindow window, SequenceWindow sequenceWindow) {
        this.window = window;
        this.sequenceWindow = sequenceWindow;
    }

    @Override
    public synchronized void actionPerformed(ActionEvent e) {
        if (e.getSource() == checkBufferTimer) {
            if (bufferThread != null) {
                window.setBufferValue(bufferThread.getCurrentBufferLoadPercent());
                if (!bufferThread.isAlive()) {
                    checkBufferTimer.stop();
                }
            }
        } else if (window.hasBeenTrigeredByVideo(e.getSource())) {
            handleVideoButton();
        } else if (window.hasBeenTriggeredByPreviousFrame(e.getSource())) {
            window.setRelativeSliderValue(-1);
        } else if (window.hasBeenTriggeredByNextFrame(e.getSource())) {
            window.setRelativeSliderValue(1);
        } else if (window.hasBeenTriggeredByPrevious10Frame(e.getSource())) {
            window.setRelativeSliderValue(-10);
        } else if (window.hasBeenTriggeredByNext10Frame(e.getSource())) {
            window.setRelativeSliderValue(10);
        } else if (window.hasBeenTriggeredBySaveXML(e.getSource())) {
            saveXML();
        }

        /*if (e.getSource() == trackAllButton) {
         *  startTrackAll();
         * }
         *
         * if (e.getSource() == stopTrackAllButton) {
         *  stopTrackAll();
         *}*/

        /*if (e.getSource() == startThreadStepButton) {
         *  startThreadStepButton.setEnabled(false);
         *  trackAllButton.setEnabled(false);
         *  stopThreadStepButton.setEnabled(true);
         *  readPositionFromROIButton.setEnabled(false);
         *  stepThread = new StepThread(sequence, phyMouse, sliderTime);
         *  stepThread.start();
         * }
         *
         * if (e.getSource() == stopThreadStepButton) {
         *  stepThread.interrupt(); //TODO: should wait
         *  startThreadStepButton.setEnabled(true);
         *  trackAllButton.setEnabled(true);
         *  stopThreadStepButton.setEnabled(false);
         *  readPositionFromROIButton.setEnabled(true);
         * }
         *
         * if (e.getSource() == mouseColorComboBox) {
         *  System.out.println("Setting mouse color: " + mouseColorComboBox.getSelectedIndex());
         *  phyMouse.setReverseThreshold(mouseColorComboBox.getSelectedIndex() == 1);
         * }
         *
         * if (e.getSource() == readPositionFromROIButton) {
         *  readPositionFromROI();
         * }
         *
         * if (e.getSource() == reverseTrackFromTButton) {
         *  phyMouse.swapIdentityRecordFromTToTheEnd(sliderTime.getValue());
         *  sequence.painterChanged(null);
         *}*/
    }

    private void handleVideoButton() {
        JFileChooser fileChooser = createFileChooser();
        if (fileChooser.showDialog(null, "Load") == JFileChooser.APPROVE_OPTION) {
            saveFileChooserPreferences(fileChooser);

            try {
                XugglerAviFile aviFile = new XugglerAviFile(fileChooser.getSelectedFile().getAbsolutePath(), true);
                video = new Video(aviFile, fileChooser.getSelectedFile());
            } catch (Exception exc) {
                MessageDialog.showDialog("File type or video-codec not supported.", MessageDialog.ERROR_MESSAGE);
                video = null;
                return;
            }

            sequenceWindow.reset(video);

            window.setCurrentTimeLabel(0, video.getTotalNumberOfFrame(), video.getTimeForFrame(0));
            window.initializeSlider((int) video.getTotalNumberOfFrame());

            createOrUpdateImageBufferThread();

            //Configure phyMouse
            synchronized (phyMouse) {
                phyMouse = new PhyMouse(sequenceWindow.getSequence());
                phyMouse.fromXML(fileChooser.getSelectedFile());
                phyMouse.generateMouse(246, 48, 0); // Mouse 1
                phyMouse.generateMouse(238, 121, (float) (Math.PI)); // Mouse 2
            }

            //Update UI
            window.setUseImageBufferOptimisation(false);
            window.setNumberOfImageForBufferTextField(false);
        }
    }

    private JFileChooser createFileChooser() {
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
        return fileChooser;
    }

    private void saveFileChooserPreferences(JFileChooser fileChooser) {
        Preferences preferences = Preferences.userRoot().node("plugins/PhysicTracker/browser");
        preferences.put("path", fileChooser.getCurrentDirectory().getAbsolutePath());
        preferences.putInt("x", fileChooser.getX());
        preferences.putInt("y", fileChooser.getY());
        preferences.putInt("width", fileChooser.getWidth());
        preferences.putInt("height", fileChooser.getHeight());
    }

    private void createOrUpdateImageBufferThread() {
        if (bufferThread != null) {
            bufferThread.interrupt();
            try {
                bufferThread.join();
            } catch (final InterruptedException e1) {
                e1.printStackTrace();
            }
        }
        if (checkBufferTimer.isRunning()) {
            checkBufferTimer.stop();
        }

        bufferThread = new ImageBufferThread(window.getCurrentFrame(), window.getNumberOfImageForBuffer(), window.getTotalNumberOfFrame(), sequenceWindow.getSequence(), video.getAviFile());
        bufferThread.setName("Buffer Thread");
        bufferThread.setPriority(Thread.NORM_PRIORITY);
        bufferThread.start();

        checkBufferTimer.start(); //Listener for display buffer loaded percentage
    }

    private void saveXML() {
        // save a backup in the backup directory
        // Check if the folder exists
        String absolutePath = video.getCurrentFile().getAbsolutePath();
        String directory = FileUtil.getDirectory(absolutePath);
        directory += FileUtil.separator + "xml tracking backup";
        FileUtil.createDir(directory);

        String backupFileName = FileUtil.getFileName(absolutePath, false);
        backupFileName += " " + new Date().toString();

        backupFileName = backupFileName.replace(":", "_"); // remove time : incompatible with // fileName.

        String backupFullName = directory + FileUtil.separator + backupFileName;

        phyMouse.saveXML(new File(backupFullName));

        // update the file
        phyMouse.saveXML(video.getCurrentFile());
    }
}
