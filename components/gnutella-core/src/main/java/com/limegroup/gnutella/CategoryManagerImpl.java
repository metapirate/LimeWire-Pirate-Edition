package com.limegroup.gnutella;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.setting.StringArraySetting;
import org.limewire.util.FileUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSortedSet;
import com.google.inject.Singleton;

@Singleton
class CategoryManagerImpl implements CategoryManager {
    
    /** A secondary category class designed to deal with the fact that programs is split up. */
    private enum InternalCategory {
        AUDIO(Category.AUDIO), 
        VIDEO(Category.VIDEO),
        IMAGE(Category.IMAGE),
        DOCUMENT(Category.DOCUMENT), 
        PROGRAM_OSX_LINUX(Category.PROGRAM),
        PROGRAM_WINDOWS(Category.PROGRAM),
        PROGRAM_ALL(Category.PROGRAM),
        OTHER(Category.OTHER),
        TORRENT(Category.TORRENT);
        
        private final Category category;
        InternalCategory(Category category) {
            this.category = category;
        }
        
        Category getCategory() { return category; }
        
        static InternalCategory fromCategory(Category category) {
            switch(category) {
            case AUDIO:
                return InternalCategory.AUDIO;
            case DOCUMENT:
                return InternalCategory.DOCUMENT;
            case IMAGE:
                return InternalCategory.IMAGE;
            case PROGRAM:
                return InternalCategory.PROGRAM_ALL;
            case VIDEO:
                return InternalCategory.VIDEO;
            case OTHER:
                return InternalCategory.OTHER;
            case  TORRENT:
                return InternalCategory.TORRENT;
            default:
                throw new IllegalArgumentException(category.toString());
            }
        }
    }
    
    /** Map between internal category && built in extensions.  */
    private final Map<InternalCategory, Collection<String>> builtInExtensionMap;
    /** Map between internal category && reference to combined extensions (built-in & remote) */
    private final Map<InternalCategory, AtomicReference<Collection<String>>> extensionMap;
    /** Map between internal category && predicate that refers to combined extensions. */
    private final Map<InternalCategory, Predicate<String>> predicateMap;
    /** Map between internal category && setting that is associated with that category. */
    private final Map<InternalCategory, StringArraySetting> settingMap;
    
    
    CategoryManagerImpl() {
        builtInExtensionMap = new EnumMap<InternalCategory, Collection<String>>(
                InternalCategory.class);

        builtInExtensionMap.put(InternalCategory.DOCUMENT, ImmutableSortedSet.orderedBy(
                String.CASE_INSENSITIVE_ORDER).add("123", "abw", "accdb", "accde", "accdr",
                "accdt", "ans", "asc", "asp", "bdr", "chm", "css", "csv", "dat", "db", "dif",
                "diz", "doc", "docm", "docx", "dotm", "dotx", "dvi", "eml", "eps", "epsf", "fm",
                "grv", "gsa", "gts", "hlp", "htm", "html", "idb", "idx", "iif", "info", "js",
                "jsp", "kfl", "kwd", "latex", "lif", "lit", "log", "man", "mcw", "mht", "mhtml",
                "mny", "msg", "obi", "odp", "ods", "odt", "ofx", "one", "onepkg", "ost", "pages",
                "pdf", "php", "pot", "potm", "potx", "pps", "ppsm", "ppsx", "ppt", "pptm", "pptx",
                "ps", "pub", "qba", "qbb", "qdb", "qbi", "qbm", "qbw", "qbx", "qdf", "qel", "qfp",
                "qpd", "qph", "qmd", "qsd", "rtf", "scd", "sdc", "sdd", "sdp", "sdw", "shw",
                "sldx", "sxc", "sxd", "sxp", "sxw", "t01", "t02", "t03", "t04", "t05", "t06",
                "t07", "t08", "t09", "t98", "t99", "ta0", "ta1", "ta2", "ta3", "ta4", "ta5", "ta6",
                "ta7", "ta8", "ta9", "tax", "tax2008", "tex", "texi", "toc", "tsv", "tvl", "txf",
                "txt", "wk1", "wk3", "wk4", "wks", "wp", "wp5", "wpd", "wps", "wri", "xhtml",
                "xlam", "xls", "xlsb", "xlsm", "xlsx", "xltm", "xltx", "xml", "xsf", "xsn", "qfx",
                "qif", "bud", "ofc", "pst", "mbf", "mn1", "mn2", "mn3", "mn4", "mn5", "mn6", "mn7",
                "mn8", "mn9", "m10", "m11", "m12", "m13", "m14", "m15", "m16", "boe", "box", "bri",
                "cnm", "dbx", "eml", "emlx", "idb", "idx", "maildb", "mbg", "mbs", "mbx", "mht",
                "msb", "msf", "msg", "nws", "pmi", "pmm", "pmx", "tbb", "toc", "vfb", "zmc", "stw",
                "odm", "ott", "wpt").build());

        builtInExtensionMap.put(InternalCategory.AUDIO, ImmutableSortedSet.orderedBy(
                String.CASE_INSENSITIVE_ORDER).add("mp3", "mpa", "mp1", "mpga", "mp2", "ra", "rm",
                "ram", "rmj", "wma", "wav", "m4a", "m4p", "lqt", "ogg", "med", "aif", "aiff",
                "aifc", "au", "snd", "s3m", "aud", "mid", "midi", "rmi", "mod", "kar", "ac3",
                "shn", "fla", "flac", "cda", "mka").build());

        builtInExtensionMap.put(InternalCategory.VIDEO, ImmutableSortedSet.orderedBy(
                String.CASE_INSENSITIVE_ORDER).add("mpg", "mpeg", "mpe", "mng", "mpv", "m1v",
                "vob", "mpv2", "mp2v", "m2p", "m2v", "mpgv", "vcd", "mp4", "dv", "dvd", "div",
                "divx", "dvx", "smi", "smil", "rv", "rmm", "rmvb", "avi", "asf", "asx", "wmv",
                "qt", "mov", "fli", "flc", "flx", "flv", "wml", "vrml", "swf", "dcr", "jve", "nsv",
                "mkv", "ogm", "cdg", "srt", "sub", "flv").build());

        builtInExtensionMap.put(InternalCategory.IMAGE, ImmutableSortedSet.orderedBy(
                String.CASE_INSENSITIVE_ORDER).add("gif", "png", "bmp", "jpg", "jpeg", "jpe",
                "jif", "jiff", "jfif", "tif", "tiff", "iff", "lbm", "ilbm", "mac", "drw", "pct",
                "img", "bmp", "dib", "rle", "ico", "ani", "icl", "cur", "emf", "wmf", "pcx", "pcd",
                "tga", "pic", "fig", "psd", "wpg", "dcx", "cpt", "mic", "pbm", "pnm", "ppm", "xbm",
                "xpm", "xwd", "sgi", "fax", "rgb", "ras").build());

        builtInExtensionMap
                .put(InternalCategory.PROGRAM_OSX_LINUX, ImmutableSortedSet.orderedBy(
                        String.CASE_INSENSITIVE_ORDER).add("app", "bin", "mdb", "sh", "csh", "awk",
                        "pl", "rpm", "deb", "gz", "gzip", "z", "bz2", "zoo", "tar", "tgz", "taz",
                        "shar", "hqx", "sit", "dmg", "7z", "jar", "zip", "nrg", "iso",
                        "jnlp", "rar", "sh").build());

        builtInExtensionMap.put(InternalCategory.PROGRAM_WINDOWS, ImmutableSortedSet.orderedBy(
                String.CASE_INSENSITIVE_ORDER).add("mdb", "exe", "zip", "jar", "cab", "msi", "msp",
                "arj", "rar", "ace", "lzh", "lha", "bin", "nrg", "iso", "jnlp", "bat",
                "lnk", "vbs").build());
        
        builtInExtensionMap.put(InternalCategory.TORRENT, ImmutableSortedSet.orderedBy(
                String.CASE_INSENSITIVE_ORDER).add("torrent").build());

        extensionMap = new EnumMap<InternalCategory, AtomicReference<Collection<String>>>(InternalCategory.class);
        for(InternalCategory category : InternalCategory.values()) {
            // Do it for everything other than OTHER
            if(category != InternalCategory.OTHER) {
                extensionMap.put(category, new AtomicReference<Collection<String>>());
            }
        }
        
        predicateMap = new EnumMap<InternalCategory, Predicate<String>>(InternalCategory.class);
        // Add all predicates that are in the extension map
        for(Map.Entry<InternalCategory, AtomicReference<Collection<String>>> entry : extensionMap.entrySet()) {
            predicateMap.put(entry.getKey(), new CollectionPredicate(entry.getValue()));
        }
        // Then add one for OTHER too
        predicateMap.put(InternalCategory.OTHER, new Predicate<String>() {
            public boolean apply(String input) {
                // This would be a terrible implementation for other categories,
                // but for OTHER it is OK because all we can do is say,
                // "do i belong in another category?... if not -> other"
                return getCategoryForExtension(input) == Category.OTHER;
            };
        });
        
        settingMap = new EnumMap<InternalCategory, StringArraySetting>(InternalCategory.class);
        settingMap.put(InternalCategory.AUDIO, LibrarySettings.ADDITIONAL_AUDIO_EXTS);
        settingMap.put(InternalCategory.DOCUMENT, LibrarySettings.ADDITIONAL_DOCUMENT_EXTS);
        settingMap.put(InternalCategory.IMAGE, LibrarySettings.ADDITIONAL_IMAGE_EXTS);
        settingMap.put(InternalCategory.PROGRAM_OSX_LINUX, LibrarySettings.ADDITIONAL_PROGRAM_OSX_LINUX_EXTS);
        settingMap.put(InternalCategory.PROGRAM_WINDOWS, LibrarySettings.ADDITIONAL_PROGRAM_WINDOWS_EXTS);
        settingMap.put(InternalCategory.VIDEO, LibrarySettings.ADDITIONAL_VIDEO_EXTS);
        settingMap.put(InternalCategory.TORRENT, LibrarySettings.ADDITIONAL_TORRENT_EXTS);
        
        rebuildExtensions();
    }
    
    /** Rebuilds all extensions so that they contain both built-in & simpp extensions. */
    private void rebuildExtensions() {
        // Redo every category that has a setting associated with it.
        for(Map.Entry<InternalCategory, StringArraySetting> entry : settingMap.entrySet()) {
            InternalCategory category = entry.getKey();
            StringArraySetting remote = entry.getValue();
            AtomicReference<Collection<String>> combinedMap = extensionMap.get(category);
            assert combinedMap != null && remote != null && category != null;
            combinedMap.set(combineAndCleanup(category, remote.get()));
        }
        
        // And then rebuild the combined PROGRAM category that has both.
        extensionMap.get(InternalCategory.PROGRAM_ALL).set(
            ImmutableSortedSet.orderedBy(String.CASE_INSENSITIVE_ORDER)
                .addAll(extensionMap.get(InternalCategory.PROGRAM_OSX_LINUX).get())
                .addAll(extensionMap.get(InternalCategory.PROGRAM_WINDOWS).get()).build());
    }
    
    private Collection<String> combineAndCleanup(InternalCategory category, String[] remote) {
        Set<String> remoteSet = new TreeSet<String>(Arrays.asList(remote));
        
        // remove everything that's built-in
        for(Collection<String> builtIn : builtInExtensionMap.values()) {
            remoteSet.removeAll(builtIn);
        }

        // Remove the stuff from the other remote extensions,
        // otherwise we can end up with two different categories having
        // the same extension
        for(Map.Entry<InternalCategory, StringArraySetting> entry : settingMap.entrySet()) {
            // If our category doesn't match the setting's category, then remove
            // all extensions from that setting.
            if(category.getCategory() != entry.getKey().getCategory()) {
                remoteSet.removeAll(Arrays.asList(entry.getValue().get()));
            }
        }
        
        Collection<String> builtIn = builtInExtensionMap.get(category);
        assert builtIn != null;

        // Build the combined map by using the built in & the remaining extensions
        return ImmutableSortedSet.orderedBy(String.CASE_INSENSITIVE_ORDER).
            addAll(builtIn).addAll(remoteSet).build();
    }

    @Override
    public Category getCategoryForExtension(String extension) {
        // note: the extension sets are all case insensitive,
        // so... no lowercasing required.
        for(Map.Entry<InternalCategory, AtomicReference<Collection<String>>> entry : extensionMap.entrySet()) {
            Collection<String> collection = entry.getValue().get();
            if(collection.contains(extension)) {
                return entry.getKey().getCategory();
            }
        }
        // If it matched nothing, it matches OTHER.
        return Category.OTHER;
    }
    
    @Override
    public Category getCategoryForFilename(String filename) {
        String extension = FileUtils.getFileExtension(filename);
        return getCategoryForExtension(extension);
    }

    @Override
    public Category getCategoryForFile(File file) {
        // note: the extension sets are all case insensitive,
        // so... no lowercasing required.        
        String extension = FileUtils.getFileExtension(file);
        return getCategoryForExtension(extension);
    }

    @Override
    public Collection<String> getExtensionsForCategory(Category category) {
        AtomicReference<Collection<String>> ref = extensionMap.get(InternalCategory.fromCategory(category));
        if(ref != null) {
            return ref.get();
        } else {
            assert category == Category.OTHER;
            return Collections.emptySet();
        }
    }

    @Override
    public Predicate<String> getExtensionFilterForCategory(Category category) {
        return predicateMap.get(InternalCategory.fromCategory(category));
    }

    @Override
    public Predicate<String> getOsxAndLinuxProgramsFilter() {
        return predicateMap.get(InternalCategory.PROGRAM_OSX_LINUX);
    }

    @Override
    public Predicate<String> getWindowsProgramsFilter() {
        return predicateMap.get(InternalCategory.PROGRAM_WINDOWS);
    }
    
    private static final class CollectionPredicate implements Predicate<String> {
        private final AtomicReference<Collection<String>> delegate;
        
        public CollectionPredicate(AtomicReference<Collection<String>> set) {
            this.delegate = set;
        }
        
        @Override
        public boolean apply(String input) {
            return delegate.get().contains(input);
        }
    }

    @Override
    public boolean containsCategory(Category category, List<String> paths) {
  
        if (paths == null) {
            return false;
        }
        
        for ( String path : paths ) {
            if (getCategoryForFilename(path) == category) {
                return true;
            }
        }
        
        return false;
    }
}
