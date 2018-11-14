package plugins.fab.MiceProfiler;

import java.util.Calendar;

import icy.image.IcyBufferedImage;

import icy.sequence.Sequence;

import icy.system.thread.ThreadUtil;

import plugins.fab.MiceProfiler.view.MiceProfilerWindow;


public class TrackAllThread extends Thread {

    private static final int ITERATION = 50;

    private final MiceProfilerWindow miceProfilerWindow;
    private final Sequence sequence;
    private final PhyMouse phyMouse;
    private final MouseGuidePainter mouseGuidePainter;

    private final int startingFrame;
    private final int totalNumberOfImage;

    public TrackAllThread(MiceProfilerWindow miceProfilerWindow, Sequence sequence, PhyMouse phyMouse, MouseGuidePainter mouseGuidePainter, int startingFrame, int totalNumberOfImage) {
        this.miceProfilerWindow = miceProfilerWindow;
        this.sequence = sequence;
        this.phyMouse = phyMouse;
        this.mouseGuidePainter = mouseGuidePainter;

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
                ThreadUtil.invokeLater(miceProfilerWindow::deactivateTrackAll);
                return;
            }

            miceProfilerWindow.setSliderTimeValue(t); // image should be updated in viewer here.
            //System.out.println("Trackall: slider updated, wait for image to be loaded.");
            IcyBufferedImage imageSource = sequence.getImage(t, 0);
            //Wait for bufferization from ImageBufferThread
            while (imageSource == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (isInterrupted()) {
                    ThreadUtil.invokeLater(miceProfilerWindow::deactivateTrackAll);
                    return;
                }

                imageSource = sequence.getImage(t, 0);
            }

            // use the record to initialize the head position of the mouse.
            //System.out.println("Trackall: use record.");
            //phyMouse.setHeadLocation(0, miceProfilerTracker.getManualHelper1().getControlPoint(t));
            //phyMouse.setHeadLocation(1, miceProfilerTracker.getManualHelper2().getControlPoint(t));
            phyMouse.computeForcesMap(imageSource);

            if (isInterrupted()) {
                ThreadUtil.invokeLater(miceProfilerWindow::deactivateTrackAll);
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
                    ThreadUtil.invokeLater(miceProfilerWindow::deactivateTrackAll);
                    return;
                }
            }

            //System.out.println("Trackall: applyMotionPrediction.");
            phyMouse.applyMotionPrediction();

            //System.out.println("Trackall: update painter.");
            updateMouseGuidePainter();

            double totalImageMs = (Calendar.getInstance().getTimeInMillis() - totalImageTimerStart);
            miceProfilerWindow.setTotalImageTimeText("total image time: " + totalImageMs + " ms / FPS: " + (((int) (10 * 1000d / totalImageMs)) / 10d));

            if (miceProfilerWindow.isLimitTrackingSpeedCheckBoxChecked()) {
                try {
                    Thread.sleep((int) (1000d / 15d));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        // System.out.print("Ok.");

        miceProfilerWindow.deactivateTrackAll();
    }

    private void updateMouseGuidePainter() {
        mouseGuidePainter.setVisible(miceProfilerWindow.isUpdatePhysicsGuidesCheckBoxChecked());

        mouseGuidePainter.getM1h().moveTo((int) phyMouse.getMouseList().get(0).getHead().getPosition().getX(), (int) phyMouse.getMouseList().get(0).getHead().getPosition().getY());
        mouseGuidePainter.getM1b().moveTo((int) phyMouse.getMouseList().get(0).getTail().getPosition().getX(), (int) phyMouse.getMouseList().get(0).getTail().getPosition().getY());

        if (phyMouse.getMouseList().size() > 1) {
            mouseGuidePainter.getM2b().moveTo((int) phyMouse.getMouseList().get(1).getHead().getPosition().getX(), (int) phyMouse.getMouseList().get(1).getHead().getPosition().getY());
            mouseGuidePainter.getM2b().moveTo((int) phyMouse.getMouseList().get(1).getTail().getPosition().getX(), (int) phyMouse.getMouseList().get(1).getTail().getPosition().getY());
        }

    }
}
