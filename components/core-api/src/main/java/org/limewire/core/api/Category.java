package org.limewire.core.api;

import java.util.Arrays;
import java.util.List;

import org.limewire.i18n.I18nMarker;


/**
 * Represents a category for the various file types represented in the application. 
 */
public enum Category {

    AUDIO(I18nMarker.marktr("Audio"), I18nMarker.marktr("Audio"), "audio"),
    VIDEO(I18nMarker.marktr("Video"), I18nMarker.marktr("Videos"), "video"),
    IMAGE(I18nMarker.marktr("Image"), I18nMarker.marktr("Images"), "image"),
    DOCUMENT(I18nMarker.marktr("Document"), I18nMarker.marktr("Documents"), "document"),
    PROGRAM(I18nMarker.marktr("Program"), I18nMarker.marktr("Programs"), "application"),
    OTHER(I18nMarker.marktr("Other"), I18nMarker.marktr("Other"), "other"),
    TORRENT(I18nMarker.marktr("Torrent"), I18nMarker.marktr("Torrents"), "torrent"),
    ;
    
    private final String plural;
    private final String singular;
    private final String schemaName;
    
    Category(String singular, String plural, String schemaName) {
        this.singular = singular;
        this.plural = plural;
        this.schemaName = schemaName;
    }
    
    /**
     * Returns the name of the category when referring to a single item. 
     */
    public String getSingularName() {
        return singular;
    }

    /**
     * Returns the name of the category when referring to many items. 
     */
    public String getPluralName() {
        return plural;
    }
    
    /**
     * Returns the schema associated with this category.
     * This is associated with XML, and is better of unused unless
     * absolutely necessary.
     */
    public String getSchemaName() {
        return schemaName;
    }
    
    @Override
    public String toString() {
        return plural;
    }
    
    /**
     * Returns a List with the categories in the order we would like them to be displayed. 
     */
    public static List<Category> getCategoriesInOrder() {
        return Arrays.asList(AUDIO, VIDEO, IMAGE, DOCUMENT, PROGRAM, OTHER);
    }
}