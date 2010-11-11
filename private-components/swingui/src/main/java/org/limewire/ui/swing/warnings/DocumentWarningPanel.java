package org.limewire.ui.swing.warnings;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.JLayeredPane;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.core.settings.SharingSettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HTMLLabel;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.components.OverlayPopupPanel;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.mainframe.GlobalLayeredPane;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class DocumentWarningPanel extends OverlayPopupPanel {
    @Resource
    private Color backgroundColor;

    @Resource
    private Font font;

    @Resource
    private Color fontColor;

    @Resource
    private Color linkFontColor;

    @Inject
    public DocumentWarningPanel(final SharedFileListManager shareListManager,
            @GlobalLayeredPane JLayeredPane layeredPane, ButtonDecorator buttonDecorator) {
        
        super(layeredPane);
        
        GuiUtils.assignResources(this);

        setLayout(new MigLayout("nogrid, insets 10"));
        setSize(320, 190);
        setPreferredSize(new Dimension(320, 190));
        setMaximumSize(new Dimension(320, 190));
        setMinimumSize(new Dimension(320, 190));
        setBackground(backgroundColor);

        final String learnMoreUrl = "http://www.limewire.com/client_redirect/?page=documentsSharing";
        HTMLLabel htmlLabel = new HTMLLabel(
                I18n
                        .tr(
                                "<html><body>Warning: You are sharing Documents with the world. These Documents may contain personal information. <a href=\"{0}\">learn more</a></body></html>",
                                learnMoreUrl));
        htmlLabel.setEditable(false);
        htmlLabel.setOpaque(false);
        htmlLabel.setFocusable(false);
        htmlLabel.setHtmlFont(font);
        htmlLabel.setHtmlForeground(fontColor);
        htmlLabel.setHtmlLinkForeground(linkFontColor);

        add(htmlLabel, "wrap");
        MultiLineLabel sharingLabel = new MultiLineLabel(I18n
                .tr("Do you want to keep sharing Documents with the world?"));
        sharingLabel.setForeground(fontColor);
        sharingLabel.setFont(font);

        add(sharingLabel, "gaptop 20, wrap");
        JXButton continueSharingButton = new JXButton(new AbstractAction(I18n
                .tr("Continue Sharing")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                SharingSettings.WARN_SHARING_DOCUMENTS_WITH_WORLD.setValue(false);
            }
        });
       buttonDecorator.decorateDarkFullImageButton(continueSharingButton, AccentType.SHADOW);

        JXButton unshareAllButton = new JXButton(new AbstractAction(I18n.tr("Unshare All")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                SharingSettings.WARN_SHARING_DOCUMENTS_WITH_WORLD.setValue(true);
                shareListManager.removeDocumentsFromPublicLists();
            }
        });
        buttonDecorator.decorateDarkFullImageButton(unshareAllButton, AccentType.SHADOW);

        continueSharingButton.setPreferredSize(new Dimension(150, 25));
        continueSharingButton.setMinimumSize(new Dimension(150, 25));
        unshareAllButton.setPreferredSize(new Dimension(150, 25));
        unshareAllButton.setMinimumSize(new Dimension(150, 25));
        
        add(continueSharingButton, "alignx center, gaptop 15");
        add(unshareAllButton, "wrap");
        
        resize();
    }

    @Override
    public void resize() {
        Rectangle parentBounds = layeredPane.getBounds();
        int w = getPreferredSize().width;
        int h = getPreferredSize().height;
        setLocation(parentBounds.width - w, parentBounds.height - h);
    }
}
