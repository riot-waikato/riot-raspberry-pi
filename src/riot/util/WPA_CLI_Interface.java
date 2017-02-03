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

    public static final int EXIT_OK = 0;

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
     *
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
                Network network = parseNetworksListLine(line);
                if (network != null) {
                    networkList.add(network);
                }
            }
            int exitCode;
            if ((exitCode = process.waitFor()) != 0) {
                System.err.println("list_networks exit code: " + exitCode);
                System.err.println("\t" + processBuilder.command());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return networkList;
    }

    /**
     * Adds an empty network entry to the wpa_supplicant.conf file and
     * returns the index number.  If an error occurs, the index is -1.
     *
     * @param interfaceName
     * @return
     */
    public static int add_network(String interfaceName)
            throws InterruptedException, IOException {
        int index = -1;
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", "wpa_cli -i "
                + interfaceName + " add_network");
        Process process = processBuilder.start();
        BufferedReader processReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = processReader.readLine();

        // should return an integer that is the index of the new network entry
        try {
            int temp = Integer.parseInt(line);
            // confirm process success
            if (EXIT_OK == process.waitFor()) {
                index = temp;
            }
        } catch (NumberFormatException e) {
            DebuggingStatement.println("Could not parse as int.");
            DebuggingStatement.println("add_network returned: " + line);
        }

        return index;
    }

    /**
     * Sets the property for the network at the given index number to the
     * value given.
     * @param interfaceName
     * @param networkNumber
     * @param property
     * @param value
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    public static boolean set_network(String interfaceName, int networkNumber,
                                      String property, String value)
            throws InterruptedException, IOException {
        /**
         * WORKAROUND: Setting properties does not return OK unless single quotes
         * surround the double quotes around the value.
         */
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c",
                "wpa_cli -i " + interfaceName + " set_network " + networkNumber +
                " " + property + " \'\"" + value + "\"\'");
        Process process = processBuilder.start();
        if (!isProcessOutput(process, "OK")) {
            System.err.println("set_network failed");
            System.err.println("\t" + processBuilder.command());
        }
        int exitCode;
        if ((exitCode = process.waitFor()) != EXIT_OK) {
            System.err.println("set_network exit code: " + exitCode);
            System.err.println("\t" + processBuilder.command());
            return false;
        }

        return true;
    }

    public static boolean enable_network(String interfaceName, int networkNumber)
            throws InterruptedException, IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c",
                "wpa_cli -i " + interfaceName + " enable_network " + networkNumber);
        Process process = processBuilder.start();
        if (!isProcessOutput(process, "OK")) {
            System.err.println("enable_network failed");
            System.err.println("\t" + processBuilder.command());
        }
        int exitCode;
        if ((exitCode = process.waitFor()) != EXIT_OK) {
            System.err.println("enable_network exit code: " + exitCode);
            System.err.println("\t" + processBuilder.command());
            return false;
        }

        return true;
    }

    /**
     * Removes the network with the given index from the default
     * wpa_supplicant.conf file.
     * @param interfaceName
     * @param networkNumber
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    public static boolean remove_network(String interfaceName, int networkNumber)
            throws InterruptedException, IOException {

        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c",
                "wpa_cli -i " + interfaceName + " remove_network " + networkNumber);
        Process process = processBuilder.start();
        if (!isProcessOutput(process, "OK")) {
            System.err.println("remove_network failed");
            System.err.println("\t" + processBuilder.command());
        }
        int exitCode;
        if ((exitCode = process.waitFor()) != EXIT_OK) {
            System.err.println("remove_network exit code: " + exitCode);
            System.err.println("\t" + processBuilder.command());
            return false;
        }

        return true;
    }

    /**
     * Given the result of a list_networks line, parses the line
     * and creates a new Network instance representing it.
     *
     * @param line
     * @return
     */
    private static Network parseNetworksListLine(String line) {
        String[] lineSplit = line.split("\t");
        if (lineSplit.length < 3) {
            System.out.println(line);
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
     *
     * @return Index of the network or -1 if not found
     */
    public static int getIndexOfNetwork(String interfaceName, String SSID) throws InterruptedException {
        ArrayList<Network> networkList = list_networks(interfaceName);
        for (Network network : networkList) {
            if (network != null && network.mSSID != null && network.mSSID.equals(SSID)) {
                return network.mNetworkID;
            }
        }

        return -1;
    }

    /**
     * For testing.
     *
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

    public static boolean isProcessOutput(Process process, String output) {
        try (BufferedReader processReader =
                     new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = processReader.readLine();
            if (line != null && line.matches(output)) {
                return true;
            }

            return false;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static void removeAllNetworks(String interfaceName)
            throws InterruptedException, IOException {
        ArrayList<Network> networks = list_networks(interfaceName);
        for (Network network: networks) {
            System.out.println("Removing network " + network.mNetworkID);
            remove_network(interfaceName, network.mNetworkID);
        }
    }

    public static boolean disable_network(String interfaceName, int networkNumber)
            throws InterruptedException, IOException {
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c",
                    "wpa_cli -i " + interfaceName + " disable_network " + networkNumber);
            Process process = processBuilder.start();
            if (!isProcessOutput(process, "OK")) {
                System.err.println("disable_network failed");
                System.err.println("\t" + processBuilder.command());
            }
            int exitCode;
            if ((exitCode = process.waitFor()) != EXIT_OK) {
                System.err.println("disable_network exit code: " + exitCode);
                System.err.println("\t" + processBuilder.command());
                return false;
            }

            return true;
    }

    /**
     * Disables the network with the given SSID.  May not work if there are multiple
     * networks with the same SSID.
     * @param interfaceName
     * @param networkName
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    public static boolean disable_network(String interfaceName, String networkName)
        throws InterruptedException, IOException {
        int networkNum = getIndexOfNetwork(interfaceName, networkName);

        return disable_network(interfaceName, networkNum);
    }
}
