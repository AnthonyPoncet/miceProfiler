package plugins.fab.MiceProfiler;

import icy.system.thread.ThreadUtil;


/**
 * Set the active image at the center of the currentFrame with an "encadrante" value. Remove useless images.
 *
 * @author Administrator
 */
class ImageBufferThread extends Thread {

    private final MiceProfilerTracker miceProfilerTracker;
    private boolean pleaseStop;
    private boolean bufferOn;

    private final int numberOfImageForBuffer;
    private final int totalNumberOfImage;

    public ImageBufferThread(MiceProfilerTracker miceProfilerTracker) {
        this.miceProfilerTracker = miceProfilerTracker;
        this.pleaseStop = false;
        this.bufferOn = true;
        this.numberOfImageForBuffer = miceProfilerTracker.getNumberOfImageForBuffer();
        this.totalNumberOfImage = miceProfilerTracker.getTotalNumberOfFrame();
    }

    @Override
    public void run() {
        try {
            while (!pleaseStop) {
                ThreadUtil.sleep(100);

                if (bufferOn) {
                    int cachedCurrentFrame = miceProfilerTracker.getCurrentFrame();
                    int frameStart = Math.min(cachedCurrentFrame - 10, 0);
                    int frameEnd = Math.max(cachedCurrentFrame + numberOfImageForBuffer, totalNumberOfImage);

                    //Remove image from sequenceOut that are outside numberOfImageForBuffer (except the last one)
                    for (int t = 0; t < (miceProfilerTracker.getSequenceOut().getSizeT() - 1); t++) {
                        if (Math.abs(t - miceProfilerTracker.getCurrentFrame()) > (numberOfImageForBuffer + 10)) {
                            // current frame changed --> interrupt
                            if (cachedCurrentFrame != miceProfilerTracker.getCurrentFrame())
                                break;
                            if (pleaseStop)
                                return;

                            miceProfilerTracker.getSequenceOut().removeImage(t, 0);
                        }
                    }

                    for (int t = frameStart; t < frameEnd; t++) {
                        if (miceProfilerTracker.getImageAt(t) == null)
                            miceProfilerTracker.getSequenceOut().setImage(t, 0, miceProfilerTracker.getAviFile().getImage(t));

                        // current frame changed --> interrupt
                        if (cachedCurrentFrame != miceProfilerTracker.getCurrentFrame())
                            break;
                        if (pleaseStop)
                            return;
                    }
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void pleaseStop() {
        this.pleaseStop = true;
    }
}
