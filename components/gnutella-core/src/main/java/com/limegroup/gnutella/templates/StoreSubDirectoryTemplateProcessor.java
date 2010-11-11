package com.limegroup.gnutella.templates;

import java.io.File;
import java.util.Map;

/**
 * This consumes a String template for describing how to save a file and returns a valid file path.  
 * Some examples would be:
 * <xmp>
 * <artist>/<album>
 * </xmp>
 */
public class StoreSubDirectoryTemplateProcessor extends StoreTemplateProcessor {
          
    /**
     * Returns an output directory specified by <code>template</code> using
     * <code>outDir</code> as the base directory.  Some sample templates are
     * <xmp>
     * <artist>
     * <artist>/<album>
     * </xmp>
     * Valid values for the keys of <code>substitutions</code> are
     * <ul>
     * <li>{@link LWSConstants#ARTIST_LABEL}</li>
     * <li>{@link #AlBUM_LABEL}</li>
     * </ul>
     * 
     * @param template template to use to create the subfolder structure
     * @param substitutions List of real values to replace template names with
     * @param outDir subdirectory to save to below the template folders
     * @return the complete path to the directory to save the file to, including
     *              any sub folders that may have been generated from the template
     * 
     * @throws IllegalTemplateException if the template contains illegal characters
     */
    public File getOutputDirectory(final String template, final Map<String,String> substitutions, 
            final File outDir) throws IllegalTemplateException {

        if (template == null) return outDir;
        if (template.equals("")) return outDir;
        
        String subDirs = performSubstitution(template, substitutions);
       
        return new File(outDir, subDirs);
    }
}
