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
 * @since    1.7
 */
public class FlushVariablesSamplerClient extends AbstractJavaSamplerClient {

    /** Name of the (optional) parameter which is associated with the variable
     *  name to be used for storing the return value of an invoked method.*/
    private final static String PARAMETER_NAME__FLUSH_VARIABLES = "variables";


    /* --------------------  Sampler-specific properties  ------------------- */

    /** Success message to be returned by this Sampler. */
    private final static String MESSAGE_SUCCESS =
            "Operation successful. Flushed variables: %s";

    /** Failure message to be returned by this Sampler. */
    private final static String MESSAGE_FAILURE = "An exception occurred: %s";

    /** Failure response code to be returned by this Sampler. */
    private final static String RESPONSE_CODE_FAILURE = "500";


    /* *************************  global variables  ************************* */


    /** Instance for requesting and processing Java Sampler parameters */
    private final FlushParameterHandler flushParametersHandler;


    /* ***************************  constructors  *************************** */


    /**
     * Constructor for a Flush Variables Handler.
     */
    public FlushVariablesSamplerClient () {

        this.flushParametersHandler = new FlushParameterHandler();
    }


    /* **************************  public methods  ************************** */


    @Override
    public SampleResult runTest (final JavaSamplerContext javaSamplerContext) {

        final SampleResult result = new SampleResult();  // to be returned;

        try {

            final String[] variableNames =
                    this.flushParametersHandler.getVariableNames(
                            javaSamplerContext,
                            FlushVariablesSamplerClient.
                            PARAMETER_NAME__FLUSH_VARIABLES);

            // might throw a FlushException;
            final String flushedVariables =
                    this.flushParametersHandler.flushVariables(variableNames);

            final String message = String.format(
                    FlushVariablesSamplerClient.MESSAGE_SUCCESS,
                    flushedVariables);

            result.setResponseOK();  // sets "OK" message by default;
            result.setResponseCodeOK();
            result.setDataType(SampleResult.TEXT);
            result.setResponseMessage(message);

        } catch (final FlushException ex) {

            final String message = String.format(
                    FlushVariablesSamplerClient.MESSAGE_FAILURE,
                    ex.getMessage());

            result.setResponseMessage(message);

            result.setResponseCode(
                    FlushVariablesSamplerClient.RESPONSE_CODE_FAILURE);
        }

        return result;
    }

    @Override
    public Arguments getDefaultParameters () {

        // do not use super.getDefaultParameters(), since it does not work;
        Arguments defaultParameters = new Arguments();

        defaultParameters.addArgument(
                FlushVariablesSamplerClient.PARAMETER_NAME__FLUSH_VARIABLES,
                null);

        return defaultParameters;
    }
}
