package com.limegroup.gnutella.xml;

import java.util.LinkedList;
import java.util.List;

import org.limewire.util.NameValue;


/**
 * Stores information pertaining to fields (elements) in xml documents.
 */
public class SchemaFieldInfo
{
    /**
     * Type of the field (eg Integer, String, complex etc).
     */
    private String _type;
    
    /**
     * Whether or not this field is editable.
     */
    private boolean _editable = true;
    
    /**
     * Whether or not this field should be hidden in GUI displays.
     * (This does not mean it will or should be hidden from searching.)
     */
    private boolean _hidden = false;
    
    /**
     * Whether the field should be completely hidden (including from searches).
     */
    private boolean _invisible = false;
    
    /**
     * The default width this field should have in columns.
     */
    private int _width = 60;
    
    /**
     * The default visibility this field should have in columns.
     */
    private boolean _visibility = false;
    
    /**
     * List (of NameValue) to store enumerated values, if associated with this
     *field.
     */
    private List<NameValue<String>> _enumerationList = null;
    
    /**
     * Canonicalized field name for which it stores the info.
     */
    private String _canonicalizedFieldName = null;
    
    //constants defining types of the fields to display
    public static final int TEXTFIELD = 1;
    public static final int OPTIONS = 2;
    
    /**
     * type of the field to display.
     */
    private int _fieldType = TEXTFIELD;
    
    /**
     * Creates a new instance of FieldInfo and initializes internal fields
     * with the passed values.
     * @param type The tye of the field (eg Integer, String, complex etc)
     */
    public SchemaFieldInfo(String type)
    {
        this._type = type;
    }   
    
    String getType()
    {
        return _type;
    }
    
    public Class<?> getJavaType()
    {
        return TypeConverter.getType(_type);
    }

    /**
     * Adds the passed value to the list of enumeration values.
     */
    void addEnumerationNameValue(String name, String value)
    {
        //create a new list, if doesn't exist
        if(_enumerationList == null)
            _enumerationList = new LinkedList<NameValue<String>>();
        
        //add the value
        _enumerationList.add(new NameValue<String>(name, value));
        
        //also set the field type to be OPTIONS
        _fieldType = OPTIONS;
    }
    
    /**
     * Returns the type of the field to display.
     */
    public int getFieldType()
    {
        return _fieldType;
    }
    
    /**
     * Sets whether or not this field is editable.
     */
    void setEditable(boolean editable) {
        this._editable = editable;
    }
    
    /**
     * Gets whether or not this is editable.
     */
    public boolean isEditable() {
        return _editable;
    }
    
    /**
     * Sets whether or not this field should be hidden.
     */
    void setHidden(boolean hidden) {
        this._hidden = hidden;
    }

    /**
     * Gets whether or not this field is hidden.
     */
    public boolean isHidden() {
        return _hidden;
    }

    /**
     * Sets whether or not this field should be hidden.
     */
    void setInvisible(boolean inviz) {
        this._invisible = inviz;
    }
    
    /**
     * Gets whether or not this field is hidden.
     */
    public boolean isInvisible() {
        return _invisible;
    }
    
    
    /**
     * Sets the default width.
     */
    void setDefaultWidth(int width) {
        this._width = width;
    }
    
    /**
     * Gets the default width.
     */
    public int getDefaultWidth() {
        return _width;
    }
    
    /**
     * Sets the default visibility.
     */
    void setDefaultVisibility(boolean viz) {
        this._visibility = viz;
    }
    
    /**
     * Gets the default visibility.
     */
    public boolean getDefaultVisibility() {
        return _visibility;
    }
    
     /**
     * Sets the canonicalized field name for which this object stores the
     * information.
     */
    void setCanonicalizedFieldName(String canonicalizedFieldName)
    {
        this._canonicalizedFieldName = canonicalizedFieldName;
    }
    
    /**
     * Returns the canonicalized field name for which this object stores the
     * information.
     */
    public String getCanonicalizedFieldName()
    {
        return _canonicalizedFieldName;
    }
    
    /**
     * Returns the List (of NameValue) to store enumerated values, 
     * if associated with this field.
     */
    public List<NameValue<String>> getEnumerationList()
    {
        return _enumerationList;
    }
    
    
}//end of class FieldInfo
