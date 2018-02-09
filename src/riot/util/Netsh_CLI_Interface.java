package riot.util;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Acts as an interface between a program and the netsh command line program for Windows.
 */
public class Netsh_CLI_Interface {

    public static final int EXIT_OK = 0;
    static final String program = "CMD";

    /**
     * Configures the hosted network settings using netsh. Network name and password are trusted and should be
     * validated prior to calling this function if necessary. Space characters are acceptable.
     * @param networkName Name of the wireless network
     * @param password The password required to connect
     */
    public boolean configureHostedNetwork(String networkName, String password) throws IOException, InterruptedException {

        ProcessBuilder processBuilder = new ProcessBuilder(program, "-c",
                "netsh wlan set hostednetwork mode=allow ssid=\"" + networkName + "\" key=\"" + password + "\"");
        Process process = processBuilder.start();

        //TODO: Find out what possible error results can happen.
        int exitCode;
        if ((exitCode = process.waitFor()) != EXIT_OK) {
            System.err.println("set hostednetwork error code:" + exitCode);
            System.err.println("\t" + processBuilder.command());
            return false;
        }

        return true;
    }

    /**
     * Runs the start hosted network command using netsh.
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean startHostedNetwork() throws IOException, InterruptedException {

        ProcessBuilder processBuilder = new ProcessBuilder(program, "-c",
                "netsh wlan start hostednetwork");
        Process process = processBuilder.start();

        //TODO: Find out what possible error results can happen.
        int exitCode;
        if ((exitCode = process.waitFor()) != EXIT_OK) {
            System.err.println("set hostednetwork error code:" + exitCode);
            System.err.println("\t" + processBuilder.command());
            return false;
        }

        return true;
    }

}
