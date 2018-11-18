package plugins.fab.MiceProfiler.controller;

import icy.gui.dialog.MessageDialog;
import plugins.fab.MiceProfiler.Utilities.XugglerAviFile;
import plugins.fab.MiceProfiler.model.Video;
import plugins.fab.MiceProfiler.view.MiceProfilerWindow;
import plugins.fab.MiceProfiler.view.SequenceWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.prefs.Preferences;

public class VideoManager implements ActionListener /*use for checkBufferTimer*/ {
    private final MiceProfilerWindow miceProfilerWindow;
    private final SequenceWindow sequenceWindow;
    private final SliderChangeManager sliderChangeManager;

    private Video video;
    private ImageBufferThread bufferThread; //video reading thread
    private final Timer checkBufferTimer = new Timer(1000, this); //Check to update buffer display percentage. TODO: move that to proper implementation

    public VideoManager(MiceProfilerWindow miceProfilerWindow, SequenceWindow sequenceWindow, SliderChangeManager sliderChangeManager) {
        this.miceProfilerWindow = miceProfilerWindow;
        this.sequenceWindow = sequenceWindow;
        this.sliderChangeManager = sliderChangeManager;
    }

    public boolean loadVideo() {
        JFileChooser fileChooser = createFileChooser();
        if (fileChooser.showDialog(null, "Load") == JFileChooser.APPROVE_OPTION) {
            saveFileChooserPreferences(fileChooser);

            try {
                XugglerAviFile aviFile = new XugglerAviFile(fileChooser.getSelectedFile().getAbsolutePath(), true);
                video = new Video(aviFile, fileChooser.getSelectedFile());
            } catch (Exception exc) {
                MessageDialog.showDialog("File type or video-codec not supported.", MessageDialog.ERROR_MESSAGE);
                video = null;
                return false;
            }

            sliderChangeManager.ignoreChange.set(true);
            miceProfilerWindow.enableAfterVideoLoading(video.getTotalNumberOfFrame(), video.getTimeForFrame(0));
            sequenceWindow.reset(video);

            manageVideoBufferization();
            return true;
        }

        return false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == checkBufferTimer) {
            if (bufferThread != null) {
                miceProfilerWindow.setBufferValue(bufferThread.getCurrentBufferLoadPercent());
                if (!bufferThread.isAlive()) {
                    checkBufferTimer.stop();
                }
            }
        } else {
            System.err.println("VideoManager::actionPerformed : Unsupported action " + source);
        }
    }

    public void advanceFrame(int offset) {
        sliderChangeManager.ignoreChange.set(true);
        miceProfilerWindow.setRelativeSliderValue(offset);
        sequenceWindow.displayImageAt(miceProfilerWindow.getCurrentFrame(), video);
    }

    public void setFrame(int frame) {
        sliderChangeManager.ignoreChange.set(true);
        miceProfilerWindow.setSliderTimeValue(frame);
        sequenceWindow.displayImageAt(miceProfilerWindow.getCurrentFrame(), video);
    }

    public Video getVideo() { return video; }

    public File getCurrentFile() {
        return video.getCurrentFile();
    }

    public void manageVideoBufferization() {
        if (!miceProfilerWindow.useImageBufferOptimisation()) {
            //TODO: do something (load all video or load current frame?)
            return;
        }

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

        bufferThread = new ImageBufferThread(
                miceProfilerWindow.getCurrentFrame(),
                miceProfilerWindow.numberOfImageForBufferOptimisation(),
                (int) video.getTotalNumberOfFrame(),
                sequenceWindow.getSequence(),
                video.getAviFile());
        bufferThread.setName("Buffer Thread");
        bufferThread.setPriority(Thread.NORM_PRIORITY);
        bufferThread.start();

        checkBufferTimer.start(); //Listener for display buffer loaded percentage
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

}
