package org.limewire.ui.swing.components;

import javax.swing.JTextField;

/**
 * A JTextField component that only accepts integer values as input. Any other
 * input will not be added and a system beep will be issued. Additionally, 
 * a min and max number can be set.
 */
public class NumericTextField extends JTextField {

    /**
     * Create a Textfield with a specified number of columns displayed.
     */
    public NumericTextField(int columns) {
        this(columns, 0, Integer.MAX_VALUE);
    }

    /**
     * Create a Textfield with a specified number of columns displayed,
     * and minimum and maximum integer values which will be accepted.
     */
    public NumericTextField(int columns, int minValue, int maxValue) {
        super(columns);
        addIntegerWithMaxValueFilter(minValue, maxValue);
        TextFieldClipboardControl.install(this);
    }

    /**
     * @return the value of the field as an int.  If field is empty, or the field text
     * cannot be retrieved as an int for any other reason, return the specified default value
     */
    public int getValue(int defaultValue) {
        try {
            return Integer.parseInt(getText());
        } catch (NumberFormatException e) {
            setValue(defaultValue);
            return defaultValue;
        }
    }

    /**
     * Given an int, set the content of the text field.
     *
     * @param value set on the field
     */
    public void setValue(int value) {
        setText(String.valueOf(value));
    }

    private void addIntegerWithMaxValueFilter(int minValue, int maxValue) {
        FilteredDocument document = new FilteredDocument();
        document.setMinBound(minValue);
        document.setMaxBound(maxValue);
        setDocument(document);
    }

}
