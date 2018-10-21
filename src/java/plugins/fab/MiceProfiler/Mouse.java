package plugins.fab.MiceProfiler;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
import net.phys2d.raw.Body;
import net.phys2d.raw.FixedJoint;
import net.phys2d.raw.shapes.Box;
import net.phys2d.raw.shapes.Circle;

class Mouse {
    private enum BodyType {
        BOX, CIRCLE
    }

    private final static float SCALE = 0.22f;
    private final static float SCALERAY = 1f;
    private final static float DAMPING = 0.1f;


    private final Body nose;
    private final Body earL;
    private final Body earR;
    private final Body neck;
    private final Body shoulderR;

    private final Body shoulderL;
    private final Body tommyL;
    private final Body tommyR;
    private final Body tommyB;
    private final Body assR;

    private final Body assL;

    private final Body tommyBody;
    private final Body tail1;
    private final Body tail2;
    private final Body tail3;
    private final Body tail4;

    private final Body tail5;

    private final Body neckAttachBody;

    private final List<Body> bodyList = Lists.newArrayList();

    private final ArrayList<Body> contourList = new ArrayList<Body>();

    private final Body headBody;
    private final Body tail;

    public Mouse(float x, float y, float alpha) {
        tommyBody = generateBody(BodyType.CIRCLE, 1, 60, 30, 30, EnergyMap.BINARY_MOUSE, false, true);
        bodyList.add(tommyBody);
        setPosition(tommyBody, 0f, 80f, x, y, alpha);

        Body tommyR = generateBody(BodyType.BOX, 1, 60, 20, 60, EnergyMap.NO_ENERGY, true, true);
        bodyList.add(tommyR);
        setPosition(tommyR, 20f, 80f, x, y, alpha);

        Body tommyL = generateBody(BodyType.BOX, 1, 60, 20, 60, EnergyMap.NO_ENERGY, true, true);
        bodyList.add(tommyL);
        setPosition(tommyL, -20f, 80f, x, y, alpha);

        Body tommyBL = generateBody(BodyType.BOX, 1, 20, 10, 10, EnergyMap.GRADIENT_MAP, true, true);
        bodyList.add(tommyBL);
        setPosition(tommyBL, -20f, 120f, x, y, alpha);

        Body tommyBR = generateBody(BodyType.BOX, 1, 20, 10, 10, EnergyMap.GRADIENT_MAP, true, true);
        bodyList.add(tommyBR);
        setPosition(tommyBR, 20f, 120f, x, y, alpha);

        Body tommyBLC = generateBody(BodyType.BOX, 1, 20, 10, 10, EnergyMap.GRADIENT_MAP, true, true);
        bodyList.add(tommyBLC);
        setPosition(tommyBLC, -20f, 60f, x, y, alpha);

        Body tommyBRC = generateBody(BodyType.BOX, 1, 20, 10, 10, EnergyMap.GRADIENT_MAP, true, true);
        bodyList.add(tommyBRC);
        setPosition(tommyBRC, 20f, 60f, x, y, alpha);

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

        phyMouse.world.add(new FixedJoint(tommyBody, tommyR));
        phyMouse.world.add(new FixedJoint(tommyBody, tommyL));

        phyMouse.world.add(new FixedJoint(tommyBody, tommyBL));
        phyMouse.world.add(new FixedJoint(tommyBody, tommyBR));

        phyMouse.world.add(new FixedJoint(tommyBody, tommyBLC));
        phyMouse.world.add(new FixedJoint(tommyBody, tommyBRC));

        // create neck
        Body neckBody;
        neckBody = generateBody(BodyType.BOX, 1, 0, 20, 60, EnergyMap.NO_ENERGY, true, true);
        bodyList.add(neckBody);
        setPosition(neckBody, 0f, 60f, x, y, alpha);

        neckBody.addExcludedBody(tommyBody);

        neckAttachBody = generateBody(BodyType.CIRCLE, 1, 0, 5, 5, EnergyMap.NO_ENERGY, true, true);
        bodyList.add(neckAttachBody);
        setPosition(neckAttachBody, 0f, 30f, x, y, alpha);

        neckAttachBody.addExcludedBody(tommyBody);
        neckAttachBody.addExcludedBody(neckBody);

        phyMouse.world.add(new FixedJoint(neckBody, neckAttachBody));

        phyMouse.generateSlideJoint(neckBody, tommyBody, 0, 4);

        headBody = generateBody(BodyType.CIRCLE, 1, 50, 20, 20, EnergyMap.GRADIENT_MAP, false, true);
        bodyList.add(headBody);
        setPosition(headBody, 0f, 0f, x, y, alpha);
        headBody.setRotation((float) (Math.PI / 4f));

        phyMouse.generateSlideJoint(neckAttachBody, headBody, 0, 2);

        // Was no energy
        tail = generateBody(BodyType.CIRCLE, 1, 0, 5, 5, EnergyMap.NO_ENERGY, true, true); // false
        bodyList.add(tail);
        setPosition(tail, 0f, 120f, x, y, alpha);
        phyMouse.world.add(new FixedJoint(tommyBody, tail));

        for (Mouse mouse : phyMouse.mouseList) {
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
            e.mouse = this;
        }
    }

    private void setPosition(Body body, float x, float y, float dx, float dy, float alpha) {
        x *= phyMouse.SCALE;
        y *= phyMouse.SCALE;

        final float xx = (float) (Math.cos(alpha) * (x - 0) - Math.sin(alpha) * (y - 0) + 0);
        final float yy = (float) (Math.cos(alpha) * (y - 0) + Math.sin(alpha) * (x - 0) + 0);

        Point2D point = new Point2D.Float(xx + dx, yy + dy);

        body.setPosition((float) point.getX(), (float) point.getY());
        body.setRotation(alpha);
    }

    public Body getHeadBody() {
        return headBody;
    }

    public Body getTail() {
        return tail;
    }

    private Body generateBody(BodyType bodyType, float mass, float ray, float width, float height, EnergyMap energyMap,
                              boolean excludeFromOtherMouseContact, boolean excludeFromAttractiveMapOwner) {

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

        body.setUserData(new EnergyInfo(ray * SCALE * SCALERAY, energyMap, excludeFromOtherMouseContact, excludeFromAttractiveMapOwner));
        body.setDamping(DAMPING);
        body.setGravityEffected(false);
        body.setCanRest(true);

        world.add(body);

        return body;
    }

}
