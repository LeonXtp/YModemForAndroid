package com.leonxtp.library;

import java.util.Locale;

/**
 * Thanks to Universal-Image-Loader:
 * https://github.com/nostra13/Android-Universal-Image-Loader/blob/master/library/src/main/java/com/nostra13/universalimageloader/core/download/ImageDownloader.java
 */
public enum SourceScheme {

    FILE("file"), ASSETS("assets"), UNKNOWN("");

    private String scheme;
    private String uriPrefix;

    SourceScheme(String scheme) {
        this.scheme = scheme;
        uriPrefix = scheme + "://";
    }

    /**
     * Defines scheme of incoming URI
     *
     * @param uri URI for scheme detection
     * @return SourceScheme of incoming URI
     */
    public static SourceScheme ofUri(String uri) {
        if (uri != null) {
            for (SourceScheme s : values()) {
                if (s.belongsTo(uri)) {
                    return s;
                }
            }
        }
        return UNKNOWN;
    }

    private boolean belongsTo(String uri) {
        return uri.toLowerCase(Locale.US).startsWith(uriPrefix);
    }

    /**
     * Removed scheme part ("scheme://") from incoming URI
     */
    public String crop(String uri) {
        if (!belongsTo(uri)) {
            throw new IllegalArgumentException(String.format("URI [%1$s] doesn't have expected scheme [%2$s]", uri, scheme));
        }
        return uri.substring(uriPrefix.length());
    }
}