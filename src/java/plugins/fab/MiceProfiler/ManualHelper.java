package plugins.fab.MiceProfiler;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;

import icy.painter.Anchor2D;
import icy.painter.Overlay;

import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceListener;

import icy.type.point.Point5D.Double;


/**
 * this helper is dedicated to help manual correction.
 */
public class ManualHelper extends Overlay implements SequenceListener {

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Static fields/initializers
    //~ ----------------------------------------------------------------------------------------------------------------

    /** static font for absolute text */
    private static final Font ABSOLUTE_FONT = new Font("Arial", Font.BOLD, 15);

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Enums
    //~ ----------------------------------------------------------------------------------------------------------------

    private enum MODE {
        NO_ACTION_MODE, RECORD_MODE
    }

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Instance fields
    //~ ----------------------------------------------------------------------------------------------------------------

    /** list of anchors created by this overlay. */
    private List<MAnchor2D> activeAnchorList = Lists.newArrayList();

    /** TODO: What is that ? * */
    private final Map<Integer, MAnchor2D> time2controlPointMap = Maps.newHashMap();
    private final Color color;

    /** Previous time position of the canvas. Stored to update the painter */
    private int previousTPosition = -1;
    private MODE currentMode = MODE.NO_ACTION_MODE;
    private double lastFrameUpdate = 0.;
    private final int mouseNumber;

    /** switchModeKeyCode is the key to switch from edit to record mode. -1 if more than 10 mice are loaded. */
    private final int switchModeKeyCode;

    /** list of all manual helper to disable certain action like record mode when an other engage it. */
    private final List<ManualHelper> manualHelperList = Lists.newArrayList();

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Constructors
    //~ ----------------------------------------------------------------------------------------------------------------

    public ManualHelper(String name, Color color, int mouseNumber) {
        super(name);
        this.color = color;
        this.mouseNumber = mouseNumber;
        // record itself
        this.manualHelperList.add(this);

        if (mouseNumber < 10) {
            switchModeKeyCode = mouseNumber + KeyEvent.VK_0;
        } else {
            switchModeKeyCode = -1;
        }

        setPriority(OverlayPriority.TEXT_NORMAL);
    }

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Methods
    //~ ----------------------------------------------------------------------------------------------------------------

    @Override
    public void keyPressed(KeyEvent e, Double imagePoint, IcyCanvas canvas) {
        if (e.getKeyCode() == switchModeKeyCode) {
            if (currentMode == MODE.NO_ACTION_MODE) {
                currentMode = MODE.RECORD_MODE;
                for (ManualHelper m : manualHelperList) {
                    if (m != this) {
                        m.setMode(MODE.NO_ACTION_MODE);
                    }
                }
            } else {
                currentMode = MODE.NO_ACTION_MODE;
            }
            e.consume();
            canvas.getSequence().overlayChanged(this);
        }
    }

    @Override
    public void mouseClick(MouseEvent e, Double imagePoint, IcyCanvas canvas) {
        if (!(canvas instanceof IcyCanvas2D))
            return;

        if (currentMode == MODE.RECORD_MODE) {
            setControlPoint(imagePoint.getX(), imagePoint.getY(), (int) imagePoint.getT());
            e.consume();
            advanceOneFrame(canvas);
        }
    }

    @Override
    public void mouseDrag(MouseEvent e, Double imagePoint, IcyCanvas canvas) {
        if (!(canvas instanceof IcyCanvas2D))
            return;

        if (currentMode == MODE.RECORD_MODE) {
            setControlPoint(imagePoint.getX(), imagePoint.getY(), (int) imagePoint.getT());
            e.consume();
            advanceOneFrame(canvas);
        }
    }

    @Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
        if (!(canvas instanceof IcyCanvas2D))
            return;

        // set color, stroke
        g.setColor(color);

        // draw in absolute the current mode.
        String modeString = "";
        switch (currentMode) {

        case NO_ACTION_MODE:
            modeString = "Play mode";
            break;

        case RECORD_MODE:
            modeString = "Record mode";
            break;
        }
        drawAbsoluteString(mouseNumber + " : " + modeString, 20, 20 * mouseNumber, g, (IcyCanvas2D) canvas);

        int currentT = canvas.getPositionT();
        /* The time window to display the controls */
        int timeWindow = 10;

        // check if the time cursor has been shifted
        if (false) {
            if (currentT != previousTPosition) {
                previousTPosition = currentT;
                try {
                    sequence.beginUpdate();

                    // display anchors for the minus -10 frame to +10 frame
                    ArrayList<MAnchor2D> newAnchorList = new ArrayList<MAnchor2D>();

                    for (int t = currentT - timeWindow; t < (currentT + timeWindow); t++) {
                        // check time bounding of the sequence
                        if (t < 0)
                            continue;
                        if (t > sequence.getSizeT())
                            continue;

                        // retrieve anchor
                        MAnchor2D anchor = getControlPoint(t);
                        if (anchor == null)
                            return;

                        // set the editability of the anchor
                        boolean editAnchor = false;

                        anchor.setColor(Color.gray);
                        anchor.setSelectedColor(Color.gray);
                        anchor.setPriority(OverlayPriority.TEXT_LOW);
                        if (currentT == t) {
                            editAnchor = true;
                            anchor.setColor(this.color);
                            anchor.setSelectedColor(this.color);
                            anchor.setPriority(OverlayPriority.TEXT_HIGH);
                        }
                        anchor.setCanBeRemoved(editAnchor);
                        anchor.setEnabled(editAnchor);

                        // add anchor
                        newAnchorList.add(anchor);
                        sequence.addOverlay(anchor);
                    }

                    // removes previous anchor point that are not in the newAnchorList
                    for (Anchor2D anchor : activeAnchorList) {
                        if (!newAnchorList.contains(anchor)) {
                            sequence.removeOverlay(anchor);
                        }
                    }

                    // place the created anchor list as the active list
                    activeAnchorList = newAnchorList;

                } finally {
                    sequence.endUpdate();
                }
            }
        }

        // paint the links between anchors.
        for (int t = currentT - timeWindow; t < (currentT + timeWindow); t++) {
            Anchor2D a1 = getControlPoint(t);
            Anchor2D a2 = getControlPoint(t + 1);

            if ((a1 != null) && (a2 != null)) {
                Line2D line = new Line2D.Double(a1.getPosition(), a2.getPosition());
                g.draw(line);
            }
        }

        // draw an ellipse for the current T
        Anchor2D a = getControlPoint(currentT);
        if (a != null) {
            Ellipse2D ellipse = new Ellipse2D.Double(a.getPositionX() - 3, a.getPositionY() - 3, 7, 7);
            g.fill(ellipse);
        }

    }

    @Override
    public void sequenceChanged(SequenceEvent sequenceEvent) {

    }

    @Override
    public void sequenceClosed(Sequence sequence) {
        manualHelperList.remove(this);
    }

    MAnchor2D getControlPoint(int t) {
        return time2controlPointMap.get(t);
    }

    private void setControlPoint(double x, double y, int t) {
        MAnchor2D a = time2controlPointMap.get(t);
        if (a == null) {
            a = new MAnchor2D(x, y);
            time2controlPointMap.put(t, a);
        } else {
            a.setPosition(x, y);
        }
    }

    private void drawAbsoluteString(String string, int x, int y, Graphics2D g, IcyCanvas2D canvas) {
        AffineTransform transform = g.getTransform();
        g.transform(canvas.getInverseTransform());
        g.setFont(ABSOLUTE_FONT);
        g.drawString(string, x, y);
        g.setTransform(transform);
    }

    private void advanceOneFrame(IcyCanvas canvas) {
        if ((System.currentTimeMillis() - lastFrameUpdate) > 200) {
            lastFrameUpdate = System.currentTimeMillis();
            canvas.getSequence().getFirstViewer().setPositionT(canvas.getSequence().getFirstViewer().getPositionT() + 1);
        }
    }

    private void setMode(MODE mode) {
        currentMode = mode;
    }

}
