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

/**
 * Wrapper class for return variables, used for distinction purposes.
 *
 * <p> Since return values of methods invoked by Samplers do not denote values
 * to be evaluated, they need to be marked by being wrapped into an instance of
 * this class. Method {@link ParameterHandler#evaluateExpression(Class, Object)}
 * will not evaluate any of these instances respectively their values. If they
 * were evaluated, a value of type <code>String</code> would be interpreted as
 * argument originating from the Test Plan, which would possibly remove quotes
 * wrongly or result in a false interpretation of the value as a path of a
 * <code>static</code> field.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 * @since    1.7
 */
public class ReturnVariable implements Serializable {

    /** Default serial version ID. */
    private static final long serialVersionUID = 1L;

    /** The embedded value. */
    private final Object value;


    /**
     * Constructor of a Return Variable.
     *
     * @param value  the value to be embedded.
     */
    public ReturnVariable (final Object value) {

        this.value = value;
    }

    /**
     * Returns the embedded value.
     *
     * @return  the embedded value.
     */
    public Object getValue () {

        return this.value;
    }
}
