package net.sf.markov4jmeter.javarequest;

import java.lang.reflect.Method;

import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

/**
 * Class which provides functions for invoking a method regarding its parent,
 * signature and parameters to be provided by a given
 * <code>JavaSamplerContext</code> instance.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 */
public class MethodInvoker {

    /** Error message for the case that a method could not be found. */
    private final static String ERROR_METHOD_NOT_FOUND =
            "Could not find method \"%s\" in class \"%s\".";

    /** Error message for the case that a method invocation fails. */
    private final static String ERROR_METHOD_INVOCATION_FAILED =
            "Invocation of method \"%s\" failed: %s";


    /* *************************  global variables  ************************* */


    /** Name of the parameter which is associated with a (fully qualified)
     *  class name. */
    private final String parameterName_className;

    /** Name of the parameter which is associated with an (encoded) object. */
    private final String parameterName_objectString;

    /** Name of the parameter which is associated with a method signature. */
    private final String parameterName_methodSignature;

    /** Instance for detecting methods in the class path of an application. */
    private final MethodFinder methodFinder;

    /** Instance for requesting and processing Java Sampler parameters */
    private final ParametersReader parameterReader;


    /* ***************************  constructors  *************************** */

    /**
     * Constructor for a Method Invoker.
     *
     * @param parameterName_className
     *     name of the parameter which is associated with a (fully qualified)
     *     class name.
     * @param parameterName_objectName
     *     name of the parameter which is associated with an (encoded) object.
     * @param parameterName_methodSignature
     *     name of the parameter which is associated with a method signature.
     * @param validateSignatures
     *     <code>true</code> if and only if the format of method signatures
     *     shall be validated.
     */
    public MethodInvoker (
            final String parameterName_className,
            final String parameterName_objectName,
            final String parameterName_methodSignature,
            final boolean validateSignatures) {

        this.parameterName_className = parameterName_className;
        this.parameterName_objectString = parameterName_objectName;
        this.parameterName_methodSignature = parameterName_methodSignature;

        this.parameterReader = new ParametersReader();
        this.methodFinder = new MethodFinder(validateSignatures);
    }


    /* **************************  public methods  ************************** */


    /**
     * Invokes a method regarding to its parent, signature and parameters
     * provided by a given <code>javaSamplerContext</code> instance.
     *
     * @param javaSamplerContext
     *     <code>javaSamplerContext</code> instance which provides parent,
     *     method signature and parameters of the method invocation.
     *
     * @return
     *     the return value of the invoked method as an object.
     *
     * @throws ParameterException
     *     if any necessary parameter could not be retrieved from the given
     *     <code>javaSamplerContext</code> instance.
     * @throws InvocationException
     *     if no matching method could be found or method invocation fails for
     *     any reason.
     */
    public Object invokeMethod (final JavaSamplerContext javaSamplerContext)
            throws ParameterException, InvocationException {

        final String methodSignature = this.parameterReader.getMethodSignature(
                javaSamplerContext, this.parameterName_methodSignature);

        final Object parent = this.parameterReader.getParent(
                javaSamplerContext,
                this.parameterName_className,
                this.parameterName_objectString);

        final Method method;

        // try to find a method of the given class/signature combination;
        // might throw an InvocationException;
        method = this.getMethodByClassNameAndSignature(
                parent instanceof Class ? (Class<?>)parent : parent.getClass(),
                methodSignature);

        // might throw a ParameterException;
        final Object[] parameters = this.parameterReader.getMethodParameters(
                javaSamplerContext,
                method);

        return this.invokeMethod(
                parent,
                method,
                parameters);
    }


    /* **************************  private methods  ************************* */


    /**
     * Detects a method with a specified signature in a given class or object.
     *
     * @param parent
     *     the class or object in which the method shall be detected.
     * @param methodSignature
     *     signature of the method to be detected.
     * @return
     *     the method which matches to the given class/object and signature.
     * @throws InvocationException
     *     if no matching method could be found.
     */
    private Method getMethodByClassNameAndSignature (
            final Class<?> parent,
            final String methodSignature) throws InvocationException {

        // try to find a method of the given class/signature combination;
        final Method method =
                this.methodFinder.getMethodByClassNameAndSignature(
                        parent,
                        methodSignature);

        if (method == null) {  // method found?

            final String message = String.format(
                    MethodInvoker.ERROR_METHOD_NOT_FOUND,
                    methodSignature,
                    parent.getName());

            throw new InvocationException(message);
        }

        return method;
    }

    /**
     * Invokes a method with a given sequence of parameters.
     *
     * @param parent
     *     the class or object in which the method shall be called.
     * @param method
     *     the method to be invoked.
     * @param parameters
     *     the parameters to be passed to the method.
     *
     * @return
     *     the return value of the method as an object.
     *
     * @throws InvocationException
     *     if the invocation fails for any reason.
     */
    private Object invokeMethod (
            final Object parent,
            final Method method,
            final Object[] parameters) throws InvocationException {

        final Object result;  // to be returned;

        try {

            // allow access of private methods; might throw a SecurityException;
            method.setAccessible(true);

            // might throw an IllegalAccess-, IllegalArgument-,
            // InvocationTarget- or NullPointerException, or an
            // ExceptionInInitializerError;
            result = method.invoke(parent, parameters);

        } catch (final Exception ex) {

            final String message = String.format(
                    MethodInvoker.ERROR_METHOD_INVOCATION_FAILED,
                    method.getName(),
                    ex.getMessage());

            throw new InvocationException(message);
        }

        return result;
    }
}
