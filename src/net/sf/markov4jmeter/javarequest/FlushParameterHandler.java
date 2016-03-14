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

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

/**
 * Handler class for requesting and processing Java Sampler parameters. This
 * class provides methods for requesting the following information from a
 * <code>JavaSamplerContext</code> instance which provides the parameters of
 * a Java Sampler:
 * <ul>
 *   <li> names of parameters to be removed from the current thread context.
 * </ul>
 * The regarding parameter names are not predefined, they must be passed by the
 * invoking instance.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 * @since    1.7
 */
public class FlushParameterHandler extends ParameterHandler {

    /** Error message for the case that a flush operation could not be
     *  completed. */
    private final static String ERROR_FLUSH_FAILED =
            "could not complete flush operation; "
            + "removed variables: %s; "
            + "failed removals: %s";


    /* **************************  public methods  ************************** */


    /**
     * Returns the names of the variables to be flushed, provided by the
     * specified <code>JavaSamplerContext</code> instance. The names must be
     * specified as a names sequence with commas as separators, indicating
     * a format like <code><i>varname1</i>, <i>varname2</i>, ...</code>
     *
     * @param javaSamplerContext
     *     instance which provides the sequence of variables to be flushed.
     * @param parameterName
     *     parameter name for the sequence of variables to be flushed.
     *
     * @return
     *     an array of variable names, ordered according to the provided names
     *     sequence.
     */
    public String[] getVariableNames (
            final JavaSamplerContext javaSamplerContext,
            final String parameterName) {

        final String[] variableNames;  // to be returned;

        final String flushVariablesStr =
                javaSamplerContext.getParameter(parameterName);

        // split() returns an array of length 1 if pTypesStr only consists
        // of whitespace, so this case needs to be considered separately;
        // otherwise invocations of methods with no parameters will fail;
        variableNames =
                flushVariablesStr == null || flushVariablesStr.matches("\\s*") ?
                new String[]{} :
                flushVariablesStr.trim().split("\\s*,\\s*");

        return variableNames;
    }

    /**
     * Removes all variables which are associated with a name of a given names
     * array from the current thread context.
     *
     * @param variableNames
     *     names whose associated variables shall be removed.
     *
     * @return
     *     a sequenced <code>String</code> representation of the removed
     *     variables; if no names have been passed, the returned
     *     <code>String</code> will be "<code>none</code>".
     *
     * @throws FlushException
     *     if one of the given names is malformed, or if for any of the names
     *     no associated variable exists.
     */
    public String flushVariables (final String[] variableNames)
            throws FlushException {

        final LinkedList<String> flushedVariables = new LinkedList<String>();
        final LinkedList<String> flushErrors      = new LinkedList<String>();

        for (final String variableName : variableNames) {

            try {

                // ignore duplicate name occurrences;
                if (flushedVariables.contains(variableName)) {

                    continue;
                }

                // might throw an IllegalArgumentException;
                this.removeVariable(variableName);

                // no exception here -> variable successfully removed;
                flushedVariables.add(variableName);

            } catch (final IllegalArgumentException ex) {

                final String error =
                        "\"" + variableName + "\" (" + ex.getMessage() + ")";

                flushErrors.add(error);
            }
        }

        final String flushedVariablesSeq =
                this.getSequenceString(flushedVariables, true);

        if ( !flushErrors.isEmpty() ) {

            final String flushErrorsSeq =
                    this.getSequenceString(flushErrors, false);

            final String message = String.format(
                    FlushParameterHandler.ERROR_FLUSH_FAILED,
                    flushedVariablesSeq,
                    flushErrorsSeq);

            throw new FlushException(message);
        }

        return flushedVariablesSeq;
    }


    /* *************************  protected methods  ************************ */


    /**
     * Removes the variable which is associated with a given name from the
     * current thread context.
     *
     * @param variableName
     *     name of the variable to be removed.
     *
     * @throws IllegalArgumentException
     *     if the given name is malformed, or if no such named variable exists.
     */
    protected void removeVariable (final String variableName)
            throws IllegalArgumentException {

        // might throw an IllegalArgumentException;
        this.variableHandler.removeVariable(variableName);
    }


    /* **************************  private methods  ************************* */


    /**
     * Returns a <code>String</code> representation for a given list of
     * <code>String</code> tokens.
     *
     * @param tokens
     *     <code>String</code> token list to be represented as a single
     *     <code>String</code>.
     * @param addQuotes
     *     <code>true</code> if and only if each token shall be wrapped into
     *     quotes.
     *
     * @return
     *     a sequenced <code>String</code> representation with each token being
     *     put into quotes optionally; if the token list is empty, the returned
     *     <code>String</code> will be "<code>none</code>".
     *
     * @throws NullPointerException
     *     if <code>null</code> has been passed as token list.
     */
    private String getSequenceString (
            final LinkedList<String> tokens,
            final boolean addQuotes) throws NullPointerException {

        final String str;  // to be returned;

        // might throw a NullPointerException, if tokens == null;
        if ( tokens.isEmpty() ) {

            str = "none";

        } else {

            final String quote = addQuotes ? "\"" : "";
            final Iterator<String> iterator = tokens.iterator();

            // might throw a NoSuchElementException (should never happen here,
            // since the iteration has at least one element);
            final StringBuffer stringBuffer =
                    new StringBuffer(quote + iterator.next() + quote);

            while ( iterator.hasNext() ) {

                stringBuffer.append(", ").
                             append(quote).
                             append(iterator.next()).
                             append(quote);
            }

            str = stringBuffer.toString();
        }

        return str;
    }
}
