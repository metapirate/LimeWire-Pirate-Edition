package org.limewire.ui.swing.transfer;

/**
 * Allows for navigating to the downloads/uploads tray views.
 */
public interface TransferTrayNavigator {

    /**
     * Shows the transfers tray.
     */
    public void hideTray();

    /**
     * Hides the transfers tray.
     */
    public void showTray();

    /**
     * @return true if the tray is showing.
     */
    public boolean isTrayShowing();

    /**
     * Opens the tray and shows the downloads view.
     */
    public void selectDownloads();

    /**
     * Opens the tray and shows the uploads view.
     */
    public void selectUploads();

    /**
     * @return true if downloads are the selected tab.
     */
    public boolean isUploadsSelected();

    /**
     * @return true if uploads are the selected tab.
     */
    public boolean isDownloadsSelected();
}
