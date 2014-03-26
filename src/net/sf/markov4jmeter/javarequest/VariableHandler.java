package net.sf.markov4jmeter.javarequest;

import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;

/**
 * Handler for storing and retrieving values of (JMeter) variables.
 *
 * <p>Variables might be used for storing certain information like return values
 * of methods permanently. They can be stored in instances of
 * {@link org.apache.jmeter.threads.JMeterVariables} for being shared with Java
 * Samplers within a single thread. The class
 * {@link org.apache.jmeter.threads.JMeterContextService} provides an instance
 * of {@link org.apache.jmeter.threads.JMeterContext} for each JMeter thread,
 * which can be requested via <code>JMeterContextService.getContext()</code>.
 * Each <code>JMeterContext</code> instance provides a
 * <code>JMeterVariables</code> object which can be used for storing any values
 * and requesting them as well.
 *
 * <p>This hander uses this technique for storing and retrieving values between
 * the Java Samplers of a thread. Variables are stored permanently, that is they
 * have to be deleted explicitly, if their values are not required anymore.
 *
 * <p>Each variable has a name and a value. An associated reference must follow
 * the standard format of JMeter variables, that is it must consist of the
 * variable name in curly braces with a leading dollar symbol:
 * <code>${<i>varname</i>}</code>
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 * @since    1.7
 */
public class VariableHandler {

    /** Regular expression which describes the variable name format. */
    private final static String REGEX__NAME =
            "((_\\p{Alnum})|\\p{Alpha})[\\p{Alnum}|_]*";

    /** Prefix of a reference marking. */
    private final static String REFERENCE_PREFIX = "${";

    /** Suffix of a reference marking. */
    private final static String REFERENCE_SUFFIX = "}";


    /* --------------------  variables error information  ------------------- */

    /** Error message for the case that a variable name is malformed. */
    private final static String ERROR_MALFORMED_VARIABLE_NAME =
            "malformed variable name \"%s\"";

    /** Error message for the case that a variable reference is malformed. */
    private final static String ERROR_MALFORMED_VARIABLE_REFERENCE =
            "malformed variable reference \"%s\"";

    /** Error message for the case that a reference is not associated with
     *  a variable. */
    private final static String ERROR_NON_ASSOCIATED_VARIABLE_NAME =
            "reference \"%s\" is not associated with any variable";


    /* **************************  public methods  ************************** */


    /**
     * Creates a variable which assigns a value to the given name.
     *
     * @param name   name of the variable to be created.
     * @param value  value to be assigned to the given name.
     *
     * @throws IllegalArgumentException  if the given name is malformed.
     */
    public void setVariable (
            final String name,
            final Object value) throws IllegalArgumentException {

        if ( !this.hasValidNameFormat(name) ) {

            final String message = String.format(
                    VariableHandler.ERROR_MALFORMED_VARIABLE_NAME,
                    name);

            throw new IllegalArgumentException(message);
        }

        JMeterVariables variables =
                JMeterContextService.getContext().getVariables();

        if (variables == null) {

            variables = new JMeterVariables();
        }

        variables.putObject(name, value);
        JMeterContextService.getContext().setVariables(variables);
    }

    /**
     * Returns the value of a variable which is associated with the given name.
     *
     * @param name
     *     name of the variable whose value shall be returned.
     * @return
     *     the value of the variable.
     *
     * @throws IllegalArgumentException
     *     if the given name is malformed, or if no such named variable exists.
     */
    protected Object getVariableValueByName (final String name)
            throws IllegalArgumentException {

        final String errorMessage = this.validateName(name);

        if (errorMessage != null) {

            throw new IllegalArgumentException(errorMessage);
        }

        // the validation of the name ensures that "variables" is defined;
        final JMeterVariables variables =
                JMeterContextService.getContext().getVariables();

        return variables.getObject(name);
    }

    /**
     * Returns the value of a variable which is associated with the given
     * reference.
     *
     * @param reference
     *     reference which is associated with the variable whose value shall be
     *     returned.
     * @return
     *     the value of the variable.
     *
     * @throws IllegalArgumentException
     *     if the given reference is malformed, or if it is not associated with
     *     a variable.
     */
    public Object getVariableValueByReference (final String reference)
            throws IllegalArgumentException {

        final String errorMessage = this.validateReference(reference);

        if (errorMessage != null) {

            throw new IllegalArgumentException(errorMessage);
        }

        final String name = this.extractName(reference);

        return this.getVariableValueByName(name);
    }

    /**
     * Removes a variable which is associated with the given name.
     *
     * @param name
     *     name of the variable to be removed.
     *
     * @return
     *     the value of the removed variable.
     *
     * @throws IllegalArgumentException
     *     if the given name is malformed, or if no such named variable exists.
     */
    public Object removeVariable (final String name)
            throws IllegalArgumentException {

        final String errorMessage = this.validateName(name);

        if (errorMessage != null) {

            throw new IllegalArgumentException(errorMessage);
        }

        // the validation of the name ensures that "variables" is defined;
        final JMeterVariables variables =
                JMeterContextService.getContext().getVariables();

        return variables.remove(name);
    }

    /**
     * Removes all variables from the current thread context.
     */
    public void removeAllVariables () {

        JMeterContextService.getContext().setVariables(null);
    }

    /**
     * Checks whether a given variable reference has a valid format, that is it
     * must be formatted as <code>${<i>varname</i>}</code>.
     *
     * @param reference
     *     variable reference to be checked.
     *
     * @return
     *     <code>true</code> if and only if the given variable reference has a
     *     valid format.
     */
    public boolean hasValidReferenceFormat (final String reference) {

        // returned name is null, if format is invalid;
        final String name = this.extractName(reference);

        return this.hasValidNameFormat(name);
    }

    /**
     * Checks whether a given variable name has a valid format, that is it must
     * consist of only alpha-numeric characters including underscore (_), and it
     * must not start with a digit.
     *
     * @param name
     *     variable name to be checked.
     *
     * @return
     *     <code>true</code> if and only if the given variable name has a valid
     *     format.
     */
    public boolean hasValidNameFormat (final String name) {

        return name != null &&
               name.matches(VariableHandler.REGEX__NAME);
    }


    /* *************************  protected methods  ************************ */


    /**
     * Extracts the variable name from a given reference by removing the
     * <code>${...}</code> environment.
     *
     * @param reference
     *     reference whose variable name shall be extracted.
     *
     * @return
     *     the extracted variable name, or <code>null</code> if the reference
     *     is malformed.
     *
     * @see #hasValidReferenceFormat(String)
     */
    protected String extractName (final String reference) {

        String name;

        try {

            // if reference == null, a NullPointerException will be thrown;
            if ( reference.startsWith(VariableHandler.REFERENCE_PREFIX) &&
                 reference.endsWith(VariableHandler.REFERENCE_SUFFIX) ) {

                // might throw a NullPointer- or IndexOutOfBoundsException;
                name = reference.substring(
                        VariableHandler.REFERENCE_PREFIX.length(),
                        reference.length() -
                        VariableHandler.REFERENCE_SUFFIX.length());
            } else {

                name = null;  // no ${...} environment -> invalid reference;
            }

        } catch (final IndexOutOfBoundsException|NullPointerException ex) {

            name = null;  // passed reference is probably null;
        }

        return name;
    }

    /**
     * Checks whether a given name is associated with a variable.
     *
     * @param name
     *     name to be checked, should be passed in a <u>valid format</u>.
     *
     * @return
     *     <code>true</code> if and only if the given name is associated with a
     *     variable.
     *
     * @see VariableHandler#hasValidNameFormat(String)
     */
    protected boolean isNameAssociatedWithVariable (final String name) {

        final JMeterVariables variables =
                JMeterContextService.getContext().getVariables();

        if (variables != null) {

            final Iterator<Entry<String,Object>> iterator =
                    variables.getIterator();

            Entry<String,Object> entry;

            while ( iterator.hasNext() ) {

                entry = iterator.next();

                if ( entry.getKey().equals(name) ) {

                    return true;
                }
            }

            return false;

        } else {

            // if variables == null, no variable has been stored yet;
            return false;
        }
    }


    /* **************************  private methods  ************************* */


    /**
     * Checks whether a given reference has a valid format and is associated
     * with a variable.
     *
     * @param reference
     *     reference to be checked.
     * @return
     *     <code>null</code> if and only if the given reference has a valid
     *     format, and if it is associated with a variable; otherwise a related
     *     error message will be returned.
     */
    private String validateReference (final String reference) {

        final String errorMessage;  // to be returned;
        final String name = this.extractName(reference);

        if (name == null) {

            errorMessage = String.format(
                    VariableHandler.ERROR_MALFORMED_VARIABLE_REFERENCE,
                    reference);

        } else {

            errorMessage = this.validateName(name);
        }

        return errorMessage;
    }

    /**
     * Checks whether a given name has a valid format and is associated with a
     * variable.
     *
     * @param name
     *     name to be checked.
     * @return
     *     <code>null</code> if and only if the given name has a valid format,
     *     and if it is associated with a variable; otherwise a related error
     *     message will be returned.
     */
    private String validateName (final String name) {

        final String errorMessage;  // to be returned;

        if ( !this.hasValidNameFormat(name) ) {

            errorMessage = String.format(
                    VariableHandler.ERROR_MALFORMED_VARIABLE_NAME,
                    name);

        // the name should be passed to isNameAssociatedWithVariable() in a
        // valid format, which has been ensured through the test above;
        } else if ( !this.isNameAssociatedWithVariable(name) ) {

            errorMessage = String.format(
                    VariableHandler.ERROR_NON_ASSOCIATED_VARIABLE_NAME,
                    name);
        } else {

            errorMessage = null;  // no error, reference is valid;
        }

        return errorMessage;
    }
}