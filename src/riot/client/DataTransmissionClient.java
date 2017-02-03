package riot.client;

import riot.database.RIOTDatabase;
import riot.network.InvalidNetworkAdapterException;
import riot.network.LinuxWifiConnection;

import java.io.*;
import java.net.Socket;
import java.sql.*;

/**
 * Created by marianne on 19/01/17.
 */
public class DataTransmissionClient implements Runnable {
    LinuxWifiConnection mConnection;

    Connection mDBConnection;

    String mServerIP = "169.254.72.1";
    int mServerPort = 65060;

    public DataTransmissionClient()
            throws IOException, InvalidNetworkAdapterException, SQLException {
        System.out.println("Preparing DataTransmissionClient...");
        mConnection = new LinuxWifiConnection("wlp9s0", "riot-waikato-072A", "riotwaikato");
        mDBConnection = DriverManager.getConnection("jdbc:sqlite:" + RIOTDatabase.DB_NAME,
                RIOTDatabase.config.toProperties());
    }

    public DataTransmissionClient(LinuxWifiConnection connection, String serverIP,
                                  int serverPort)
            throws IOException, SQLException {

        System.out.println("Preparing DataTransmissionClient...");
        if (connection == null) {
            throw new NullPointerException();
        }
        mConnection = connection;
        mServerIP = serverIP;
        mServerPort = serverPort;
        mDBConnection = DriverManager.getConnection("jdbc:sqlite:" + RIOTDatabase.DB_NAME,
                RIOTDatabase.config.toProperties());
    }

    /**
     * 1. Establish network connection
     * 2. Connect to server socket.
     * 3. Send data from SQLite3 database to server.
     */
    @Override
    public void run() {
        try {
            // check database structure is correct
            try {
                if (!RIOTDatabase.verifyDatabaseStructure(mDBConnection)) {
                    RIOTDatabase.createTables(mDBConnection);
                }
            } catch (SQLException e) {
                System.err.println("Could not verify database structure...");
                closeDatabaseConnection();
                return;
            }

            // connect to server
            while (true) {
                while (!mConnection.isConnected()) {
                    try {
                        if (mConnection.establishConnection()) {
                            System.out.println("Connecting to target...");
                            Thread.sleep(5000);
                        }
                    } catch (IOException ex) {
                        System.err.println("Could not connect to target network: " + mConnection.mTargetSSID);
                    }
                }
                System.out.println("Connected to target Wifi AP.");

                Socket socket = null;
                try {
                    Statement statement = mDBConnection.createStatement();
                    System.out.println("Querying database.");
                    int count = RIOTDatabase.getCount(mDBConnection, "lux");
                    System.out.println("DataTransmissionClient found " + count + " rows.");
                    if (count > 0) {
                        String deleteFromLux = "DELETE FROM lux WHERE id = ?";
                        String deleteFromEntry = "DELETE FROM entry WHERE id = ?";
                        PreparedStatement deleteLuxStmt = mDBConnection.prepareStatement(deleteFromLux);
                        PreparedStatement deleteEntryStmt = mDBConnection.prepareStatement(deleteFromEntry);
                        System.out.println("Opening socket to server.");
                        socket = new Socket(mServerIP, mServerPort);
                        System.out.println("Opened socket.");
                        ResultSet results = statement.executeQuery(RIOTDatabase.LUX_QUERY);
                        PrintWriter socketWriter = new PrintWriter(socket.getOutputStream(), true);
                        while (results.next()) {
                            socketWriter.println("LUX " + results.getString("dev_id") +
                                    " " + results.getInt("id") +
                                    " " + results.getFloat("lux"));
                            socketWriter.flush();
                            int id = results.getInt("id");
                            System.out.println("Deleting id " + id);
                            deleteLuxStmt.setInt(1, id);
                            deleteLuxStmt.execute();
                            deleteEntryStmt.setInt(1, id);
                            deleteEntryStmt.execute();
                        }
                    }
                } catch (IOException ex) {
                    System.err.println("Could not establish connection to server...");
                    ex.printStackTrace();
                } catch (SQLException ex) {
                    System.err.println("Could not execute database query...");
                    closeDatabaseConnection();
                    ex.printStackTrace();
                    return;
                } finally {
                    try {
                        if (socket != null && !socket.isClosed()) {
                            socket.close();
                        }
                    } catch (Exception ex) {
                        // Ignore exceptions
                    }
                }
                Thread.sleep(5000);
            }
        } catch (InterruptedException ex) {
            closeDatabaseConnection();
            System.err.println("DataTransmissionClient was interrupted.");
            return;
        }
    }

    /**
     * Closes connection to the database.
     */
    void closeDatabaseConnection() {
        try {
            if (mDBConnection != null && !mDBConnection.isClosed()) {
                mDBConnection.close();
            }
        } catch (Exception ex) {
            // ignore as thread will end soon
        }
    }

    public static void main(String[] args) {
        try {
            LinuxWifiConnection connection = new LinuxWifiConnection("wlp9s0",
                    "Fletcher", "porkhouse");
            (new Thread(new DataTransmissionClient(connection, "", 0))).start();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (InvalidNetworkAdapterException ex) {
            ex.printStackTrace();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
