package plugins.fab.MiceProfiler.view;

import java.awt.*;
import java.awt.image.BufferedImage;

import java.util.List;

import icy.canvas.Canvas2D;

import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerListener;

import icy.main.Icy;

import icy.painter.Overlay;

import icy.plugin.abstract_.Plugin;

import icy.sequence.Sequence;

import icy.system.thread.ThreadUtil;

import plugins.fab.MiceProfiler.ManualHelper;
import plugins.fab.MiceProfiler.MouseGuidePainter;
import plugins.fab.MiceProfiler.model.Video;


public class SequenceWindow {

    private final Sequence sequence;

    private MouseGuidePainter mouseGuidePainter;

    /** list of all manual helper to disable certain action like record mode when an other engage it. */
    private List<ManualHelper> manualHelperList;

    public SequenceWindow() {
        this.sequence = new Sequence();
    }

    public void initialize(Plugin plugin, Overlay overlay, ViewerListener listener) {
        BufferedImage bImage = new BufferedImage(400, 400, BufferedImage.TYPE_3BYTE_BGR);
        sequence.setImage(0, 0, bImage);
        sequence.removeAllImages();
        sequence.addOverlay(overlay);

        plugin.addSequence(sequence);

        //Listen action on Sequence window
        ThreadUtil.invokeLater(() -> {
            Viewer v = Icy.getMainInterface().getFirstViewer(sequence);

            if (v != null) {
                v.addListener(listener);
            } else {
                throw new IllegalStateException("Mice Profiler Tracker Runnable failed because viewer is null.");
            }
        });
    }

    public void reset(Video video) {
        sequence.removeAllImages();
        sequence.setName(video.getVideoName());
        displayImageAt(0, video);

        //Configure sequence (remove all listener and Overlay and add new ones)
        manualHelperList.forEach(h -> {
            sequence.removeListener(h);
            sequence.removeOverlay(h);
        });
        manualHelperList.clear();
        sequence.removeOverlay(mouseGuidePainter);
        manualHelperList.add(new ManualHelper("Manual Helper", Color.RED, 1, manualHelperList));
        manualHelperList.add(new ManualHelper("Manual Helper", Color.GREEN, 2, manualHelperList));
        manualHelperList.forEach(h -> {
            sequence.addListener(h);
            sequence.addOverlay(h);
        });
        //sequence.addOverlay(new LockScrollHelperOverlay());
        mouseGuidePainter = new MouseGuidePainter();
        sequence.addOverlay(mouseGuidePainter);
    }

    //public paint

    public void displayImageAt(int frameNumber, Video video) {
        if (video != null) {
            if (sequence.getImage(frameNumber, 0) == null) {
                boolean wasEmpty = sequence.getNumImage() == 0;

                sequence.setImage(frameNumber, 0, video.getImage(frameNumber));

                if (wasEmpty) {
                    for (Viewer viewer : sequence.getViewers()) {
                        if (viewer.getCanvas() instanceof Canvas2D) {
                            ((Canvas2D) viewer.getCanvas()).centerImage();
                        }
                    }
                }
            }
        }
    }

    public Sequence getSequence() {
        return sequence;
    }
}
