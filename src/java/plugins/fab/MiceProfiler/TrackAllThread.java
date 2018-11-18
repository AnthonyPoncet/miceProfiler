package plugins.fab.MiceProfiler;

import java.util.Calendar;

import icy.image.IcyBufferedImage;

import icy.system.thread.ThreadUtil;

import plugins.fab.MiceProfiler.controller.PhyMouseManager;
import plugins.fab.MiceProfiler.controller.VideoManager;
import plugins.fab.MiceProfiler.view.MiceProfilerWindow;
import plugins.fab.MiceProfiler.view.MouseGuidesPainter;
import plugins.fab.MiceProfiler.view.SequenceWindow;


public class TrackAllThread extends Thread {

    private static final int ITERATION = 50;

    private final MiceProfilerWindow miceProfilerWindow;
    private final VideoManager videoManager;
    private final SequenceWindow sequenceWindow;
    private final PhyMouseManager phyMouseManager;
    private final MouseGuidesPainter mouseGuidesPainter;

    private final int startingFrame;
    private final int totalNumberOfImage;

    public TrackAllThread(MiceProfilerWindow miceProfilerWindow, VideoManager videoManager, SequenceWindow sequenceWindow, PhyMouseManager phyMouseManager, MouseGuidesPainter mouseGuidesPainter, int startingFrame, int totalNumberOfImage) {
        this.miceProfilerWindow = miceProfilerWindow;
        this.videoManager = videoManager;
        this.sequenceWindow = sequenceWindow;
        this.phyMouseManager = phyMouseManager;
        this.mouseGuidesPainter = mouseGuidesPainter;
        this.startingFrame = startingFrame;
        this.totalNumberOfImage = totalNumberOfImage;
    }

    @Override
    public void run() {
        System.out.println("Trackall: run()");
        for (int t = startingFrame; t < totalNumberOfImage; t++) {
            //System.out.println("Trackall: frame number " + t);
            double totalImageTimerStart = Calendar.getInstance().getTimeInMillis();

            if (isInterrupted()) {
                ThreadUtil.invokeLater(miceProfilerWindow::enableAfterTrackAllStopped);
                return;
            }

            videoManager.setFrame(t); // image should be updated in viewer here.
            //System.out.println("Trackall: slider updated, wait for image to be loaded.");
            IcyBufferedImage imageSource = sequenceWindow.getSequence().getImage(t, 0);
            //Wait for bufferization from ImageBufferThread
            while (imageSource == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (isInterrupted()) {
                    ThreadUtil.invokeLater(miceProfilerWindow::enableAfterTrackAllStopped);
                    return;
                }

                imageSource = sequenceWindow.getSequence().getImage(t, 0);
            }

            // use the record to initialize the head position of the mouse.
            //System.out.println("Trackall: use record.");
            //phyMouseManager.setHeadLocation(0, miceProfilerTracker.getManualHelper1().getControlPoint(t));
            //phyMouseManager.setHeadLocation(1, miceProfilerTracker.getManualHelper2().getControlPoint(t));
            PhyMouse phyMouse = phyMouseManager.getPhyMouse();
            phyMouse.generateMap(imageSource);

            if (isInterrupted()) {
                ThreadUtil.invokeLater(miceProfilerWindow::enableAfterTrackAllStopped);
                return;
            }

            for (int i = 0; i < ITERATION; i++) {
                //System.out.println("Trackall: compute and worldstep, iteration " + i + ".");
                if ((i > 3) && (phyMouse.isStable())) {
                    break;
                }

                phyMouse.computeForces();
                phyMouse.worldStep(t);

                if (isInterrupted()) {
                    ThreadUtil.invokeLater(miceProfilerWindow::enableAfterTrackAllStopped);
                    return;
                }
            }

            //System.out.println("Trackall: applyMotionPrediction.");
            phyMouse.applyMotionPrediction();

            //System.out.println("Trackall: update painter.");
            updateMouseGuidePainter();

            double totalImageMs = (Calendar.getInstance().getTimeInMillis() - totalImageTimerStart);
            miceProfilerWindow.setTotalImageTimeText("total image time: " + totalImageMs + " ms / FPS: " + (((int) (10 * 1000d / totalImageMs)) / 10d));

            if (miceProfilerWindow.limitTrackingSpeed()) {
                try {
                    Thread.sleep((int) (1000d / 15d));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        // System.out.print("Ok.");

        miceProfilerWindow.enableAfterTrackAllStopped();
    }

    private void updateMouseGuidePainter() {
        mouseGuidesPainter.setVisible(miceProfilerWindow.updatePhysicsGuides());

        PhyMouse phyMouse = phyMouseManager.getPhyMouse();

        mouseGuidesPainter.getMouseAGuide().getHead().moveTo((int) phyMouse.getMouseA().getHead().getPosition().getX(), (int) phyMouse.getMouseA().getHead().getPosition().getY());
        mouseGuidesPainter.getMouseAGuide().getBottom().moveTo((int) phyMouse.getMouseA().getTail().getPosition().getX(), (int) phyMouse.getMouseA().getTail().getPosition().getY());

        mouseGuidesPainter.getMouseBGuide().getHead().moveTo((int) phyMouse.getMouseB().getHead().getPosition().getX(), (int) phyMouse.getMouseB().getHead().getPosition().getY());
        mouseGuidesPainter.getMouseBGuide().getBottom().moveTo((int) phyMouse.getMouseB().getTail().getPosition().getX(), (int) phyMouse.getMouseB().getTail().getPosition().getY());
    }
}
