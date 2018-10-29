package plugins.fab.MiceProfiler;

import java.util.Calendar;

import icy.image.IcyBufferedImage;

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
    private final PhyMouse phyMouse;
    private final MouseGuidePainter mouseGuidePainter;
    private boolean shouldRun;

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Constructors
    //~ ----------------------------------------------------------------------------------------------------------------

    public TrackAllThread(MiceProfilerTracker miceProfilerTracker) {
        this.miceProfilerTracker = miceProfilerTracker;
        this.phyMouse = miceProfilerTracker.getPhyMouse();
        this.mouseGuidePainter = miceProfilerTracker.getMouseGuidePainter();
        this.shouldRun = true;
    }

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Methods
    //~ ----------------------------------------------------------------------------------------------------------------

    @Override
    public void run() {
        for (int t = miceProfilerTracker.getCurrentFrame(); t < miceProfilerTracker.getTotalNumberOfFrame(); t++) {
            double totalImageTimerStart = Calendar.getInstance().getTimeInMillis();

            if (!shouldRun) {
                ThreadUtil.invokeLater(miceProfilerTracker::deactivateTrackAll);
                return;
            }

            miceProfilerTracker.setSliderTimeValue(t); // image should be updated in viewer here.

            IcyBufferedImage imageSourceR = miceProfilerTracker.getImageAt(t);

            while (imageSourceR == null) {
                imageSourceR = miceProfilerTracker.getImageAt(t);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // test arret
                if (!shouldRun) {
                    miceProfilerTracker.deactivateTrackAll();
                    return;
                }
            }

            // Convert input image.
            IcyBufferedImage imageSource = imageSourceR;

            // use the record to initialize the head position of the mouse.
            phyMouse.setHeadLocation(0, miceProfilerTracker.getManualHelperA().getControlPoint(miceProfilerTracker.getCurrentFrame()));
            phyMouse.setHeadLocation(1, miceProfilerTracker.getManualHelperB().getControlPoint(miceProfilerTracker.getCurrentFrame()));
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

    public boolean isShouldRun() {
        return shouldRun;
    }

    public void stopShouldRun() {
        shouldRun = false;
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
