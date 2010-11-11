package org.limewire.ui.swing.action;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;

import org.limewire.core.api.URN;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;

public class BitziLookupAction extends AbstractAction {

    /**
     * Setting for the bitzi lookup string, has to be processed through 
     * {@link MessageFormat#format(java.lang.String, java.lang.Object[])} for
     * adding the urn.
     */
    private static final String BITZI_LOOKUP_URL =
        "http://bitzi.com/lookup/{0}?ref=limewire";
    
    private final PropertiableFile propertiableFile;
    
    public BitziLookupAction(PropertiableFile file) {
        super(I18n.tr("Lookup on Bitzi"));
        this.propertiableFile = file;
    }
    
    public void actionPerformed(ActionEvent e) {
         URN urn = propertiableFile.getUrn();
         if (urn != null) {
             String urnStr = urn.toString();
             int hashstart = 1+urnStr.indexOf(":",4);
             String lookupUrl = MessageFormat.format(BITZI_LOOKUP_URL, 
                     new Object[] { urnStr.substring(hashstart) });
             NativeLaunchUtils.openURL(lookupUrl);
         }
    }
}
