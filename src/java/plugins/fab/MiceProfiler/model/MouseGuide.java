package plugins.fab.MiceProfiler.model;

import icy.painter.Anchor2D;

public class MouseGuide {
    private final Anchor2D head;
    private final Anchor2D bottom;

    public MouseGuide(Anchor2D head, Anchor2D bottom) {
        this.head = head;
        this.bottom = bottom;
    }

    public Anchor2D getHead() {
        return head;
    }

    public Anchor2D getBottom() {
        return bottom;
    }

    public double getAlpha() {
        return Math.atan2(head.getY() - bottom.getY(), head.getX() - bottom.getX());
    }
}
