package plugins.fab.MiceProfiler;

import icy.image.IcyBufferedImage;

class StepThread extends Thread {
    private MiceProfilerTracker miceProfilerTracker;
    boolean shouldRun = true;

    public StepThread(MiceProfilerTracker miceProfilerTracker) {
        this.miceProfilerTracker = miceProfilerTracker;
    }

    @Override
    public void run() {
        while (shouldRun) {
            IcyBufferedImage imageSourceR = null;
            while (imageSourceR == null) {
                imageSourceR = miceProfilerTracker.getImageAt(miceProfilerTracker.getCurrentFrame());
            }

            IcyBufferedImage imageSource = imageSourceR; // assume good format.

            phyMouse.computeForcesMap(imageSource);
            synchronized (phyMouse)
            {
                phyMouse.computeForces();
                phyMouse.worldStep(miceProfilerTracker.getCurrentFrame());
            }

            miceProfilerTracker.sequenceOut.painterChanged(null);
        }
    }
}
