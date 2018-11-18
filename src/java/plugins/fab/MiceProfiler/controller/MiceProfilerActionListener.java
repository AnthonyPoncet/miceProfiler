package plugins.fab.MiceProfiler.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import java.util.Date;

import icy.file.FileUtil;

import plugins.fab.MiceProfiler.PhyMouse;
import plugins.fab.MiceProfiler.StepThread;
import plugins.fab.MiceProfiler.TrackAllThread;
import plugins.fab.MiceProfiler.view.MiceProfilerWindow;
import plugins.fab.MiceProfiler.view.MouseGuidesPainter;
import plugins.fab.MiceProfiler.view.SequenceWindow;


public class MiceProfilerActionListener implements ActionListener {

    private final VideoManager videoManager;
    private final PhyMouseManager phyMouseManager;
    private final MiceProfilerWindow miceProfilerWindow;
    private final SequenceWindow sequenceWindow;

    //Trackall
    private TrackAllThread trackAllThread;

    //Step Animation
    private StepThread stepThread;

    public MiceProfilerActionListener(VideoManager videoManager, PhyMouseManager phyMouseManager, MiceProfilerWindow miceProfilerWindow, SequenceWindow sequenceWindow) {
        this.videoManager = videoManager;
        this.phyMouseManager = phyMouseManager;
        this.miceProfilerWindow = miceProfilerWindow;
        this.sequenceWindow = sequenceWindow;
    }

    @Override
    public synchronized void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (miceProfilerWindow.hasBeenTriggeredByVideoSourceButton(source)) {
            if (videoManager.loadVideo()) {
                phyMouseManager.reset(videoManager.getCurrentFile());
            }
        } else if (miceProfilerWindow.hasBeenTriggeredByPreviousFrame(source)) {
            videoManager.advanceFrame(-1);
        } else if (miceProfilerWindow.hasBeenTriggeredByNextFrame(source)) {
            videoManager.advanceFrame(1);
        } else if (miceProfilerWindow.hasBeenTriggeredByPrevious10Frame(source)) {
            videoManager.advanceFrame(-10);
        } else if (miceProfilerWindow.hasBeenTriggeredByNext10Frame(source)) {
            videoManager.advanceFrame(10);
        } else if (miceProfilerWindow.hasBeenTriggeredByTrackAllButton(source)) {
            startTrackAll();
        } else if (miceProfilerWindow.hasBeenTriggeredByStopTrackAllButton(source)) {
            stopTrackAll();
        } else if (miceProfilerWindow.hasBeenTriggeredByReadPositionFromROIButton(source)) {
            initializeMousesPositionFromGuides();
        } else if (miceProfilerWindow.hasBeenTriggeredBySaveXML(source)) {
            saveXML();
        } else if (miceProfilerWindow.hasBeenTriggeredByReverseTrackFromTButton(source)) {
            //phyMouse.swapIdentityRecordFromTToTheEnd(sliderTime.getValue());
            //sequence.painterChanged(null);
        } else if (miceProfilerWindow.hasBeenTriggeredByStartThreadStepButton(source)) {
            // startThreadStepButton.setEnabled(false);
            //trackAllButton.setEnabled(false);
            //stopThreadStepButton.setEnabled(true);
            //readPositionFromROIButton.setEnabled(false);
            //stepThread = new StepThread(sequence, phyMouse, sliderTime);
            //stepThread.start();
        } else if (miceProfilerWindow.hasBeenTriggeredByStopThreadStepButton(source)) {
            //stepThread.interrupt(); //TODO: should wait
            //startThreadStepButton.setEnabled(true);
            //trackAllButton.setEnabled(true);
            //stopThreadStepButton.setEnabled(false);
            //readPositionFromROIButton.setEnabled(true);
        } else {
            System.err.println("MiceProfilerActionListener: Unsupported action " + source);
        }


        /*if (e.getSource() == mouseColorComboBox) {
         *  System.out.println("Setting mouse color: " + mouseColorComboBox.getSelectedIndex());
         *  phyMouse.setReverseThreshold(mouseColorComboBox.getSelectedIndex() == 1);
         * }
         * */
    }

    private void startTrackAll() {
        stopTrackAll();
        initializeMousesPositionFromGuides();
        trackAllThread = new TrackAllThread(
                miceProfilerWindow,
                videoManager,
                sequenceWindow,
                phyMouseManager,
                sequenceWindow.getMouseGuidesPainter(),
                miceProfilerWindow.getCurrentFrame(),
                (int) videoManager.getVideo().getTotalNumberOfFrame());
        trackAllThread.start();
        miceProfilerWindow.enableAfterTrackAllStarted();
    }

    private void saveXML() {
        // save a backup in the backup directory
        // Check if the folder exists
        String absolutePath = videoManager.getCurrentFile().getAbsolutePath();
        String directory = FileUtil.getDirectory(absolutePath);
        directory += FileUtil.separator + "xml tracking backup";
        FileUtil.createDir(directory);

        String backupFileName = FileUtil.getFileName(absolutePath, false);
        backupFileName += " " + new Date().toString();

        backupFileName = backupFileName.replace(":", "_"); // remove time : incompatible with // fileName.

        String backupFullName = directory + FileUtil.separator + backupFileName;

        phyMouseManager.getPhyMouse().saveXML(new File(backupFullName));

        //update the file
        phyMouseManager.getPhyMouse().saveXML(videoManager.getCurrentFile());
    }

    private void initializeMousesPositionFromGuides() {
        phyMouseManager.getPhyMouse().clearMapForROI();

        MouseGuidesPainter mouseGuidesPainter = sequenceWindow.getMouseGuidesPainter();
        phyMouseManager.getPhyMouse().generateMouse(mouseGuidesPainter.getMouseAGuide(), mouseGuidesPainter.getMouseBGuide());

        //phyMouseManager.getPhyMouse().recordMousePosition(sliderTime.getValue());

        //sequence.painterChanged(null);
    }

    private void stopTrackAll() {
        if (trackAllThread != null) {
            trackAllThread.interrupt();
            try {
                trackAllThread.join();
            } catch (final InterruptedException e1) {
                e1.printStackTrace();
            }
        }

        miceProfilerWindow.enableAfterTrackAllStopped();
    }
}
