package com.limegroup.gnutella.templates;

import java.util.Map;

/**
 * Creates a unique file name based on a template and a set of substitutable values.
 */
public class StoreFileNameTemplateProcessor extends StoreTemplateProcessor {
    
    /**
     * Returns a fileName based on a template and the values to substitute into that 
     * template. If the template or the substituted values contain illegal file name
     * characters, an IllegalTemplateException will be thrown 
 *
     * @param template template to use, if template is null a IllegalTemplateException is thrown
     * @param substitutions maps template variables to actual values
     * @return a file name in the form of the template
     * @throws IllegalTemplateException if template is null or the template contains illegal characters
     */
    public String getFileName(final String template, final Map<String,String> substitutions) 
            throws IllegalTemplateException {
        if( template == null )
            throw new IllegalTemplateException(0, -1, template);

        return performSubstitution(template, substitutions);
    }
}
