package com.limegroup.gnutella.i18n;

/**
 * Describes a line in a properties file.
 */
class Line {
    private final String wholeLine;
    private final String key;
    private final String value;
    private final int braces;

    private final boolean extraComment;

    /**
     * TODO: does not handle all comment lines properly!<br />
     * TODO: does not separate key=value pairs properly!<br />
     * TODO: does not decode continuation lines properly (continuation lines
     * should be already joined in a upper layer before passing data line here.)<br />
     * TODO: ignores lines that don't have an = or #.<br />
     * For these reasons, properties are read using the standard properties
     * reader, and extra keys are added when needed from lines with
     * extraComment=true. The Line class will be used mostly for keeping the
     * information kept in comment lines, or in a few key=value pairs that were
     * not uncommented by the contributing translator.
     * 
     * @param data
     *            a data line to parse and store.
     */
    Line(String data) {
        if (data == null)
            throw new NullPointerException("null data");
        this.wholeLine = data;
        data.trim();
        boolean ignored = false, extraComment = false;
        if (data.startsWith("#?")) { //$NON-NLS-1$
            // attempt to look for a key=value pair after this special mark
            data = data.substring(2).trim();
            extraComment = true;
        } else if (data.startsWith("#") || data.equals("")) { //$NON-NLS-1$//$NON-NLS-2$
            // an empty line or comment line is kept in this.wholeLine
            ignored = true;
        }
        String key = null, value = null;
        int braces = 0;
        if (!ignored) {
            final int eq;
            if ((eq = data.indexOf('=')) != -1) {
                key = data.substring(0, eq).trim();
                value = data.substring(eq + 1).trim();
                if (extraComment && (value.equals(""))) { //$NON-NLS-1$
                    // Handle an extraComment line like "#? key=value" with an
                    // empty key or empty value as a simple comment line stored
                    // in this.wholeLine but not handled as a acceptable
                    // supplementary key=value resource
                    ignored = true;
                } else
                    braces = parseBraceCount(value);
            }
        }
        if (ignored) {
            // This is a comment or empty line, whose actual content is kept in
            // this.wholeLine without any modification.
            this.key = null;
            this.value = null;
            this.extraComment = false;
            this.braces = 0;
        } else {
            this.key = key;
            this.value = value;
            this.extraComment = extraComment;
            this.braces = braces;
        }
    }

    static int parseBraceCount(String value) {
        int count = 0;
        int startIdx = value.indexOf('{');
        while (startIdx != -1) {
            int endIdx = value.indexOf('}', startIdx);
            if (endIdx != -1) {
                try {
                    Integer.parseInt(value.substring(startIdx + 1, endIdx));
                    count++;
                } catch (NumberFormatException nfe) {/* ignored */}
                startIdx = endIdx + 1;
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * @return true if this is a not a key=value line.
     */
    boolean isComment() {
        return this.key == null;
    }

    /**
     * @return this whole text line (comment or key=value).
     */
    String getLine() {
        return this.wholeLine;
    }

    /**
     * @return this line key, or null if isComment() is true.
     */
    String getKey() {
        return this.key;
    }

    /**
     * @return this line value, or null if isComment() is true.
     */
    String getValue() {
        return this.value;
    }

    /**
     * @return true if the line had an extra "#? " in front. Such line contains
     *         an additional key=value resource which should be added to the
     *         loaded properties. This is useful only to handle the case where
     *         translators forget to remove the "#? " leading mark in front of
     *         their contributed translations, and avoids manual editing of
     *         these incoming files by the LimeWire developers team.
     */
    boolean hadExtraComment() {
        return this.extraComment;
    }

    /**
     * @return the number of brace pairs this line had
     */
    int getBraceCount() {
        return this.braces;
    }
}
