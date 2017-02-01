
package com.clockwork.export;

import java.io.IOException;

/**
 * <code>Savable</code> is an interface for objects that can be serialized
 * using jME's serialization system. 
 * 
 */
public interface Savable {
    void write(JmeExporter ex) throws IOException;
    void read(JmeImporter im) throws IOException;
}