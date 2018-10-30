package plugins.fab.MiceProfiler;

import java.util.Calendar;

import icy.image.IcyBufferedImage;

import icy.sequence.Sequence;

import icy.system.thread.ThreadUtil;


class TrackAllThread extends Thread {

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Static fields/initializers
    //~ ----------------------------------------------------------------------------------------------------------------

    private static final int ITERATION = 50;

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Instance fields
    //~ ----------------------------------------------------------------------------------------------------------------

    private final MiceProfilerTracker miceProfilerTracker;
    private final Sequence sequence;
    private final PhyMouse phyMouse;
    private final MouseGuidePainter mouseGuidePainter;

    private final int startingFrame;
    private final int totalNumberOfImage;

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Constructors
    //~ ----------------------------------------------------------------------------------------------------------------

    public TrackAllThread(MiceProfilerTracker miceProfilerTracker, Sequence sequence, PhyMouse phyMouse, MouseGuidePainter mouseGuidePainter, int startingFrame, int totalNumberOfImage) {
        this.miceProfilerTracker = miceProfilerTracker;
        this.sequence = sequence;
        this.phyMouse = phyMouse;
        this.mouseGuidePainter = mouseGuidePainter;

        this.startingFrame = startingFrame;
        this.totalNumberOfImage = totalNumberOfImage;
    }

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Methods
    //~ ----------------------------------------------------------------------------------------------------------------

    @Override
    public void run() {
        for (int t = startingFrame; t < totalNumberOfImage; t++) {
            double totalImageTimerStart = Calendar.getInstance().getTimeInMillis();

            if (isInterrupted()) {
                ThreadUtil.invokeLater(miceProfilerTracker::deactivateTrackAll);
                return;
            }

            miceProfilerTracker.setSliderTimeValue(t); // image should be updated in viewer here.

            IcyBufferedImage imageSource = sequence.getImage(t, 0);
            //Wait for bufferization from ImageBufferThread
            while (imageSource == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // test arret
                if (isInterrupted()) {
                    ThreadUtil.invokeLater(miceProfilerTracker::deactivateTrackAll);
                    return;
                }

                imageSource = sequence.getImage(t, 0);
            }

            // use the record to initialize the head position of the mouse.
            phyMouse.setHeadLocation(0, miceProfilerTracker.getManualHelperA().getControlPoint(t));
            phyMouse.setHeadLocation(1, miceProfilerTracker.getManualHelperB().getControlPoint(t));
            phyMouse.computeForcesMap(imageSource);

            for (int i = 0; i < ITERATION; i++) {
                phyMouse.computeForces();
                phyMouse.worldStep(t);
            }

            phyMouse.applyMotionPrediction();

            updateMouseGuidePainter();

            double totalImageMs = (Calendar.getInstance().getTimeInMillis() - totalImageTimerStart);
            miceProfilerTracker.setTotalImageTimeText("total image time: " + totalImageMs + " ms / FPS: " + (((int) (10 * 1000d / totalImageMs)) / 10d));

            if (miceProfilerTracker.isLimitTrackingSpeedCheckBoxChecked()) {
                try {
                    Thread.sleep((int) (1000d / 15d));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        // System.out.print("Ok.");

        miceProfilerTracker.deactivateTrackAll();
    }

    private void updateMouseGuidePainter() {
        mouseGuidePainter.setVisible(miceProfilerTracker.isUpdatePhysicsGuidesCheckBoxChecked());

        mouseGuidePainter.getM1h().moveTo((int) phyMouse.getMouseList().get(0).getHead().getPosition().getX(), (int) phyMouse.getMouseList().get(0).getHead().getPosition().getY());
        mouseGuidePainter.getM1b().moveTo((int) phyMouse.getMouseList().get(0).getTail().getPosition().getX(), (int) phyMouse.getMouseList().get(0).getTail().getPosition().getY());

        if (phyMouse.getMouseList().size() > 1) {
            mouseGuidePainter.getM2b().moveTo((int) phyMouse.getMouseList().get(1).getHead().getPosition().getX(), (int) phyMouse.getMouseList().get(1).getHead().getPosition().getY());
            mouseGuidePainter.getM2b().moveTo((int) phyMouse.getMouseList().get(1).getTail().getPosition().getX(), (int) phyMouse.getMouseList().get(1).getTail().getPosition().getY());
        }

    }
}
