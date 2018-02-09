package riot.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CLI_Interface_Utils {
    /**
     * Checks if a process has returned the expected string.
     * @param process The process to check
     * @param output The expected output
     * @return
     */
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


}
