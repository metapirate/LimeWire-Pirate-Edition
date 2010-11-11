/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.limewire.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.limewire.util.FileUtils;



/**
 * Unzip a file. "Imported" from Ant, with small adaptations.
 *
 * @author costin@dnt.ro
 */
public final class Expand {

    /**
     * Ensure that this class will never be constructed.
     */
    private Expand() {}

    /**
     * Expand the specified source file into the specified destination
     * directory.
     *
     * @param source the source <tt>File</tt> to expand
     * @param dest the destination directory in which to expand the 
     *  source file
     * @throws <tt>IOException</tt> if the source file cannot be found,
     *  if the destination directory cannot be written to, or there is
     *  any other IO error
     */
    public static void expandFile(File source, File dest) throws IOException {        
        expandFile(source, dest, false, null);
    }

    /**
     * Expand the specified source file into the specified destination
     * directory.
     *
     * @param source the source <tt>File</tt> to expand
     * @param dest the destination directory in which to expand the 
     *  source file
     * @throws <tt>IOException</tt> if the source file cannot be found,
     *  if the destination directory cannot be written to, or there is
     *  any other IO error
     */
    public static void expandFile(File source, File dest, boolean overwrite) 
        throws IOException {
            expandFile(source, dest, overwrite, null);
    }
    
    /**
     * Expands the source file to destination.  If overwrite is true, all files
     * will be overwritten (regardless of modification time).  If 'names'
     * is non-null, any file in 'names' will be expanded regardless of modification time.
     */
    public static void expandFile(File source, File dest, boolean overwrite, String[] names) 
      throws IOException {
            
        InputStream in = null;
        try {
            FileUtils.setWriteable(source);
            in = new BufferedInputStream(new FileInputStream(source));
            expandFile(in, dest, overwrite, names);
        } finally {
            IOUtils.close(in);
        }
    }
    
    /**
     * Expands the input stream to destination.  If overwrite is true, all files
     * will be overwritten (regardless of modification time).  If 'names'
     * is non-null, any file in 'names' will be expanded regardless of modification time.
     * <p>
     * Does NOT close the InputStream.
     */
    public static void expandFile(InputStream is, File dest, boolean overwrite, String[] names) 
      throws IOException {
        ZipInputStream zis = new ZipInputStream(is);
        ZipEntry ze = null;
        
        while ((ze = zis.getNextEntry()) != null) {
            File f = new File(dest, ze.getName());
            // create intermediary directories - sometimes zip don't add them
            File dirF=new File(f.getParent());
            FileUtils.setWriteable(dirF);
            dirF.mkdirs();
            
            if (ze.isDirectory()) {
                f.mkdirs(); 
            } else if ( ze.getTime() > f.lastModified() ||
                        overwrite || inNames(ze.getName(), names)) {
                FileUtils.setWriteable(f);
                byte[] buffer = new byte[1024];
                int length = 0;
                OutputStream fos = null;
                try {
                    fos = new BufferedOutputStream(new FileOutputStream(f));
                
                    while ((length = zis.read(buffer)) >= 0) {
                        fos.write(buffer, 0, length);
                    }
                } finally {
                    IOUtils.close(fos);
                }
            }
        }
    }
    
    private static boolean inNames(String name, String[] all) {
        if(all == null || name == null)
            return false;
        for(int i = 0; i < all.length; i++)
            if(name.startsWith(all[i]))
                return true;
        return false;
    }
}
