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
package plugins.fab.MiceProfiler.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;

import icy.canvas.IcyCanvas;

import icy.painter.Anchor2D;
import icy.painter.Anchor2D.Anchor2DPositionListener;
import icy.painter.Overlay;

import icy.sequence.Sequence;

import icy.type.point.Point5D;
import plugins.fab.MiceProfiler.model.MouseGuide;


/**
 * Display mouse location editor
 *
 * @author Fabrice de Chaumont
 */
public class MouseGuidesPainter extends Overlay implements Anchor2DPositionListener {
    private final MouseGuide mouseAGuide;
    private final MouseGuide mouseBGuide;

    private boolean visible;

    public MouseGuidesPainter() {
        super("Mouse Guide", OverlayPriority.SHAPE_NORMAL);

        Anchor2D mouseAHead = new Anchor2D(100, 100);
        Anchor2D mouseABottom = new Anchor2D(100, 200);
        mouseAHead.addPositionListener(this);
        mouseABottom.addPositionListener(this);
        this.mouseAGuide = new MouseGuide(mouseAHead, mouseABottom);

        Anchor2D mouseBHead = new Anchor2D(200, 100);
        Anchor2D mouseBBottom = new Anchor2D(200, 200);
        mouseBHead.addPositionListener(this);
        mouseBBottom.addPositionListener(this);
        this.mouseBGuide = new MouseGuide(mouseBHead, mouseBBottom);

        this.visible = true;
    }

    public MouseGuide getMouseAGuide() {
        return mouseAGuide;
    }

    public MouseGuide getMouseBGuide() {
        return mouseBGuide;
    }

    /**
     * @param visible the visible to set
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void mousePressed(MouseEvent e, Point5D.Double imagePoint, IcyCanvas canvas) {
        if (mouseAGuide.getHead().isSelected() || mouseAGuide.getBottom().isSelected() || mouseBGuide.getHead().isSelected() || mouseBGuide.getBottom().isSelected())
            e.consume();
    }

    @Override
    public void mouseDrag(MouseEvent e, Point5D.Double imagePoint, IcyCanvas canvas) {
        if (!visible)
            return;

        // forward event
        mouseAGuide.getHead().mouseDrag(e, imagePoint, canvas);
        mouseAGuide.getBottom().mouseDrag(e, imagePoint, canvas);
        mouseBGuide.getHead().mouseDrag(e, imagePoint, canvas);
        mouseBGuide.getBottom().mouseDrag(e, imagePoint, canvas);
    }

    @Override
    public void mouseMove(MouseEvent e, Point5D.Double imagePoint, IcyCanvas canvas) {
        if (!visible)
            return;

        // forward event
        mouseAGuide.getHead().mouseMove(e, imagePoint, canvas);
        mouseAGuide.getBottom().mouseMove(e, imagePoint, canvas);
        mouseBGuide.getHead().mouseMove(e, imagePoint, canvas);
        mouseBGuide.getBottom().mouseMove(e, imagePoint, canvas);
    }

    @Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
        if ((g == null) || (sequence == null))
            return;

        if (!visible)
            return;

        g.setColor(Color.RED);
        Line2D line1 = new Line2D.Double(mouseAGuide.getHead().getX(), mouseAGuide.getHead().getY(), mouseAGuide.getBottom().getX(), mouseAGuide.getBottom().getY());
        g.draw(line1);

        g.setColor(Color.GREEN);
        Line2D line2 = new Line2D.Double(mouseBGuide.getHead().getX(), mouseBGuide.getHead().getY(), mouseBGuide.getBottom().getX(), mouseBGuide.getBottom().getY());
        g.draw(line2);

        int fontSize = (int) canvas.canvasToImageDeltaX(18);
        if (fontSize < 1)
            fontSize = 1;
        Font font = new Font("Arial", Font.BOLD, fontSize);
        g.setFont(font);

        g.setColor(Color.RED);
        g.drawString("h", (float) mouseAGuide.getHead().getX(), (float) mouseAGuide.getHead().getY());
        g.drawString("", (float) mouseAGuide.getBottom().getX(), (float) mouseAGuide.getBottom().getY());

        g.setColor(Color.GREEN);
        g.drawString("h", (float) mouseBGuide.getHead().getX(), (float) mouseBGuide.getHead().getY());
        g.drawString("", (float) mouseBGuide.getBottom().getX(), (float) mouseBGuide.getBottom().getY());

        mouseAGuide.getHead().paint(g, sequence, canvas);
        mouseAGuide.getBottom().paint(g, sequence, canvas);
        mouseBGuide.getHead().paint(g, sequence, canvas);
        mouseBGuide.getBottom().paint(g, sequence, canvas);
    }

    @Override
    public void positionChanged(Anchor2D source) {
        painterChanged();
    }
}
