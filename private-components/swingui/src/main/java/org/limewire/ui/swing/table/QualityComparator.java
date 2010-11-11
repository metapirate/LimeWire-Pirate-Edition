package org.limewire.ui.swing.table;

import java.util.Comparator;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.PropertiableFile;

/**
 * Compares the quality value for a pair of PropertiableFile objects.
 */
public class QualityComparator implements Comparator<PropertiableFile> {
    @Override
    public int compare(PropertiableFile o1, PropertiableFile o2) {
        Object quality1 = o1.getProperty(FilePropertyKey.QUALITY);
        Object quality2 = o2.getProperty(FilePropertyKey.QUALITY);

        if (quality1 instanceof Number) {
            if (quality2 instanceof Number) {
                int q1 = ((Number) quality1).intValue();
                int q2 = ((Number) quality2).intValue();               
                return (q1 == q2)? compareBitRate(o1, o2) : (q1 < q2)? -1 : 1;
            } else {
                return 1;
            }
        } else {
            return (quality2 instanceof Number) ? -1 : 0;
        }
    }
    
    private int compareBitRate(PropertiableFile o1, PropertiableFile o2){
        Object bitRate1 = o1.getProperty(FilePropertyKey.BITRATE);
        Object bitRate2 = o2.getProperty(FilePropertyKey.BITRATE);

        if (bitRate1 instanceof Number) {
            if (bitRate2 instanceof Number) {
                int b1 = ((Number) bitRate1).intValue();
                int b2 = ((Number) bitRate2).intValue();               
                return (b1 == b2)? 0 : (b1 < b2)? -1 : 1;
            } else {
                return 1;
            }
        } else {
            return (bitRate2 instanceof Number) ? -1 : 0;
        }
    }
}
