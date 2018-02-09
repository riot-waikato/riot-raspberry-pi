package riot.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;

/**
 * Represents lux data received from a sensor and the query used to insert it into
 * the database.
 * Created by marianne on 24/01/17.
 */
public class LuxData extends Data {
    static final String mPacketType = "lux";
    static final int mPacketLength = 5;
    static final String mLuxInsertQuery = "INSERT INTO lux VALUES (?)";
    static final String mEntryInsertQuery = "INSERT INTO entry VALUES (?, ?, ?, 0)";

    String mDeviceID;
    float mValue;
    long mTimestamp;

    public LuxData(String deviceID, float value, int sequence, long timestamp) {
        mDeviceID = deviceID;
        mValue = value;
        mSequence = sequence;
        mTimestamp = timestamp;
    }

    /**
     * Inserts data into the given database using PreparedStatements.
     *
     * @param connection
     * @throws SQLException See https://docs.oracle.com/javase/tutorial/jdbc/basics/transactions.html
     */
    public void insertIntoDatabase(Connection connection) {

        PreparedStatement luxStatement = null;
        PreparedStatement entryStatement = null;

        // Both statements need to be committed, otherwise a rollback will
        // be neccessary.
        try {
            connection.setAutoCommit(false);

            // lux table insert
            luxStatement = connection.prepareStatement(mLuxInsertQuery);
            luxStatement.setFloat(1, mValue);
            luxStatement.executeUpdate();

            entryStatement = connection.prepareStatement(mEntryInsertQuery);
            entryStatement.setLong(1, getCurrentTimeStamp());
            entryStatement.setLong(2, mTimestamp);
            entryStatement.setString(3, mDeviceID);
            entryStatement.executeUpdate();

            // commit both statements
            connection.commit();
        } catch (SQLException ex) {
            // rollback any transactions that have been done
            if (connection != null) {
                try {
                    System.err.print("Transaction is being rolled back.");
                    connection.rollback();
                } catch (SQLException ex2) {
                    System.err.print("Could not rollback transactions.");
                    ex2.printStackTrace();
                }
            }
        } finally {
            try {
                if (luxStatement != null) {
                    luxStatement.close();
                }
            } catch (SQLException ex) {
                // ignore: ending transaction
            }

            try {
                if (entryStatement != null) {
                    entryStatement.close();
                }
            } catch (SQLException ex) {
                // ignore: ending transaction
            }

            try {
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                System.err.println("Could not re-enable auto-commit.");
            }
        }
    }

    // TODO: Find better place for this function.
    long getCurrentTimeStamp() {
        Calendar calendar = Calendar.getInstance();
        return calendar.getTimeInMillis() / 1000L;
    }
}
