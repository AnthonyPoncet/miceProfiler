package plugins.fab.MiceProfiler;

import javax.swing.*;

import icy.image.IcyBufferedImage;

import icy.painter.Overlay;
import icy.sequence.Sequence;


public class StepThread extends Thread {

    private final Sequence sequence;
    private final PhyMouse phyMouse;
    private final JSlider sliderTime;

    public StepThread(Sequence sequence, PhyMouse phyMouse, JSlider sliderTime) {
        this.sequence = sequence;
        this.phyMouse = phyMouse;
        this.sliderTime = sliderTime;
    }

    @Override
    public void run() {
        while (true) {
            IcyBufferedImage imageSource = sequence.getImage(sliderTime.getValue(), 0);
            //Wait for ImageBufferThread to load data
            while (imageSource == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // test arret
                if (isInterrupted()) {
                    return;
                }

                imageSource = sequence.getImage(sliderTime.getValue(), 0);
            }

            synchronized (phyMouse) {
                phyMouse.generateMap(imageSource);
                phyMouse.computeForces();
                phyMouse.worldStep(sliderTime.getValue());
            }

            sequence.overlayChanged((Overlay)null);
        }
    }
}
