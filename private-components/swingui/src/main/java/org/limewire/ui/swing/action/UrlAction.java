package org.limewire.ui.swing.action;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import org.limewire.concurrent.FutureEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.HTMLPane;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.HTMLPane.LoadResult;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.ResizeUtils;

public class UrlAction extends AbstractAction {
    private final LaunchType type;
    private String url;
    private final String title;
    private final GetParamAppender getParamAppender;

    /**
     * Constructs an UrlAction whose name is its URL with a given launch type and the param
     *  appender passed.
     */
    public UrlAction(String url, LaunchType type, GetParamAppender getParamAppender) {
        this(url, url, null, type, getParamAppender);
    }

    /**
     * Constructs an action that will spawn the url in an in process popup with the given title.
     */
    public UrlAction(String url, String title, GetParamAppender getParamAppender) {
        this(url, url, title, LaunchType.POPUP, getParamAppender);
    }

    /**
     * Constructs an UrlAction whose name is its URL, without any identifying
     * information added to the URL and the default {@link LaunchType}.
     */
    public UrlAction(String url) {
        this(url, url, null, LaunchType.EXTERNAL_BROWSER, null);
    }

    /**
     * Constructs an UrlAction with a specific name & url, without any identifying
     * information added to the URL.
     */
    public UrlAction(String name, String url) {
        this(name, url, null, LaunchType.EXTERNAL_BROWSER, null);
    }

    /**
     * Constructs an UrlAction with a specific name & url, a specific launch type, and 
     *  without any identifying information added to the URL.
     */
    public UrlAction(String name, String url, LaunchType type) {
        this(name, url, null, type, null);
    }

    /**
     * Constructs an UrlAction with a specific name & url, with identifying
     * information added to the URL, and a specific launch type.
     */
    public UrlAction(String name, String url, String title, LaunchType type, GetParamAppender getParamAppender) {
        super(name);
        this.url = url;
        this.type = type;
        this.title = title;
        this.getParamAppender = getParamAppender;
        putValue(Action.SHORT_DESCRIPTION, url);
    }

    public void setURL(String url) {
        this.url = url;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String urlToShow = url;
        if (getParamAppender != null) {
            urlToShow = getParamAppender.appendParams(urlToShow);
        }

        if (type == LaunchType.EXTERNAL_BROWSER) {
            NativeLaunchUtils.openURL(urlToShow);
        }
        else {
            showPopup(urlToShow, title);
        }
    }

    private static void showPopup(final String urlToShow, final String title) {
        new LimeJDialog() {
            {   getContentPane().setLayout(new BorderLayout());
            HTMLPane browser = new HTMLPane();

            browser.addHyperlinkListener(new HyperlinkListener() {
                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == EventType.ACTIVATED) {
                        NativeLaunchUtils.openURL(e.getURL().toString());
                    }
                }
            });

            JScrollPane scrollPane = new JScrollPane(browser, 
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            getContentPane().add(scrollPane);

            ResizeUtils.forceSize(this, new Dimension(600,400));

            setTitle(title);
            setModal(true);
            setResizable(true);
            setAlwaysOnTop(true);
            getContentPane();
            pack();
            setLocationRelativeTo(null);

            // If popout browser does not work use the system browser.
            browser.setPageAsynchronous(urlToShow, null).addFutureListener(new EventListener<FutureEvent<LoadResult>>() {
                @SwingEDTEvent
                @Override
                public void handleEvent(FutureEvent<LoadResult> event) {
                    if (event.getResult() != LoadResult.SERVER_PAGE) {
                        dispose();
                        NativeLaunchUtils.openURL(urlToShow);
                    }
                }

            });

            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            setVisible(true);
            }
        };
    }

    /**
     * The type of show action to be used.
     */
    public enum LaunchType {
        /** Launch the url in an external browser **/
        EXTERNAL_BROWSER, 

        /** Spawn a temporary dialogue to show the url **/
        POPUP;
    }

    /**
     * An interface used to append get params to the url before launching it.
     */
    public static interface GetParamAppender {
        public String appendParams(String original);
    }
}