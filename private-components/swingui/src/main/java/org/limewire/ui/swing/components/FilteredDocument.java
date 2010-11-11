package org.limewire.ui.swing.components;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import org.limewire.core.api.FilePropertyKey;

/**
 * Simplified {@link PlainDocument} filtered for text length and character type.
 */
public class FilteredDocument extends PlainDocument {

    private boolean acceptsNumeric = true;
    private boolean acceptsAlphabetic = true;
    private boolean acceptsNonAlphanumeric = true;
    private int maxChars = Integer.MAX_VALUE;
    
    private int minBound = Integer.MIN_VALUE;
    private int maxBound = Integer.MAX_VALUE;
    
    /**
     * Generate a document based on the validity requirements of a {@link FilePropertyKey}.
     * <p> NOTE: If no filtering is required then no document is added.
     */
    public static void configure(JTextField textField, FilePropertyKey key) {
        if (key != null && FilePropertyKey.isLong(key)) {
            FilteredDocument document = new FilteredDocument();
            document.setAcceptsAlphabetic(false);
            document.setAcceptsNumeric(true);
            document.setAcceptsNonAlphanumeric(false);
            switch (key) {
                case YEAR :
                case BITRATE :
                    document.setMaxChars(4);
                    break;
            }
            textField.setDocument(document);
        }
    }
    
    @Override
    public void insertString(int offset, String str, AttributeSet a) throws BadLocationException {
        boolean accept = true;
        
        if (getLength() + str.length() > maxChars) {
            return;
        }
        
        if (!acceptsAlphabetic || !acceptsNumeric || !acceptsNonAlphanumeric) {
            for ( byte c : str.getBytes() ) { 
                boolean isAlphabetic = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
                boolean isNumeric = c >= '0' && c <= '9';
            
                accept &= (acceptsAlphabetic && isAlphabetic) 
                       || (acceptsNumeric && isNumeric)
                       || (acceptsNonAlphanumeric && !isAlphabetic && !isNumeric);

            }

            if (!accept) {
                return;
            }
        }
        
        if (minBound != Integer.MIN_VALUE || maxBound != Integer.MAX_VALUE) { 
            String currentText = getText(0,getLength());
            String joinedString = currentText.substring(0, offset) + str + currentText.substring(offset);
            
            if (!"-".equals(joinedString)) { 
                try {
                    int value = Integer.parseInt(joinedString);
                
                    if (value < minBound || value > maxBound) {
                        return;
                    }
                }
                catch (NumberFormatException e) {
                    return;
                }
            } else if (minBound > -1 && maxBound > -1) {
                return;
            }
        }
        
        super.insertString(offset, str, a);
    }
    
    /**
     * If numerals can be be entered.
     */
    public void setAcceptsNumeric(boolean accepts) {
        acceptsNumeric = accepts;
    }
    
    /**
     * If letters, capitals and lower cases, can be be entered.
     */
    public void setAcceptsAlphabetic(boolean accepts) {
        acceptsAlphabetic = accepts;
    }
    
    /**
     * If non-alphabet-non-numerals can be entered.  This includes 
     *  white space, the negative sign, and decimal points.  This
     *  should be on if requiring these.
     */
    public void setAcceptsNonAlphanumeric(boolean accepts) {
        acceptsNonAlphanumeric = accepts;
    }
    
    /**
     * The maximum number of characters that can be held.
     */
    public void setMaxChars(int max) {
        maxChars = max;
    }

    /**
     * Sets a minimum bound for number input and makes the field accept integers by defacto.
     * 
     * <p> NOTE: only makes sense with values less than 2 since it will otherwise entry
     *            will be impossible (ie. if max is 33 and min 2 can only type numbers starting with 2)
     */
    public void setMinBound(int min) {
        if (min > 1) {
            throw new IllegalArgumentException("Using a minimum bound that is more than 1 would make things impossible to use.");
        }
        
        minBound = min;
    }
    
    /**
     * Sets a maximum bound and makes the field accept integers by defacto 
     * 
     * <p> NOTE: only makes sense with values greater than negative 2 since it will otherwise entry
     *            will be impossible (ie. if minimum is -33 and max -11 can only type numbers starting with -1) 
     */
    public void setMaxBound(int max) {
        if (max < 1) {
            throw new IllegalArgumentException("Using a maximum bound that is less than -1 would make things impossible to use.");
        }
        
        maxBound = max;
    }
}
