# YModemForAndroid

YModem For Android is a library that easy to transmit file data with some terminal devices like BloothLE using [ymodem protocol](https://en.wikipedia.org/wiki/YMODEM). 

## Notice

Though suitable, this library doesn't supply a ble component for transmitting data with terminal, it's your responsibility to encapsulate your own. 

## Get Started

Supported URI formats:
``` 
"file:///storage/emulated/0/filename.bin" // from SD card
"assets://image.png" // from assets
```

### Initiation
``` java
        ymodem = new YModem.Builder()
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
```

### Start transmission
``` java
ymodem.start();
```

### Received data from terminal
When you received response from the ble terminal, tell ymodem to handle it:

``` java
ymodem.onReceiveData(data);
```
The param data should be byte array.

### Stop
``` java
ymodem.stop();
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
 
## Reference
[Wikipedia YMODEM](https://en.wikipedia.org/wiki/YMODEM)
[xmodem、ymodem、zmodem](http://web.cecs.pdx.edu/~rootd/catdoc/guide/TheGuide_226.html)
[aesirot ymodem on github](https://github.com/aesirot/ymodem)

## License

MIT