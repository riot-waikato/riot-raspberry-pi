package riot.database;

import org.sqlite.SQLiteConfig;

import java.io.IOException;
import java.sql.*;

/**
 * Represents the RIOT database which is present on any devices that store sensor data.  Contains
 * table definitions and default configuration.
 * <p>
 * Requires a JDBC to connect to the database. Should work on any platform as long as
 * a JDBC exists.
 * <p>
 * Tested with this one on Raspberry Pi and Windows:
 * https://github.com/xerial/sqlite-jdbc
 * <p>
 * Created by marianne on 24/01/17.
 */
public class RIOTDatabase {

    /**
     * Get a new connection to the default RIOT database on this device with the correct
     * configuration.
     *
     * @return
     * @throws SQLException
     */
    public static Connection getNewConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + DB_NAME, config.toProperties());
    }

    static class Table {
        public String mName;
        public String mCreateStatement;

        public Table(String name, String createStatement) {
            mName = name;
            mCreateStatement = createStatement;
        }
    }

    public static final String DB_NAME = "RIOTDatabase.db";

    static final String USER_TABLE = "CREATE TABLE IF NOT EXISTS user(name TEXT NOT NULL," +
            "user_id INT PRIMARY KEY NOT NULL);";
    static final String DEV_TABLE = "CREATE TABLE IF NOT EXISTS dev(_id INT NOT NULL," +
            "dev_id TEXT PRIMARY KEY NOT NULL," +
            "foreign key(_id) references user(id));";
    static final String ENTRY_TABLE = "CREATE TABLE IF NOT EXISTS entry (entry_date DATE NOT NULL," +
            "real_date DATE NOT NULL," +
            "dev_id TEXT NOT NULL," +
            "id INT PRIMARY KEY NOT NULL," +
            "foreign key(dev_id) references dev(dev_id));";
    static final String MOTION_TABLE = "CREATE TABLE IF NOT EXISTS motion(gx REAL NOT NULL," +
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
    public static final String LUX_TABLE = "CREATE TABLE IF NOT EXISTS lux(lux REAL NOT NULL," +
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
     * Sets journal mode as WRITE AHEAD LOGGING and enforces foreign keys.
     */
    static SQLiteConfig config = new SQLiteConfig();

    static {
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.enforceForeignKeys(true);
    }

    /**
     * Runs all create table statements. The create statements should all include
     * 'IF NOT EXISTS' to avoid unnecessary errors.
     * @param connection
     * @return
     * @throws SQLException
     */
    public static boolean createTables(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        for (Table table : tables) {
            try {
                statement.executeUpdate(table.mCreateStatement);
            } catch (SQLException ex) {
                System.err.println("Could not create table: " + table.mName);
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
}
