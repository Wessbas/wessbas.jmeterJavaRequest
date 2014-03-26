package net.sf.markov4jmeter.javarequest;

import java.util.concurrent.ConcurrentHashMap;

/**
 * This class provides methods for storing and reading thread-related variables.
 *
 * <p> Each thread is identified by its thread ID which is used as a key for
 * requesting a thread-related set of variables from a global hash map. Each set
 * of variables is even stored in a hash map, with variables being identified by
 * their names. Hash maps are implemented as {@link ConcurrentHashMap} instances
 * for values being read and written concurrently.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 * @since    1.5
 */
public class VariablesPool {

    /** Hash map which assigns hash maps to thread (IDs). */
    private volatile static
            ConcurrentHashMap<Long, ConcurrentHashMap<String, Object>> hashMap =
            new ConcurrentHashMap<Long, ConcurrentHashMap<String, Object>>(1);


    /* **************************  public methods  ************************** */


    /**
     * Checks whether a given key is contained in the hash map which is
     * assigned to the current thread (ID).
     *
     * @param key  key to be checked.
     *
     * @return
     *     <code>true</code> if and only if the given key is contained in the
     *     hash map of the current thread.
     */
    public static boolean containsKey (final String key) {

        return VariablesPool.getHashMapForThread().containsKey(key);
    }

    /**
     * Inserts a given key/value pair into the hash map which is assigned to
     * the current thread (ID).
     *
     * @param key    key to be inserted.
     * @param value  value to be assigned to the given key.
     */
    public static void put (final String key, final Object value) {

        VariablesPool.getHashMapForThread().put(key, value);
    }

    /**
     * Returns the value assigned to a given key in the hash map which is
     * assigned to the current thread (ID).
     *
     * @param key
     *     key whose value shall be read.
     *
     * @return
     *     the value which is assigned to the given key, or <code>null</code>
     *     if no mapping is available.
     */
    public static Object get (final String key) {

        return VariablesPool.getHashMapForThread().get(key);
    }

    /**
     * Removes a given key (including its assigned value) from the hash map
     * which is assigned to the current thread (ID).
     *
     * @param key  key to be removed.
     * @return
     *     the removed value which was assigned to the given key, or
     *     <code>null</code> if the key is not contained in the hash map.
     */
    public static Object remove (final String key) {

        return VariablesPool.getHashMapForThread().remove(key);
    }


    /* **************************  private methods  ************************* */


    /**
     * Returns the hash map which is assigned to the current thread (ID). In
     * case no hash map is assigned yet, a new instance will be created.
     *
     * @return  the hash map which is assigned to the current thread (ID).
     */
    private static ConcurrentHashMap<String, Object> getHashMapForThread () {

        ConcurrentHashMap<String, Object> hashMapForThread;  // to be returned;

        long id = Thread.currentThread().getId();

        // initialize a thread-related hash map;
        hashMapForThread = new ConcurrentHashMap<String, Object>(1);

        // putIfAbsent operates atomically;
        ConcurrentHashMap<String, Object> value =
                VariablesPool.hashMap.putIfAbsent(id, hashMapForThread);

        return value == null ? hashMapForThread : value;
    }
}