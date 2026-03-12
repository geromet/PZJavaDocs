/*
 * Decompiled with CFR 0.152.
 */
package zombie.savefile;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import zombie.Lua.LuaManager;
import zombie.ZomboidFileSystem;
import zombie.core.logger.ExceptionLogger;
import zombie.core.textures.Texture;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.network.Account;
import zombie.network.Server;
import zombie.util.PZSQLUtils;

public class AccountDBHelper {
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static AccountDBHelper instance;
    Connection conn;
    private static LocalDateTime lastSave;

    public static synchronized AccountDBHelper getInstance() {
        if (instance == null) {
            instance = new AccountDBHelper();
        }
        return instance;
    }

    public Connection create() {
        Statement stat;
        if (this.conn != null) {
            return this.conn;
        }
        File dbDir = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "db");
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
        String filename = ZomboidFileSystem.instance.getCacheDir() + File.separator + "db" + File.separator + (SteamUtils.isSteamModeEnabled() ? "ServerListSteam.db" : "ServerList.db");
        File dbFile = new File(filename);
        dbFile.setReadable(true, false);
        dbFile.setExecutable(true, false);
        dbFile.setWritable(true, false);
        if (!dbFile.exists()) {
            try {
                dbFile.createNewFile();
                this.conn = PZSQLUtils.getConnection(dbFile.getAbsolutePath());
                stat = this.conn.createStatement();
                stat.executeUpdate("CREATE TABLE IF NOT EXISTS server (\n id INTEGER PRIMARY KEY AUTOINCREMENT,\n name TEXT NOT NULL,\n ip TEXT NOT NULL,\n port INTEGER NOT NULL,\n serverPassword TEXT,\n description TEXT,\n mods TEXT,\n icon BLOB,\n banner BLOB,\n panelBackground BLOB,\n screenBackground BLOB,\n lastOnline TEXT,\n lastDataUpdate TEXT\n);");
                stat.executeUpdate("CREATE TABLE IF NOT EXISTS account (\n id INTEGER PRIMARY KEY AUTOINCREMENT,\n serverId INTEGER NOT NULL,\n playerFirstAndLastName  TEXT,\n username TEXT NOT NULL,\n password TEXT,\n isSavePassword INTEGER DEFAULT 0,\n isUseSteamRelay INTEGER DEFAULT 0,\n authType INTEGER DEFAULT 1,\n icon BLOB,\n timePlayed INTEGER DEFAULT 0,\n lastLogon TEXT,\n FOREIGN KEY (serverId) REFERENCES server (id)\n);");
                stat.close();
            }
            catch (Exception e) {
                ExceptionLogger.logException(e);
                DebugLog.log("failed to create accounts database");
                System.exit(1);
            }
        }
        if (this.conn == null) {
            try {
                this.conn = PZSQLUtils.getConnection(dbFile.getAbsolutePath());
            }
            catch (Exception ex) {
                ExceptionLogger.logException(ex);
                DebugLog.log("failed to create ServerList database");
                System.exit(1);
            }
        }
        try {
            stat = this.conn.createStatement();
            stat.executeQuery("PRAGMA JOURNAL_MODE=TRUNCATE;");
            stat.close();
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
            DebugLog.log("failed to config ServerList.db");
            System.exit(1);
        }
        try {
            this.conn.setAutoCommit(false);
        }
        catch (SQLException e) {
            DebugLog.log("failed to setAutoCommit for ServerList.db");
        }
        String oldServerListFilename = LuaManager.getLuaCacheDir() + File.separator + (SteamUtils.isSteamModeEnabled() ? "ServerListSteam.txt" : "ServerList.txt");
        File oldDBFile = new File(oldServerListFilename);
        if (oldDBFile.exists()) {
            this.parseInputFile(oldDBFile.getAbsolutePath());
            oldDBFile.delete();
        }
        return this.conn;
    }

    private void parseInputFile(String filename) {
        HashMap<String, String> data = new HashMap<String, String>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename));){
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length != 2) continue;
                if (parts[0].trim().equals("name") && !data.isEmpty()) {
                    int serverId = this.insertServer(data);
                    if (serverId > 0) {
                        this.insertAccount(data, serverId);
                        this.conn.commit();
                        System.out.println("Data loaded successfully!");
                    }
                    data.clear();
                }
                data.put(parts[0].trim(), parts[1].trim());
            }
            if (!data.isEmpty()) {
                int serverId = this.insertServer(data);
                if (serverId > 0) {
                    this.insertAccount(data, serverId);
                    this.conn.commit();
                    System.out.println("Data loaded successfully!");
                }
                data.clear();
            }
        }
        catch (IOException e) {
            System.err.println("Error reading input file: " + e.getMessage());
        }
        catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    private int insertServer(HashMap<String, String> data) throws SQLException {
        String sql = "INSERT INTO server (name, ip, port, serverPassword, description) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = this.conn.prepareStatement("INSERT INTO server (name, ip, port, serverPassword, description) VALUES (?, ?, ?, ?, ?)", 1);){
            pstmt.setString(1, data.getOrDefault("name", ""));
            pstmt.setString(2, data.getOrDefault("ip", ""));
            pstmt.setInt(3, Integer.parseInt(data.getOrDefault("port", "0")));
            pstmt.setString(4, data.getOrDefault("serverpassword", ""));
            pstmt.setString(5, data.getOrDefault("description", ""));
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys();){
                    if (rs.next()) {
                        int n = rs.getInt(1);
                        return n;
                    }
                }
            }
            int n = -1;
            return n;
        }
    }

    private void insertAccount(HashMap<String, String> data, int serverId) throws SQLException {
        String sql = "INSERT INTO account (serverId, username, password, isSavePassword, isUseSteamRelay, authType, timePlayed) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String accountStr = data.getOrDefault("account", "");
        if (!accountStr.isEmpty()) {
            String[] accountData = accountStr.split(";;");
            try (PreparedStatement pstmt = this.conn.prepareStatement("INSERT INTO account (serverId, username, password, isSavePassword, isUseSteamRelay, authType, timePlayed) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");){
                pstmt.setInt(1, serverId);
                pstmt.setString(2, accountData[0]);
                pstmt.setString(3, accountData[1]);
                pstmt.setBoolean(4, Boolean.parseBoolean(accountData[2]));
                pstmt.setBoolean(5, Boolean.parseBoolean(accountData[3]));
                pstmt.setInt(6, Integer.parseInt(accountData[4]));
                pstmt.setInt(7, 0);
                pstmt.executeUpdate();
            }
            return;
        }
        try (PreparedStatement pstmt = this.conn.prepareStatement("INSERT INTO account (serverId, username, password, isSavePassword, isUseSteamRelay, authType, timePlayed) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");){
            pstmt.setInt(1, serverId);
            pstmt.setString(2, data.getOrDefault("user", ""));
            pstmt.setString(3, data.getOrDefault("password", ""));
            pstmt.setBoolean(4, Boolean.parseBoolean(data.getOrDefault("remember", "false")));
            pstmt.setBoolean(5, Boolean.parseBoolean(data.getOrDefault("usesteamrelay", "false")));
            pstmt.setString(6, data.getOrDefault("authType", ""));
            pstmt.setInt(7, 0);
            pstmt.executeUpdate();
        }
    }

    public ArrayList<Server> getServerList() {
        this.create();
        ArrayList<Server> servers = new ArrayList<Server>();
        try {
            String serverSql = "SELECT s.* FROM server s LEFT JOIN account a ON s.id = a.serverId GROUP BY s.id ORDER BY MAX(a.lastLogon) DESC NULLS LAST;";
            try (Statement stmt = this.conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT s.* FROM server s LEFT JOIN account a ON s.id = a.serverId GROUP BY s.id ORDER BY MAX(a.lastLogon) DESC NULLS LAST;");){
                while (rs.next()) {
                    Server server = new Server();
                    server.setID(rs.getInt("id"));
                    server.setName(rs.getString("name"));
                    server.setIp(rs.getString("ip"));
                    server.setPort(rs.getInt("port"));
                    server.setServerPassword(rs.getString("serverPassword"));
                    server.setDescription(rs.getString("description"));
                    server.setLastOnline(this.parseDateTime(rs.getString("lastOnline")));
                    server.setLastDataUpdate(this.parseDateTime(rs.getString("lastDataUpdate")));
                    this.loadAccountsForServer(server);
                    servers.add(server);
                }
            }
            this.conn.close();
            this.conn = null;
        }
        catch (SQLException e) {
            System.err.println("Error retrieving server list: " + e.getMessage());
        }
        return servers;
    }

    private void loadAccountsForServer(Server server) throws SQLException {
        String accountSql = "SELECT * FROM account WHERE serverId = ?";
        try (PreparedStatement pstmt = this.conn.prepareStatement("SELECT * FROM account WHERE serverId = ?");){
            pstmt.setInt(1, server.getID());
            try (ResultSet rs = pstmt.executeQuery();){
                while (rs.next()) {
                    Account account = new Account();
                    account.setID(rs.getInt("id"));
                    account.setUserName(rs.getString("username"));
                    account.setPwd(rs.getString("password"));
                    account.setSavePwd(rs.getBoolean("isSavePassword"));
                    account.setUseSteamRelay(rs.getBoolean("isUseSteamRelay"));
                    account.setAuthType(rs.getInt("authType"));
                    account.setPlayerFirstAndLastName(rs.getString("playerFirstAndLastName"));
                    InputStream iconData = rs.getBinaryStream("icon");
                    if (iconData != null) {
                        account.setIcon(new Texture("accountIcon" + account.getID(), new BufferedInputStream(iconData), false));
                    }
                    account.setTimePlayed(rs.getInt("timePlayed"));
                    account.setLastLogon(this.parseDateTime(rs.getString("lastLogon")));
                    server.addAccount(account);
                }
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, formatter);
        }
        catch (Exception e) {
            System.err.println("Error parsing datetime: " + dateTimeStr);
            return null;
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public int saveNewServer(Server server) {
        this.create();
        String sql = "INSERT INTO server (name, ip, port, serverPassword, description, lastOnline, lastDataUpdate) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = this.conn.prepareStatement("INSERT INTO server (name, ip, port, serverPassword, description, lastOnline, lastDataUpdate) VALUES (?, ?, ?, ?, ?, ?, ?)", 1);){
            pstmt.setString(1, server.getName());
            pstmt.setString(2, server.getIp());
            pstmt.setInt(3, server.getPort());
            pstmt.setString(4, server.getServerPassword());
            pstmt.setString(5, server.getDescription());
            pstmt.setString(6, server.getLastOnline() == null ? null : server.getLastOnline().format(formatter));
            pstmt.setString(7, server.getLastDataUpdate() == null ? null : server.getLastDataUpdate().format(formatter));
            int affectedRows = pstmt.executeUpdate();
            this.conn.commit();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys();){
                    if (rs.next()) {
                        int result = rs.getInt(1);
                        this.conn.close();
                        this.conn = null;
                        int n = result;
                        return n;
                    }
                }
            }
            this.conn.close();
            this.conn = null;
            return -1;
        }
        catch (SQLException e) {
            System.err.println("Error saving new server: " + e.getMessage());
        }
        return -1;
    }

    public boolean updateServer(Server server) {
        boolean bl;
        block9: {
            if (server == null || server.getID() <= 0) {
                return false;
            }
            this.create();
            String sql = "UPDATE server SET name = ?, ip = ?, port = ?, serverPassword = ?, description = ?, lastOnline = ?, lastDataUpdate = ? WHERE id = ?";
            PreparedStatement pstmt = this.conn.prepareStatement("UPDATE server SET name = ?, ip = ?, port = ?, serverPassword = ?, description = ?, lastOnline = ?, lastDataUpdate = ? WHERE id = ?");
            try {
                pstmt.setString(1, server.getName());
                pstmt.setString(2, server.getIp());
                pstmt.setInt(3, server.getPort());
                pstmt.setString(4, server.getServerPassword());
                pstmt.setString(5, server.getDescription());
                pstmt.setString(6, server.getLastOnline() == null ? null : server.getLastOnline().format(formatter));
                pstmt.setString(7, server.getLastDataUpdate() == null ? null : server.getLastDataUpdate().format(formatter));
                pstmt.setInt(8, server.getID());
                int affectedRows = pstmt.executeUpdate();
                this.conn.commit();
                this.conn.close();
                this.conn = null;
                boolean bl2 = bl = affectedRows > 0;
                if (pstmt == null) break block9;
            }
            catch (Throwable throwable) {
                try {
                    if (pstmt != null) {
                        try {
                            pstmt.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (SQLException e) {
                    System.err.println("Error updating server: " + e.getMessage());
                    return false;
                }
            }
            pstmt.close();
        }
        return bl;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public boolean deleteServer(Server server) {
        if (server.getID() <= 0) {
            return false;
        }
        this.create();
        try {
            String deleteAccountsSql = "DELETE FROM account WHERE serverId = ?";
            try (PreparedStatement pstmt2 = this.conn.prepareStatement("DELETE FROM account WHERE serverId = ?");){
                pstmt2.setInt(1, server.getID());
                pstmt2.executeUpdate();
            }
            String deleteServerSql = "DELETE FROM server WHERE id = ?";
            try (PreparedStatement pstmt = this.conn.prepareStatement("DELETE FROM server WHERE id = ?");){
                pstmt.setInt(1, server.getID());
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    this.conn.commit();
                    this.conn.close();
                    this.conn = null;
                    boolean bl2 = true;
                    return bl2;
                }
                this.conn.rollback();
                this.conn.close();
                this.conn = null;
                boolean bl = false;
                return bl;
            }
        }
        catch (SQLException e) {
            try {
                if (this.conn != null) {
                    this.conn.rollback();
                }
                this.conn.close();
                this.conn = null;
            }
            catch (SQLException ex) {
                System.err.println("Error during rollback: " + ex.getMessage());
            }
            System.err.println("Error deleting server and accounts: " + e.getMessage());
            return false;
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public int saveNewAccount(Server server, Account account) {
        this.create();
        String sql = "INSERT INTO account (serverId, username, password, isSavePassword, isUseSteamRelay, authType, timePlayed, lastLogon) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = this.conn.prepareStatement("INSERT INTO account (serverId, username, password, isSavePassword, isUseSteamRelay, authType, timePlayed, lastLogon) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", 1);){
            String currentTime = LocalDateTime.now().format(formatter);
            pstmt.setInt(1, server.getID());
            pstmt.setString(2, account.getUserName());
            pstmt.setString(3, account.getPwd());
            pstmt.setBoolean(4, account.isSavePwd());
            pstmt.setBoolean(5, account.getUseSteamRelay());
            pstmt.setInt(6, account.getAuthType());
            pstmt.setInt(7, account.getTimePlayed());
            pstmt.setString(8, account.getLastLogon());
            int affectedRows = pstmt.executeUpdate();
            this.conn.commit();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys();){
                    if (rs.next()) {
                        int result = rs.getInt(1);
                        this.conn.close();
                        this.conn = null;
                        int n = result;
                        return n;
                    }
                }
            }
            this.conn.close();
            this.conn = null;
            return -1;
        }
        catch (SQLException e) {
            System.err.println("Error saving new account: " + e.getMessage());
        }
        return -1;
    }

    public boolean updateAccount(Account account) {
        boolean bl;
        block9: {
            if (account.getID() <= 0) {
                return false;
            }
            this.create();
            String sql = "UPDATE account SET username = ?, password = ?, isSavePassword = ?, isUseSteamRelay = ?, authType = ?, timePlayed = ?, lastLogon = ?, playerFirstAndLastName = ? WHERE id = ?";
            PreparedStatement pstmt = this.conn.prepareStatement("UPDATE account SET username = ?, password = ?, isSavePassword = ?, isUseSteamRelay = ?, authType = ?, timePlayed = ?, lastLogon = ?, playerFirstAndLastName = ? WHERE id = ?");
            try {
                pstmt.setString(1, account.getUserName());
                pstmt.setString(2, account.getPwd());
                pstmt.setBoolean(3, account.isSavePwd());
                pstmt.setBoolean(4, account.getUseSteamRelay());
                pstmt.setInt(5, account.getAuthType());
                pstmt.setInt(6, account.getTimePlayed());
                pstmt.setString(7, account.getLastLogon());
                pstmt.setString(8, account.getPlayerFirstAndLastName());
                pstmt.setInt(9, account.getID());
                int affectedRows = pstmt.executeUpdate();
                this.conn.commit();
                this.conn.close();
                this.conn = null;
                boolean bl2 = bl = affectedRows > 0;
                if (pstmt == null) break block9;
            }
            catch (Throwable throwable) {
                try {
                    if (pstmt != null) {
                        try {
                            pstmt.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (SQLException e) {
                    System.err.println("Error updating account: " + e.getMessage());
                    return false;
                }
            }
            pstmt.close();
        }
        return bl;
    }

    public boolean deleteAccount(Account account) {
        boolean bl;
        block9: {
            if (account.getID() <= 0) {
                return false;
            }
            this.create();
            String sql = "DELETE FROM account WHERE id = ?";
            PreparedStatement pstmt = this.conn.prepareStatement("DELETE FROM account WHERE id = ?");
            try {
                pstmt.setInt(1, account.getID());
                int affectedRows = pstmt.executeUpdate();
                this.conn.commit();
                this.conn.close();
                this.conn = null;
                boolean bl2 = bl = affectedRows > 0;
                if (pstmt == null) break block9;
            }
            catch (Throwable throwable) {
                try {
                    if (pstmt != null) {
                        try {
                            pstmt.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (SQLException e) {
                    System.err.println("Error deleting account: " + e.getMessage());
                    return false;
                }
            }
            pstmt.close();
        }
        return bl;
    }

    public void setupLastSave() {
        lastSave = LocalDateTime.now();
    }

    public boolean updateAccountIconAndData(String serverAddress, int port, String username, ByteBuffer icon) {
        boolean bl;
        block8: {
            this.create();
            LocalDateTime currentTime = LocalDateTime.now();
            Duration duration = Duration.between(lastSave, currentTime);
            String sql = "UPDATE account\nSET \n    icon = ?,\n    timePlayed = ?,\n    lastLogon = ?\nWHERE \n    username = ? \n    AND serverId IN (\n        SELECT id \n        FROM server \n        WHERE ip = ? AND port = ?\n    );";
            PreparedStatement pstmt = this.conn.prepareStatement("UPDATE account\nSET \n    icon = ?,\n    timePlayed = ?,\n    lastLogon = ?\nWHERE \n    username = ? \n    AND serverId IN (\n        SELECT id \n        FROM server \n        WHERE ip = ? AND port = ?\n    );");
            try {
                pstmt.setBytes(1, icon.array());
                pstmt.setInt(2, (int)duration.toMinutes());
                pstmt.setString(3, currentTime.format(formatter));
                pstmt.setString(4, username);
                pstmt.setString(5, serverAddress);
                pstmt.setInt(6, port);
                int affectedRows = pstmt.executeUpdate();
                this.conn.commit();
                this.conn.close();
                this.conn = null;
                boolean bl2 = bl = affectedRows > 0;
                if (pstmt == null) break block8;
            }
            catch (Throwable throwable) {
                try {
                    if (pstmt != null) {
                        try {
                            pstmt.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (SQLException e) {
                    System.err.println("Error updating account: " + e.getMessage());
                    return false;
                }
            }
            pstmt.close();
        }
        return bl;
    }

    static {
        lastSave = LocalDateTime.now();
    }
}

