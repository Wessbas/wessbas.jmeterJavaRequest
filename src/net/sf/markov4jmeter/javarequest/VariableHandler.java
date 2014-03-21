package net.sf.markov4jmeter.javarequest;

import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
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
 * <p>Each variable has an identification key which must follow the standard
 * format of JMeter variables, that is a valid name in curly braces with a
 * leading dollar symbol: <code>${<i>name</i>}</code>
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 */
public class VariableHandler {


    /* **************************  public methods  ************************** */


    /**
     * Creates a variable which assigns a value to the given key.
     *
     * @param key    key of the variable to be created.
     * @param value  value to be assigned to the given key.
     */
    public void setVariable (final String key, final Object value) {

        JMeterVariables variables =
                JMeterContextService.getContext().getVariables();

        if (variables == null) {

            variables = new JMeterVariables();
            JMeterContextService.getContext().setVariables(variables);
        }

        variables.putObject(key, value);
    }

    /**
     * Returns the value of the variable which is assigned to the given key.
     *
     * @param key
     *     key of the variable whose value shall be returned.
     * @return
     *     a valid <code>Object</code> instance if and only if a variable is
     *     assigned to the given key and its value is defined.
     */
    public Object getVariable (final String key) {

        final JMeterVariables variables =
                JMeterContextService.getContext().getVariables();

        final Object returnValue =
                (variables != null) ? variables.getObject(key) : null;

        return returnValue;
    }

    /**
     * Removes a variable which is associated with the given key.
     *
     * @param key
     *     key of the variable to be removed.
     *
     * @return
     *     the value of the removed variable, or <code>null</code> if no
     *     variable is associated with the given key.
     */
    public Object removeVariable (final String key) {

        final JMeterVariables variables =
                JMeterContextService.getContext().getVariables();

        return variables.remove(key);
    }

    /**
     * Removes all variables from the current thread context.
     */
    public void removeAllVariables () {

        JMeterContextService.getContext().setVariables(null);
    }


    /* -----------  methods for storing values with optional keys  ---------- */

    /**
     * Sets a value in the <code>JMeterContext</code> instance of the current
     * thread. The value might be optionally assigned to a parameter-defined
     * key, provided by the given <code>JavaSamplerContext</code> instance; a
     * default key might even be passed for the case that the
     * <code>JavaSamplerContext</code> instance does not provide a key. If
     * neither a parameter-defined key nor a default key is available (both are
     * <code>null</code>), nothing will be stored.
     *
     * @param javaSamplerContext
     *     instance which provides an optional parameter-defined key.
     * @param parameterName
     *     name of the parameter which defines an optional key.
     * @param defaultKey
     *     optional default key to be used, if no parameter-defined key is
     *     available.
     * @param value
     *     value to be stored.
     */
    public void storeValue (
            final JavaSamplerContext javaSamplerContext,
            final String parameterName,
            final String defaultKey,
            final Object value) {

        final String key = javaSamplerContext.getParameter(parameterName);

        if ( this.isDefinedValue(key) ) {

            this.setVariable(key, value);

        } else if ( this.isDefinedValue(defaultKey) ) {

            this.setVariable(defaultKey, value);
        }

        // store nothing here, since no key is defined;
    }


    /* **************************  private methods  ************************* */


    /**
     * Checks whether a given value is defined, that is it does not equal
     * <code>null</code> and consists of at least one character.
     *
     * @param value  value to be checked.
     *
     * @return  <code>true</code> if and only if the given value is defined.
     */
    private boolean isDefinedValue (final String value) {

        return value != null && value.length() > 0;
    }
}