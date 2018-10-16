package plugins.fab.MiceProfiler;

import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.type.point.Point5D.Double;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

public class LockScrollHelperOverlay extends Overlay {

	private boolean lockingPan = false;

	/** static font for absolute text */
	private final static Font ABSOLUTE_FONT = new Font("Arial", Font.BOLD , 15 );
	private final static int X = 150;
    private final static int Y = 150;

	public LockScrollHelperOverlay() {
		super("Lock Scroll Helper");
		setPriority(OverlayPriority.IMAGE_NORMAL);
	}

	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
		if (canvas instanceof IcyCanvas2D){
			if (lockingPan) {
				g.setColor(Color.red);
				drawAbsoluteString("E: Mouse pan disabled (use numpad arrow keys)", X, Y, g, (IcyCanvas2D)canvas);
			} else {
				g.setColor(Color.gray);
				drawAbsoluteString("E: Mouse pan enabled", X, Y, g, (IcyCanvas2D)canvas);
			}
		}
	}

    @Override
    public void keyPressed(KeyEvent e, Double imagePoint, IcyCanvas canvas) {
        if (e.getKeyChar()=='e' || e.getKeyChar()=='E' ) {
            lockingPan = !lockingPan;
            e.consume();
            canvas.getSequence().overlayChanged(this);
        }
    }

    @Override
    public void mouseDrag(MouseEvent e, Double imagePoint, IcyCanvas canvas) {
        if (lockingPan) {
            e.consume();
        }
    }

    private void drawAbsoluteString(String string, int x, int y, Graphics2D g, IcyCanvas2D canvas) {
        AffineTransform transform = g.getTransform();
        g.transform(canvas.getInverseTransform());
        g.setFont(ABSOLUTE_FONT);
        g.drawString(string, x, y);
        g.setTransform(transform);
    }

}
