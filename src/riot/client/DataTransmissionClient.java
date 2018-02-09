package riot.client;

import riot.database.RIOTDatabase;
import riot.network.LinuxWifiConnection;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.*;

/**
 * Created by marianne on 19/01/17.
 */
public class DataTransmissionClient implements Runnable {

    LinuxWifiConnection mNetworkConnection;

    String mServerIP = "169.254.72.1";
    int mServerPort = 65060;

    Connection mDBConnection;

    // Queries
    String deleteFromLux = "DELETE FROM lux WHERE id = ?";
    String deleteFromEntry = "DELETE FROM entry WHERE id = ?";

    /**
     * Sets up a client thread for transmitting data to the central server.
     * <p>
     * None of the arguments can be null and the server port must be between
     * 1 and 65535.
     *
     * @param networkConnection  The parameters of the network connection which
     *                           will be used to connect to the server.
     * @param serverIP           The IP address of the server.
     * @param serverPort         The port on the server that the client will connect
     *                           to.
     * @param databaseConnection A connection to the database containing the
     *                           data to be transferred.
     */
    public DataTransmissionClient(LinuxWifiConnection networkConnection,
                                  String serverIP,
                                  int serverPort,
                                  Connection databaseConnection) {

        if (networkConnection == null) {
            throw new NullPointerException("Network connection cannot be null.");
        }
        if (serverIP == null) {
            throw new NullPointerException("Server IP cannot be null.");
        }
        //TODO: Check if string is IP address.
        if (serverPort < 1 || serverPort > 65535) {
            throw new IllegalArgumentException("Server port was not between 1 and 65535.");
        }
        if (databaseConnection == null) {
            throw new NullPointerException("Database connection cannot be null.");
        }

        mNetworkConnection = networkConnection;
        mServerIP = serverIP;
        mServerPort = serverPort;
        mDBConnection = databaseConnection;
    }

    /**
     * Used only in the case that someone forgot to release the resources
     * manually. This method may never be called so do not rely on it.
     */
    @Override
    protected void finalize() {
        try {
            if (mDBConnection != null) {
                mDBConnection.close();
            }
        } catch (SQLException e) {
            // Ignore problems when finalizing.
        }
    }

    /**
     * Cleans up resources when we no longer need this instance.
     * Try to call this whenever the thread will return.
     */
    void releaseResources() {
        try {
            if (mDBConnection != null) {
                mDBConnection.close();
            }
        } catch (SQLException e) {
            // Ignore problems when finalizing.
        }
    }

    /**
     * Checks the connection to the database and that the structure of the
     * database is what is expected.  If this function returns true, it
     * should be safe to execute queries.
     *
     * @return
     */
    boolean databaseStructureIsCorrect() {

        // Verify database structure.
        try {
            if (RIOTDatabase.createTables(mDBConnection)) {
                return true;
            }
        } catch (SQLException ex) {
            System.err.println("DataTransmissionClient: Could not verify database structure.");
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * 1. Check database structure is correct.
     * 2. Establish network connection
     * 3. Connect to serverThread socket.
     * 4. Send data from SQLite3 database to serverThread.
     */
    @Override
    public void run() {

        // If the database cannot be read or the structure is wrong there is
        // nothing this thread can do.
        if (!databaseStructureIsCorrect()) {
            releaseResources();
            return;
        }

        while (true) {
            try { // catches InterruptedException

                // Check wireless connection
                while (!mNetworkConnection.isConnected()) {
                    if (mNetworkConnection.establishConnection()) {
                        Thread.sleep(5000);
                    }
                }

                int count = RIOTDatabase.getCount(mDBConnection, "lux");
                System.out.println("DataTransmissionClient found " + count + " rows.");
                if (count > 0) {

                    // Declare all AutoCloseable resources needed to send data from
                    // database to serverThread.
                    try (// Socket-related declarations
                         Socket socket = new Socket(mServerIP, mServerPort);
                         PrintWriter socketWriter = new PrintWriter(socket.getOutputStream(), true);
                         BufferedReader socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                         // Database-related declarations
                         Statement luxQuery = mDBConnection.createStatement();
                         ResultSet results = luxQuery.executeQuery(RIOTDatabase.LUX_QUERY);
                         PreparedStatement deleteLuxStmt = mDBConnection.prepareStatement(deleteFromLux);
                         PreparedStatement deleteEntryStmt = mDBConnection.prepareStatement(deleteFromEntry);
                    ) {
                        socket.setSoTimeout(1000);

                        // Cursor starts at position prior to first row.
                        while (results.next()) {

                            // Transmit data.
                            socketWriter.println("LUX " + results.getString("dev_id") +
                                    " " + results.getInt("id") +
                                    " " + results.getFloat("lux"));
                            socketWriter.flush();

                            // TODO: Implement an ACK from server.
                            try {
                                socketReader.readLine();
                            } catch (SocketTimeoutException ex) {
                                System.out.println("Did not receive an acknowledgement from the server.");
                            }

                            // Delete data.
                            int id = results.getInt("id");
                            System.out.println("Deleting id " + id);
                            deleteLuxStmt.setInt(1, id);
                            deleteLuxStmt.execute();
                            deleteEntryStmt.setInt(1, id);
                            deleteEntryStmt.execute();
                        }
                    }
                }

                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                releaseResources();
                return;
            } catch (IOException ex) {
                releaseResources();
                return;
            } catch (SQLException ex) {
                releaseResources();
                return;
            }
        }

    }

    /**
     * FOR TESTING ONLY.
     *
     * @param args
     */
    public static void main(String[] args) {

    }
}
