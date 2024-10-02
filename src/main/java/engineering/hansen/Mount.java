package engineering.hansen;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

public class Mount {
    private final boolean[][] arcs = new boolean[7][12];
    private final Weapon[] weapons;
    private final String name;

    public boolean[][] getArcs() { return arcs; }
    public Weapon[] getWeapons() { return weapons; }
    public String getName() { return name; }
    public Weapon[] whatCanFire(int x, int y, int z) {
        if (! arcs[x][y]) return null;
        ArrayList<Weapon> tmp = new ArrayList<>();
        for (Weapon weapon : weapons) if (weapon.getRangeBandFor(z) != null) tmp.add(weapon);
        return tmp.toArray(new Weapon[0]);
    }

    Mount(Connection c, int rowid, String name, String arc) {
        Weapon[] weapons1 = null;
        this.name = name;

        for (int i = 0 ; i < 7 ; i++)
            for (int j = 0; j < 12; j++)
                arcs[i][j] = false;
        boolean[] arcmap = new boolean[arc.length()];
        for (int i = 0 ; i < arc.length() ; i++)
            arcmap[i] = arc.charAt(i) == 'T';
        arcs[0][0] = arcmap[0];
        System.arraycopy(arcmap, 1, arcs[1], 0, 6);
        System.arraycopy(arcmap, 7, arcs[2], 0, 12);
        System.arraycopy(arcmap, 19, arcs[3], 0, 12);
        System.arraycopy(arcmap, 31, arcs[4], 0, 12);
        System.arraycopy(arcmap, 43, arcs[5], 0, 6);
        arcs[6][0] = arcmap[49];


        try {
            var stmt = c.prepareStatement("select sum(quantity) as q from mounts_and_weapons where mount = ?");
            stmt.setInt(1, rowid);
            var rs = stmt.executeQuery();
            rs.next();
            weapons1 = new Weapon[rs.getInt("q")];
            rs.close();
            stmt.close();

            stmt = c.prepareStatement("select weapon, quantity from mounts_and_weapons where mount = ?");
            stmt.setInt(1, rowid);
            rs = stmt.executeQuery();
            int i = 0;
            while (rs.next()) {
                var qty = rs.getInt("quantity");
                var wpn = rs.getInt("weapon");
                for (int j = 0; j < qty; i++, j++) {
                    weapons1[i] = new Weapon(c, wpn);
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception: " + e.getMessage());
        } finally {
            weapons = weapons1;
        }
    }
}
