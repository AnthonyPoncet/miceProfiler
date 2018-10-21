package plugins.fab.MiceProfiler;

import icy.image.IcyBufferedImage;
import icy.system.thread.ThreadUtil;

import java.util.Calendar;

class TrackAllThread extends Thread {
    private final MiceProfilerTracker miceProfilerTracker;
    private boolean shouldRun;

    public TrackAllThread(MiceProfilerTracker miceProfilerTracker) {
        this.miceProfilerTracker = miceProfilerTracker;
        this.shouldRun = true;
    }

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
            phyMouse.setHeadLocation(0, manualHelperA.getControlPoint(miceProfilerTracker.getCurrentFrame()));
            phyMouse.setHeadLocation(1, manualHelperB.getControlPoint(miceProfilerTracker.getCurrentFrame()));
            phyMouse.computeForcesMap(imageSource);

            for (int i = 0; i < ITERATION; i++) {
                if (useTotalSystemEnergyStopConditionBox.isSelected()) {
                    if (i > 3) {
                        if (phyMouse.isStable()) {
                            // System.out.println("iteration : " + i );
                            break;
                        }
                    }
                }

                phyMouse.computeForces();
                phyMouse.worldStep(t);
            }

            phyMouse.applyMotionPrediction();

            updateMouseGuidePainter();

            double totalImageMs = (Calendar.getInstance().getTimeInMillis() - totalImageTimerStart);
            miceProfilerTracker.totalImageTime.setText("total image time: " + totalImageMs + " ms / FPS: " + ((int) (10 * 1000d / totalImageMs)) / 10d);

            if (miceProfilerTracker.limitTrackingSpeedCheckBox.isSelected()) {
                try {
                    Thread.sleep( (int)(1000d / 15d) );
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        // System.out.print("Ok.");

        miceProfilerTracker.deactivateTrackAll();
    }

    private void updateMouseGuidePainter() {
        mouseGuidePainter.setVisible(miceProfilerTracker.updatePhysicsGuidesCheckBox.isSelected());

        mouseGuidePainter.getM1h().moveTo(
                (int) phyMouse.getMouseList().get(0).getHeadBody().getPosition().getX(),
                (int) phyMouse.getMouseList().get(0).getHeadBody().getPosition().getY());
        mouseGuidePainter.getM1b().moveTo(
                (int) phyMouse.getMouseList().get(0).getTail().getPosition().getX(),
                (int) phyMouse.getMouseList().get(0).getTail().getPosition().getY());

        if (phyMouse.getMouseList().size() > 1) {
            mouseGuidePainter.getM2b().moveTo(
                    (int) phyMouse.getMouseList().get(1).getHeadBody().getPosition().getX(),
                    (int) phyMouse.getMouseList().get(1).getHeadBody().getPosition().getY());
            mouseGuidePainter.getM2b().moveTo(
                    (int) phyMouse.getMouseList().get(1).getTail().getPosition().getX(),
                    (int) phyMouse.getMouseList().get(1).getTail().getPosition().getY());
        }

    }
}
