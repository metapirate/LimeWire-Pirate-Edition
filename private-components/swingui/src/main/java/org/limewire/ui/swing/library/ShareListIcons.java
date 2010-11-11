package org.limewire.ui.swing.library;

import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.ui.swing.util.GuiUtils;

public class ShareListIcons {
    @Resource
    private Icon publicIcon;
    @Resource
    private Icon unsharedIcon;
    @Resource
    private Icon sharedIcon;

    public ShareListIcons() {
        GuiUtils.assignResources(this);
    }

    public Icon getListIcon(SharedFileList sharedFileList) {
        if (sharedFileList.isPublic())
            return getPublicIcon();
        else if (sharedFileList.getFriendIds().size() == 0)
            return getUnsharedIcon();
        else
            return getSharedIcon();
    }

    public Icon getPublicIcon() {
        return publicIcon;
    }

    public Icon getUnsharedIcon() {
        return unsharedIcon;
    }

    public Icon getSharedIcon() {
        return sharedIcon;
    }
}
