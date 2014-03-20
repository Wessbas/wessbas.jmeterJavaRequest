package net.sf.markov4jmeter.javarequest;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

/**
 * Java Sampler Client for invoking methods by using the Java Reflection API.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 */
public class ReflectiveJavaSamplerClient extends AbstractJavaSamplerClient {

    /** Default key which is associated with the lately returned value
     *  of an invoked method. */
    protected final static String DEFAULT_KEY_RETURN_VALUE = "${rv}";

    /** Name of the parameter which is associated with a (fully qualified)
     *  class name. */
    private final static String PARAMETER_NAME__CLASS_NAME = "class";

    /** Name of the parameter which is associated with an (encoded) object. */
    private final static String PARAMETER_NAME__OBJECT_STRING = "object";

    /** Name of the parameter which is associated with a method signature. */
    private final static String PARAMETER_NAME__METHOD_SIGNATURE = "method";

    /** Name of the (optional) parameter which is associated with the variable
     *  name to be used for storing the return value of an invoked method.*/
    private final static String PARAMETER_NAME__RETURN_VALUE = "rvariable";

    /** (Maximum) number of parameters named <code>arg0</code>,
     *  <code>arg1</code>, <code>arg2</code>, ...
     *
     *  <p><u>Note</u>: this is required for filling in the GUI form of a Java
     *  Sampler in JMeter with parameter input rows; user input for these
     *  parameters will be accepted, any other self-defined parameters will be
     *  ignored by JMeter.
     */
    private final static int NUMBER_OF_DEFAULT_METHOD_PARAMETERS = 20;

    /** <code>true</code> if and only if the format of method signatures shall
     *  be validated.
     *
     *  @see MethodFinder#MethodFinder(boolean)
     */
    private final static boolean VALIDATE_BOOLEAN = true;


    /* --------------------  Sampler-specific properties  ------------------- */

    /** Success message to be returned by this Sampler. */
    private final static String MESSAGE_SUCCESS = "Operation successful.";

    /** Failure message to be returned by this Sampler. */
    private final static String MESSAGE_FAILURE = "An exception occurred: %s";

    /** Failure response code to be returned by this Sampler. */
    private final static String RESPONSE_CODE_FAILURE = "500";

    /** Encoding to be used for response (text) data. */
    private final static String ENCODING = "UTF-8";


    /* *************************  global variables  ************************* */


    /** Instance for invoking a method regarding its parent, signature and
     *  parameters to be provided by a given <code>JavaSamplerContext</code>
     *  instance.*/
    private final MethodInvoker methodInvoker;

    /** Handler for storing and retrieving values of (JMeter) variables. */
    private final VariableHandler variableHandler;


    /* ***************************  constructors  *************************** */


    /**
     * Constructor for a Reflective Java Sampler Client.
     */
    public ReflectiveJavaSamplerClient () {

        super();

        this.methodInvoker = new MethodInvoker(
                ReflectiveJavaSamplerClient.PARAMETER_NAME__CLASS_NAME,
                ReflectiveJavaSamplerClient.PARAMETER_NAME__OBJECT_STRING,
                ReflectiveJavaSamplerClient.PARAMETER_NAME__METHOD_SIGNATURE,
                ReflectiveJavaSamplerClient.VALIDATE_BOOLEAN);

        this.variableHandler = new VariableHandler();
    }


    /* **************************  public methods  ************************** */


    @Override
    public SampleResult runTest (final JavaSamplerContext javaSamplerContext) {

        final SampleResult result = new SampleResult();  // to be returned;

        result.sampleStart();  // start time measurement;

        try {

            // might throw an InvocationException;
            final Object rValue =
                    this.methodInvoker.invokeMethod(javaSamplerContext);

            this.variableHandler.storeValue(
                    javaSamplerContext,
                    ReflectiveJavaSamplerClient.PARAMETER_NAME__RETURN_VALUE,
                    ReflectiveJavaSamplerClient.DEFAULT_KEY_RETURN_VALUE,
                    rValue);


            result.setResponseOK();  // sets "OK" message by default;
            result.setResponseCodeOK();

            result.setDataType(SampleResult.TEXT);

            result.setResponseData(
                    (String) rValue,
                    ReflectiveJavaSamplerClient.ENCODING);

            result.setResponseMessage(
                    ReflectiveJavaSamplerClient.MESSAGE_SUCCESS);

        } catch (final Exception ex) {

            final String message = String.format(
                    ReflectiveJavaSamplerClient.MESSAGE_FAILURE,
                    ex.getMessage());

            result.setResponseMessage(message);

            result.setResponseCode(
                    ReflectiveJavaSamplerClient.RESPONSE_CODE_FAILURE);
        }

        finally {

            result.sampleEnd();  // stop time measurement;
        }

        return result;
    }

    @Override
    public Arguments getDefaultParameters() {

        // do not use super.getDefaultParameters(), since it does not work;
        Arguments defaultParameters = new Arguments();

        defaultParameters.addArgument(
                ReflectiveJavaSamplerClient.PARAMETER_NAME__CLASS_NAME,
                null);

        defaultParameters.addArgument(
                ReflectiveJavaSamplerClient.PARAMETER_NAME__OBJECT_STRING,
                null);

        defaultParameters.addArgument(
                ReflectiveJavaSamplerClient.PARAMETER_NAME__METHOD_SIGNATURE,
                null);

        defaultParameters.addArgument(
                ReflectiveJavaSamplerClient.PARAMETER_NAME__RETURN_VALUE,
                null);

        for (int i = 0; i < ReflectiveJavaSamplerClient.
                NUMBER_OF_DEFAULT_METHOD_PARAMETERS; i++) {

            defaultParameters.addArgument(
                    ParametersReader.ARG_NAME_PREFIX + i, null);
        }

        return defaultParameters;
    }
}