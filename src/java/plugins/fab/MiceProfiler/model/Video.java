package plugins.fab.MiceProfiler.model;

import java.awt.image.BufferedImage;

import java.io.File;

import plugins.fab.MiceProfiler.XugglerAviFile;


public class Video {

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Instance fields
    //~ ----------------------------------------------------------------------------------------------------------------

    private final XugglerAviFile aviFile;
    private final File currentFile;

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Constructors
    //~ ----------------------------------------------------------------------------------------------------------------

    public Video(XugglerAviFile aviFile, File currentFile) {
        this.aviFile = aviFile;
        this.currentFile = currentFile;
    }

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Methods
    //~ ----------------------------------------------------------------------------------------------------------------

    public XugglerAviFile getAviFile() {
        return aviFile;
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public String getVideoName() {
        return currentFile.getName();
    }

    public BufferedImage getImage(int frame) {
        return aviFile.getImage(frame);
    }

    public long getTotalNumberOfFrame() {
        return aviFile.getTotalNumberOfFrame();
    }

    public String getTimeForFrame(int frame) {
        return aviFile.getTimeForFrame(frame);
    }
}
