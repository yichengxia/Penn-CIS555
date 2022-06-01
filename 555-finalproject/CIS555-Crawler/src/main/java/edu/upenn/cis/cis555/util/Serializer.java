package edu.upenn.cis.cis555.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * This is a serializer to convert between bytes and object.
 */
public class Serializer {
    
    /**
     * Convert object to bytes
     * @param object
     * @return byte res
     */
    public static byte[] toBinary(Serializable object) {
        byte[] res = null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(object);
                oos.flush();
                res = baos.toByteArray();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * Convert bytes to object
     * @param bytes
     * @return object res
     */
    public static Object toObject(byte[] bytes) {
        Object res = null;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            try (ObjectInputStream ois = new ObjectInputStream(bais)) {
                res = ois.readObject();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }
}
