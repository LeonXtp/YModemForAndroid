package com.leonxtp.library;

/**
 * Listener of the transmission process
 */
public interface YModemListener {

    /* the data package has been encapsulated */
    void onDataReady(byte[] data);

    /*just the file data progress*/
    void onProgress(int currentSent, int total);

    /* the file has been correctly sent to the terminal */
    void onSuccess();

    /* the task has failed with several remedial measures like retrying some times*/
    void onFailed(String reason);

}