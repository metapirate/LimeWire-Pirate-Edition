/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package org.limewire.mojito.util;

import java.io.UnsupportedEncodingException;


/**
 * Miscellaneous utilities for Arrays.
 */
public final class ArrayUtils {
    
    private static final char[] HEX = {
        '0', '1', '2', '3', '4', '5', 
        '6', '7', '8', '9', 'A', 'B',
        'C', 'D', 'E', 'F'
    };
    
    private static final String[] BIN = {
        "0000", "0001", "0010", "0011", "0100", "0101", 
        "0110", "0111", "1000", "1001", "1010", "1011",
        "1100", "1101", "1110", "1111"
    };
    
    private ArrayUtils() {}
    
    /**
     * Returns data as a HEX String.
     */
    public static String toHexString(byte[] data) {
        return toHexString(data, 0, data.length, -1);
    }
    
    /**
     * Returns data as a HEX String and inserts new lines
     * every wrapAtColumn.
     */
    public static String toHexString(byte[] data, int wrapAtColumn) {
        return toHexString(data, 0, data.length, wrapAtColumn);
    }
    
    /**
     * Returns data as a hex encoded String.
     */
    public static String toHexString(byte[] data, int offset, int length) {
        return toHexString(data, offset, length, -1);
    }
    
    /**
     * Returns data as a hex encoded String.
     */
    public static String toHexString(byte[] data, int offset, int length, int wrapAtColumn) {
        int end = offset+length;
        
        if (offset < 0 || length < 0 || end > data.length) {
            throw new IllegalArgumentException("offset=" + offset + ", length=" + length);
        }
        
        StringBuilder buffer = new StringBuilder(length * 2);
        int column = 0;
        for(int i = offset; i < end; i++) {
            
            if (wrapAtColumn > 0 && column > wrapAtColumn) {
                buffer.append("\n");
                column = 0;
            }
            
            buffer.append(HEX[(data[i] >> 4) & 0xF]).append(HEX[data[i] & 0xF]);
            column += 2;
        }
        return buffer.toString();
    }
    
    /**
     * Returns data as BIN String.
     */
    public static String toBinString(byte[] data) {
        return toBinString(data, 0, data.length, -1);
    }
    
    /**
     * Returns data as BIN String and inserts new lines
     * every wrapAtColumn.
     */
    public static String toBinString(byte[] data, int wrapAtColumn) {
        return toBinString(data, 0, data.length, wrapAtColumn);
    }
    
    /**
     * Returns data as a binary encoded String.
     */
    public static String toBinString(byte[] data, int offset, int length) {
        return toBinString(data, offset, length, -1);
    }
    
    /**
     * Returns data as a binary encoded String.
     */
    public static String toBinString(byte[] data, int offset, int length, int wrapAtColumn) {
        int end = offset+length;
        
        if (offset < 0 || length < 0 || end > data.length) {
            throw new IllegalArgumentException("offset=" + offset + ", length=" + length);
        }
        
        StringBuilder buffer = new StringBuilder(length * 8);
        int column = 0;
        for(int i = offset; i < end; i++) {
            
            if (wrapAtColumn > 0 && column >= wrapAtColumn) {
                buffer.append("\n");
                column = 0;
            }
            
            buffer.append(BIN[(data[i] >> 4) & 0xF]).append(BIN[data[i] & 0xF]).append(" ");
            column += 9;
        }
        
        if (buffer.length() > 0) {
            buffer.setLength(buffer.length()-1);
        }
        return buffer.toString();
    }
    
    /**
     * Converts a HEX String to a byte value.
     */
    public static byte[] parseHexString(String data) {
        if (data.length() % 2 != 0) {
            data = "0" + data;
        }
        
        byte[] buffer = new byte[data.length()/2];
        for(int i = 0, j = 0; i < buffer.length; i++) {
            int hi = parseHexChar(data.charAt(j++));
            int lo = parseHexChar(data.charAt(j++));
            buffer[i] = (byte)(((hi & 0xF) << 4) | (lo & 0xF));
        }
        return buffer;
    }
    
    private static int parseHexChar(char c) {
        switch(c) {
            case '0': return 0;
            case '1': return 1;
            case '2': return 2;
            case '3': return 3;
            case '4': return 4;
            case '5': return 5;
            case '6': return 6;
            case '7': return 7;
            case '8': return 8;
            case '9': return 9;
            case 'a': return 10;
            case 'A': return 10;
            case 'b': return 11;
            case 'B': return 11;
            case 'c': return 12;
            case 'C': return 12;
            case 'd': return 13;
            case 'D': return 13;
            case 'e': return 14;
            case 'E': return 14;
            case 'f': return 15;
            case 'F': return 15;
            default: throw new NumberFormatException("Unknown digit: " + c);
        }
    }
    
    /**
     * A helper method to convert a 4 character ASCII String
     * into an integer.
     */
    public static int toInteger(String ascii) {
        if (ascii == null) {
            throw new NullPointerException("String is null");
        }
        
        char[] chars = ascii.toCharArray();
        if (chars.length != 4) {
            throw new IllegalArgumentException("String must be 4 characters long");
        }
        
        int id = 0;
        for(char c : chars) {
            id = (id << 8) | (c & 0xFF);
        }
        return id;
    }
    
    /**
     * A helper method to convert each of vendorId's 4 bytes
     * into an ASCII character and to return them as String.
     */
    public static String toString(int num) {
        try {
            byte[] name = new byte[]{
                (byte)((num >> 24) & 0xFF),
                (byte)((num >> 16) & 0xFF),
                (byte)((num >>  8) & 0xFF),
                (byte)((num      ) & 0xFF)
            };
            return new String(name, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
