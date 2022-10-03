package org.example;
import java.io.*;
import java.net.*;

public class GssUtil {

    private static boolean DEBUG = false;
    
    /*
     * Write a token byte[] to OutputStream.
     * Return: 0 on success, -1 on failure
     */
    public static int WriteToken(OutputStream outStream, byte[] outputToken)
    {
        if (DEBUG)
            System.out.println("Entered WriteToken...");
       
        try { 

            /* First send the size of our byte array */
            byte[] size = Util.intToByteArray(outputToken.length);

            if (DEBUG)
                System.out.println("... sending byte array size: " +
                    Util.byteArrayToInt(size));
            outStream.write(size);

            /* Now send our actual byte array */
            if (DEBUG) {
                System.out.println("... sending byte array: ");
                printByteArray(outputToken);
                System.out.println("... outputToken.length = " + 
                    outputToken.length);
            }
            outStream.write(outputToken);

            return 0;

        } catch (IOException e) {

            e.printStackTrace();
            return -1;

        }
    }
   
    /*
     * Read a token byte[] from InputStream.
     * Return byte[] on success, null on failure
     */ 
    public static byte[] ReadToken(InputStream inStream) 
    {
        if (DEBUG)
            System.out.println("Entered ReadToken...");

        byte[] inputTokenBuffer = null;

        try {

            int data;
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            /* First read the incomming array size (first 4 bytes) */
            int array_size = 0;
            byte[] temp = null;
            for (int i = 0; i < 4; i++) {
                data = inStream.read();
                if (DEBUG)
                    System.out.println("ReadToken... read() returned: " + data);
                out.write(data);
            }
            temp = out.toByteArray();
            array_size = Util.byteArrayToInt(temp);
            out.reset();

            if (DEBUG)
                System.out.println("... got byte array size = " + array_size);

            if (array_size < 0)
                return null;

            /* Now read our full array */
            for (int j = 0; j < array_size; j++) {
                data = inStream.read();
                out.write(data);
            }

            if (DEBUG) {
                System.out.println("... got data: ");
                Util.printByteArray(out.toByteArray());
                System.out.println("... returning from ReadToken, success");
            }

            return out.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
   
    /*
     * Print a byte[], for debug purposes.
     */ 
    public static void printByteArray(byte[] input)
    {
        for (int i = 0; i < input.length; i++ ) {
            System.out.format("%02X ", input[i]);
        }
        System.out.println();
    }

    /* Based on http://snippets.dzone.com/posts/show/93 */
    public static byte[] intToByteArray(int input)
    {
        byte[] out = new byte[4];
        for (int i = 0; i < 4; i++) {
            int offset = (out.length - 1 - i) * 8;
            out[i] = (byte) ((input >>> offset) & 0xFF);
        }
        return out;
    }

    /* Based on http://snippets.dzone.com/posts/show/93 */
    public static int byteArrayToInt(byte[] data) 
    {
        if (data == null || data.length != 4) return 0x0;
        return (int)(
                (0xff & data[0]) << 24 |
                (0xff & data[1]) << 16 |
                (0xff & data[2]) << 8  |
                (0xff & data[3])  << 0
                );
    }
    
    public static void printSubString(String first, String second) {
        System.out.printf(" | %-18s=  %s\n", first, second);
    }    
    public static void printSubString(String first, boolean second) {
        String s = Boolean.toString(second);
        printSubString(first, s);
    }
    public static void printSubString(String first, int second) {
        String s = Integer.toString(second);
        printSubString(first, s);
    }
}