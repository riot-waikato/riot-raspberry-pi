package riot.data;

/**
 * Examines a line of data and interprets it, wrapping it in the appropriate
 * data class.
 *
 * Supported packet types:
 * LUX
 *
 * Created by marianne on 1/04/17.
 */
public class DataFactory {

    /**
     * Returns the appropriate data type by examining the format of the
     * packet.
     * @param packet
     * @return
     */
    public static Data getData(String packet) {
        String[] split = packet.split(" ");

        // check if lux packet
        if (split[0].compareTo("lux") == 0
                && split.length == LuxData.mPacketLength) {

            return new LuxData(split[1], Float.valueOf(split[2]),
                    Integer.valueOf(split[3]), Long.valueOf(split[4]));
        } else {
            System.err.println("Could not determine packet type.");
            System.err.println("\t" + packet);
            return null;
        }
    }
}
