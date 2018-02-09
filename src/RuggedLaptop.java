import riot.database.RIOTDatabase;
import riot.server.DataReceptionServer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * The main program which will run on the Rugged Laptop. Rugged Laptop runs a Windows
 * environment.
 * Created by marianne on 8/04/17.
 */
public class RuggedLaptop {
    static DataReceptionServer serverThread;

    public static void main(String[] args) {
        try {
            // SETUP
            serverThread = new DataReceptionServer(65060,
                    RIOTDatabase.getNewConnection());

            // RUN
            new Thread(serverThread).start();

        } catch (IOException | SQLException ex) {
            ex.printStackTrace();
        }
    }
}
