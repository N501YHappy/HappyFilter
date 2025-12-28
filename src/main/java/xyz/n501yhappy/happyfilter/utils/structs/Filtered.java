package xyz.n501yhappy.happyfilter.utils.structs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Filtered {
    private List<Area> areas = new ArrayList<>();
    private boolean isFiltered;

    public Filtered(List<Area> areas, boolean isFiltered) {
        this.areas = areas;
        this.isFiltered = isFiltered;
    }

    public boolean isFiltered() {
        return isFiltered;
    }

    public Filtered merge(Filtered other) {
        this.areas.addAll(other.areas);
        this.isFiltered |= other.isFiltered;
        clearCoverd();
        return this;
    }

    public List<Area> getAreas() {
        return areas;
    }

    public void setAreas(List<Area> areas) {
        this.areas = areas;
    }
    public  void clearCoverd() { //删除重复覆盖的区间
        List<Area> areas = this.areas;
         Collections.sort(areas);
        List<Area> keep = new ArrayList<>();
        int maxR = -1;
        for (Area a : areas) {
            if (a.getR() <= maxR) continue;
            keep.add(a);
            maxR = a.getR();
        }
        this.areas = keep;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Filtered{");
        sb.append("isFiltered=").append(isFiltered);
        sb.append(", ranges=[");
        for (int i = 0; i < areas.size(); i++) {
            if (i > 0)
                sb.append(", ");
            Area area = areas.get(i);
            sb.append('[').append(area.getL()).append(',').append(area.getR()).append(')');
        }
        sb.append("], rangesCount=").append(areas.size()).append('}');
        return sb.toString();
    }
}