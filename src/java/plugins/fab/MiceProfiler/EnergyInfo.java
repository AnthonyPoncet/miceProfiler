package plugins.fab.MiceProfiler;

import net.phys2d.math.ROVector2f;

import java.util.ArrayList;
import java.util.List;

class EnergyInfo {
    private final boolean excludeFromAttractiveMapOwner;
    private final boolean excludeFromOtherMouse;
    private final float ray;
    private final EnergyMap energyMap;

    /**
     * position list for motion prediction list(0) should be speed à t-1
     * after each motion prediction, remove (0) from the list.
     **/
    private final List<ROVector2f> previousPositionList = new ArrayList<ROVector2f>();

    /** speed */
    private float vx = 0;
    /** speed */
    private float vy = 0;
    /** mouse which own this energy */
    private final Mouse mouse= null;

    public EnergyInfo(float ray, EnergyMap energyMap, boolean excludeFromOtherMouse, boolean excludeFromAttractiveMapOwner) {
        this.ray = ray;
        this.energyMap = energyMap;
        this.excludeFromOtherMouse = excludeFromOtherMouse;
        this.excludeFromAttractiveMapOwner = excludeFromAttractiveMapOwner;
    }

    public void setVx(float vx) {
        this.vx = vx;
    }

    public void setVy(float vy) {
        this.vy = vy;
    }

    public boolean isExcludeFromOtherMouse() {
        return excludeFromOtherMouse;
    }

    public float getRay() {
        return ray;
    }

    public EnergyMap getEnergyMap() {
        return energyMap;
    }

    public float getVx() {
        return vx;
    }

    public float getVy() {
        return vy;
    }
}
