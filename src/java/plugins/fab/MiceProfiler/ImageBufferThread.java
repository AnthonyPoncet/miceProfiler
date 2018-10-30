/**
 *  Copyright Murex S.A.S., 2003-2018. All Rights Reserved.
 *
 *  This software program is proprietary and confidential to Murex S.A.S and its affiliates ("Murex") and, without limiting the generality of the foregoing reservation of rights, shall not be accessed, used, reproduced or distributed without the
 *  express prior written consent of Murex and subject to the applicable Murex licensing terms. Any modification or removal of this copyright notice is expressly prohibited.
 */
package plugins.fab.MiceProfiler;

import icy.sequence.Sequence;


/**
 * Load numberOfFrameForBuffer frame into Sequence (buffer) from aviFile. First frame being currentTimeSliderValue - 10
 * Last frame being currentTimeSliderValue + numberOfFrameForBuffer (or totalNumberOfFrame if overflow)
 */
class ImageBufferThread extends Thread {

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Instance fields
    //~ ----------------------------------------------------------------------------------------------------------------

    private final int currentTimeSliderValue;
    private final int numberOfFrameForBuffer;
    private final int totalNumberOfFrame;

    private final Sequence sequence;
    private final XugglerAviFile aviFile;

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Constructors
    //~ ----------------------------------------------------------------------------------------------------------------

    public ImageBufferThread(int currentTimeSliderValue, int numberOfFrameForBuffer, int totalNumberOfFrame, Sequence sequence, XugglerAviFile aviFile) {
        this.currentTimeSliderValue = currentTimeSliderValue;
        this.numberOfFrameForBuffer = numberOfFrameForBuffer;
        this.totalNumberOfFrame = totalNumberOfFrame;

        this.sequence = sequence;
        this.aviFile = aviFile;
    }

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Methods
    //~ ----------------------------------------------------------------------------------------------------------------

    @Override
    public void run() {
        try {
            int frameStart = getFirstFrame();
            int frameEnd = getLastFrame();

            System.out.println("ImageBufferThread: start, window --> " + frameStart + " to " + frameEnd);

            //Remove buffered image from sequence that are outside [frameStart;frameEnd]
            for (int time = 0; time < sequence.getSizeT(); time++) {
                if ((time < frameStart) || (time > frameEnd)) {
                    sequence.removeImage(time, 0);
                }

                if (isInterrupted()) {
                    System.out.println("ImageBufferThread: Interrupted!");
                    return;
                }
            }

            System.out.println("ImageBufferThread: Remove buffered image --> done");

            //Add missing buffered image [frameStart;frameEnd]
            for (int time = frameStart; time < frameEnd; time++) {
                if (sequence.getImage(time, 0) == null)
                    sequence.setImage(time, 0, aviFile.getImage(time));

                if (isInterrupted()) {
                    System.out.println("ImageBufferThread: Interrupted!");
                    return;
                }
            }

            System.out.println("ImageBufferThread: Add missing buffered image --> done");
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public int getCurrentBufferLoadPercent() {
        int frameStart = getFirstFrame();
        int frameEnd = getLastFrame();
        float nbImage = frameEnd - frameStart;
        float nbImageLoaded = 0;

        for (int t = frameStart; t < frameEnd; t++) {
            if (sequence.getImage(t, 0) != null)
                nbImageLoaded++;
        }

        return (int) (nbImageLoaded * 100f / nbImage);
    }

    private int getLastFrame() {
        return Math.min(currentTimeSliderValue + numberOfFrameForBuffer, totalNumberOfFrame);
    }

    private int getFirstFrame() {
        return Math.max(currentTimeSliderValue - 10, 0); //Load 10 more frame before requested value
    }
}
