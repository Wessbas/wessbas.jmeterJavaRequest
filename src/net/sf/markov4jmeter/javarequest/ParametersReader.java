package net.sf.markov4jmeter.javarequest;

import java.io.IOException;
import java.lang.reflect.Method;

import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

/**
 * Reader class for requesting and processing Java Sampler parameters. This
 * class provides methods for requesting the following information from a
 * <code>JavaSamplerContext</code> instance which provides the parameters of
 * a Java Sampler:
 * <ul>
 *   <li> method <i>parent</i> information as an <code>Object</code>
 *        instance (denoting a <code>Class</code> object or an instance of a
 *        class);
 *
 *   <li> method <i>signature</i> as String;
 *
 *   <li> method <i>parameters</i> as <code>Object</code> instance for a
 *        detected <code>Method</code> instance.
 * </ul>
 * The regarding parameter names are not predefined, they must be passed by the
 * invoking instance. A parameter value might be formatted as
 * <code>${...}</code>, indicating a reference to a variable; in that case, the
 * parameter will be resolved by requesting the value of a matching variable in
 * the <code>JMeterContext</code> instance of the thread.
 *
 * @see VariableHandler
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 * @since    1.7
 */
public class ParametersReader {

    /** Prefix for generic argument names.
     *  @see #getMethodParameterNames(Method) */
    public final static String ARG_NAME_PREFIX = "arg";

    /** Regular expression for variable name validation. */
    private final static String REGEX__NAME =
            "(\\p{Alpha}|_)[\\p{Alnum}|_]*";

    /** Regular expression for variable key validation (<code>${...}</code>). */
    private final static String REGEX__VARIABLE_KEY =
            "^\\$\\{" + ParametersReader.REGEX__NAME + "\\}$";


    /* ---------------------  parent error information  --------------------- */

    /** Error message for the case that a class cannot be found. */
    private final static String ERROR_CLASS_NOT_FOUND =
            "Could not associate \"%s\" with any class.";

    /** Error message for the case that parent information is ambiguous. */
    private final static String ERROR_PARENT_AMBIGUOUS =
            "Ambiguous class/object definition (parameters \"%s\" and \"%s\" "
            + "are not allowed to be used simultaneously).";

    /** Error message for the case that no parent information is available. */
    private final static String ERROR_PARENT_UNDEFINED =
            "no class/object information available; parameters \"%s\" or "
            + "\"%s\" need to be defined.";

    /** Error message for the case that a <code>String</code> cannot be
     *  re-converted to an <code>Object</code>. */
    private final static String ERROR_OBJECT_NOT_DESERIALIZABLE =
            "Could not deserialize object from String \"%s\": %s";


    /* ----------------  method signature error information  ---------------- */

    /** Error message for the case that a method signature is undefined. */
    private final static String ERROR_METHOD_SIGNATURE_UNDEFINED =
            "Could not retrieve method signature value for parameter "
            + "name \"%s\"";


    /* --------------------  parameter error information  ------------------- */

    /** Error message for the case that a parameter is undefined. */
    private final static String ERROR_PARAMETER_VALUE_UNDEFINED =
            "Undefined parameter value for argument \"%s\".";

    /** Error message for the case that a parameter value is invalid. */
    private final static String ERROR_PARAMETER_VALUE_INVALID =
            "Invalid parameter value for argument \"%s\" (type %s): %s";

    /** Error message for the case that a boolean value cannot be parsed. */
    private final static String ERROR_INVALID_BOOLEAN_VALUE =
            "Invalid boolean value: %s";

    /** Error message for the case that a character value cannot be parsed. */
    private final static String ERROR_INVALID_CHAR_VALUE =
            "Invalid character value: %s";


    /* *************************  global variables  ************************* */


    /** Instance for serializing objects to <code>String</code>s and vice versa.
     */
    private final ObjectStringConverter objectStringConverter;

    /** Instance for requesting values of variables. */
    private final VariableHandler variableHandler;


    /* ***************************  constructors  *************************** */


    /**
     * Constructor for a Parameters Reader.
     */
    public ParametersReader () {

        this.objectStringConverter = new ObjectStringConverter();
        this.variableHandler       = new VariableHandler();
    }


    /* **************************  public methods  ************************** */


    /* ---------  methods for retrieving method parent information  --------- */

    /**
     * Returns the method <i>parent</i> information provided by an instance of
     * <code>JavaSamplerContext</code>.
     *
     * <p>A parent might be a (fully qualified) class name indicating a static
     * method call, or a (serialized) object indicating an instance method call.
     * The arguments for the parameters regarding class names or serialized
     * objects are exclusively; if both are defined, a
     * <code>ParameterException</code> will be thrown, indicating ambiguous
     * parameters.
     * <ul>
     *   <li> If an argument for a class name parameter is defined, the related
     *   parameter value will be read from the <code>JavaSamplerContext</code>
     *   instance, and a representative <code>Class</code> instance will be
     *   returned.
     *
     *   <li> In case an argument for a (serialized) object parameter is
     *    defined, its value will be read from the
     *    <code>JavaSamplerContext</code>, and the deserialized object will be
     *    returned.
     * </ul>
     *
     * @param javaSamplerContext
     *     instance which provides the method parent information.
     * @param parameterName_className
     *     name of the parameter which contains a (fully qualified) class name.
     * @param parameterName_objectString
     *     name of the parameter which contains a serialized object.
     *
     * @return
     *     a valid instance of <code>Class</code>, if a (fully qualified) class
     *     name could be read successfully, or a (deserialized) object if its
     *     related serialization <code>String</code> could be read and processed
     *     successfully.
     *
     * @throws ParameterException
     *     if any parameter is undefined or invalid, or if parent parameters
     *     are ambiguous.
     */
    public Object getParent (
            final JavaSamplerContext javaSamplerContext,
            final String parameterName_className,
            final String parameterName_objectString)
                    throws ParameterException {

        Object parent;

        final String className =
                javaSamplerContext.getParameter(parameterName_className);

        final String objectString =
                javaSamplerContext.getParameter(parameterName_objectString);

        if (className != null && objectString != null) {

            final String message = String.format(
                    ParametersReader.ERROR_PARENT_AMBIGUOUS,
                    parameterName_className,
                    parameterName_objectString);

            throw new ParameterException(message);
        }

        if (className != null) {

            // might throw a ParameterException;
            parent = this.getClassByClassName(className);

        } else if (objectString != null) {

            // might throw a ParameterException;
            parent = this.string2Object(objectString);

        } else {

            final String message = String.format(
                    ParametersReader.ERROR_PARENT_UNDEFINED,
                    parameterName_className,
                    parameterName_objectString);

            throw new ParameterException(message);
        }

        return parent;
    }


    /* -------------  methods for retrieving a method signature  ------------ */

    /**
     * Returns the signature of a method, provided by the specified
     * <code>JavaSamplerContext</code> instance.
     *
     * @param javaSamplerContext
     *     instance which provides the method signature.
     * @param parameterName
     *     name of the parameter which maps to the method signature.
     *
     * @return
     *     the method signature as <code>String</code>.
     *
     * @throws ParameterException
     *     if the method signature is undefined.
     */
    public String getMethodSignature (
            final JavaSamplerContext javaSamplerContext,
            final String parameterName) throws ParameterException {

        final String methodSignature =
                javaSamplerContext.getParameter(parameterName);

        if (methodSignature == null) {

            final String message = String.format(
                    ParametersReader.ERROR_METHOD_SIGNATURE_UNDEFINED,
                    parameterName);

            throw new ParameterException(message);
        }

        return methodSignature;
    }


    /* -------------  methods for retrieving method parameters  ------------- */

    /**
     * Returns parameter objects according to the signature of the given method
     * and provided by the specified <code>JavaSamplerContext</code> instance.
     *
     * @param javaSamplerContext
     *     instance which provides the parameter objects.
     * @param method
     *     method whose parameter objects shall be returned.
     *
     * @return
     *     a sequence of typed parameter objects, according to the method
     *     signature.
     *
     * @throws ParameterException
     *     if any parameter could not be obtained.
     */
    public Object[] getMethodParameters (
            final JavaSamplerContext javaSamplerContext,
            final Method method) throws ParameterException {

        final Object[] parameters;

        final Class<?>[] parameterTypes = method.getParameterTypes();
        final String[]   parameterNames = this.getMethodParameterNames(method);

        // might throw an ParameterException;
        final String[] parameterValues = this.getMethodParameterValues(
                javaSamplerContext,
                parameterNames);

        // might throw an ParameterException;
        parameters = this.resolveMethodParameters(
                parameterTypes,
                parameterNames,
                parameterValues);

        return parameters;
    }


    /* **************************  private methods  ************************* */


    /* ---------  methods for retrieving method parent information  --------- */

    /**
     * Returns a <code>Class</code> instance representing the class which is
     * indicated by the given (fully qualified) class name. The specified class
     * must be included in the current class path.
     *
     * @param className
     *     name of the class whose <code>Class</code> representation shall be
     *     returned.
     *
     * @return
     *     the <code>Class</code> representation of the specified class.
     *
     * @throws ParameterException
     *     if the class could not be detected in the current class path.
     */
    private Class<?> getClassByClassName (final String className)
            throws ParameterException {

        final Class<?> parent;

        try {

            // might throw a LinkageError, an ExceptionInInitializerError
            // or a ClassNotFoundException;
            parent = Class.forName(className);

        } catch (final Exception ex) {

            final String message = String.format(
                    ParametersReader.ERROR_CLASS_NOT_FOUND,
                    className);

            throw new ParameterException(message);
        }

        return parent;
    }

    /**
     * Converts a given <code>String</code> to its represented
     * <code>Object</code>.
     *
     * @param objectString
     *     <code>String</code> to be converted to an <code>Object</code>.
     *
     * @return
     *     An <code>Object</code> which is represented through the given
     *     <code>String</code>.
     *
     * @throws ParameterException
     *     if the given <code>String</code> does not represent an
     *     <code>Object</code>, or if the conversion fails for any other reason.
     *
     * @see ObjectStringConverter#string2Object(String)
     */
    private Object string2Object (final String objectString)
            throws ParameterException {

        final Object object;

        try {

            object = this.objectStringConverter.string2Object(objectString);

        } catch (final Exception ex) {

            final String message = String.format(
                    ParametersReader.ERROR_OBJECT_NOT_DESERIALIZABLE,
                    objectString,
                    ex.getMessage());

            throw new ParameterException(message);
        }

        return object;
    }


    /* -------------  methods for retrieving method parameters  ------------- */

    /**
     * Returns the parameter names of a given method.
     *
     * <p><u>Note:</u> since Java Reflection does not support the extraction of
     * parameter names directly, generic names will be generated, consisting of
     * an "<code>arg</code>" prefix, followed by a position index:
     * <code>arg0</code>, <code>arg1</code>, <code>arg2</code>, ...
     *
     * @param method
     *     method whose parameter names shall be returned.
     *
     * @return
     *     a sequence of parameter names, according to the signature of the
     *     given method.
     */
    private String[] getMethodParameterNames (final Method method) {

        final String[] parameterNames;  // to be returned;
        final int n = method.getParameterTypes().length;

        parameterNames = new String[n];

        for (int i = 0; i < n; i++) {

            parameterNames[i] = ParametersReader.ARG_NAME_PREFIX + i;
        }

        return parameterNames;
    }

    /**
     * Reads a sequence of parameter values according to their related names
     * from a given <code>JavaSamplerContext</code> instance.
     *
     * @param javaSamplerContext
     *     instance providing the parameter values to be read.
     * @param parameterNames
     *     names of the parameters whose values shall be read.
     *
     * @return
     *     a sequence of parameter values, ordered according to the given name
     *     sequence.
     *
     * @throws ParameterException
     *     if any parameter could not be read.
     */
    private String[] getMethodParameterValues (
            final JavaSamplerContext javaSamplerContext,
            final String[] parameterNames) throws ParameterException {

        final String[] parameterValues;  // to be returned;
        final int n = parameterNames.length;

        parameterValues = new String[n];

        String parameterName;
        String parameterValue;

        for (int i = 0; i < n; i++) {

            parameterName  = parameterNames[i];
            parameterValue = javaSamplerContext.getParameter(parameterName);

            if (parameterValue == null) {

                final String message = String.format(
                        ParametersReader.ERROR_PARAMETER_VALUE_UNDEFINED,
                        parameterName);

                throw new ParameterException(message);
            }

            parameterValues[i] = parameterValue;
        }

        return parameterValues;
    }

    /**
     * Resolves a sequence of parameter <code>String</code> values regarding to
     * their associated types. If a type denotes a <i>BaseType</i>, its related
     * <code>String</code> will be parsed using a standard wrapper method
     * (<code>Float.parseFloat()</code>, <code>Integer.parseInt()</code>, ...);
     * in case a type denotes an <i>Array</i> or <i>Object</i>, its related
     * <code>String</code> will be decoded to an instance of the regarding type.
     *
     * @param parameterTypes
     *     parameter types, with each type being a <i>BaseType</i>, <i>Array</i>
     *     or <i>Object</i>.
     * @param parameterNames
     *     parameter names, ordered according to the given type sequence; the
     *     names are just required for information purposes if any error occurs.
     * @param parameterValues
     *     parameter values to be resolved, ordered according to the given type
     *     sequence. A parameter value might be formatted as
     *     <code>${...}</code>, indicating a reference to a variable; in that
     *     case, the parameter will be resolved by requesting the value of a
     *     matching variable in the <code>JMeterContext</code> instance of the
     *     thread.
     *
     * @return
     *     a sequence of parsing/decoding results as objects, according to the
     *     given type sequence.
     *
     * @throws ParameterException
     *     if any <code>String</code> value does not match to its given type,
     *     or if parsing/decoding fails for any reason.
     *
     * @see #resolveMethodParameter(Class, String)
     */
    private Object[] resolveMethodParameters (
            final Class<?>[] parameterTypes,
            final String[]   parameterNames,
            final String[]   parameterValues) throws ParameterException {

        final Object[] objects;  // to be returned;
        final int n = parameterTypes.length;

        objects = new Object[n];

        for (int i = 0; i < n; i++) {

            final Class<?> parameterType  = parameterTypes[i];
            final String   parameterValue = parameterValues[i];

            try {

                final boolean isVariable = parameterValue.trim().matches(
                        ParametersReader.REGEX__VARIABLE_KEY);

                // resolveMethodParameter() might throw a ParameterException;
                final Object object = isVariable ?
                        this.variableHandler.getVariable(parameterValue.trim()):
                        this.resolveMethodParameter(
                                parameterType,
                                parameterValue);

                objects[i] = object;

            } catch (final ParameterException ex) {

                // give an informational message, including the parameter name;
                final String message = String.format(
                        ParametersReader.ERROR_PARAMETER_VALUE_INVALID,
                        parameterNames[i],
                        parameterType,
                        ex.getMessage());

                throw new ParameterException(message);
            }
        }

        return objects;
    }

    /**
     * Resolves a parameter <code>String</code> value regarding to a given type.
     * If the type denotes a <i>BaseType</i>, the <code>String</code> will be
     * parsed using a standard wrapper method (<code>Float.parseFloat()
     * </code>, <code>Integer.parseInt()</code>, ...); in case the type denotes
     * an <i>Array</i> or <i>Object</i>, the <code>String</code> will be decoded
     * to an instance of the regarding type.
     *
     * @param type
     *     parameter type, might be a <i>BaseType</i>, <i>Array</i> or
     *     <i>Object</i>.
     * @param value
     *     parameter value to be resolved.
     *
     * @return
     *     the parsing/decoding result as an object, according to the given
     *     type.
     *
     * @throws ParameterException
     *     if the <code>String</code> value does not match to the given type,
     *     or if parsing/decoding fails for any reason.
     */
    private Object resolveMethodParameter (
            final Class<?> type,
            final String value) throws ParameterException {

        Object object = null;  // to be returned;

        try {

            if ( type.equals(boolean.class) ) {

                if (Boolean.TRUE.toString().equals(value)) {

                    object = Boolean.TRUE;

                } else if (Boolean.FALSE.toString().equals(value)) {

                    object = Boolean.FALSE;

                } else {

                    final String message = String.format(
                            ParametersReader.ERROR_INVALID_BOOLEAN_VALUE,
                            value);

                    throw new ParameterException(message);
                }
            }

            else if ( type.equals(byte.class) ) {

                // use decode() instead of parseByte(), since 0x-notation won't
                // be accepted otherwise; might throw a NumberFormatException;
                object = Byte.decode(value);
            }

            else if ( type.equals(char.class) ) {

                if (value == null || value.length() != 1) {

                    final String message = String.format(
                            ParametersReader.ERROR_INVALID_CHAR_VALUE,
                            value);

                    throw new ParameterException(message);
                }

                object = value.charAt(0);
            }

            else if ( type.equals(double.class) ) {

                // might throw a NumberFormat- or NullPointerException;
                object = Double.parseDouble(value);
            }

            else if ( type.equals(float.class) ) {

                // might throw a NumberFormat- or NullPointerException;
                object = Float.parseFloat(value);
            }

            else if ( type.equals(int.class) ) {

                // might throw a NumberFormatException;
                object = Integer.parseInt(value);
            }

            else if ( type.equals(long.class) ) {

                // might throw a NumberFormatException;
                object = Long.parseLong(value);
            }

            else if ( type.equals(short.class) ) {

                // might throw a NumberFormatException;
                object = Short.parseShort(value);
            }

            else {

                // might throw an IO-, Security-, NullPointer- or
                // ClassNotFoundException;
                object = this.objectStringConverter.string2Object(value);
            }

        } catch (final NumberFormatException
                     | SecurityException
                     | NullPointerException
                     | ClassNotFoundException
                     | IOException ex) {

            // just wrap non-ParameterExceptions here;
            throw new ParameterException(ex.getMessage());
        }

        return object;
    }
}
