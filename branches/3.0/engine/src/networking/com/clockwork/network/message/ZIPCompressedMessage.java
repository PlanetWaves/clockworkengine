
package com.clockwork.network.message;

import com.clockwork.network.Message;
import com.clockwork.network.serializing.Serializable;

/**
 * Compress a message using this ZIPCompressedMessage class
 *
 */
@Serializable()
public class ZIPCompressedMessage extends CompressedMessage {
    private static int compressionLevel = 6;

    public ZIPCompressedMessage() {
        super();
    }

    public ZIPCompressedMessage(Message msg) {
        super(msg);
    }

    public ZIPCompressedMessage(Message msg, int level) {
        super(msg);
        setLevel(level);
    }

    /**
     * Set the compression level, where 1 is the best compression but slower and 9 is the weakest
     *  compression but the quickest. Default is 6.
     *
     * @param level The level.
     */
    public static void setLevel(int level) {
        compressionLevel = level;
    }

    public int getLevel() { return compressionLevel; }
}