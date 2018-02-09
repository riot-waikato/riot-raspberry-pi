package riot.util;

/**
 * Indicates that user input did not match a white-listed pattern and could not be trusted.
 */
public class UntrustedInputException extends Exception {

    UntrustedInputException() {
        super();
    }

    UntrustedInputException(String s) {
        super(s);
    }
}
