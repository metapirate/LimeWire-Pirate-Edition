package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.Application;
import org.limewire.ui.swing.action.UrlAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.ScrollingTextPane;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;
import org.limewire.util.VersionUtils;

/** The about window. */
class AboutWindow {
    
	private final JDialog dialog;
	private final ScrollingTextPane textPane;
	private final JButton button;
	private final JCheckBox scrollBox = new JCheckBox(I18n.tr("Automatically Scroll"));

	/**
	 * Constructs the elements of the about window.
	 */
	AboutWindow(JFrame frame, Application application) {
	    dialog = new LimeJDialog(frame);
	    
        if (!OSUtils.isMacOSX()) {
            dialog.setModal(true);
        }

		dialog.setSize(new Dimension(450, 400));            
		dialog.setResizable(false);
		dialog.setTitle(I18n.tr("About LimeWire"));
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.addWindowListener(new WindowAdapter() {
		    @Override
		    public void windowOpened(WindowEvent we) {
		        button.requestFocusInWindow();
		    }
		    @Override
            public void windowClosed(WindowEvent we) {
		        textPane.stopScroll();
		    }
		    @Override
            public void windowClosing(WindowEvent we) {
		        textPane.stopScroll();
		    }
		});		

        //  set up scrolling pane
        textPane = createScrollingPane();
        textPane.addHyperlinkListener(GuiUtils.getHyperlinkListener());

        //  set up limewire version label
        JLabel client = new JLabel(I18n.tr("LimeWire") +
                " " + application.getVersion());
        client.setHorizontalAlignment(SwingConstants.CENTER);
        
        //  set up java version label
        JLabel java = new JLabel("Java " + VersionUtils.getJavaVersion());
        java.setHorizontalAlignment(SwingConstants.CENTER);
        
        //  set up limewire.com label
        HyperlinkButton url = new HyperlinkButton("http://www.limewire.com");
        url.addActionListener(new UrlAction("http://www.limewire.com"));
        url.setHorizontalAlignment(SwingConstants.CENTER);

        //  set up scroll check box
		scrollBox.setSelected(true);
		scrollBox.setOpaque(false);
		scrollBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if (scrollBox.isSelected())
					textPane.startScroll();
				else
					textPane.stopScroll();
			}
		});

        //  set up close button
        button = new JButton(I18n.tr("Close"));
        dialog.getRootPane().setDefaultButton(button);
        button.setToolTipText(I18n.tr("Close This Window"));
        button.addActionListener(GuiUtils.getDisposeAction());

        //  layout window
		JComponent pane = (JComponent)dialog.getContentPane();
		GuiUtils.addHideAction(pane);
		
		pane.setLayout(new MigLayout("insets 4 4 4 4, fill"));
		pane.setBackground(GuiUtils.getMainFrame().getBackground());
        
        pane.add(client, "span, growx, wrap");
        pane.add(java, "span, growx, wrap");
        pane.add(url, "span, growx, wrap");
        pane.add(textPane, "grow, wrap");
        pane.add(scrollBox, "split 2, growx, push");
        pane.add(button, "alignx right");	
	}

	private ScrollingTextPane createScrollingPane() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");

        Color color = new JLabel().getForeground();
        String hex = GuiUtils.colorToHex(color);
        sb.append("<body text='#" + hex + "'>");

        //  introduction
        sb.append(I18n.tr("Inspired by LimeWire\'s owner, Mark Gorton, the LimeWire project is a " + 
                "collaborative <a href=\"http://www.limewire.org/\">open source effort</a> involving " +
                "programmers and researchers from all over the world.  " +
                "LimeWire is also, of course, the result of the countless hours of work by LimeWire\'s developers:"));

        sb.append("<br>");

        sb.append("<table border=\"0\" cellspacing=\"5\">" +                 
                " <tr> " + 
                "   <td>Felix Berger</td>" + 
                "   <td>David Chen</td>" + 
                " </tr>" + 
                " <tr>" + 
                "   <td>Priyank Dhillon</td>" + 
                "   <td>Mike Everett</td>" + 
                " </tr>" + 
                " <tr>" + 
                "   <td>Karundeep Gill</td>" + 
                "   <td>Roger Kapsi</td>" +
                " </tr>" + 
                " <tr>" + 
                "   <td>Greg Kellum</td>" + 
                "   <td>Marc London</td>" + 
                " </tr>" + 
                " <tr>" + 
                "   <td>Greg Maggioncalda</td>" + 
                "   <td>Aditya Malpani</td>" + 
                " </tr>" + 
                " <tr>" + 
                "   <td>Jorge Mancheno</td>" + 
                "   <td>Michael Rogers</td>" + 
                " </tr>" + 
                " <tr>" + 
                "   <td>Anthony Roscoe</td>" + 
                "   <td>Neha Sharma</td>" + 
                " </tr>" + 
                " <tr>" + 
                "   <td>Mike Sorvillo</td>" + 
                "   <td>Michael Tiraborrelli</td>" + 
                " </tr>" + 
                " <tr>" + 
                "   <td>Matt Turkel</td>" + 
                "   <td>Peter Vertenten</td>" + 
                " </tr>" + 
                " <tr>" + 
                "   <td>Ernie Yu</td>" + 
                " </tr>" + 
                " </table>");
        
        //  previous developers
        sb.append(I18n.tr("In addition, the following individuals have worked on the LimeWire team in the past but have since moved on to other projects:"));

        sb.append("<table border=\"0\" cellspacing=\"5\">" +                 
                " <tr>" +
                "   <td>Mario Aquino</td>" +
                "   <td>Aubrey Arago</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Zlatin Balevsky</td>" +
                "   <td>Zenzele Bell</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Anthony Bow</td>" +
                "   <td>Sam Berlin</td>" + 
                " </tr>" +
                " <tr>" +
                "   <td>Katie Catillaz</td>" +
                "   <td>Wynne Chan</td>" + 
                " </tr>" +
                " <tr>" +
                "   <td>Susheel Daswani</td>" +
                "   <td>Luck Dookchitra</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Kevin Faaborg</td>" +
                "   <td>Adam Fisk</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Bobby Fonacier</td>" +
                "   <td>Meghan Formel</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Jay Jeyaratnam</td>" +
                "   <td>Curtis Jones</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Tim Julien</td>" + 
                "   <td>Tarun Kapoor</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Mark Kornfilt</td>" +
                "   <td>Akshay Kumar</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Angel Leon</td>" +
                "   <td>Nathan Lovejoy</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Karl Magdsick</td>" +
                "   <td>Yusuke Naito</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Dave Nicponski</td>" +
                "   <td>Christine Nicponski</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Tim Olsen</td>" +
                "   <td>Jeff Palm</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Jason Pelzer</td>" +
                "   <td>Steffen Pingel</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Christopher Rohrs</td>" +
                "   <td>Justin Schmidt</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Varnali Shah</td>" + 
                "   <td>Arthur Shim</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Anurag Singla</td>" +
                "   <td>Francesca Slade</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Robert Soule</td>" +
                "   <td>Rachel Sterne</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Sumeet Thadani</td>" +
                "   <td>Ron Vogl</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Peng Wang</td>" + 
                "   <td>Kurt Wasserman</td>" + 
                " </tr>" +
                " <tr>" +
                "   <td>E.J. Wolborsky</td>" +
                " </tr>" +
                " </table>");

        //  open source contributors
        sb.append(I18n.tr("LimeWire open source contributors have provided significant code and many bug fixes, ideas, research, etc. to the project as well. Those listed below have either written code that is distributed with every version of LimeWire, have identified serious bugs in the code, or both:"));
        sb.append("<table border=\"0\" cellspacing=\"5\">" +                 
                " <tr>" +
                "   <td>Richie Bielak</td>" +
                "   <td>Johanenes Blume</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Jerry Charumilind</td>" +
                "   <td>Marvin Chase</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Robert Collins</td>" +
                "   <td>Kenneth Corbin</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Kyle Furlong</td>" +
                "   <td>David Graff</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Andy Hedges</td>" +
                "   <td>Michael Hirsch</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Panayiotis Karabassis</td>" +
                "   <td>Marcin Koraszewski</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Jens-Uwe Mager</td>" +
                "   <td>Misagh Moayyed</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Gordon Mohr</td>" +
                "   <td>Chance Moore</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Miguel Munoz</td>" +
                "   <td>Rick T. Piazza</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Eugene Romanenko</td>" +
                "   <td>Gregorio Roper</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>William Rucklidge</td>" +
                "   <td>Claudio Santini</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Phil Schalm</td>" +
                "   <td>Eric Seidel</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Philippe Verdy</td>" +
                "   <td>Cameron Walsh</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>Stephan Weber</td>" +
                "   <td>Jason Winzenried</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>'Tobias'</td>" +
                "   <td>'deacon72'</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>'MaTZ'</td>" +
                "   <td>'RickH'</td>" +
                " </tr>" +
                " <tr>" +
                "   <td>'PNomolos'</td>" +
                "   <td>'ultracross'</td>" +
                " </tr>" + 
                " </table>");
         
        //  internationalization contributors
        sb.append(I18n.tr("LimeWire would also like to thank the many contributors to the internationalization project, both for the application itself and for the LimeWire web site."));
        sb.append("<p>");
        
        //  community VIPs
        sb.append(I18n.tr("Several colleagues in the Gnutella community merit special thanks. These include:"));

        sb.append("<table border=\"0\" cellspacing=\"5\">" +                 
                "<tr><td>Vincent Falco -- Free Peers, Inc.</td></tr>" + 
                "<tr><td>Gordon Mohr -- Bitzi, Inc.</td></tr>" + 
                "<tr><td>John Marshall -- Gnucleus</td></tr>" + 
                "<tr><td>Jason Thomas -- Swapper</td></tr>" + 
                "<tr><td>Brander Lien -- ToadNode</td></tr>" + 
                "<tr><td>Angelo Sotira -- www.gnutella.com</td></tr>" + 
                "<tr><td>Marc Molinaro -- www.gnutelliums.com</td></tr>" + 
                "<tr><td>Simon Bellwood -- www.gnutella.co.uk</td></tr>" + 
                "<tr><td>Serguei Osokine</td></tr>" + 
                "<tr><td>Justin Chapweske</td></tr>" + 
                "<tr><td>Mike Green</td></tr>" + 
                "<tr><td>Raphael Manfredi</td></tr>" + 
                "<tr><td>Tor Klingberg</td></tr>" + 
                "<tr><td>Mickael Prinkey</td></tr>" + 
                "<tr><td>Sean Ediger</td></tr>" + 
                "<tr><td>Kath Whittle</td></tr>" + 
                "</table>" );
        
        //  conclusion
        sb.append(I18n.tr("Finally, LimeWire would like to extend its sincere thanks to those developers, users, and all others who have contributed their ideas to the project. Without LimeWire users, the P2P Network would not exist."));
        
        // bt notice
        sb.append("<small>");
        sb.append("<br><br>");
        sb.append(I18n.tr("Yo dawg, I heard you like pirates, so I pirated your piracy so you can pirate while you pirate."));
        sb.append("</small>");
        
        sb.append("</body></html>");
        
        return new ScrollingTextPane(sb.toString());
    }

    /**
	 * Displays the "About" dialog window to the user.
	 */
	void showDialog() {
	    if (dialog.getParent().isVisible()) {
	        dialog.setLocationRelativeTo(dialog.getParent());
        } else { 
            dialog.setLocation(GuiUtils.getScreenCenterPoint(dialog));
        }

		if (scrollBox.isSelected()) {
			ActionListener startTimerListener = new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
				    //need to check isSelected() again,
				    //it might have changed in the past 10 seconds.
				    if (scrollBox.isSelected()) {
				        //activate scroll timer
					    textPane.startScroll();
					}
				}
			};
			
			Timer startTimer = new Timer(10000, startTimerListener);
			startTimer.setRepeats(false);			
			startTimer.start();
		}
		dialog.setVisible(true);
	}
}
