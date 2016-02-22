/***************************************************************************
 * Copyright (c) 2016 the WESSBAS project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/


package net.sf.markov4jmeter.javarequest;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

/**
 * Handler class for requesting and processing Java Sampler parameters. This
 * class provides methods for requesting the following information from a
 * <code>JavaSamplerContext</code> instance which provides the parameters of
 * a Java Sampler:
 * <ul>
 *   <li> method-<i>parent</i> information as an <code>Object</code>
 *        instance (denoting a <code>Class</code> object or an instance of a
 *        class);
 *
 *   <li> method-<i>signature</i> as <code>String</code>;
 *
 *   <li> method-<i>parameters</i> as <code>Object</code> instance for a
 *        detected <code>Method</code> instance.
 * </ul>
 * The regarding parameter names are not predefined, they must be passed by the
 * invoking instance.
 *
 * @see VariableHandler
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 * @since    1.7
 */
public class ParameterHandler {

    /** Prefix for generic argument names.
     *
     *  @see #getMethodParameterNames(Method) */
    public final static String ARG_NAME_PREFIX = "arg";


    /* ---------------------  parent error information  --------------------- */

    /** Error message for the case that a class cannot be found. */
    private final static String ERROR_CLASS_NOT_FOUND =
            "could not associate \"%s\" with any class";

    /** Error message for the case that a static field cannot be found. */
    private final static String ERROR_STATIC_FIELD_NOT_FOUND =
            "could not associate \"%s\" with any static field";

    /** Error message for the case that parent information is ambiguous. */
    private final static String ERROR_PARENT_AMBIGUOUS =
            "ambiguous class/object definition (parameters \"%s\" and \"%s\" "
            + "are not allowed to be used simultaneously)";

    /** Error message for the case that no parent information is available. */
    private final static String ERROR_PARENT_UNDEFINED =
            "no class/object information available; parameters \"%s\" or "
            + "\"%s\" need to be defined";

    /** Error message for the case that the parent information is invalid. */
    private final static String ERROR_PARENT_INVALID =
            "class/object information invalid (%s)";

    /* ----------------  method signature error information  ---------------- */

    /** Error message for the case that a method signature is undefined. */
    private final static String ERROR_METHOD_SIGNATURE_UNDEFINED =
            "could not retrieve method signature value for parameter "
            + "name \"%s\"";


    /* --------------------  parameter error information  ------------------- */

    /** Error message for the case that a parameter is undefined. */
    private final static String ERROR_PARAMETER_VALUE_UNDEFINED =
            "undefined parameter value for argument \"%s\"";

    /** Error message for the case that a parameter value is invalid. */
    private final static String ERROR_PARAMETER_VALUE_INVALID =
            "invalid parameter value for argument \"%s\" (type %s): %s";

    /** Error message for the case that a boolean value cannot be parsed. */
    private final static String ERROR_INVALID_BOOLEAN_VALUE =
            "invalid boolean value: %s";

    /** Error message for the case that a character value cannot be parsed. */
    private final static String ERROR_INVALID_CHAR_VALUE =
            "invalid character value: %s";

    /** Error message for the case that a referred type does not match an
     *  expected type. */
    private final static String ERROR_INVALID_REFERENCED_TYPE =
            "referenced type (%s) does not match the expected type (%s)";

    /** Error message for the case that a <code>String</code> value is quoted
     *  improperly. */
    private final static String ERROR_IMPROPER_STRING_QUOTES =
            "improperly quoted String: %s";


    /* -----------------  return variable error information  ---------------- */

    /** Error message for the case that a variable could not be stored. */
    private final static String ERROR_VARIABLE_STORING_FAILED =
            "could not store variable (%s)";

    /** Error message for the case that the name of the return variable is
     *  invalid. */
    private final static String ERROR_RETURN_VARIABLE_NAME_INVALID =
            "invalid return variable name \"%s\" (parameter \"%s\")";


    /* ----------------------  primitive type mappings  --------------------- */

    /** Hash map which assigns wrapper classes to their related primitive
     *  types. */
    private static HashMap<Class<?>, Class<?>> PRIMITIVE_DATA_TYPE_MAPPINGS;

    static {

        // hash map initialization;
        ParameterHandler.PRIMITIVE_DATA_TYPE_MAPPINGS =
                new HashMap<Class<?>, Class<?>>();

        PRIMITIVE_DATA_TYPE_MAPPINGS.put(boolean.class, Boolean.class  );
        PRIMITIVE_DATA_TYPE_MAPPINGS.put(byte.class,    Byte.class     );
        PRIMITIVE_DATA_TYPE_MAPPINGS.put(char.class,    Character.class);
        PRIMITIVE_DATA_TYPE_MAPPINGS.put(double.class,  Double.class   );
        PRIMITIVE_DATA_TYPE_MAPPINGS.put(float.class,   Float.class    );
        PRIMITIVE_DATA_TYPE_MAPPINGS.put(int.class,     Integer.class  );
        PRIMITIVE_DATA_TYPE_MAPPINGS.put(long.class,    Long.class     );
        PRIMITIVE_DATA_TYPE_MAPPINGS.put(short.class,   Short.class    );
    }


    /* *************************  global variables  ************************* */


    /** Instance for encoding and decoding <code>String</code>s to
     *  <code>Object</code>s and vice versa. */
    protected final ObjectStringConverter objectStringConverter;

    /** Handler for storing and retrieving values of (JMeter) variables. */
    protected final VariableHandler variableHandler;


    /* ***************************  constructors  *************************** */


    /**
     * Constructor for a Parameters Handler.
     */
    public ParameterHandler () {

        this.objectStringConverter = new ObjectStringConverter();
        this.variableHandler       = new VariableHandlerVP();
    }


    /* **************************  public methods  ************************** */


    /* --------------  methods for retrieving parameter values  ------------- */

    /**
     * Returns the method <i>parent</i> information provided by an instance of
     * <code>JavaSamplerContext</code>.
     *
     * <p>A parent might be a (fully qualified) class name indicating a static
     * method call, or an (encoded) object indicating an instance method call.
     * The arguments for the parameters regarding class names or objects are
     * exclusively; if both are defined, a <code>ParameterException</code> will
     * be thrown, notifying about ambiguous parameters.
     * <ul>
     *   <li> If an argument for a class name parameter is defined, the related
     *   parameter value will be read from the <code>JavaSamplerContext</code>
     *   instance, and a representative <code>Class</code> instance will be
     *   returned.
     *
     *   <li> In case an argument for an (encoded) object parameter is defined,
     *    its value will be read from the <code>JavaSamplerContext</code>, and
     *    the decoded object will be returned. An object parameter might even
     *    denote a reference to a variable, formatted as <code>${...}</code>;
     *    in that case, the related object will be requested from the thread
     *    context which handles all variables including their objects.
     * </ul>
     *
     * @param javaSamplerContext
     *     instance which provides the method parent information.
     * @param parameterName_className
     *     name of the parameter which contains a (fully qualified) class name.
     * @param parameterName_objectString
     *     name of the parameter which contains an (encoded) object.
     *
     * @return
     *     a valid instance of <code>Class</code>, if a (fully qualified) class
     *     name could be read successfully, or an (decoded) object if its
     *     related (encoding) <code>String</code> could be read and processed
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

        Object parent = null;

        final String className =
                javaSamplerContext.getParameter(parameterName_className);

        final String objectString =
                javaSamplerContext.getParameter(parameterName_objectString);

        if ( this.isDefinedValue(className) &&
             this.isDefinedValue(objectString)) {

            final String message = String.format(
                    ParameterHandler.ERROR_PARENT_AMBIGUOUS,
                    parameterName_className,
                    parameterName_objectString);

            throw new ParameterException(message);
        }

        if ( this.isDefinedValue(className) ) {

            // might throw a ParameterException;
            parent = this.getClassByClassName(className);

        } else if ( this.isDefinedValue(objectString) ) {

            try {

                // allow objects of all types; might throw ParameterException;
                parent = this.evaluateExpression(Object.class, objectString);

            } catch (final ParameterException ex) {

                final String message = String.format(
                        ParameterHandler.ERROR_PARENT_INVALID,
                        ex.getMessage());

                throw new ParameterException(message);
            }

        } else {

            final String message = String.format(
                    ParameterHandler.ERROR_PARENT_UNDEFINED,
                    parameterName_className,
                    parameterName_objectString);

            throw new ParameterException(message);
        }

        return parent;
    }

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

        if ( !this.isDefinedValue(methodSignature) ) {

            final String message = String.format(
                    ParameterHandler.ERROR_METHOD_SIGNATURE_UNDEFINED,
                    parameterName);

            throw new ParameterException(message);
        }

        return methodSignature;
    }

    /**
     * Returns parameter objects according to the signature of a given method
     * and provided by the specified <code>JavaSamplerContext</code> instance.
     *
     * @param javaSamplerContext
     *     instance which provides the parameter objects.
     * @param method
     *     method whose parameter objects shall be returned.
     *
     * @return
     *     a sequence of typed parameter objects, according to the given method
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

    /**
     * Returns the (optional) name of the variable which will be associated
     * with the return value; the name will be read from the specified
     * <code>JavaSamplerContext</code> instance.
     *
     * @param javaSamplerContext
     *     instance which provides the variable name.
     * @param parameterName
     *     name of the parameter which maps to the variable name.
     *
     * @return
     *     the name of the variable, or <code>null</code> if no name is defined.
     *
     * @throws ParameterException
     *     if the variable name has an invalid format.
     *
     * @see VariableHandler#hasValidNameFormat(String)
     */
    public String getReturnVariableName (
            final JavaSamplerContext javaSamplerContext,
            final String parameterName) throws ParameterException {

        final String name = javaSamplerContext.getParameter(parameterName);

        if ( !this.isDefinedValue(name) ) {

            return null;
        }

        if ( !this.variableHandler.hasValidNameFormat(name) ) {

            final String message = String.format(
                    ParameterHandler.ERROR_RETURN_VARIABLE_NAME_INVALID,
                    parameterName);

            throw new ParameterException(message);
        }

        return name;
    }


    /* --------------------  methods for storing values  -------------------- */

    /**
     * Stores a value in the current thread context by assigning it to the
     * variable of a given name. The value itself will be <u>encoded</u> and
     * stored as a <code>String</code>. That is, it must be decoded when being
     * requested and processed.
     *
     * @param name
     *     name of the variable to which the value will be assigned.
     * @param value
     *     value to be stored.
     *
     * @throws ParameterException
     *     if the value could not be stored for any reason.
     */
    public void storeEncodedValue (
            final String name,
            final Serializable value) throws ParameterException {

        try {

            // might throw an EncodingException;
            final String encodedValue =
                    this.objectStringConverter.encodeValue(value);

            // store the encoded value; do NOT store "value" instead, since
            // JMeter (version 2.11 r1554548 and 2.10 r1533061 as well) will
            // try to cast any object to a String when switching between
            // Samplers; this results generally in a "silent" exception,
            // stopping the execution of the regarding Sampler with information
            // being only available in the JMeter log file;

            // might throw an IllegalArgumentException;
            this.variableHandler.setVariable(name, encodedValue);

        } catch (final EncodingException|IllegalArgumentException ex) {

            final String message = String.format(
                    ParameterHandler.ERROR_VARIABLE_STORING_FAILED,
                    ex.getMessage());

            throw new ParameterException(message);
        }
    }


    /* *************************  protected methods  ************************ */


    /* ----------------  methods for evaluating expressions  ---------------- */

    /**
     * Resolves a given expression.
     *
     * <p>Single values will be evaluated regarding to the given type.
     * References might be even <i>chained</i>, that is one reference might
     * point to another. This method resolves such chains recursively, expecting
     * a value of matching type at the end of a reference chain.
     *
     * @param type
     *     expected type of the result value.
     * @param value
     *     the expression to be resolved.
     *
     * @return
     *     the value which results from resolving the given expression.
     *
     * @throws ParameterException
     *     if resolving fails for any reason, or if the type of the resulting
     *     value does not match the given type.
     */
    // TODO: -expressions- are currently assumed to be either a reference or a
    // single value; for evaluating compound expressions, this method might be
    // overwritten and extended.
    protected Object evaluateExpression (
            final Class<?> type,
            final Object   value) throws ParameterException {

        Object object = null;  // to be returned;

        final String trimmedValue =
                value instanceof String ? ((String) value).trim() : null;

        if ( this.variableHandler.hasValidReferenceFormat(trimmedValue) ) {

            // resolve possible references; might throw a ParameterException;
            object = this.resolveReference(type, trimmedValue);

        } else if (value instanceof ReturnVariable) {

            // if "value" is a return value, just return its value (the decoded
            // object which has been wrapped into the ReturnVariable instance);
            object = ((ReturnVariable) value).getValue();

        } else {

            // resolveValue() might throw a ParameterException;
            object = value instanceof String ?
                    this.resolveValue(type, (String) value) : value;

            // check whether the resolved object is invalid;
            if ( object != null &&
                 !type.isAssignableFrom(object.getClass()) &&
                 !this.compliesObjectTypeWithWrapperType(type, object) ) {

                final String message = String.format(
                        ParameterHandler.ERROR_INVALID_REFERENCED_TYPE,
                        object.getClass().getName(),
                        type.getName());

                throw new ParameterException(message);
            }
        }

        return object;
    }

    /**
     * Resolves a reference regarding to a given type.
     *
     * @param type
     *     type which might be a <i>BaseType</i>, <i>Array</i> or <i>Object</i>.
     * @param reference
     *     reference to be resolved; this must be a <u>trimmed</u>
     *     <code>String</code>, formatted as <code>${<i>varname</i>}</code>.
     *
     * @return
     *     the value which results from resolving the reference.
     *
     * @throws ParameterException
     *      will be thrown in the following cases:
     *      <ul>
     *        <li> if the given reference is malformed, or if it is not
     *             associated with a variable;
     *        <li> if parsing/decoding of a value fails for any reason;
     *        <li> if the type of the resulting value does not match the given
     *             type.
     *      </ul>
     */
    protected Object resolveReference (
            final Class<?> type,
            final String   reference) throws ParameterException {

            // might throw a ParameterException;
            final Object value =
                    this.variableHandler.getVariableValueByReference(reference);

            // recursive call; might throw a ParameterException;
            return this.evaluateExpression(type, value);
    }

    /**
     * Resolves a parameter <code>String</code> value regarding to a given type.
     * If the type denotes a <i>BaseType</i>, the <code>String</code> will be
     * parsed using a standard wrapper method (<code>Float.parseFloat()</code>,
     * <code>Integer.parseInt()</code>, ...); in case the type denotes an
     * <i>Array</i> or <i>Object</i>, the <code>String</code> will be decoded
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
     *     if the <code>String</code> value does not match the given type, or
     *     if parsing/decoding fails for any reason.
     */
    protected Object resolveValue (final Class<?> type, final String value)
            throws ParameterException {

        Object object = null;  // to be returned;

        try {

            if ( type.equals(boolean.class) ) {

                if (Boolean.TRUE.toString().equals(value)) {

                    object = Boolean.TRUE;

                } else if (Boolean.FALSE.toString().equals(value)) {

                    object = Boolean.FALSE;

                } else {

                    final String message = String.format(
                            ParameterHandler.ERROR_INVALID_BOOLEAN_VALUE,
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
                            ParameterHandler.ERROR_INVALID_CHAR_VALUE,
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

                // coding methods might throw EncodingException;
                object = this.objectStringConverter.isEncodedObject(value) ?
                         this.objectStringConverter.decodeValue(value) :

                         "null".equals(value) ? null :

                         // unquote() and getStaticField might throw a
                         // ParameterException;
                         this.isStringStartingOrEndingWithQuote(value) ?
                                 this.unquote(value) : getStaticField(value);
            }

        } catch (final NumberFormatException
                     | NullPointerException
                     | EncodingException ex) {

            // just wrap non-ParameterExceptions here;
            throw new ParameterException( ex.getMessage() );
        }

        return object;
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
                    ParameterHandler.ERROR_CLASS_NOT_FOUND,
                    className);

            throw new ParameterException(message);
        }

        return parent;
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

            parameterNames[i] = ParameterHandler.ARG_NAME_PREFIX + i;
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

            if ( !this.isDefinedValue(parameterValue) ) {

                final String message = String.format(
                        ParameterHandler.ERROR_PARAMETER_VALUE_UNDEFINED,
                        parameterName);

                throw new ParameterException(message);
            }

            parameterValues[i] = parameterValue;
        }

        return parameterValues;
    }


    /* --------------  methods for resolving method parameters  ------------- */

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
     *     if any <code>String</code> value does not match its given type,
     *     or if parsing/decoding fails for any reason.
     *
     * @see #evaluateExpression(Class, Object)
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
            final String   parameterName  = parameterNames[i];
            final String   parameterValue = parameterValues[i];

            try {

                // might throw a ParameterException;
                final Object object = this.evaluateExpression(
                        parameterType,
                        parameterValue);

                objects[i] = object;

            } catch (final ParameterException ex) {

                // give an informational message, including the parameter name;
                final String message = String.format(
                        ParameterHandler.ERROR_PARAMETER_VALUE_INVALID,
                        parameterName,
                        parameterType.getName(),
                        ex.getMessage());

                throw new ParameterException(message);
            }
        }

        return objects;
    }

    /**
     * Checks whether the type of a given object complies with a wrapper class
     * type. This test is required for passing return values (which are always
     * objects) to methods with primitive types as parameters.
     *
     * @param type
     *     type which is assumed to be primitive, for example
     *     <code>int.class</code>.
     * @param object
     *     object whose type is assumed to comply with a wrapper class, for
     *     example <code>Integer.class</code>
     *
     * @return
     *     <code>true</code> if and only if the type of the given object
     *     complies with a wrapper class type.
     */
    private boolean compliesObjectTypeWithWrapperType (
            final Class<?> type,
            final Object   object) {

        final Class<?> wrapperClass =
                ParameterHandler.PRIMITIVE_DATA_TYPE_MAPPINGS.get(type);

        // isAssignableFrom() might throw a NullPointerException (should never
        // happen here, since object provides a valid Class instance);
        return wrapperClass != null ?
               wrapperClass.isAssignableFrom( object.getClass() ) : false;
    }


    /* --------------------------  helping methods  ------------------------- */

    /**
     * Checks whether a given key or value is defined, that is it does not equal
     * <code>null</code> and consists of at least one character.
     *
     * @param value  <code>String</code> to be checked.
     *
     * @return  <code>true</code> if and only if the given value is defined.
     */
    private boolean isDefinedValue (final String value) {

        return value != null && value.length() > 0;
    }

    /**
     * Removes the quotes (<code>"..."</code>) from a given <code>String</code>.
     *
     * @param value
     *     <code>String</code> to be unquoted.
     *
     * @return
     *     the unquoted <code>String</code>.
     *
     * @throws ParameterException
     *     if the given <code>String</code> is improperly quoted.
     */
    private String unquote (final String value) throws ParameterException {

        if (value.startsWith("\"") && value.endsWith("\"")) {

            return value.substring(1, value.length() - 1);

        } else {

            final String message = String.format(
                    ParameterHandler.ERROR_IMPROPER_STRING_QUOTES,
                    value);

            throw new ParameterException (message);
        }
    }

    /**
     * Checks whether a given <code>String</code> starts or ends with a quote.
     *
     * @param value
     *     value to be checked.
     *
     * @return
     *     <code>true</code> if and only if the given <code>String</code> starts
     *     or ends with a quote.
     */
    private boolean isStringStartingOrEndingWithQuote (final String value) {

        return value.startsWith("\"") || value.endsWith("\"");
    }

    /**
     * Returns the <code>static</code> field which is indicated by the given
     * <code>String</code>, for example, <code>"java.lang.System.out"</code> .
     *
     * @param value
     *     <code>String</code> which indicates a <code>static</code> field.
     *
     * @return
     *     the <code>static</code> field indicated by the given
     *     <code>String</code>.
     *
     * @throws ParameterException
     *     if the given <code>String</code> is invalid or does not denote a
     *     <code>static</code> field.
     */
    private Object getStaticField (final String value)
            throws ParameterException {

        try {

            final int index = value.lastIndexOf('.');

            // might throw an IndexOutOfBoundsException;
            final String cName = value.substring(0, index);
            final String fName = value.substring(index + 1, value.length());

            // might throw a ClassNotFoundException or LinkageError or
            // ExceptionInInitializerError;
            final Class<?> c = Class.forName(cName);

            final Field field = c.getDeclaredField(fName);

            // might throw a SecurityException;
            field.setAccessible(true);

            // might throw an IllegalAccess-, IllegalArgument- or
            // NullPointerException, or an ExceptionInInitializerError;
            return field.get(null);

        } catch (final IndexOutOfBoundsException
                     | ClassNotFoundException
                     | LinkageError  // includes ExceptionInInitializerError
                     | NoSuchFieldException
                     | SecurityException
                     | IllegalArgumentException
                     | IllegalAccessException
                     | NullPointerException ex) {

            final String message = String.format(
                    ParameterHandler.ERROR_STATIC_FIELD_NOT_FOUND,
                    value);

            throw new ParameterException(message);
        }
    }
}
