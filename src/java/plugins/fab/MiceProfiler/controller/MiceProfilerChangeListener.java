package plugins.fab.MiceProfiler.controller;


import plugins.fab.MiceProfiler.view.MiceProfilerWindow;
import plugins.fab.MiceProfiler.view.SequenceWindow;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class MiceProfilerChangeListener implements ChangeListener {
    private final VideoManager videoManager;
    private final MiceProfilerWindow miceProfilerWindow;
    private final SequenceWindow sequenceWindow;

    private final SliderChangeManager sliderChangeManager;

    public MiceProfilerChangeListener(VideoManager videoManager, MiceProfilerWindow miceProfilerWindow, SequenceWindow sequenceWindow, SliderChangeManager sliderChangeManager) {
        this.videoManager = videoManager;
        this.miceProfilerWindow = miceProfilerWindow;
        this.sequenceWindow = sequenceWindow;
        this.sliderChangeManager = sliderChangeManager;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (miceProfilerWindow.hasBeenTriggeredBySliderTime(e.getSource())) {
            if (sliderChangeManager.ignoreChange.get()) {
                sliderChangeManager.ignoreChange.set(false);
                return;
            }

            System.out.println("Slider Update!! ");
            videoManager.manageVideoBufferization();
            sequenceWindow.displayImageAt(miceProfilerWindow.getCurrentFrame(), videoManager.getVideo());

            /*TODO: this code should go somewhere else
            if (trackAllButton.isEnabled()) { // should be something better...
                // set mouseA
                {
                    MouseInfoRecord record = phyMouse.getMouseARecords().get(sliderTime.getValue());
                    if (record != null) {
                        mouseGuidePainter.getMouseAHead().setPosition(record.getHeadPosition());
                        mouseGuidePainter.getMouseABottom().setPosition(record.getTailPosition());
                    }
                }

                // set mouseB
                {
                    MouseInfoRecord record = phyMouse.getMouseBRecords().get(sliderTime.getValue());
                    if (record != null) {
                        mouseGuidePainter.getMouseBHead().setPosition(record.getHeadPosition());
                        mouseGuidePainter.getMouseBBottom().setPosition(record.getTailPosition());
                    }
                }
            }*/
        }
    }
}
