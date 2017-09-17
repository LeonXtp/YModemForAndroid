package com.leonxtp.library;

import android.content.Context;

import java.io.IOException;

/**
 * THE YMODEM:
 * *SENDER: ANDROID APP *------------------------------------------* RECEIVER: BLE DEVICE*
 * 0x31 ---------------------------------------------------------->*
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

public class YmodemBLE implements FileStreamThread.DataRaderListener {

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
    private static final int MAX_PACKAGE_SEND_ERROR_TIMES = 5;
    //the timeout interval for a single package
    private static final int PACKAGE_TIME_OUT = 6000;

    public YmodemBLE(Context context, String fileNameString, String fileMd5String, YModemListener listener) {
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
    }

    /**
     * Method for the outer caller when received data from the terminal
     */
    public void onReceiveData(byte[] respData) {
        //Stop the package timer
        timerHelper.stopTimer();
        if (respData != null && respData.length > 0) {
            switch (CURR_STEP) {
                case STEP_HELLO:
                    handleHello(respData);
                    break;
                case STEP_FILE_NAME:
                    handleFileName(respData);
                    break;
                case STEP_FILE_BODY:
                    handleFileBody(respData[0]);
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
        streamThread = new FileStreamThread(mContext, fileNameString, this);
        CURR_STEP = STEP_HELLO;
        L.f("sayHello!!!");
        byte[] hello = YModemUtil.getYModelHello();
        if (listener != null) {
            listener.onDataReady(hello);
        }
    }

    private void sendFileName() {
        CURR_STEP = STEP_FILE_NAME;
        L.f("sendFileName");
        try {
            int fileByteSize = streamThread.getFileByteSize();
            byte[] hello = YModemUtil.getFileNamePackage(fileNameString, fileByteSize
                    , fileMd5String);
            if (listener != null) {
                listener.onDataReady(hello);
            }
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
        if (listener != null) {
            currSending = data;
            //Start the timer, it will be cancelled when reponse received,
            // or trigger the timeout and resend the current package data
            timerHelper.startTimer(timeoutListener, PACKAGE_TIME_OUT);
            listener.onDataReady(data);
        }
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

    /**
     * ==============================================================================
     * Method for handling the response of a package
     * ==============================================================================
     */
    private void handleHello(byte[] value) {
        int character = value[0];
        if (character == ST_C) {//Receive "C" for "HELLO"
            packageErrorTimes = 0;
            sendFileName();
        } else {
            handleOthers(character);
        }
    }

    //The file name package was responsed
    private void handleFileName(byte[] value) {
        if (value.length == 2 && value[0] == ACK && value[1] == ST_C) {//Receive 'ACK C' for file name
            packageErrorTimes = 0;
            startSendFileData();
        } else if (value[0] == ST_C) {//Receive 'C' for file name, this package should be resent
            handlePackageFail();
        } else {
            handleOthers(value[0]);
        }
    }

    private void handleFileBody(int character) {
        if (character == ACK) {//Receive ACK for file data
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

        } else if (character == ST_C) {
            //Receive C for file data, the ymodem cannot handle this circumstance, transmission failed...
            if (listener != null) {
                listener.onFailed();
            }
        } else {
            handleOthers(character);
        }
    }

    private void handleEOT(byte[] value) {
        if (value[0] == ACK) {
            packageErrorTimes = 0;
            sendEND();
        } else if (value[0] == ST_C) {//As we haven't received ACK, we should resend EOT
            handlePackageFail();
        } else {
            handleOthers(value[0]);
        }
    }

    private void handleEnd(byte[] character) {
        if (character[0] == ACK) {//The last ACK represents that the transmission has been finished, but we should validate the file
            packageErrorTimes = 0;
        } else if ((new String(character)).equals(MD5_OK)) {//The file data has been checked,Well Done!
            stop();
            if (listener != null) {
                listener.onSuccess();
            }
        } else if ((new String(character)).equals(MD5_ERR)) {//Oops...Transmission Failed...
            stop();
            if (listener != null) {
                listener.onFailed();
            }
        } else {
            handleOthers(character[0]);
        }
    }

    private void handleOthers(int character) {
        if (character == NAK) {//We need to resend this package as the terminal failed when checking the crc
            handlePackageFail();
        } else if (character == CAN) {//Some big problem occurred, transmission failed...
            stop();
        }
    }

    //Handle a failed package data ,resend it up to MAX_PACKAGE_SEND_ERROR_TIMES times.
    //If still failed, then the transmission failed.
    private void handlePackageFail() {
        packageErrorTimes++;
        if (packageErrorTimes < MAX_PACKAGE_SEND_ERROR_TIMES) {
            if (listener != null) {
                listener.onDataReady(currSending);
            }
        } else {
            //Still, we stop the transmission, release the resources
            stop();
            if (listener != null) {
                listener.onFailed();
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
            if (currSending != null) {
                handlePackageFail();
            }
        }
    };

}
