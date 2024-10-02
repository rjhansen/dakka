package engineering.hansen;

public class RangeBand {
    private final int range;
    private final int accuracy;
    private final int damage;
    private final int penetration;

    public int getRange() { return range; }
    public int getAccuracy() { return accuracy; }
    public int getDamage() { return damage; }
    public int getPenetration() { return penetration; }

    RangeBand(int r, int a, int d, int p) {
        range = r;
        accuracy = a;
        damage = d;
        penetration = p;
    }
}