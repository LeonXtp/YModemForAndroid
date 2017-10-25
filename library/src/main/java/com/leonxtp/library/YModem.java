package com.leonxtp.library;

import android.content.Context;

import java.io.IOException;

/**
 * THE YMODEM:
 * *SENDER: ANDROID APP *------------------------------------------* RECEIVER: BLE DEVICE*
 * HELLO BOOTLOADER ---------------------------------------------->*
 * <---------------------------------------------------------------* C
 * SOH 00 FF filename0fileSizeInByte0MD5[90] ZERO[38] CRC CRC----->*
 * <---------------------------------------------------------------* ACK C
 * STX 01 FE data[1024] CRC CRC ---------------------------------->*
 * <---------------------------------------------------------------* ACK
 * STX 02 FF data[1024] CRC CRC ---------------------------------->*
 * <---------------------------------------------------------------* ACK
 * ...
 * ...
 * <p>
 * STX 08 F7 data[1000] CPMEOF[24] CRC CRC ----------------------->*
 * <---------------------------------------------------------------* ACK
 * EOT ----------------------------------------------------------->*
 * <---------------------------------------------------------------* ACK
 * SOH 00 FF ZERO[128] ------------------------------------------->*
 * <---------------------------------------------------------------* ACK
 * <---------------------------------------------------------------* MD5_OK
 * <p>
 * Created by leonxtp on 2017/9/16.
 * Modified by leonxtp on 2017/9/16
 */

public class YModem implements FileStreamThread.DataRaderListener {

    private static final int STEP_HELLO = 0x00;
    private static final int STEP_FILE_NAME = 0x01;
    private static final int STEP_FILE_BODY = 0x02;
    private static final int STEP_EOT = 0x03;
    private static final int STEP_END = 0x04;
    private static int CURR_STEP = STEP_HELLO;

    private static final byte ACK = 0x06; /* ACKnowlege */
    private static final byte NAK = 0x15; /* Negative AcKnowlege */
    private static final byte CAN = 0x18; /* CANcel character */
    private static final byte ST_C = 'C';
    private static final String MD5_OK = "MD5_OK";
    private static final String MD5_ERR = "MD5_ERR";

    private Context mContext;
    private String filePath;
    private String fileNameString = "LPK001_Android";
    private String fileMd5String = "63e7bb6eed1de3cece411a7e3e8e763b";
    private YModemListener listener;

    private TimeOutHelper timerHelper = new TimeOutHelper();
    private FileStreamThread streamThread;

    //bytes has been sent of this transmission
    private int bytesSent = 0;
    //package data of current sending, used for int case of fail
    private byte[] currSending = null;
    private int packageErrorTimes = 0;
    private static final int MAX_PACKAGE_SEND_ERROR_TIMES = 6;
    //the timeout interval for a single package
    private static final int PACKAGE_TIME_OUT = 6000;

    /**
     * Construct of the YModemBLE,you may don't need the fileMD5 checking,remove it
     *
     * @param filePath       absolute path of the file
     * @param fileNameString file name for sending to the terminal
     * @param fileMd5String  md5 for terminal checking after transmission finished
     * @param listener
     */
    public YModem(Context context, String filePath,
                  String fileNameString, String fileMd5String,
                  YModemListener listener) {
        this.filePath = filePath;
        this.fileNameString = fileNameString;
        this.fileMd5String = fileMd5String;
        this.mContext = context;
        this.listener = listener;
    }

    /**
     * Start the transmission
     */
    public void start() {
        sayHello();
    }

    /**
     * Stop the transmission when you don't need it or shut it down in accident
     */
    public void stop() {
        bytesSent = 0;
        currSending = null;
        packageErrorTimes = 0;
        if (streamThread != null) {
            streamThread.release();
        }
        timerHelper.stopTimer();
        timerHelper.unRegisterListener();
    }

    /**
     * Method for the outer caller when received data from the terminal
     */
    public void onReceiveData(byte[] respData) {
        //Stop the package timer
        timerHelper.stopTimer();
        if (respData != null && respData.length > 0) {
            L.f("YModem received " + respData.length + " bytes.");
            switch (CURR_STEP) {
                case STEP_HELLO:
                    handleHello(respData);
                    break;
                case STEP_FILE_NAME:
                    handleFileName(respData);
                    break;
                case STEP_FILE_BODY:
                    handleFileBody(respData);
                    break;
                case STEP_EOT:
                    handleEOT(respData);
                    break;
                case STEP_END:
                    handleEnd(respData);
                    break;
                default:
                    break;
            }
        } else {
            L.f("The terminal do responsed something, but received nothing??");
        }
    }

    /**
     * ==============================================================================
     * Methods for sending data begin
     * ==============================================================================
     */
    private void sayHello() {
        streamThread = new FileStreamThread(mContext, filePath, this);
        CURR_STEP = STEP_HELLO;
        L.f("sayHello!!!");
        byte[] hello = YModemUtil.getYModelHello();
        sendPackageData(hello);
    }

    private void sendFileName() {
        CURR_STEP = STEP_FILE_NAME;
        L.f("sendFileName");
        try {
            int fileByteSize = streamThread.getFileByteSize();
            byte[] fileNamePackage = YModemUtil.getFileNamePackage(fileNameString, fileByteSize
                    , fileMd5String);
            sendPackageData(fileNamePackage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startSendFileData() {
        CURR_STEP = STEP_FILE_BODY;
        L.f("startSendFileData");
        streamThread.start();
    }

    //Callback from the data reading thread when a data package is ready
    @Override
    public void onDataReady(byte[] data) {
        sendPackageData(data);
    }

    private void sendEOT() {
        CURR_STEP = STEP_EOT;
        L.f("sendEOT");
        if (listener != null) {
            listener.onDataReady(YModemUtil.getEOT());
        }
    }

    private void sendEND() {
        CURR_STEP = STEP_END;
        L.f("sendEND");
        if (listener != null) {
            try {
                listener.onDataReady(YModemUtil.getEnd());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendPackageData(byte[] packageData) {
        if (listener != null && packageData != null) {
            currSending = packageData;
            //Start the timer, it will be cancelled when reponse received,
            // or trigger the timeout and resend the current package data
            timerHelper.startTimer(timeoutListener, PACKAGE_TIME_OUT);
            listener.onDataReady(packageData);
        }
    }

    /**
     * ==============================================================================
     * Method for handling the response of a package
     * ==============================================================================
     */
    private void handleHello(byte[] value) {
        int character = value[0];
        if (character == ST_C) {//Receive "C" for "HELLO"
            L.f("Received 'C'");
            packageErrorTimes = 0;
            sendFileName();
        } else {
            handleOthers(character);
        }
    }

    //The file name package was responsed
    private void handleFileName(byte[] value) {
        if (value.length == 2 && value[0] == ACK && value[1] == ST_C) {//Receive 'ACK C' for file name
            L.f("Received 'ACK C'");
            packageErrorTimes = 0;
            startSendFileData();
        } else if (value[0] == ST_C) {//Receive 'C' for file name, this package should be resent
            L.f("Received 'C'");
            handlePackageFail("Received 'C' without 'ACK' after sent file name");
        } else {
            handleOthers(value[0]);
        }
    }

    private void handleFileBody(byte[] value) {
        if (value.length == 1 && value[0] == ACK) {//Receive ACK for file data
            L.f("Received 'ACK'");
            packageErrorTimes = 0;
            bytesSent += currSending.length;
            try {
                if (listener != null) {
                    listener.onProgress(bytesSent, streamThread.getFileByteSize());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            streamThread.keepReading();

        } else if (value.length == 1 && value[0] == ST_C) {
            L.f("Received 'C'");
            //Receive C for file data, the ymodem cannot handle this circumstance, transmission failed...
            handlePackageFail("Received 'C' after sent file data");
        } else {
            handleOthers(value[0]);
        }
    }

    private void handleEOT(byte[] value) {
        if (value[0] == ACK) {
            L.f("Received 'ACK'");
            packageErrorTimes = 0;
            sendEND();
        } else if (value[0] == ST_C) {//As we haven't received ACK, we should resend EOT
            handlePackageFail("Received 'C' after sent EOT");
        } else {
            handleOthers(value[0]);
        }
    }

    private void handleEnd(byte[] character) {
        if (character[0] == ACK) {//The last ACK represents that the transmission has been finished, but we should validate the file
            L.f("Received 'ACK'");
            packageErrorTimes = 0;
        } else if ((new String(character)).equals(MD5_OK)) {//The file data has been checked,Well Done!
            L.f("Received 'MD5_OK'");
            stop();
            if (listener != null) {
                listener.onSuccess();
            }
        } else if ((new String(character)).equals(MD5_ERR)) {//Oops...Transmission Failed...
            L.f("Received 'MD5_ERR'");
            stop();
            if (listener != null) {
                listener.onFailed("MD5 check failed!!!");
            }
        } else {
            handleOthers(character[0]);
        }
    }

    private void handleOthers(int character) {
        if (character == NAK) {//We need to resend this package as the terminal failed when checking the crc
            L.f("Received 'NAK'");
            handlePackageFail("Received NAK");
        } else if (character == CAN) {//Some big problem occurred, transmission failed...
            L.f("Received 'CAN'");
            if (listener != null) {
                listener.onFailed("Received CAN");
            }
            stop();
        }
    }

    //Handle a failed package data ,resend it up to MAX_PACKAGE_SEND_ERROR_TIMES times.
    //If still failed, then the transmission failed.
    private void handlePackageFail(String reason) {
        packageErrorTimes++;
        L.f("Fail:" + reason + " for " + packageErrorTimes + " times");
        if (packageErrorTimes < MAX_PACKAGE_SEND_ERROR_TIMES) {
            sendPackageData(currSending);
        } else {
            //Still, we stop the transmission, release the resources
            stop();
            if (listener != null) {
                listener.onFailed(reason);
            }
        }
    }

    /* The InputStream data reading thread was done */
    @Override
    public void onFinish() {
        sendEOT();
    }

    //The timeout listener
    private TimeOutHelper.ITimeOut timeoutListener = new TimeOutHelper.ITimeOut() {
        @Override
        public void onTimeOut() {
            L.f("------ time out ------");
            if (currSending != null) {
                handlePackageFail("package timeout...");
            }
        }
    };

    public static class Builder {
        private Context context;
        private String filePath;
        private String fileNameString;
        private String fileMd5String;
        private YModemListener listener;

        public Builder with(Context context) {
            this.context = context;
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileNameString = fileName;
            return this;
        }

        public Builder checkMd5(String fileMd5String) {
            this.fileMd5String = fileMd5String;
            return this;
        }

        public Builder callback(YModemListener listener) {
            this.listener = listener;
            return this;
        }

        public YModem build() {
            return new YModem(context, filePath, fileNameString, fileMd5String, listener);
        }

    }

}
