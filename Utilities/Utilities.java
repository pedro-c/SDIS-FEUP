package Utilities;

import javax.xml.bind.DatatypeConverter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.Timestamp;

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
            return new BigInteger(hash).abs();

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

    public static int get32bitHashValue(BigInteger bigInteger) {
        return Math.abs(bigInteger.intValue());
    }

    public static long getTimestamp() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        return timestamp.getTime();
    }
}
