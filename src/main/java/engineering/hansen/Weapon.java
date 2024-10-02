package engineering.hansen;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

public class Weapon {
    private final String name;
    private final RangeBand[] bands;
    private final HashSet<String> attributes = new HashSet<>();

    public String getName() { return name; }
    public String[] getAttributes() { return attributes.toArray(new String[0]); }
    public RangeBand[] getRangeBands() { return bands; }
    public RangeBand getRangeBandFor(int range) {
        for (var band : bands) if (band.getRange() >= range) return band;
        return null;
    }

    Weapon(Connection c, int wpnid) {
        String name1 = "Unknown";
        RangeBand[] bands1 = null;
        try {
            var stmt = c.prepareStatement("select name from weapons where rowid = ?");
            stmt.setInt(1, wpnid);
            var rs = stmt.executeQuery();
            rs.next();
            name1 = rs.getString("name");
            stmt.close();
            stmt = c.prepareStatement("select attribute from weapon_to_weapon_attributes where weapon = ?");
            stmt.setInt(1, wpnid);
            rs = stmt.executeQuery();
            while (rs.next()) {
                var stmt2 = c.prepareStatement("select description from weapon_attributes where rowid = ?");
                stmt2.setInt(1, rs.getInt("attribute"));
                var rs2 = stmt2.executeQuery();
                rs.next();
                attributes.add(rs2.getString("description"));
                rs2.close();
                stmt2.close();
            }
            stmt.close();
            stmt = c.prepareStatement("select distance, accuracy, damage, penetration " +
                    "from weapon_range_bands where weapon = ? order by distance");
            stmt.setInt(1, wpnid);
            rs = stmt.executeQuery();
            ArrayList<RangeBand> tmpBands = new ArrayList<>();
            while (rs.next()) {
                var d = rs.getInt("distance");
                var a = rs.getInt("accuracy");
                var dmg = rs.getInt("damage");
                var p = rs.getInt("penetration");
                tmpBands.add(new RangeBand(d, a, dmg, p));
            }
            rs.close();
            stmt.close();
            bands1 = tmpBands.toArray(new RangeBand[0]);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            name = name1;
            bands = bands1;
        }
    }
}