package org.limewire.core.api.search;

import java.util.EnumMap;

import org.limewire.core.api.Category;

/**
 * All categories that can be searched.
 */
public enum SearchCategory {
    ALL(null, 0), 
    AUDIO(Category.AUDIO, 1), 
    VIDEO(Category.VIDEO, 2), 
    IMAGE(Category.IMAGE, 3),
    DOCUMENT(Category.DOCUMENT, 4), 
    PROGRAM(Category.PROGRAM, 5), 
    OTHER(Category.OTHER, 6),
    TORRENT(Category.TORRENT, 7);
    
    private static final EnumMap<Category, SearchCategory> perCategory = new EnumMap<Category, SearchCategory>(Category.class);

    static {
        for(SearchCategory searchCategory : values()) {
            if(searchCategory.category != null) {
                perCategory.put(searchCategory.category, searchCategory);
            }
        }
    }
    
    private final Category category;
    private final int id;
 
    private SearchCategory(Category category, int id) {
        this.id = id;
        this.category = category;
    }
    
    /**
     * Returns the SearchCategory associated with the given Category.
     * This will never return {@link SearchCategory#ALL} because Categories
     * are specific to types.
     */
    public static SearchCategory forCategory(Category category) {
        return perCategory.get(category);
    }
    
    /** 
     * Returns the {@link SearchCategory} associated with the given ID, as
     * returned by {@link SearchCategory#getId()}.
     * 
     * <p>If the id does not match any members ALL is returned.
     */
    public static SearchCategory forId(Integer id) {
        for(SearchCategory category : values()) {
            if(id.equals(category.id)) {
                return category;
            }
        }
        return ALL;
    }
    
    /**
     * Returns the ID associated with this category.  This ID is suitable
     * for storage in properties that can be reused over multiple sessions.
     * To get the {@link SearchCategory} associated with the id in later
     * sessions, use {@link SearchCategory#forId(Integer)}.
     */
    public int getId() {
        return id;
    }
    
    /** Returns the {@link Category} most closely associated with this {@link SearchCategory}. */
    public Category getCategory() {
        return category;
    }
}