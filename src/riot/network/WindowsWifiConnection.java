package riot.network;

import java.io.IOException;

/**
 * Designed to handle wifi connections between components of the RIOT project when run on a Windows platform.
 */
public class WindowsWifiConnection extends NetworkConnection {
    @Override
    public boolean isConnected() throws InterruptedException, IOException {
        return false;
    }

    @Override
    public boolean establishConnection() throws InterruptedException, IOException {
        return false;
    }
}
