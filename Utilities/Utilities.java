package Utilities;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utilities {

    /**
     * Returns a hexadecimal encoded SHA-256 hash for the input String.
     *
     * @param data
     * @return string with Hash
     */
    public static BigInteger createHash(String data) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes("UTF-8"));
            return new BigInteger(hash);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Use javax.xml.bind.DatatypeConverter class in JDK to convert byte array
     * to a hexadecimal string. Note that this generates hexadecimal in upper case.
     *
     * @param hash
     * @return string with hash in hexadecimal
     */
    public static String bytesToHex(byte[] hash) {
        return DatatypeConverter.printHexBinary(hash);
    }

    public static int get32bitHashValue(BigInteger bigInteger){
        return Math.abs(bigInteger.intValue());
    }

    public static long getTimestamp(){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        return timestamp.getTime();
    }

    /**
     * Serializes an object
     * @param object
     * @return
     */
    public static byte[] serializeObject(Object object){

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(buffer);
            oos.writeObject(object);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return buffer.toByteArray();
    }

    public static Object deserializeObject(byte[] data){

        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = null;

        Object object = null;
        try {
            ois = new ObjectInputStream(bis);
            try {
                object = ois.readObject();
                ois.close();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return object;
    }
}
