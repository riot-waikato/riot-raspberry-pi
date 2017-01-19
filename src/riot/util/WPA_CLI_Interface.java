package riot.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Runs wpa_cli commands on Linux.  Assumes that the wpa_supplicant.conf file
 * has been modified to allowed the user access (default is root only).
 * Created by marianne on 19/01/17.
 */
public class WPA_CLI_Interface {

    public static class Network {
        public final int mNetworkID;
        public final String mSSID;
        public final String mBSSID;
        public final String mFlags;

        Network(int networkID, String SSID, String BSSID, String flags) {
            mNetworkID = networkID;
            mSSID = SSID;
            mBSSID = BSSID;
            mFlags = flags;
        }

        public String toString() {
            return mNetworkID + " : " + mSSID + " : " + mBSSID + " : " + mFlags;
        }
    }

    /**
     * Lists networks using "wpa_cli list_networks" and returns the list of
     * networks.
     * @param interfaceName
     * @return
     */
    public static ArrayList<Network> list_networks(String interfaceName) throws InterruptedException {
        ArrayList<Network> networkList = new ArrayList<>();
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c",
                "wpa_cli list_networks -i " + interfaceName);
        try {
            Process process = processBuilder.start();
            BufferedReader processReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = processReader.readLine(); // discard header line
            while ((line = processReader.readLine()) != null) {
                networkList.add(parseNetworksListLine(line));
            }
                int exitCode;
                if ((exitCode = process.waitFor()) != 0) {
                    System.err.println("list_networks exited with: " + exitCode);
                }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return networkList;
    }

    public static boolean remove_network(String interfaceName, int networkNumber) throws InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", "wpa_cli -i "
                + interfaceName + " remove_network " + networkNumber);
        processBuilder.inheritIO();
        try {
            Process process = processBuilder.start();
            int exitCode;
            if ((exitCode = process.waitFor()) != 0) {
                System.err.println("remove_network exited with: " + exitCode);
                return false;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return true;
    }

    /**
     * Given the result of a list_networks line, parses the line
     * and creates a new Network instance representing it.
     * @param line
     * @return
     */
    private static Network parseNetworksListLine(String line) {
        String[] lineSplit = line.split("\t");
        if (lineSplit.length < 3) {
            return null;
        }
        int networkID = Integer.parseInt(lineSplit[0]);
        String SSID = lineSplit[1];
        String BSSID = lineSplit[2];

        // flags may not be present for a network
        String flags = null;
        if (lineSplit.length > 3) {
            flags = lineSplit[3];
        }

        return new Network(networkID, SSID, BSSID, flags);
    }

    /**
     * Returns the index of the first network with the given SSID
     * in the list of configured networks.  The index can be used to enable/disable
     * the network connection in other commands.
     * @return Index of the network or -1 if not found
     */
    public static int getIndexOfNetwork(String interfaceName, String SSID) throws InterruptedException {
        ArrayList<Network> networkList = list_networks(interfaceName);
        for (Network network : networkList) {
            if (network.mSSID.equals(SSID)) {
                return network.mNetworkID;
            }
        }

        return -1;
    }

    /**
     * For testing.
     * @param args
     */
    public static void main(String[] args) {
        try {
            ArrayList<Network> networkList = list_networks("wlan1");
            for (Network network : networkList) {
                System.out.println(network);
            }

            System.out.println(getIndexOfNetwork("wlan1", "riot-waikato-072A"));
        } catch (InterruptedException ex) {
            System.err.println("Execution interrupted.");
        }
    }
}
