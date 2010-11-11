package com.limegroup.gnutella.xml;

/**
 * The exception is thrown when a GML Document's template cannot be loaded.
 *
 * @author Ron Vogl
 */
public class SchemaNotFoundException
    extends Exception {
    public SchemaNotFoundException() {}

    public SchemaNotFoundException(String message) {
        super(message);
    }
}
