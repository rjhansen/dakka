package engineering.hansen;

import java.sql.Connection;
import java.sql.SQLException;

public class Ship {
    private final String name;
    private final Mount[] mounts;
    public Mount[] getMounts() { return mounts; }
    public String getName() { return name; }

    @Override
    public String toString() { return name;}

    Ship(Connection c, int shipclass) {
        Mount[] mounts1 = null;
        String name1;
        int i = 0;

        var query = "select rowid, setting, name from ship_classes where rowid = ?";
        try (var stmt = c.prepareStatement(query)) {
            stmt.setInt(1, shipclass);
            var rs = stmt.executeQuery();
            rs.next();
            name1 = rs.getString("name");


            var stmt2 = c.prepareStatement("select count(*) from mounts where ship = ?");
            stmt2.setInt(1, shipclass);
            rs = stmt2.executeQuery();
            rs.next();
            mounts1 = new Mount[rs.getInt(1)];
            stmt2.close();

            stmt2 = c.prepareStatement("select rowid, name, arc from mounts where ship = ? order by name");
            stmt2.setInt(1, shipclass);
            rs = stmt2.executeQuery();
            while (rs.next()) mounts1[i++] =
                    new Mount(c, rs.getInt("rowid"), rs.getString("name"), rs.getString("arc"));
            stmt2.close();
        } catch (SQLException e) {
            e.printStackTrace();
            name1 = "Unknown";
        }
        name = name1;
        mounts = mounts1;
    }
}
