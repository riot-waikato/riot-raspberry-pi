package riot.database;

import org.sqlite.SQLiteConfig;

import java.io.IOException;
import java.sql.*;

/**
 * Created by marianne on 24/01/17.
 */
public class RIOTDatabase {

    static class Table {
        public String mName;
        public String mCreateStatement;

        public Table(String name, String createStatement) {
            mName = name;
            mCreateStatement = createStatement;
        }
    }

    public static final String DB_NAME = "tester2.db";

    static final String USER_TABLE = "CREATE TABLE user(name TEXT NOT NULL," +
            "user_id INT PRIMARY KEY NOT NULL);";
    static final String DEV_TABLE = "CREATE TABLE dev(_id INT NOT NULL," +
            "dev_id TEXT PRIMARY KEY NOT NULL," +
            "foreign key(_id) references user(id));";
    static final String ENTRY_TABLE = "CREATE TABLE entry (entry_date DATE NOT NULL," +
            "real_date DATE NOT NULL," +
            "dev_id TEXT NOT NULL," +
            "id INT PRIMARY KEY NOT NULL," +
            "foreign key(dev_id) references dev(dev_id));";
    static final String MOTION_TABLE = "CREATE TABLE motion(gx REAL NOT NULL," +
            "gy REAL NOT NULL," +
            "gz REAL NOT NULL," +
            "ax REAL NOT NULL," +
            "ay REAL NOT NULL," +
            "az REAL NOT NULL," +
            "mx REAL NOT NULL," +
            "my REAL NOT NULL," +
            "int REAL NOT NULL," +
            "entry_id INT NOT NULL," +
            "foreign key(entry_id) references entry(id));";
    public static final String LUX_TABLE = "CREATE TABLE lux(lux REAL NOT NULL," +
            "id INT NOT NULL," +
            "foreign key(id) references entry(id));";

    public static final String LUX_QUERY = "SELECT real_date, dev_id, lux, lux.id " +
            "from lux inner join entry on lux.id = entry.id;";
    public static final String LUX_COUNT = "SELECT COUNT (*) FROM lux";

    public static final String ENTRY_INSERT = "INSERT INTO entry VALUES (%s, %s, %s);";
    public static final String LUX_INSERT = "INSERT INTO lux VALUES (%s, %s)";

    /**
     * The tables required in the database and their corresponding
     * CREATE statements.
     */
    static Table[] tables = {
            new Table("user", USER_TABLE),
            new Table("dev", DEV_TABLE),
            new Table("entry", ENTRY_TABLE),
            new Table("motion", MOTION_TABLE),
            new Table("lux", LUX_TABLE)
    };

    /**
     * Pre-configured settings used when connecting to the database.
     */
    public static SQLiteConfig config = new SQLiteConfig();

    static {
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.enforceForeignKeys(true);
    }

    public static boolean verifyDatabaseStructure(Connection connection) throws SQLException {
        for (Table table : tables) {
            if (!tableExists(connection, table.mName)) {
                return false;
            }
        }
        return true;
    }

    public static boolean tableExists(Connection connection, String tableName) throws SQLException {
        if (connection != null && !connection.isClosed()) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet res = metaData.getTables(null, null,
                    tableName, null);

            // getTables will return all tables that contain table name so check them all
            while (res.next()) {
                if (res.getString("TABLE_NAME").matches(tableName)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean createTables(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        for (Table table : tables) {
            if (!tableExists(connection, table.mName)) {
                System.out.println("Creating table " + table.mName);
                try {
                    statement.executeUpdate(table.mCreateStatement);
                } catch (SQLException ex) {
                    System.err.println("Could not create table: " + table.mName);
                }
            }
        }
        return true;
    }

    /**
     * Counts the number of rows in a table.
     *
     * @param connection
     * @param tableName
     * @return
     */
    public static int getCount(Connection connection, String tableName) throws SQLException {
        int count = 0;
        if (connection != null && !connection.isClosed()) {
            Statement statement = connection.createStatement();
            ResultSet results = statement.executeQuery("SELECT COUNT(*) FROM " + tableName + ";");
            while (results.next()) {
                count = results.getInt(1);
                System.out.println("count: " + count);
            }
        }

        return count;
    }

    /**
     * Deletes a row with the given ID number in the 'id' column from a table.
     * Can only be used on tables that include an 'id' column.
     * The SQLite JDBC does not implement row deletion.  This must be done
     * manually.
     * @return True if the process exited successfully.
     */
    public static boolean deleteRowByID(String tableName, int ID) throws InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c",
                "sqlite3 " + DB_NAME + " 'BEGIN; PRAGMA JOURNAL=WAL; DELETE FROM " + tableName +
                        " WHERE id = " + ID + "'; END TRANSACTION;");
        processBuilder.inheritIO();
        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            // exit code is 0 if process completed successfully
            if (exitCode == 0) {
                return true;
            }
        } catch (IOException ex) {
            // TODO: Remove stack trace after testing.
            ex.printStackTrace();
        }
        return false;
    }
}
