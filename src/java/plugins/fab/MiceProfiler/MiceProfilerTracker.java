/*
 * Copyright 2011, 2012 Institut Pasteur.
 *
 * This file is part of MiceProfiler.
 *
 * MiceProfiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MiceProfiler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MiceProfiler. If not, see <http://www.gnu.org/licenses/>.
 */
package plugins.fab.MiceProfiler;

import icy.plugin.abstract_.PluginActionable;
import plugins.fab.MiceProfiler.controller.*;
import plugins.fab.MiceProfiler.view.MiceProfilerWindow;
import plugins.fab.MiceProfiler.view.SequenceWindow;


/**
 * @author Fabrice de Chaumont
 */
public class MiceProfilerTracker extends PluginActionable {

    //interface PluginActionable
    @Override
    public void run() {
        MiceProfilerWindow miceProfilerWindow = new MiceProfilerWindow();
        SequenceWindow sequenceWindow = new SequenceWindow();

        SliderChangeManager sliderChangeManager = new SliderChangeManager();

        VideoManager videoManager = new VideoManager(miceProfilerWindow, sequenceWindow, sliderChangeManager);
        PhyMouseManager phyMouseManager = new PhyMouseManager();
        MiceProfilerActionListener miceProfilerActionListener = new MiceProfilerActionListener(videoManager, phyMouseManager, miceProfilerWindow, sequenceWindow);
        MiceProfilerChangeListener miceProfilerChangeListener = new MiceProfilerChangeListener(videoManager, miceProfilerWindow, sequenceWindow, sliderChangeManager);
        SequenceOverlay sequenceOverlay = new SequenceOverlay(videoManager, phyMouseManager, miceProfilerWindow);
        SequenceViewerListener sequenceViewerListener = new SequenceViewerListener(miceProfilerWindow);

        sliderChangeManager.ignoreChange.set(true);
        miceProfilerWindow.initialize(miceProfilerActionListener, miceProfilerChangeListener);
        sequenceWindow.initialize(this, sequenceOverlay, sequenceViewerListener);

        // Start Physics Engines
        System.out.println("----------");
        System.out.println("Mice Profiler / Fab / Version 7sssss");
        System.out.println("Red mice: occupante /// Green: visiteur");
    }
}
