package net.sf.markov4jmeter.javarequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import biz.source_code.base64Coder.Base64Coder;

/**
 * Converter class for encoding and decoding <code>String</code>s to
 * <code>Object</code>s and vice versa.
 *
 * <p>This converter builds on the {@link Base64Coder} class by Christian
 * d'Heureuse, used for encoding and decoding data in Base64 format as described
 * in RFC 1521.
 *
 * @see Base64Coder
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 */
public class ObjectStringConverter {

    /** <code>Lock</code> to be used for <code>static</code> decoding methods.*/
    private final Lock lock_decode = new ReentrantLock();

    /** <code>Lock</code> to be used for <code>static</code> encoding methods.*/
    private final Lock lock_encode = new ReentrantLock();


    /**
     * Decodes a given <code>String</code> to its represented
     * <code>Object</code>.
     *
     * @param str
     *     <code>String</code> to be decoded to an <code>Object</code>.
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
    public Object string2Object (final String str)
            throws IllegalArgumentException,
                   IOException,
                   SecurityException,
                   ClassNotFoundException {

        Object object;

        ObjectInputStream objectInputStream = null;

        try {

            final byte [] data;

            // TODO: Base64Coder methods should not be static;
            this.lock_decode.lock();

            try {

                // might throw an IllegalArgumentException;
                data = Base64Coder.decode(str);

            } finally {

                this.lock_decode.unlock();
            }

            final ByteArrayInputStream byteArrayInputStream =
                    new ByteArrayInputStream(data);

            // might throw a StreamCorrupted-, IO-, Security- or
            // NullPointerException (StreamCorrupted- is an IOException,
            // NullPointerException should never happen here);
            objectInputStream = new ObjectInputStream(byteArrayInputStream);

            // might throw a ClassNotFound-, InvalidClass-, StreamCorrupted-,
            // OptionalData- or IOException (InvalidClass-, StreamCorrupted-,
            // OptionalDataException denote IOExceptions);
            object  = objectInputStream.readObject();

        } finally {

            try {

                // might throw an IOException;
                objectInputStream.close();

            } catch (final IOException ex) {

                // ignore exception, since this is the "finally" block;
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
    public String object2String (final Serializable object)
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

            // TODO: Base64Coder methods should not be static;
            this.lock_encode.lock();

            try {

                str = new String(Base64Coder.encode(
                        byteArrayOutputStream.toByteArray()));

            } finally {

                this.lock_encode.unlock();
            }

        } finally {

            if (objectOutputStream != null) {

                try {

                    // might throw an IOException;
                    objectOutputStream.close();

                } catch (final IOException ex) {

                    // ignore exception, since this is the finally block;
                }
            }
        }

        return str;
    }
}
