package net.sf.markov4jmeter.javarequest;

/**
 * Exception to be thrown if any parameter error occurs.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 */
public class ParameterException extends Exception {

    /** Default serial version ID. */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor with a specific error message.
     *
     * @param message  additional information about the error which occurred.
     */
    public ParameterException (final String message) {

        super(message);
    }
}