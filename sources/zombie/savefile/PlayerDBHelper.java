/*
 * Decompiled with CFR 0.152.
 */
package zombie.savefile;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import zombie.ZomboidFileSystem;
import zombie.core.BoxedStaticValues;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.util.PZSQLUtils;
import zombie.vehicles.VehicleDBHelper;

public final class PlayerDBHelper {
    public static Connection create() {
        Statement stat;
        Connection conn = null;
        String filename = ZomboidFileSystem.instance.getCurrentSaveDir();
        File dbDir = new File(filename);
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
        File dbFile = new File(filename + File.separator + "players.db");
        dbFile.setReadable(true, false);
        dbFile.setExecutable(true, false);
        dbFile.setWritable(true, false);
        if (!dbFile.exists()) {
            try {
                dbFile.createNewFile();
                conn = PZSQLUtils.getConnection(dbFile.getAbsolutePath());
                stat = conn.createStatement();
                stat.executeUpdate("CREATE TABLE localPlayers (id   INTEGER PRIMARY KEY NOT NULL,name STRING,wx    INTEGER,wy    INTEGER,x    FLOAT,y    FLOAT,z    FLOAT,worldversion    INTEGER,data BLOB,isDead BOOLEAN);");
                stat.executeUpdate("CREATE TABLE networkPlayers (id   INTEGER PRIMARY KEY NOT NULL,world TEXT,username TEXT,playerIndex   INTEGER,name STRING,steamid STRING,x    FLOAT,y    FLOAT,z    FLOAT,worldversion    INTEGER,data BLOB,isDead BOOLEAN);");
                stat.executeUpdate("CREATE INDEX inpusername ON networkPlayers (username);");
                stat.close();
            }
            catch (Exception e) {
                ExceptionLogger.logException(e);
                DebugLog.log("failed to create players database");
                System.exit(1);
            }
        }
        if (conn == null) {
            try {
                conn = PZSQLUtils.getConnection(dbFile.getAbsolutePath());
            }
            catch (Exception ex) {
                ExceptionLogger.logException(ex);
                DebugLog.log("failed to create players database");
                System.exit(1);
            }
        }
        try {
            stat = conn.createStatement();
            stat.executeQuery("PRAGMA JOURNAL_MODE=TRUNCATE;");
            stat.close();
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
            DebugLog.log("failed to config players.db");
            System.exit(1);
        }
        try {
            conn.setAutoCommit(false);
        }
        catch (SQLException e) {
            DebugLog.log("failed to setAutoCommit for players.db");
        }
        return conn;
    }

    public static void rollback(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.rollback();
        }
        catch (SQLException e) {
            ExceptionLogger.logException(e);
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public static boolean isPlayerAlive(String saveDir, int playerSqlId) {
        if (Core.getInstance().isNoSave()) {
            return false;
        }
        File file = new File(saveDir + File.separator + "map_p.bin");
        if (file.exists()) {
            return true;
        }
        if (VehicleDBHelper.isPlayerAlive(saveDir, playerSqlId)) {
            return true;
        }
        if (playerSqlId == -1) {
            return false;
        }
        try {
            File dbFile = new File(saveDir + File.separator + "players.db");
            if (!dbFile.exists()) {
                return false;
            }
            dbFile.setReadable(true, false);
            try (Connection connection = PZSQLUtils.getConnection(dbFile.getAbsolutePath());){
                String sql = "SELECT isDead FROM localPlayers WHERE id=?";
                try (PreparedStatement pstmt = connection.prepareStatement("SELECT isDead FROM localPlayers WHERE id=?");){
                    pstmt.setInt(1, playerSqlId);
                    ResultSet rs = pstmt.executeQuery();
                    if (!rs.next()) return false;
                    boolean bl = !rs.getBoolean(1);
                    return bl;
                }
            }
        }
        catch (Throwable t) {
            ExceptionLogger.logException(t);
        }
        return false;
    }

    public static ArrayList<Object> getPlayers(String saveDir) throws SQLException {
        ArrayList<Object> players = new ArrayList<Object>();
        if (Core.getInstance().isNoSave()) {
            return players;
        }
        File dbFile = new File(saveDir + File.separator + "players.db");
        if (!dbFile.exists()) {
            return players;
        }
        dbFile.setReadable(true, false);
        try (Connection connection = PZSQLUtils.getConnection(dbFile.getAbsolutePath());){
            String sql = "SELECT id, name, isDead FROM localPlayers";
            try (PreparedStatement pstmt = connection.prepareStatement("SELECT id, name, isDead FROM localPlayers");){
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    int sqlId = rs.getInt(1);
                    String name = rs.getString(2);
                    boolean isDead = rs.getBoolean(3);
                    players.add(BoxedStaticValues.toDouble(sqlId));
                    players.add(name);
                    players.add(isDead ? Boolean.TRUE : Boolean.FALSE);
                }
            }
        }
        return players;
    }

    public static boolean containsNetworkPlayer(String saveDir, String name, String world) throws SQLException {
        if (Core.getInstance().isNoSave()) {
            return false;
        }
        File dbFile = new File(saveDir + File.separator + "players.db");
        if (!dbFile.exists()) {
            return false;
        }
        dbFile.setReadable(true, false);
        try (Connection connection = PZSQLUtils.getConnection(dbFile.getAbsolutePath());){
            String sql = "SELECT id FROM networkPlayers WHERE world = ? and username = ?";
            try (PreparedStatement pstmt = connection.prepareStatement("SELECT id FROM networkPlayers WHERE world = ? and username = ?");){
                pstmt.setString(1, world);
                pstmt.setString(2, name);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    boolean bl = true;
                    return bl;
                }
            }
        }
        return false;
    }

    public static void removePlayer(String saveDir, String name, String world) throws SQLException {
        File dbFile = new File(saveDir + File.separator + "players.db");
        if (!dbFile.exists()) {
            return;
        }
        dbFile.setReadable(true, false);
        try (Connection connection = PZSQLUtils.getConnection(dbFile.getAbsolutePath());){
            String sql = "DELETE FROM networkPlayers WHERE world = ? and username = ?";
            try (PreparedStatement pstmt = connection.prepareStatement("DELETE FROM networkPlayers WHERE world = ? and username = ?");){
                pstmt.setString(1, world);
                pstmt.setString(2, name);
                pstmt.executeUpdate();
            }
        }
    }

    public static void setPlayer1(String saveDir, int sqlID) throws SQLException {
        block47: {
            if (Core.getInstance().isNoSave()) {
                return;
            }
            if (sqlID == 1) {
                return;
            }
            File dbFile = new File(saveDir + File.separator + "players.db");
            if (!dbFile.exists()) {
                return;
            }
            dbFile.setReadable(true, false);
            try (Connection connection = PZSQLUtils.getConnection(dbFile.getAbsolutePath());){
                boolean hasPlayer1 = false;
                boolean hasSelectedPlayer = false;
                int nonPlayer1 = -1;
                int maxID = -1;
                String sql = "SELECT id FROM localPlayers";
                try (PreparedStatement pstmt = connection.prepareStatement(sql);){
                    ResultSet rs = pstmt.executeQuery();
                    while (rs.next()) {
                        int id = rs.getInt(1);
                        if (id == 1) {
                            hasPlayer1 = true;
                        } else if (nonPlayer1 == -1 || nonPlayer1 > id) {
                            nonPlayer1 = id;
                        }
                        if (id == sqlID) {
                            hasSelectedPlayer = true;
                        }
                        maxID = Math.max(maxID, id);
                    }
                }
                if (sqlID <= 0) {
                    if (!hasPlayer1) {
                        return;
                    }
                    sql = "UPDATE localPlayers SET id=? WHERE id=?";
                    pstmt = connection.prepareStatement(sql);
                    try {
                        pstmt.setInt(1, maxID + 1);
                        pstmt.setInt(2, 1);
                        pstmt.executeUpdate();
                    }
                    finally {
                        if (pstmt != null) {
                            pstmt.close();
                        }
                    }
                    return;
                }
                if (!hasSelectedPlayer) {
                    return;
                }
                if (hasPlayer1) {
                    sql = "UPDATE localPlayers SET id=? WHERE id=?";
                    pstmt = connection.prepareStatement(sql);
                    try {
                        pstmt.setInt(1, maxID + 1);
                        pstmt.setInt(2, 1);
                        pstmt.executeUpdate();
                        pstmt.setInt(1, 1);
                        pstmt.setInt(2, sqlID);
                        pstmt.executeUpdate();
                        pstmt.setInt(1, sqlID);
                        pstmt.setInt(2, maxID + 1);
                        pstmt.executeUpdate();
                        break block47;
                    }
                    finally {
                        if (pstmt != null) {
                            pstmt.close();
                        }
                    }
                }
                sql = "UPDATE localPlayers SET id=? WHERE id=?";
                pstmt = connection.prepareStatement(sql);
                try {
                    pstmt.setInt(1, 1);
                    pstmt.setInt(2, sqlID);
                    pstmt.executeUpdate();
                }
                finally {
                    if (pstmt != null) {
                        pstmt.close();
                    }
                }
            }
        }
    }
}

