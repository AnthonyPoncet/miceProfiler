package plugins.fab.MiceProfiler;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import net.phys2d.math.ROVector2f;


class EnergyInfo {

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Instance fields
    //~ ----------------------------------------------------------------------------------------------------------------

    private final float ray;
    private final EnergyMap energyMap;
    private final boolean excludeFromOtherMouse;

    /**
     * position list for motion prediction list(0) should be speed à t-1 after each motion prediction, remove (0) from
     * the list.
     */
    public final List<ROVector2f> previousPositionList = Lists.newArrayList();

    /** speed */
    public float vx;
    public float vy;

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Constructors
    //~ ----------------------------------------------------------------------------------------------------------------

    public EnergyInfo(float ray, EnergyMap energyMap, boolean excludeFromOtherMouse) {
        this.ray = ray;
        this.energyMap = energyMap;
        this.excludeFromOtherMouse = excludeFromOtherMouse;
        this.vx = 0;
        this.vy = 0;
    }

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Methods
    //~ ----------------------------------------------------------------------------------------------------------------

    public boolean isExcludeFromOtherMouse() {
        return excludeFromOtherMouse;
    }

    public float getRay() {
        return ray;
    }

    public EnergyMap getEnergyMap() {
        return energyMap;
    }
}
