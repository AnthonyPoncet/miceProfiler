package plugins.fab.MiceProfiler;

import icy.image.IcyBufferedImage;


class StepThread extends Thread {

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Instance fields
    //~ ----------------------------------------------------------------------------------------------------------------

    private final MiceProfilerTracker miceProfilerTracker;
    private final PhyMouse phyMouse;
    boolean shouldRun = true;

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Constructors
    //~ ----------------------------------------------------------------------------------------------------------------

    public StepThread(MiceProfilerTracker miceProfilerTracker) {
        this.miceProfilerTracker = miceProfilerTracker;
        this.phyMouse = miceProfilerTracker.getPhyMouse();
    }

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Methods
    //~ ----------------------------------------------------------------------------------------------------------------

    @Override
    public void run() {
        while (shouldRun) {
            IcyBufferedImage imageSourceR = null;
            while (imageSourceR == null) {
                imageSourceR = miceProfilerTracker.getImageAt(miceProfilerTracker.getCurrentFrame());
            }

            IcyBufferedImage imageSource = imageSourceR; // assume good format.

            synchronized (phyMouse) {
                phyMouse.computeForcesMap(imageSource);
                phyMouse.computeForces();
                phyMouse.worldStep(miceProfilerTracker.getCurrentFrame());
            }

            miceProfilerTracker.getSequenceOut().painterChanged(null);
        }
    }
}
