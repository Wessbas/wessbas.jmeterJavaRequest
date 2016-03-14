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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import biz.source_code.base64Coder.Base64Coder;

/**
 * Class for encoding and decoding <code>String</code>s to <code>Object</code>s
 * and vice versa.
 *
 * <p>This converter builds on the {@link Base64Coder} class by Christian
 * d'Heureuse, used for encoding and decoding data in <i>Base 64</i> format as
 * described in RFC 1521.
 *
 * @see Base64Coder
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 */
public class ObjectStringConverter {


    /** Prefix of an "encoding <code>String</code>" marking. */
    private final static String MARK_PREFIX = "\\obj{";

    /** Suffix of an "encoding <code>String</code>" marking. */
    private final static String MARK_SUFFIX = "}";

    /** Error message for the case that the removal of a <code>String</code>
     *  marking failed. */
    private final static String ERROR_UNMARKING_FAILED =
            "could not unmark value string \"%s\"";

    /** Error message for the case that a <code>String</code> could not be
     *  decoded to an <code>Object</code>. */
    private final static String ERROR_DECODING_FAILED =
            "could not decode string \"%s\" (%s)";

    /** Error message for the case that an <code>Object</code> could not be
     * encoded to a <code>String</code>. */
    private final static String ERROR_ENCODING_FAILED =
            "could not encode value \"%s\" (%s)";


    /* **************************  public methods  ************************** */


    /**
     * Decodes a given <code>String</code> to its represented
     * <code>Object</code>.
     *
     * @param value
     *     <code>String</code> to be decoded to an <code>Object</code>.
     *
     * @return
     *     a decoded <code>Object</code> which is represented through the given
     *     <code>String</code>.
     *
     * @throws EncodingException
     *     if the given <code>String</code> does not represent an
     *     <code>Object</code>, or if decoding fails for any other reason.
     */
    public Object decodeValue (final String value) throws EncodingException {

        final Object object;

        try {

            // might throw an EncodingException;
            final String valueString = this.unmarkEncodingString(value);

            // might throw an IllegalArgument-, IO-, Security- or
            // ClassNotFoundException;
            object = this.string2Object(valueString);

        } catch (final Exception ex) {

            final String message = String.format(
                    ObjectStringConverter.ERROR_DECODING_FAILED,
                    value,
                    ex.getMessage());

            throw new EncodingException(message);
        }

        return object;
    }

    /**
     * Encodes a given <code>Serializable</code> instance to a
     * <code>String</code> representation.
     *
     * @param value
     *     <code>Serializable</code> instance to be encoded to a
     *     <code>String</code>.
     *
     * @return
     *     an encoding <code>String</code> which represents the given
     *     <code>Serializable</code> instance.
     *
     * @throws EncodingException
     *     if the given <code>Serializable</code> instance cannot be encoded
     *     for any reason.
     */
    public String encodeValue (final Serializable value)
            throws EncodingException {

        final String encoding;  // to be returned;

        try {

            // might throw an IO- or SecurityException;
            final String valueString = this.object2String(value);

            encoding = this.markEncodingString(valueString);

        } catch (final Exception ex) {

            final String message = String.format(
                    ObjectStringConverter.ERROR_ENCODING_FAILED,
                    value,
                    ex.getMessage());

            throw new EncodingException(message);
        }

        return encoding;
    }

    /**
     * Checks whether a given value is a <code>String</code> which represents
     * an encoded <code>Object</code>.
     *
     * @param value
     *     value to be checked.
     *
     * @return
     *     <code>true</code> if and only if the given value is a
     *     <code>String</code> which represents an encoded <code>Object</code>.
     */
    public boolean isEncodedObject (final Object value) {

        if ( value == null || !(value instanceof String) ) {

            return false;
        }

        final String valueString = (String) value;

        return valueString.startsWith(ObjectStringConverter.MARK_PREFIX) &&
               valueString.endsWith(ObjectStringConverter.MARK_SUFFIX);
    }


    /* *************************  protected methods  ************************ */


    /**
     * Adds an "encoding <code>String</code>" marking to a given encoding
     * <code>String</code>.
     *
     * @param encodingString  encoding <code>String</code> to be marked.
     *
     * @return  the marked encoding <code>String</code>.
     */
    protected String markEncodingString (final String encodingString) {

        return ObjectStringConverter.MARK_PREFIX +
               encodingString +
               ObjectStringConverter.MARK_SUFFIX;
    }

    /**
     * Removes the "encoding <code>String</code>" marking from a given encoding
     * <code>String</code>.
     *
     * @param encodingString
     *     encoding <code>String</code> whose marking shall be removed.
     *
     * @return
     *     the encoding <code>String</code> without marking.
     *
     * @throws EncodingException
     *     if the removal of a marking failed due to an unmarked
     *     <code>String</code>.
     */
    protected String unmarkEncodingString (final String encodingString)
            throws EncodingException {

        if ( this.isEncodedObject(encodingString) ) {

            // might throw IndexOutOfBoundsException (should not happen here);
            return encodingString.substring(
                    ObjectStringConverter.MARK_PREFIX.length(),
                    encodingString.length() -
                    ObjectStringConverter.MARK_SUFFIX.length());

        } else {

            final String message = String.format(
                    ObjectStringConverter.ERROR_UNMARKING_FAILED,
                    encodingString);

            throw new EncodingException(message);
        }
    }


    /* **************************  private methods  ************************* */


    /**
     * Decodes a given <code>String</code> to its represented
     * <code>Object</code>.
     *
     * @param str
     *     <code>String</code> to be decoded to an <code>Object</code>;
     *     the given <code>String</code> must be an <u>encoded</u>
     *     <code>Object</code>.
     *
     * @return
     *     An <code>Object</code> which is represented through the given
     *     <code>String</code>.
     *
     * @throws IllegalArgumentException
     *     if the passed <code>String</code> is not valid <i>Base64</i> encoded
     *     data.
     * @throws IOException
     *     if any I/O error occurs.
     * @throws SecurityException
     *     if any illegal operation is being detected.
     * @throws ClassNotFoundException
     *     if the class of the <code>Object</code> cannot be found.
     */
    private Object string2Object (final String str)
            throws IllegalArgumentException,
                   IOException,
                   SecurityException,
                   ClassNotFoundException {

        Object object = null;

        ObjectInputStream objectInputStream = null;

        try {

            final byte [] data;

            // might throw an IllegalArgumentException;
            data = Base64Coder.decode(str);

            final ByteArrayInputStream byteArrayInputStream =
                    new ByteArrayInputStream(data);

            // might throw a StreamCorrupted-, IO-, Security-, or
            // NullPointerException (StreamCorrupted- is an IOException,
            // NullPointerException should never happen here);
            // note: might additionally throw an EOFException!
            objectInputStream = new ObjectInputStream(byteArrayInputStream);

            // might throw a ClassNotFound-, InvalidClass-, StreamCorrupted-,
            // OptionalData- or IOException (InvalidClass-, StreamCorrupted-,
            // OptionalDataException denote IOExceptions);
            object = objectInputStream.readObject();

        } catch (final EOFException ex) {

            final String message = String.format(
                    ObjectStringConverter.ERROR_DECODING_FAILED,
                    str);

            throw new IOException (message);

        } finally {

            if (objectInputStream != null) {

                try {

                    // might throw an IOException;
                    objectInputStream.close();

                } catch (final IOException ex) {

                    // ignore exception, since this is the "finally" block;
                    // TODO: exception message should be written to log file;
                }
            }
        }

        return object;
    }

    /**
     * Encodes a given <code>Object</code> to a representative
     * <code>String</code>.
     *
     * @param object
     *     <code>Object</code> to be encoded to a <code>String</code>.
     *
     * @return
     *     A <code>String</code> representation of the given
     *     <code>Object</code>.
     *
     * @throws IOException
     *     if any I/O error occurs.
     * @throws SecurityException
     *     if any illegal operation is being detected.
     */
    private String object2String (final Serializable object)
            throws IOException, SecurityException {

        String str;  // to be returned;

        ObjectOutputStream objectOutputStream = null;

        try {

            final ByteArrayOutputStream byteArrayOutputStream =
                    new ByteArrayOutputStream();

            // might throw an IO-, Security- or NullPointerException
            // (NullPointerException should never happen here);
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);

            // might throw an InvalidClass-, NotSerializable- or IOException;
            // (all these types denote IOExceptions);
            objectOutputStream.writeObject(object);

            str = new String(Base64Coder.encode(
                    byteArrayOutputStream.toByteArray()));
        } finally {

            if (objectOutputStream != null) {

                try {

                    // might throw an IOException;
                    objectOutputStream.close();

                } catch (final IOException ex) {

                    // ignore exception, since this is the "finally" block;
                    // TODO: exception message should be written to log file;
                }
            }
        }

        return str;
    }
}
