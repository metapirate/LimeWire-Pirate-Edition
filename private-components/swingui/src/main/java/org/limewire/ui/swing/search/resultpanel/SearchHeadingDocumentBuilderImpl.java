package org.limewire.ui.swing.search.resultpanel;

import java.text.MessageFormat;

import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.util.I18n;

@LazySingleton
public class SearchHeadingDocumentBuilderImpl implements SearchHeadingDocumentBuilder {

    public String getHeadingDocument(SearchHeading heading, BasicDownloadState downloadState, boolean isSpam) {
        if (isSpam) {
            return I18n.tr("{0} is marked as spam.", wrapHeading(heading.getText()));
        } else {
            switch(downloadState) {
            case DOWNLOADING:
                String downloadMessage = I18n.tr("Downloading {0}...");
                return MessageFormat.format(downloadMessage, wrapHeading(heading.getText(downloadMessage)));
            case NOT_STARTED:
                return wrapHeading(underLine(wrapForDownload(heading.getText())));
            case DOWNLOADED:
            case LIBRARY:
                String message = I18n.tr("{0} is in your Library.");
                return MessageFormat.format(message, wrapHeading(heading.getText(message)));
            case REMOVED:
                String removeMessage = I18n.tr("{0} was removed for your protection.");
                return MessageFormat.format(removeMessage, wrapHeading(heading.getText(removeMessage)));
            }
        }
        return "";
    }

    private String wrapHeading(String heading) {
        return "<span class=\"title\">" + heading + "</span>";
    }
    
    private String wrapForDownload(String heading) {
        return "<a href=\"#download\">" + heading + "</a>";
    }

    private String underLine(String heading) {
        return "<u>" + heading + "</u>";
    }
}
