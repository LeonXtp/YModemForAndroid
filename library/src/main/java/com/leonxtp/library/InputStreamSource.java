package com.leonxtp.library;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Get InputStream from different source, files from sd card/assets supported.
 * <p>
 * Created by leonxtp on 2017/9/17.
 * Modified by leonxtp on 2017/9/17
 */

public class InputStreamSource {

    private static final int BUFFER_SIZE = 32 * 1024;
    private static final String ERROR_UNSUPPORTED_SCHEME = "Unsupported file source";

    InputStream getStream(Context context, String imageUri) throws IOException {
        switch (SourceScheme.ofUri(imageUri)) {

            case FILE:
                return getStreamFromFile(imageUri);

            case ASSETS:
                return getStreamFromAssets(context, imageUri);

            case UNKNOWN:
            default:
                return getStreamFromOtherSource(imageUri);
        }
    }

    private InputStream getStreamFromFile(String fileUri) throws IOException {
        String filePath = SourceScheme.FILE.crop(fileUri);
        return new BufferedInputStream(new FileInputStream(filePath), BUFFER_SIZE);
    }

    private InputStream getStreamFromAssets(Context context, String fileUri) throws IOException {
        String filePath = SourceScheme.ASSETS.crop(fileUri);
        return context.getAssets().open(filePath);
    }

    private InputStream getStreamFromOtherSource(String fileUri) throws IOException {
        throw new UnsupportedOperationException(String.format(ERROR_UNSUPPORTED_SCHEME, fileUri));
    }

}
