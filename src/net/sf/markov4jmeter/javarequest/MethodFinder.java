package net.sf.markov4jmeter.javarequest;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Finder class for detecting methods in the class path of an application.
 *
 * <p>This class provides exactly one <code>public</code> method which allows
 * the detection of a method with a specified signature in the current class
 * path.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 * @since    1.7
 */
public class MethodFinder {

    /** Regular expression for method/class/package name validation. */
    private final static String REGEX__NAME =
            "(\\p{Alpha}|_)[\\p{Alnum}|_]*";

    /** Regular expression for (fully qualified) class name validation. */
    private final static String REGEX__CLASS_NAME =
            REGEX__NAME + "(\\." + REGEX__NAME + ")*";

    /** Regular expression for array type validation. */
    private final static String REGEX__ARRAY_TYPE =
            "\\[+([ZBCDFIJS]|(L" + REGEX__CLASS_NAME + ";))";

    /** Regular expression for type validation. */
    private final static String REGEX__TYPE =
            "((" + REGEX__CLASS_NAME + ")|(" + REGEX__ARRAY_TYPE + "))";

    /** Regular expression for type sequence validation. */
    private final static String REGEX__TYPE_SEQUENCE =
            REGEX__TYPE + "\\s*(\\s*,\\s*" + REGEX__TYPE + "\\s*)*)?";

    /** Regular expression for method signature validation. */
    private final static String METHOD_SIGNATURE_FORMAT =
            "^\\s*" + REGEX__NAME +
            "\\s*\\((\\s*" + REGEX__TYPE_SEQUENCE + "\\s*\\)" +
            "(\\s*:\\s*" + REGEX__TYPE + ")?\\s*$";

    /** Error message for the case that an invalid method signature has been
     *  passed to any finder method. */
    private final static String ERROR_INVALID_METHOD_SIGNATURE =
            "Invalid method signature: \"%s\"";

    /** Default value for the {@link #validateSignatures} flag. */
    private final static boolean DEFAULT_VALIDATE_SIGNATURES = true;


    /** <code>true</code> if and only if method signatures shall be validated
     *  before being parsed. If enabled, signature validation will be done via
     *  regular expressions at the beginning of the
     *  {@link #getMethodByClassNameAndSignature(String, String)} method; the
     *  validation process itself might take a small amount of additional time,
     *  but format errors will be detected earlier to avoid further processing
     *  of a possibly invalid method signature.
     */
    private final boolean validateSignatures;


    /* ***************************  constructors  *************************** */


    /**
     * Constructor for a Method Finder.
     *
     * @param validateSignatures
     *     <code>true</code> if and only if method signatures shall be validated
     *     before being parsed. If enabled, signature validation will be done
     *     via regular expressions at the beginning of the
     *     {@link #getMethodByClassNameAndSignature(String, String)} method; the
     *     validation process itself might take a small amount of additional
     *     time, but format errors will be detected earlier to avoid further
     *     processing of a possibly invalid method signature.
     */
    public MethodFinder (final boolean validateSignatures) {

        this.validateSignatures = validateSignatures;
    }

    /**
     * Constructor for a Method Finder, with signature validation being enabled
     * by default.
     */
    public MethodFinder () {

        this(MethodFinder.DEFAULT_VALIDATE_SIGNATURES);
    }


    /* **************************  public methods  ************************** */


    /**
     * Returns a <code>Method</code> instance representing a method of a given
     * signature, which is detectable in the current class path.
     * A signature needs to be formatted as
     * <blockquote>
     *   <i>methodName</i>(<i>parameterType1</i>,
     *                     <i>parameterType2</i>, ... ) : <i>returnType</i>
     * </blockquote>
     * whereas each type must be specified in <i>FieldType</i> notation, as
     * defined in <i>The Java Virtual Machine Specification - Java SE 7 Edition
     * </i> (page 78 ff.). For example, the signature
     * <blockquote>
     *     <code>someMethod(I, [[I, [Ljava.lang.String;):F</code>
     * </blockquote>
     * denotes a method named <i>someMethod</i>, with parameters of type
     * <code>int</code>, <code>int[][]</code> and <code>String[]</code>,
     * respectively. The return type is <code>float</code>.
     *
     * <p>FieldTypes can be distinguished between
     * <i>BaseTypes</i>, <i>Arrays</i> and <i>ObjectTypes</i>
     * (classes/interfaces).
     * <ul>
     *   <li><p>BaseTypes must be specified by their associated characters:
     *       <p>
     *       <table border=1>
     *         <tr><th> BaseType    </th><th> Character </th></tr>
     *         <tr><td> boolean      </td><td>    Z     </td></tr>
     *         <tr><td> byte         </td><td>    B     </td></tr>
     *         <tr><td> char         </td><td>    C     </td></tr>
     *         <tr><td> double       </td><td>    D     </td></tr>
     *         <tr><td> float        </td><td>    F     </td></tr>
     *         <tr><td> int          </td><td>    I     </td></tr>
     *         <tr><td> long         </td><td>    J     </td></tr>
     *         <tr><td> short        </td><td>    S     </td></tr>
     *       </table>
     *    <li>Arrays must be specified as FieldTypes with leading <code>[</code>
     *        characters, indicating the regarding array dimension. For example,
     *        <code>[I</code> denotes an <code>int[]</code> array,
     *        <code>[[F</code> denotes a <code>float[][]</code> array etc.
     *    <li> ObjectTypes, that is a class or interface, must be specified by
     *         their fully qualified names, including a leading <code>L</code>
     *         character and a closing semicolon, for example,
     *         <code>Ljava.lang.String;</code> denotes the type
     *         <code>String</code>.
     * </ul>
     *
     * <p><u>Note:</u> <code>Method</code> instances associate <i>BaseTypes</i>
     * (<code>boolean</code>, <code>int</code>, <code>float</code>, ...) with
     * their related wrapper classes, any conversion is done implicitly.
     *
     * @param parent
     *     class in which the method is defined.
     * @param methodSignature
     *     the method signature to be searched for.
     *
     * @return
     *     a valid instance of {@link Method} if the method could be detected,
     *     or <code>null</code> if the method is unknown.
     */
    public Method getMethodByClassNameAndSignature (
            final Class<?> parent,
            final String methodSignature) {

        Method method = null;  // to be returned;

        if ( !this.validateSignatures ||
              this.isSignatureValid(methodSignature)) {

            try {

                final ArrayList<Class<?>> parameterTypes =
                        new ArrayList<Class<?>>();

                // methodSignatureTokens might be null, if any parsing error
                // occurs -> NullPointerException to be thrown hereinafter;
                final MethodSignatureTokens methodSignatureTokens =
                        this.parseMethodSignature(methodSignature);

                for (final String pTypeStr :
                     methodSignatureTokens.parameterTypes) {

                    // forName() might throw a LinkageError, an
                    // ExceptionInInitializerError or a ClassNotFoundException;
                    parameterTypes.add(this.getClassByTypeString(pTypeStr));
                }

                // might throw NoSuchMethod-, NullPointer- or SecurityException;
                method = parent.getDeclaredMethod(
                        methodSignatureTokens.methodName,
                        parameterTypes.toArray(new Class<?>[]{}));

            } catch (final Exception ex) {

                // keep method being null for indicating an error;
            }
        }

        return method;
    }


    /* **************************  private methods  ************************* */


    /**
     * Checks whether a given method signature is <i>valid</i>. A signature is
     * valid if and only if its format conforms to the required format of the
     * {@link #getMethodByClassNameAndSignature(String, String)} method.
     *
     * @param methodSignature
     *     method signature to be checked.
     *
     * @return
     *     <code>true</code> if and only if the given method signature is valid.
     *
     * @see #getMethodByClassNameAndSignature(String, String)
     */
    private boolean isSignatureValid (final String methodSignature) {

        return methodSignature != null &&
            methodSignature.matches(MethodFinder.METHOD_SIGNATURE_FORMAT);
    }

    /**
     * Returns the <code>Class</code> representation of a given type. A type
     * must be specified in <i>FieldType</i> notation, as defined in <i>The Java
     * Virtual Machine Specification - Java SE 7 Edition</i> (page 78 ff.).
     * FieldTypes can be distinguished between <i>BaseTypes</i>, <i>Arrays</i>
     * and <i>ObjectTypes</i> (classes/interfaces).
     * <ul>
     *   <li><p>BaseTypes must be passed as their associated characters:
     *       <p>
     *       <table border=1>
     *         <tr><th> BaseType    </th><th> Character </th></tr>
     *         <tr><td> boolean      </td><td>    Z     </td></tr>
     *         <tr><td> byte         </td><td>    B     </td></tr>
     *         <tr><td> char         </td><td>    C     </td></tr>
     *         <tr><td> double       </td><td>    D     </td></tr>
     *         <tr><td> float        </td><td>    F     </td></tr>
     *         <tr><td> int          </td><td>    I     </td></tr>
     *         <tr><td> long         </td><td>    J     </td></tr>
     *         <tr><td> short        </td><td>    S     </td></tr>
     *       </table>
     *    <li>Arrays must be specified as FieldTypes with leading <code>[</code>
     *        characters, indicating the regarding array dimension. For example,
     *        <code>[I</code> denotes an <code>int[]</code> array,
     *        <code>[[F</code> denotes a <code>float[][]</code> array etc.
     *    <li> ObjectTypes, that is a class or interface, must be specified by
     *         their fully qualified names, including a leading <code>L</code>
     *         character and a closing semicolon, for example,
     *         <code>Ljava.lang.String;</code> denotes the type
     *         <code>String</code>.
     *
     * @param type
     *     type whose <code>Class</code> representation shall be determined.
     *
     * @return
     *     a valid {@link Class} instance, or <code>null</code> if no class
     *     representation could be determined.
     */
    private Class<?> getClassByTypeString (final String type) {

        Class<?> c;  // to be returned;

        switch (type) {

            case "Z" :

                c = boolean.class;
                break;

            case "B" :

                c = byte.class;
                break;

            case "C" :

                c = char.class;
                break;

            case "S" :

                c = short.class;
                break;

            case "I" :

                c = int.class;
                break;

            case "J" :

                c = long.class;
                break;

            case "F" :

                c = float.class;
                break;

            case "D" :

                c = double.class;
                break;

            default:  // no BaseType  ->  ObjectType or Array type;

                try {

                    // forName() might throw a LinkageError, an
                    // ExceptionInInitializerError or a ClassNotFoundException;
                    c = Class.forName(type);

                } catch (final ClassNotFoundException ex) {

                    c = null;  // error;
                }
        }

        return c;
    }

    /**
     * Parses a given method signature for method name, sequence of parameters
     * and return type.
     *
     * <p>The signature needs to be formatted as required by the
     * {@link #getMethodByClassNameAndSignature(String, String)} method.
     *
     * @param methodSignature
     *     the method signature to be parsed.
     *
     * @return
     *     an instance which contains the elements of the method signature as
     *     <code>String</code> tokens.
     *
     * @see #getMethodByClassNameAndSignature(String, String)
     */
    private MethodSignatureTokens parseMethodSignature (
            final String methodSignature) {

        MethodSignatureTokens methodSignatureTokens;  // to be returned;

        final int i = methodSignature.indexOf('('),
                  j = methodSignature.lastIndexOf(')'),
                  k = methodSignature.lastIndexOf(':');

        try {

            final String methodName = methodSignature.substring(0, i).trim();
            final String pTypesStr  = methodSignature.substring(i + 1, j).trim();

            // return type is optional -> null, if undefined;
            final String returnType = (k >= 0) ?
                    methodSignature.substring(k + 1).trim() : null;

            // split() returns an array of length 1 if pTypesStr only consists
            // of whitespace, so this case needs to be considered separately;
            // otherwise invocations of methods with no parameters will fail;
            final String[] parameterTypes = pTypesStr.matches("\\s*") ?
                    new String[]{} :
                    pTypesStr.split("\\s*,\\s*");

            methodSignatureTokens = new MethodSignatureTokens(
                    methodName,
                    parameterTypes,
                    returnType);

        } catch (final StringIndexOutOfBoundsException ex) {

            final String message = String.format(
                    MethodFinder.ERROR_INVALID_METHOD_SIGNATURE,
                    methodSignature);

            System.err.println(message);
            methodSignatureTokens = null;  // error;
        }

        return methodSignatureTokens;
    }


    /* *************************  internal classes  ************************* */


    /**
     * Internal class for collecting tokens which result from parsing a method
     * signature. Tokens are stored as <String>String</code>s, e.g. method name,
     * parameter types and return type.
     *
     * @author   Eike Schulz (esc@informatik.uni-kiel.de)
     * @version  1.0
     */
    private static class MethodSignatureTokens {

        /** Name of the method. */
        final String methodName;

        /** Parameter types, ordered as their related values  need to be
         *  passed to the method. */
        final String[] parameterTypes;

        /** Return type of the method. */
        final String returnType;

        /**
         * Constructor for a collection of method signature tokens.
         *
         * @param methodName
         *     name of the method.
         * @param parameterTypes
         *     parameter types, ordered as their related values  need to be
         *     passed to the method.
         * @param returnType
         *     return type of the method.
         */
        public MethodSignatureTokens (
                final String methodName,
                final String[] parameterTypes,
                final String returnType) {

            this.methodName     = methodName;
            this.parameterTypes = parameterTypes;
            this.returnType     = returnType;
        }

        /**
         * {@inheritDoc}
         * <p> This method is implemented for <b>testing purposes</b> only.
         */
        @Override
        public String toString () {

            final StringBuffer sb = new StringBuffer();

            for (int i = 0, n = this.parameterTypes.length - 1; i <= n; i++) {

                sb.append(this.parameterTypes[i]);

                if (i < n) {

                    sb.append(",");
                }
            }

            return
                this.methodName + "(" + sb.toString() + "):" + this.returnType;
        }
    }
}
