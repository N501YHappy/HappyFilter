package xyz.n501yhappy.happyfilter.utils.structs;
public final class Area implements Comparable<Area> { //开区间
    private final int l;
    private final int r;
    public int getL() {
        return l;
    }
    public int getR() {
        return r;
    }

    public Area(int l, int r) {
        if (l > r) throw new IllegalArgumentException("l must be <= r");
        this.l = l;
        this.r = r;
    }

    public int length() {
        return r - l;
    }

    @Override //左端点升序，左相同再比右
    public int compareTo(Area o) {
        int c = Integer.compare(this.l, o.l);
        return c != 0 ? c : Integer.compare(this.r, o.r);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Area)) return false;
        Area area = (Area) o;
        return l == area.l && r == area.r;
    }
    @Override
    public String toString() {
        return "[" + l + ", " + r + ")";
    }
}