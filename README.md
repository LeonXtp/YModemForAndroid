# YModemForAndroid
-
YModem For Android is a library that easy to transmit file data with some terminal devices like BloothLE using [ymodem protocol](https://en.wikipedia.org/wiki/YMODEM). 

## Get Started
-
Supported URI formats:
``` 
"file:///mnt/sdcard/image.png" // from SD card
"assets://image.png" // from assets
```

##### Initiation
``` java
        ymodemBLE = new YmodemBLE.Builder()
                .with(this)
                .filePath("assets://demo.bin")
                .fileName("demo.bin")
                .checkMd5("lsfjlhoiiw121241l241lgljaf")
                .callback(new YModemListener() {
                    @Override
                    public void onDataReady(byte[] data) {
                        //send this data[] to your ble component here...
                    }

                    @Override
                    public void onProgress(int currentSent, int total) {
                        //the progress of the file data has transmitted
                    }

                    @Override
                    public void onSuccess() {
                        //we are well done with md5 checked
                    }

                    @Override
                    public void onFailed() {
                        //the task has failed for several times of trying
                    }
                }).build();

        ymodemBLE.start();
```

##### Received data from terminal
When you received response from the ble terminal, tell ymodemBLE to handle it:

``` java
ymodemBLE.onReceiveData(data);
```
The param data should be byte array.

##### Stop
``` java
ymodemBLE.stop();
```
Just enjoy it!

## About 

The concrete ymodem protocol implemented in this library:

```
 * MY YMODEM IMPLEMTATION
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
 ```

## License

MIT