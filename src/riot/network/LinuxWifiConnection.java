package riot.network;

import riot.util.DebuggingStatement;
import riot.util.WPA_CLI_Interface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Monitors a Wifi connection using the Linux command line.
 * Created by marianne on 19/01/17.
 */
public class LinuxWifiConnection extends LinuxNetworkConnection {

    public final String mAdapterName;
    public final String mTargetSSID;    // SSID that we wish to monitor connection to
    public final String mPSK;

    /**
     * Creates a wifi connection instance with a desired AP SSID to connect to
     * and monitors the connection status.
     *
     * @param adapterName
     * @param targetSSID
     * @throws InvalidNetworkAdapterException
     */
    public LinuxWifiConnection(String adapterName, String targetSSID, String PSK)
            throws InvalidNetworkAdapterException {

        if (isValidInterfaceName(adapterName)) {
            mAdapterName = adapterName;
            mTargetSSID = targetSSID;
            mPSK = PSK;
        } else {
            throw new InvalidNetworkAdapterException();
        }
    }

    /**
     * Gets the SSID of the current wireless connection.  Will return
     * null if not connected.
     *
     * @return
     * @throws IOException if the process cannot start
     */
    public String getConnectionSSID() {
        String line = null;
        ProcessBuilder getWirelessSSID = new ProcessBuilder("/bin/sh", "-c",
                "iwgetid " + mAdapterName + " -r");

        try {
            Process process = getWirelessSSID.start();

            try (BufferedReader processReader =
                         new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                line = processReader.readLine();
            } catch (IOException ex) {
                ex.printStackTrace();
                DebuggingStatement.println("Could not read from process.");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            DebuggingStatement.println("Could not start process.");
        }

        return line;
    }

    /**
     * Checks if the SSID of the network that the adapter is connected to
     * is the same as the target SSID of this instance.
     *
     * @return true if they are the same
     */
    public boolean isConnected() {
        return getConnectionSSID().equals(mTargetSSID);
    }

    public boolean establishConnection() throws InterruptedException, IOException {
        int index = WPA_CLI_Interface.getIndexOfNetwork(mAdapterName, mTargetSSID);  // network index
        if (index == -1) {
            System.out.println("Adding network " + mTargetSSID);
            // add network to wpa_supplicant.conf file
            index = WPA_CLI_Interface.add_network(mAdapterName);
            if (index == -1) {
                System.err.println("Failed to add network to wpa_supplicant.conf");
                System.err.println("\t" + mAdapterName);
                return false;
            }
            if (!WPA_CLI_Interface.set_network(mAdapterName, index, "ssid", mTargetSSID)) {
                System.err.println("Failed to set network SSID...");
                return false;
            }
            if (!WPA_CLI_Interface.set_network(mAdapterName, index, "psk", mPSK)) {
                System.err.println("Failed to set pre-shared key...");
                return false;
            }
        }

        String SSID = getConnectionSSID();
        if (!isConnected() && !SSID.isEmpty()) {
            System.out.println("Disabling network: " + SSID);
            if (!WPA_CLI_Interface.disable_network(mAdapterName, SSID)) {
                System.err.println("Could not disable network: " + SSID);
            }
        }

        System.out.println("Enabling network " + mTargetSSID);
        if (!WPA_CLI_Interface.enable_network(mAdapterName, index)) {
            System.err.println("Network could not be enabled...");
            return false;
        }

        return true;
    }

    public static void main(String[] args) {
        System.out.println(getInterfaceNames());
        try {
            LinuxWifiConnection test = new LinuxWifiConnection("wlp9s0", "Fletcher", "porkhouse");
            System.out.println(test.isConnected());
            System.out.println("Connected to: " + test.getConnectionSSID());

            ArrayList<WPA_CLI_Interface.Network> networkList = WPA_CLI_Interface.list_networks("wlp9s0");
            for (WPA_CLI_Interface.Network network : networkList) {
                System.out.println(network.mNetworkID + " : " + network.mSSID + " : " + network.mBSSID + " : " + network.mFlags);
            }
        } catch (InvalidNetworkAdapterException ex) {
            ex.printStackTrace();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}
