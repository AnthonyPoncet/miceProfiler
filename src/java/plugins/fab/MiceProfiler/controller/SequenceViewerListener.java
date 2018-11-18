package plugins.fab.MiceProfiler.controller;

import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerListener;

import icy.sequence.DimensionId;

import plugins.fab.MiceProfiler.view.MiceProfilerWindow;


public class SequenceViewerListener implements ViewerListener {

    private final MiceProfilerWindow miceProfilerWindow;

    public SequenceViewerListener(MiceProfilerWindow miceProfilerWindow) {
        this.miceProfilerWindow = miceProfilerWindow;
    }

    //TODO: check if it is not redundant. Changing slider modify frame display and not the other way around.
    @Override
    public void viewerChanged(ViewerEvent event) {
        /*if ((event.getType() == ViewerEvent.ViewerEventType.POSITION_CHANGED) && (event.getDim() == DimensionId.T)) {
            miceProfilerWindow.setSliderTimeValue(event.getSource().getPositionT());
        }*/
    }

    @Override
    public void viewerClosed(Viewer viewer) {
        // ignore
    }
}
