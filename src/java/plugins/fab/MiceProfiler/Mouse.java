package plugins.fab.MiceProfiler;

import java.awt.geom.Point2D;

import java.util.List;

import com.google.common.collect.Lists;

import net.phys2d.raw.Body;
import net.phys2d.raw.FixedJoint;
import net.phys2d.raw.World;
import net.phys2d.raw.shapes.Box;
import net.phys2d.raw.shapes.Circle;


class Mouse {

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Static fields/initializers
    //~ ----------------------------------------------------------------------------------------------------------------

    private static final float SCALE = 0.22f;
    private static final float SCALERAY = 1f;
    private static final float DAMPING = 0.1f;

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Enums
    //~ ----------------------------------------------------------------------------------------------------------------

    private enum BodyType {
        BOX, CIRCLE
    }

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Instance fields
    //~ ----------------------------------------------------------------------------------------------------------------

    private final Body head;
    private final Body tail;
    private final Body tommyBody;
    private final Body neckAttachBody;

    private final List<Body> bodyList = Lists.newArrayList();

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Constructors
    //~ ----------------------------------------------------------------------------------------------------------------

    public Mouse(World world, float x, float y, float alpha, PhyMouse phyMouse) {
        tommyBody = generateBody(BodyType.CIRCLE, 1, 60, 30, 30, EnergyMap.BINARY_MOUSE, false);
        bodyList.add(this.tommyBody);
        setPosition(this.tommyBody, 0f, 80f, x, y, alpha);
        world.add(tommyBody);

        Body tommyR = generateBody(BodyType.BOX, 1, 60, 20, 60, EnergyMap.NO_ENERGY, true);
        bodyList.add(tommyR);
        setPosition(tommyR, 20f, 80f, x, y, alpha);
        world.add(tommyR);

        Body tommyL = generateBody(BodyType.BOX, 1, 60, 20, 60, EnergyMap.NO_ENERGY, true);
        bodyList.add(tommyL);
        setPosition(tommyL, -20f, 80f, x, y, alpha);
        world.add(tommyL);

        Body tommyBL = generateBody(BodyType.BOX, 1, 20, 10, 10, EnergyMap.GRADIENT_MAP, true);
        bodyList.add(tommyBL);
        setPosition(tommyBL, -20f, 120f, x, y, alpha);
        world.add(tommyBL);

        Body tommyBR = generateBody(BodyType.BOX, 1, 20, 10, 10, EnergyMap.GRADIENT_MAP, true);
        bodyList.add(tommyBR);
        setPosition(tommyBR, 20f, 120f, x, y, alpha);
        world.add(tommyBR);

        Body tommyBLC = generateBody(BodyType.BOX, 1, 20, 10, 10, EnergyMap.GRADIENT_MAP, true);
        bodyList.add(tommyBLC);
        setPosition(tommyBLC, -20f, 60f, x, y, alpha);
        world.add(tommyBLC);

        Body tommyBRC = generateBody(BodyType.BOX, 1, 20, 10, 10, EnergyMap.GRADIENT_MAP, true);
        bodyList.add(tommyBRC);
        setPosition(tommyBRC, 20f, 60f, x, y, alpha);
        world.add(tommyBRC);

        tommyR.addExcludedBody(tommyBRC);
        tommyL.addExcludedBody(tommyBLC);
        tommyR.addExcludedBody(tommyBR);
        tommyL.addExcludedBody(tommyBL);
        tommyBody.addExcludedBody(tommyBL);
        tommyBody.addExcludedBody(tommyBR);
        tommyBody.addExcludedBody(tommyBLC);
        tommyBody.addExcludedBody(tommyBRC);
        tommyBody.addExcludedBody(tommyL);
        tommyBody.addExcludedBody(tommyR);

        world.add(new FixedJoint(tommyBody, tommyR));
        world.add(new FixedJoint(tommyBody, tommyL));

        world.add(new FixedJoint(tommyBody, tommyBL));
        world.add(new FixedJoint(tommyBody, tommyBR));

        world.add(new FixedJoint(tommyBody, tommyBLC));
        world.add(new FixedJoint(tommyBody, tommyBRC));

        // create neck
        Body neckBody;
        neckBody = generateBody(BodyType.BOX, 1, 0, 20, 60, EnergyMap.NO_ENERGY, true);
        bodyList.add(neckBody);
        setPosition(neckBody, 0f, 60f, x, y, alpha);
        world.add(neckBody);

        neckBody.addExcludedBody(tommyBody);

        neckAttachBody = generateBody(BodyType.CIRCLE, 1, 0, 5, 5, EnergyMap.NO_ENERGY, true);
        bodyList.add(neckAttachBody);
        setPosition(neckAttachBody, 0f, 30f, x, y, alpha);
        world.add(neckAttachBody);

        neckAttachBody.addExcludedBody(tommyBody);
        neckAttachBody.addExcludedBody(neckBody);

        world.add(new FixedJoint(neckBody, neckAttachBody));

        phyMouse.generateSlideJoint(neckBody, tommyBody, 0, 4);

        head = generateBody(BodyType.CIRCLE, 1, 50, 20, 20, EnergyMap.GRADIENT_MAP, false);
        bodyList.add(head);
        setPosition(head, 0f, 0f, x, y, alpha);
        head.setRotation((float) (Math.PI / 4f));
        world.add(head);

        phyMouse.generateSlideJoint(neckAttachBody, head, 0, 2);

        // Was no energy
        tail = generateBody(BodyType.CIRCLE, 1, 0, 5, 5, EnergyMap.NO_ENERGY, true); // false
        bodyList.add(tail);
        setPosition(tail, 0f, 120f, x, y, alpha);
        world.add(tail);
        world.add(new FixedJoint(tommyBody, tail));

        for (Mouse mouse : phyMouse.getMouseList()) {
            // create link with existing mice
            if (mouse != this) {
                for (Body bodyA : bodyList) {
                    for (Body bodyB : bodyList) {
                        EnergyInfo eA = (EnergyInfo) bodyA.getUserData();
                        EnergyInfo eB = (EnergyInfo) bodyB.getUserData();

                        if (eA.isExcludeFromOtherMouse() || eB.isExcludeFromOtherMouse()) {
                            bodyA.addExcludedBody(bodyB);
                            bodyB.addExcludedBody(bodyA);
                        }
                    }
                }
            }
        }

        for (Body body : bodyList) {
            EnergyInfo e = (EnergyInfo) body.getUserData();
            //e.mouse = this;
        }
    }

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Methods
    //~ ----------------------------------------------------------------------------------------------------------------

    public Body getHead() {
        return head;
    }

    public Body getTail() {
        return tail;
    }

    public Body getTommyBody() {
        return tommyBody;
    }

    public Body getNeckAttachBody() {
        return neckAttachBody;
    }

    public List<Body> getBodyList() {
        return bodyList;
    }

    private void setPosition(Body body, float x, float y, float dx, float dy, float alpha) {
        x *= SCALE;
        y *= SCALE;

        final float xx = (float) ((Math.cos(alpha) * (x - 0)) - (Math.sin(alpha) * (y - 0)) + 0);
        final float yy = (float) ((Math.cos(alpha) * (y - 0)) + (Math.sin(alpha) * (x - 0)) + 0);

        Point2D point = new Point2D.Float(xx + dx, yy + dy);

        body.setPosition((float) point.getX(), (float) point.getY());
        body.setRotation(alpha);
    }

    private Body generateBody(BodyType bodyType, float mass, float ray, float width, float height, EnergyMap energyMap, boolean excludeFromOtherMouseContact) {
        Body body;
        switch (bodyType) {

        case BOX:
            body = new Body("", new Box(width * SCALE, height * SCALE), mass);
            break;

        case CIRCLE:
            body = new Body("", new Circle(width * SCALE), mass);
            break;

        default:
            throw new IllegalStateException("Mouse::generateBody: Unknown BodyType.");
        }

        body.setUserData(new EnergyInfo(ray * SCALE * SCALERAY, energyMap, excludeFromOtherMouseContact));
        body.setDamping(DAMPING);
        body.setGravityEffected(false);
        body.setCanRest(true);

        return body;
    }

}
