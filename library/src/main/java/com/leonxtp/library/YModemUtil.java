package com.leonxtp.library;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Util for encapsulating data package of ymodem protocol
 * <p>
 * Created by leonxtp on 2017/9/16.
 * Modified by leonxtp on 2017/9/16
 */

public class YModemUtil {

    /*This is my concrete ymodem start signal, customise it to your needs*/
    private static final String HELLO = "HELLO BOOTLOADER";

    private static final byte SOH = 0x01; /* Start Of Header with data size :128*/
    private static final byte STX = 0x02; /* Start Of Header with data size : 1024*/
    private static final byte EOT = 0x04; /* End Of Transmission */
    private static final byte CPMEOF = 0x1A;/* Fill the last package if not long enough */

    private static CRC16 crc16 = new CRC16();

    /**
     * Get the first package data for hello with a terminal
     */
    public static byte[] getYModelHello() {
        return HELLO.getBytes();
    }

    /**
     * Get the file name package data
     *
     * @param fileNameString file name in String
     * @param fileByteSize   file byte size of int value
     * @param fileMd5String  the md5 of the file in String
     */
    public static byte[] getFileNamePackage(String fileNameString,
                                            int fileByteSize,
                                            String fileMd5String) throws IOException {

        byte seperator = 0x0;
        String fileSize = fileByteSize + "";
        byte[] byteFileSize = fileSize.getBytes();

        byte[] fileNameBytes1 = concat(fileNameString.getBytes(),
                new byte[]{seperator},
                byteFileSize);

        byte[] fileNameBytes2 = Arrays.copyOf(concat(fileNameBytes1,
                new byte[]{seperator},
                fileMd5String.getBytes()), 128);

        byte seq = 0x00;
        return getDataPackage(fileNameBytes2, 128, seq);
    }

    /**
     * Get a encapsulated package data block
     *
     * @param block      byte data array
     * @param dataLength the actual content length in the block without 0 filled in it.
     * @param sequence   the package serial number
     * @return a encapsulated package data block
     */
    public static byte[] getDataPackage(byte[] block, int dataLength, byte sequence) throws IOException {

        byte[] header = getDataHeader(sequence, block.length == 1024 ? STX : SOH);

        //The last package, fill CPMEOF if the dataLength is not sufficient
        if (dataLength < block.length) {
            int startFil = dataLength;
            while (startFil < block.length) {
                block[startFil] = CPMEOF;
                startFil++;
            }
        }

        //We should use short size when writing into the data package as it only needs 2 bytes
        short crc = (short) crc16.calcCRC(block);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeShort(crc);
        dos.close();

        byte[] crcBytes = baos.toByteArray();

        return concat(header, block, crcBytes);
    }

    /**
     * Get the EOT package
     */
    public static byte[] getEOT() {
        return new byte[]{EOT};
    }

    /**
     * Get the Last package
     */
    public static byte[] getEnd() throws IOException {
        byte seq = 0x00;
        return getDataPackage(new byte[128], 128, seq);
    }

    /**
     * Get InputStream from Assets, you can customize it from the other sources
     *
     * @param fileAbsolutePath absolute path of the file in asstes
     */
    public static InputStream getInputStream(Context context, String fileAbsolutePath) throws IOException {
        return new InputStreamSource().getStream(context, fileAbsolutePath);
    }

    private static byte[] getDataHeader(byte sequence, byte start) {
        //The serial number of the package increases Cyclically up to 256
        byte modSequence = (byte) (sequence % 0x256);
        byte complementSeq = (byte) ~modSequence;

        return concat(new byte[]{start},
                new byte[]{modSequence},
                new byte[]{complementSeq});
    }

    private static byte[] concat(byte[] a, byte[] b, byte[] c) {
        int aLen = a.length;
        int bLen = b.length;
        int cLen = c.length;
        byte[] concated = new byte[aLen + bLen + cLen];
        System.arraycopy(a, 0, concated, 0, aLen);
        System.arraycopy(b, 0, concated, aLen, bLen);
        System.arraycopy(c, 0, concated, aLen + bLen, cLen);
        return concated;
    }
}
