/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import zombie.core.logger.ExceptionLogger;

public class PZSQLUtils {
    public static void init() {
        try {
            Class.forName("org.sqlite.JDBC");
        }
        catch (ClassNotFoundException e) {
            ExceptionLogger.logException(e);
            System.exit(1);
        }
        PZSQLUtils.setupSqliteVariables();
    }

    private static void setupSqliteVariables() {
        if (System.getProperty("os.name").contains("OS X")) {
            System.setProperty("org.sqlite.lib.path", PZSQLUtils.searchPathForSqliteLib("libsqlitejdbc.dylib"));
            System.setProperty("org.sqlite.lib.name", "libsqlitejdbc.dylib");
        } else if (System.getProperty("os.name").startsWith("Win")) {
            System.setProperty("org.sqlite.lib.path", PZSQLUtils.searchPathForSqliteLib("sqlitejdbc.dll"));
            System.setProperty("org.sqlite.lib.name", "sqlitejdbc.dll");
        } else {
            System.setProperty("org.sqlite.lib.path", PZSQLUtils.searchPathForSqliteLib("libsqlitejdbc.so"));
            System.setProperty("org.sqlite.lib.name", "libsqlitejdbc.so");
        }
    }

    private static String searchPathForSqliteLib(String library) {
        for (String path : System.getProperty("java.library.path", "").split(File.pathSeparator)) {
            File file = new File(path, library);
            if (!file.exists()) continue;
            return path;
        }
        return "";
    }

    public static Connection getConnection(String absolutePath) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + absolutePath);
    }
}

