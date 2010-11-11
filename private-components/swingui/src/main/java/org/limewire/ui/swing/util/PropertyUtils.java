package org.limewire.ui.swing.util;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.search.SearchResult;

public class PropertyUtils {
    
    public static String getTitle(FileItem item){
        if(item.getProperty(FilePropertyKey.TITLE) == null){
            return item.getName();
        } else {
            return (String)item.getProperty(FilePropertyKey.TITLE); 
        }
    }
    
    public static String getTitle(DownloadItem item){
       return item.getFileName();
    }
    
    public static String getTitle(SearchResult result){
        return result.getFileName();
     }
    
    public static String getToolTipText(Object o){
        if(o instanceof FileItem){
            return getTitle((FileItem)o);
        } else if(o instanceof DownloadItem){
            return getTitle((DownloadItem)o);
        } else if(o instanceof SearchResult){
            return getTitle((SearchResult)o);
        }
        return o.toString();
    }

}
