package edu.upenn.cis.cis555.util;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Comparator;

/**
 * This is the comparator to compare two byte keys.
 */
public class KeyComparator implements Comparator<byte[]>, Serializable {

    @Override
    public int compare(byte[] key1, byte[] key2) {
        return new BigInteger(key1).compareTo(new BigInteger(key2));
    }
}
