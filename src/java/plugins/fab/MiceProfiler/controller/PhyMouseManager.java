package plugins.fab.MiceProfiler.controller;

import plugins.fab.MiceProfiler.PhyMouse;

import java.io.File;

public class PhyMouseManager {

    private PhyMouse phyMouse;

    public void reset(File videoFile) {
        phyMouse = new PhyMouse();
        phyMouse.initializeMouseRecordsFromXML(videoFile);
    }

    public PhyMouse getPhyMouse() {
        return phyMouse;
    }
}
