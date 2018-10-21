package plugins.fab.MiceProfiler;

import icy.system.thread.ThreadUtil;

/**
 * centre l image active sur le currentFrame avec une valeur encadrante. Enleve les images inutiles
 *
 * @author Administrator
 */
class ImageBufferThread extends Thread {
    private final MiceProfilerTracker miceProfilerTracker;
    private boolean pleaseStop;
    private boolean bufferOn;
    private final int numberOfImageForBuffer;

    public ImageBufferThread(MiceProfilerTracker miceProfilerTracker) {
        this.miceProfilerTracker = miceProfilerTracker;
        this.pleaseStop = false;
        this.bufferOn = true;
        this.numberOfImageForBuffer = miceProfilerTracker.getNumberOfImageForBuffer();
    }

    @Override
    public void run() {
        try {
            while (!pleaseStop) {
                ThreadUtil.sleep(100);

                if (bufferOn) {
                    int cachedCurrentFrame = miceProfilerTracker.getCurrentFrame();
                    int frameStart = miceProfilerTracker.getCurrentFrame() - 10;
                    int frameEnd = miceProfilerTracker.getCurrentFrame() + numberOfImageForBuffer;

                    if (frameStart < 0)
                        frameStart = 0;
                    if (frameEnd > miceProfilerTracker.getTotalNumberOfFrame())
                        frameEnd = (int) miceProfilerTracker.getTotalNumberOfFrame();

                    // enleve les images hors numberOfImageForBuffer (except la dernire)
                    for (int t = 0; t < miceProfilerTracker.sequenceOut.getSizeT() - 1; t++) {
                        if (Math.abs(t - miceProfilerTracker.getCurrentFrame()) > numberOfImageForBuffer + 10) {
                            // current frame changed --> interrupt
                            if (cachedCurrentFrame != miceProfilerTracker.getCurrentFrame())
                                break;
                            if (pleaseStop)
                                return;

                            miceProfilerTracker.sequenceOut.removeImage(t, 0);
                        }
                    }

                    for (int t = frameStart; t < frameEnd; t++) {
                        if (miceProfilerTracker.getImageAt(t) == null)
                            miceProfilerTracker.sequenceOut.setImage(t, 0, aviFile.getImage(t));

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

    public synchronized void setPleaseStop(boolean pleaseStop) {
        this.pleaseStop = pleaseStop;
    }
}
