package net.sf.markov4jmeter.javarequest;

/**
 * Handler for storing and retrieving values of (JMeter) variables.
 *
 * <p>This class supports in addition to {@link VariableHandler} methods for
 * storing, reading and deleting variables which might be put into a dedicated
 * variable pool for sharing objects between Samplers. Those variables can be
 * accessed via service methods of class {@link VariablesPool}.
 *
 * <p>This circumvents using the sets of variables provided by thread-related
 * <code>org.apache.jmeter.threads.JMeterContext</code> instances, since
 * objects handled by those contexts are assumed to be <code>String</code>s
 * only. They are requested by JMeter via <code>get(String)</code> method of
 * class <code>org.apache.jmeter.threads.JMeterVariables</code>. That method
 * casts each object to class <code>String</code> in-between each Sampler
 * switch, as detected in JMeter Version <i>2.11 r1554548</i> and <i>2.10
 * r1533061</i> as well.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 * @since    1.7
 */
public class VariableHandlerVP extends VariableHandler {

    /** Error message for the case that a variable name is malformed. */
    private final static String ERROR_MALFORMED_VARIABLE_NAME =
            "malformed variable name \"%s\"";

    /** Error message for the case that a variable reference is malformed. */
    private final static String ERROR_MALFORMED_VARIABLE_REFERENCE =
            "malformed variable reference \"%s\"";


    /* **************************  public methods  ************************** */


    /**
     * Creates a variable which assigns a value to the given name. The variable
     * will be explicitly stored in the variable pool, related to the current
     * thread.
     *
     * @param name   name of the variable to be created.
     * @param value  value to be assigned to the given name.
     *
     * @throws IllegalArgumentException  if the given name is malformed.
     */
    public void setVariableInVariablesPool (
            final String name,
            final Object value) throws IllegalArgumentException {

        if ( !this.hasValidNameFormat(name) ) {

            final String message = String.format(
                    VariableHandlerVP.ERROR_MALFORMED_VARIABLE_NAME,
                    name);

            throw new IllegalArgumentException(message);
        }

        VariablesPool.put(name, value);
    }

    @Override
    public Object getVariableValueByReference (final String reference)
            throws IllegalArgumentException {

        if ( !this.hasValidReferenceFormat(reference) ) {

            final String message = String.format(
                    VariableHandlerVP.ERROR_MALFORMED_VARIABLE_REFERENCE,
                    reference);

            throw new IllegalArgumentException(message);
        }

        final String name = this.extractName(reference);

        // at first, look into the variable pool for any definition;
        if ( VariablesPool.containsKey(name) ) {

            return VariablesPool.get(name);
        }

        return super.getVariableValueByReference(reference);
    }

    @Override
    public Object removeVariable (final String name)
            throws IllegalArgumentException {

        if ( !this.hasValidNameFormat(name) ) {

            final String message = String.format(
                    VariableHandlerVP.ERROR_MALFORMED_VARIABLE_NAME,
                    name);

            throw new IllegalArgumentException(message);
        }

        if ( VariablesPool.containsKey(name) ) {

            return VariablesPool.remove(name);

        } else {

            // note: test for valid name will be done in super class, too;
            // might throw an IllegalArgumentException;
            return super.removeVariable(name);
        }
    }
}
