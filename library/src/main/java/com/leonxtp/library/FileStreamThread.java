package com.leonxtp.library;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread for reading input Stream and encapsulating into a ymodem package
 * <p>
 * Created by leonxtp on 2017/9/16.
 * Modified by leonxtp on 2017/9/16
 */

public class FileStreamThread extends Thread {

    private Context mContext;
    private InputStream inputStream = null;
    private DataRaderListener listener;
    private String filePath;
    private AtomicBoolean isDataAcknowledged = new AtomicBoolean(false);
    private boolean isKeepRunning = false;
    private int fileByteSize = 0;

    public FileStreamThread(Context mContext, String filePath, DataRaderListener listener) {
        this.mContext = mContext;
        this.filePath = filePath;
        this.listener = listener;
    }

    public int getFileByteSize() throws IOException {
        if (fileByteSize == 0 || inputStream == null) {
            initStream();
        }
        return fileByteSize;
    }

    @Override
    public void run() {
        try {
            prepareData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void prepareData() throws IOException {
        initStream();
        byte[] block = new byte[1024];
        int dataLength;
        byte blockSequence = 1;//The data package of a file is actually started from 1
        isDataAcknowledged.set(true);
        isKeepRunning = true;
        while (isKeepRunning) {

            if (!isDataAcknowledged.get()) {
                try {
                    //We need to sleep for a while as the sending 1024 bytes data from ble would take several seconds
                    //In my circumstances, this can be up to 3 seconds.
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            if ((dataLength = inputStream.read(block)) == -1) {
                L.f("The file data has all been read...");
                if (listener != null) {
                    onStop();
                    listener.onFinish();
                }
                break;
            }

            byte[] packige = YModemUtil.getDataPackage(block, dataLength, blockSequence);

            if (listener != null) {
                listener.onDataReady(packige);
            }

            blockSequence++;
            isDataAcknowledged.set(false);
        }

    }

    /**
     * When received response from the terminal ,we should keep the thread keep going
     */
    public void keepReading() {
        isDataAcknowledged.set(true);
    }

    public void release() {
        onStop();
        listener = null;
    }

    private void onStop() {
        isKeepRunning = false;
        isDataAcknowledged.set(false);
        fileByteSize = 0;
        onReadFinished();
    }

    private void initStream() {
        if (inputStream == null) {
            try {
                inputStream = YModemUtil.getInputStream(mContext, filePath);
                fileByteSize = inputStream.available();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void onReadFinished() {
        if (inputStream != null) {
            try {
                inputStream.close();
                inputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public interface DataRaderListener {
        void onDataReady(byte[] data);

        void onFinish();
    }

}
