package riot.util;

/**
 * Created by marianne on 18/01/17.
 */
public class DebuggingStatement {

    static final boolean DEBUG = true;

    public static void println(String s) {
        if (DEBUG) {
            System.out.println(s);
        }
    }
}
