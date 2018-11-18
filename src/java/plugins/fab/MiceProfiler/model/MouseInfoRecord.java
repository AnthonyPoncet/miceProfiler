package plugins.fab.MiceProfiler.model;

import java.awt.geom.Point2D;


public class MouseInfoRecord {

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Instance fields
    //~ ----------------------------------------------------------------------------------------------------------------

    private final Point2D headPosition;
    private final Point2D neckPosition; // body that links to the head to compute head angle.
    private final Point2D bodyPosition;
    private final Point2D tailPosition;

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Constructors
    //~ ----------------------------------------------------------------------------------------------------------------

    public MouseInfoRecord(Point2D headPosition, Point2D tailPosition, Point2D bodyPosition, Point2D neckPosition) {
        this.headPosition = headPosition;
        this.neckPosition = neckPosition;
        this.bodyPosition = bodyPosition;
        this.tailPosition = tailPosition;
    }

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Methods
    //~ ----------------------------------------------------------------------------------------------------------------

    public Point2D getHeadPosition() {
        return headPosition;
    }

    public Point2D getNeckPosition() {
        return neckPosition;
    }

    public Point2D getBodyPosition() {
        return bodyPosition;
    }

    public Point2D getTailPosition() {
        return tailPosition;
    }
}
