/*
 * Decompiled with CFR 0.152.
 */
package zombie.vehicles;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.util.PZSQLUtils;

public final class VehicleDBHelper {
    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean isPlayerAlive(String saveDir, int playerSqlId) {
        Connection connection;
        File file = new File(saveDir + File.separator + "map_p.bin");
        if (file.exists()) {
            return true;
        }
        if (playerSqlId == -1) {
            return false;
        }
        File dbFile = new File(saveDir + File.separator + "vehicles.db");
        dbFile.setReadable(true, false);
        if (!dbFile.exists()) {
            return false;
        }
        try {
            connection = PZSQLUtils.getConnection(dbFile.getAbsolutePath());
        }
        catch (Exception ex) {
            DebugLog.log("failed to get connection to vehicles database: " + dbFile.getAbsolutePath());
            ExceptionLogger.logException(ex);
            return false;
        }
        boolean isAlive = false;
        String sql = "SELECT isDead FROM localPlayers WHERE id=?";
        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement("SELECT isDead FROM localPlayers WHERE id=?");
            pstmt.setInt(1, playerSqlId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                isAlive = !rs.getBoolean(1);
            }
        }
        catch (SQLException e1) {
            boolean bl = false;
            return bl;
        }
        finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                connection.close();
            }
            catch (SQLException e3) {
                System.out.println(e3.getMessage());
            }
        }
        return isAlive;
    }
}

