package engineering.hansen;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class DataRow {
    private final SimpleStringProperty mount;
    private final SimpleStringProperty weapon;
    private final SimpleIntegerProperty accuracy;
    private final SimpleIntegerProperty damage;
    private final SimpleIntegerProperty penetration;
    private final SimpleBooleanProperty fire;

    public String getMount() { return mount.get(); }
    public void setMount(String mount) { this.mount.set(mount); }
    public String getWeapon() { return weapon.get(); }
    public void setWeapon(String weapon) { this.weapon.set(weapon); }
    public Integer getAccuracy() { return accuracy.get(); }
    public void setAccuracy(Integer accuracy) { this.accuracy.set(accuracy); }
    public Integer getDamage() { return damage.get(); }
    public void setDamage(Integer damage) { this.damage.set(damage); }
    public Integer getPenetration() { return penetration.get(); }
    public void setPenetration(Integer penetration) { this.penetration.set(penetration); }
    public Boolean getFire() { return fire.get(); }
    public void setFire(Boolean fire) { this.fire.set(fire); }
    public SimpleBooleanProperty fireProperty() { return fire; }

    public DataRow(String m, String w, int a, int d, int p, boolean f) {
        mount = new SimpleStringProperty(m);
        weapon = new SimpleStringProperty(w);
        accuracy = new SimpleIntegerProperty(a);
        damage = new SimpleIntegerProperty(d);
        penetration = new SimpleIntegerProperty(p);
        fire = new SimpleBooleanProperty(f);
    }
}
