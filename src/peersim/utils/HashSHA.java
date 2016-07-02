package peersim.utils;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashSHA {

    /**
     * Makes a Hash to route a prefix over DHT
     *
     * @param b
     * @param prefix
     * @return
     */
    private static MessageDigest md;
    private static byte[] buffer, digest;
    private static String hash = "";

    /**
     * Makes a Hash to route a prefix over DHT
     *
     * @param b
     * @param prefix
     * @return
     */
    public static BigInteger applyHash(double input) {
        hash = "";
        buffer = Double.toString(input).getBytes();

        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        md.update(buffer);
        digest = md.digest();

        for (byte aux : digest) {
            int b = aux & 0xff;
            if (Integer.toHexString(b).length() == 1) {
                hash += "0";
            }
            hash += Integer.toHexString(b);
        }
        BigInteger bi = new BigInteger(hash, 16);
        //BigInteger bi = new BigInteger(digest);

        return bi;
    }

    public static BigInteger applyHash(String input) throws UnsupportedEncodingException {
        hash = "";
        buffer = input.getBytes();

        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        md.update(buffer);
        digest = md.digest();

        for (byte aux : digest) {
            int b = aux & 0xff;
            if (Integer.toHexString(b).length() == 1) {
                hash += "0";
            }
            hash += Integer.toHexString(b);
        }

        BigInteger bi = new BigInteger(hash, 16);
        return bi;
    }

}
