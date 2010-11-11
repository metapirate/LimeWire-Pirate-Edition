package org.limewire.ui.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.limewire.ui.support.FatalBugManager;
import org.limewire.ui.swing.util.GuiUtils;

final class GuiLoader {

    /** 
     * Creates an {@link Initializer} instance that constructs the 
     * necessary classes for the application.
     *
     * <p>Invoked by {@link Main} by reflection.
     *
     * @param args the array of command line arguments
     * @param frame the splash screen; null, if no splash is displayed
     */
    public void load(String args[], Frame splashFrame, Image splashImage) {
        try { 
            //sanityCheck();
            Initializer initializer = new Initializer();
            initializer.initialize(args, splashFrame, splashImage);
        } 
//        catch(StartupFailedException sfe) {
//            GuiUtils.hideAndDisposeAllWindows();
//            showCorruptionError(sfe);
//            System.exit(1);
//        }
        catch(Throwable err) {
            GuiUtils.hideAndDisposeAllWindows();
            try {
                FatalBugManager.handleFatalBug(err);
            } catch(Throwable t) {
                Throwable error = err;
                try {
                    t.initCause(err);
                    error = t;
                } catch(Throwable ignored) {}
                showCorruptionError(error);
                System.exit(1);
            }
        }
    }

    /**
     * Display a standardly formatted internal error message
     * coming from the backend.
     *
     * @param err the <tt>Throwable</tt> object containing information 
     *  about the error
     */ 
    private static final void showCorruptionError(Throwable err) {
        err.printStackTrace();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("LimeWire version @version@");
        pw.print("Java version ");
        pw.print(System.getProperty("java.version", "?"));
        pw.print(" from ");
        pw.println(System.getProperty("java.vendor", "?"));
        pw.print(System.getProperty("os.name", "?"));
        pw.print(" v. ");
        pw.print(System.getProperty("os.version", "?"));
        pw.print(" on ");
        pw.println(System.getProperty("os.arch", "?"));
        Runtime runtime = Runtime.getRuntime();
        pw.println("Free/total memory: "
                   +runtime.freeMemory()+"/"+runtime.totalMemory());
        pw.println();
        
        err.printStackTrace(pw);
        
        pw.println();
        
        pw.println("STARTUP ERROR!");
        pw.println();
        
        pw.flush();
        
        displayError(sw.toString());
    }
    
    /**
     * Displays an internal error with specialized formatting.
     */
    private static final void displayError(String error) {
        System.out.println("Error: " + error);
        final JDialog DIALOG = new JDialog();
        DIALOG.setModal(true);
        final Dimension DIALOG_DIMENSION = new Dimension(350, 200);
        final Dimension INNER_SIZE = new Dimension(300, 150);
        DIALOG.setSize(DIALOG_DIMENSION);

        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        mainPanel.setLayout(new BorderLayout());


        String instr0;
        String instr1;
        String instr2;
        String instr3;
        String instr4;
        String instr5;
        
        instr0 = "One or more necessary files appear to be invalid.";
        instr1 = "This is generally caused by a corrupted installation.";
        instr2 = "Please try downloading and installing LimeWire again.";
        instr3 = "If the problem persists, please visit www.limewire.com ";
        instr4 = "and click the 'Support' link.  ";
        instr5 = "Thank you.";

        JLabel label0 = new JLabel(instr0);
        JLabel label1 = new JLabel(instr1);
        JLabel label2 = new JLabel(instr2);
        JLabel label3 = new JLabel(instr3);
        JLabel label4 = new JLabel(instr4);
        JLabel label5 = new JLabel(instr5);
        
        JPanel labelPanel = new JPanel();
        JPanel innerLabelPanel = new JPanel();
        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
        innerLabelPanel.setLayout(new BoxLayout(innerLabelPanel, BoxLayout.Y_AXIS));
        innerLabelPanel.add(label0);
        innerLabelPanel.add(label1);
        innerLabelPanel.add(label2);
        innerLabelPanel.add(label3);
        innerLabelPanel.add(label4);
        innerLabelPanel.add(label5);
        innerLabelPanel.add(Box.createVerticalStrut(6));
        labelPanel.add(innerLabelPanel);
        labelPanel.add(Box.createHorizontalGlue());


        final JTextArea textArea = new JTextArea(error);
        textArea.selectAll();
        textArea.copy();
        textArea.setColumns(50);
        textArea.setEditable(false);
        JScrollPane scroller = new JScrollPane(textArea);
        scroller.setBorder(BorderFactory.createEtchedBorder());
        scroller.setPreferredSize(INNER_SIZE);


        JPanel buttonPanel = new JPanel();
        JButton copyButton = new JButton("Copy Report");
        copyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                textArea.selectAll();
                textArea.copy();
            }
        });
        JButton quitButton = new JButton("Ok");
        quitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                DIALOG.dispose();
            }
        });
        buttonPanel.add(copyButton);
        buttonPanel.add(quitButton);

        mainPanel.add(labelPanel, BorderLayout.NORTH);
        mainPanel.add(scroller, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        DIALOG.getContentPane().add(mainPanel);
        DIALOG.pack();        

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension dialogSize = DIALOG.getSize();
        DIALOG.setLocation((screenSize.width - dialogSize.width)/2,
                           (screenSize.height - dialogSize.height)/2);
        DIALOG.setVisible(true);
    }   
    
    /**
     * Determines whether or not specific files exist and are the correct size.
     * If they do not exist or are the incorrect size, 
     * throws MissingResourceException.
     */         
//    private static void sanityCheck() throws StartupFailedException {
//        File test = new File("gpl.txt");
//        boolean isCVS = false;
//        
//        // If the gpl.txt exists, then we're running off of CVS.
//        if( test.exists() && test.isFile() ) {
//            isCVS = true;
//        }
//        // If it doesn't, we're a production version.
//        else {
//            isCVS = false;
//        }
//        
//        String root = isCVS ? "../lib/jars" : (isWindows() ? "./lib" : ".");        
//        File themesJar = new File(root, "themes.jar");
//        
//        if( !isCVS && (!themesJar.exists() || !themesJar.isFile()) )
//            throw new StartupFailedException("invalid themes.jar");
//            
//        // Then do more advanced hash checks.
//        try {
//            verifyHashes(root);
//        } catch(IOException e) {
//            throw new StartupFailedException(e.getMessage());
//        }
//    }
//    
//    /**
//     * Loads up a file 'hashes' and verifies that all the files/hashes
//     * in it match the hashes we have.
//     */
//    private static void verifyHashes(String root) throws IOException, StartupFailedException {
//        // Load up the file with the hashes.
//        Properties props = new Properties();
//        FileInputStream fis = null;
//        try {
//            fis = new FileInputStream(new File(root, "hashes"));
//            props.load(fis);
//        } finally {
//            if( fis != null ) {
//                try {
//                    fis.close();
//                    fis = null;
//                } catch(IOException ignored) {}
//            }
//        }
//        
//        //Iterate through each expected hash to verify that
//        //the files on disk are not corrupted.
//        Enumeration names = props.propertyNames();
//        while(names.hasMoreElements()) {
//            String name = (String)names.nextElement();
//            String storedHash = props.getProperty(name);
//            String realHash = hash(new File(root, name));
//            if(!realHash.equals(storedHash)) {
//                throw new StartupFailedException(
//                    "file [" + name + "] has hash of [" + realHash +
//                    "] instead of expected [" + storedHash + "]");
//            }
//        }
//    }
//    
//    /**
//     * Determines the MD5 hash of the specified file.
//     */
//    private static String hash(File f) throws IOException {        
//        FileInputStream fis = null;     
//        try {
//            fis = new FileInputStream(f);
//            MessageDigest md = null;
//            try {
//                md = MessageDigest.getInstance("MD5");
//            } catch(NoSuchAlgorithmException e) {
//                throw new IOException("Unknown algorithm: MD5");
//            }
//            
//            byte[] buffer = new byte[16384];
//            int read;
//            while ((read=fis.read(buffer)) != -1) {
//                md.update(buffer, 0, read);
//            }
//            
//            return toHexString(md.digest());
//        } finally {
//            if( fis != null ) {
//                try {
//                    fis.close();
//                } catch(IOException ignored) {}
//            }
//        }
//    }
//    
//    /**
//     * Converts a 16-byte array of unsigned bytes
//     * to a 32 character hex string.
//     */
//    private static String toHexString(byte[] data) {
//        StringBuilder sb = new StringBuilder(32);
//        for(int i = 0; i < data.length; i++) {
//            int asInt = data[i] & 0x000000FF;
//            String x = Integer.toHexString(asInt);
//            if(x.length() == 1)
//                sb.append("0");
//            sb.append(x);
//        }
//        return sb.toString().toUpperCase();
//    }
//    
//    /**
//     * @return if we're running on windows without using OSUtils.
//     */
//    private static boolean isWindows() {
//        String os = System.getProperty("os.name").toLowerCase(Locale.US);
//        return os.contains("windows");
//    }

//    private static class StartupFailedException extends Exception {
//        StartupFailedException(String msg) {
//            super(msg);
//        } 
//        
//        StartupFailedException() {
//            super();
//        }
//    }
    
}
