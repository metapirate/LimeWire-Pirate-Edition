package org.limewire.ui.swing.advanced.connection;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.swing.table.TableColumn;

import org.limewire.core.api.connection.ConnectionItem;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.CommonUtils;

import ca.odell.glazedlists.gui.TableFormat;

/**
 * This defines the table format for the Connection table.  For formatting
 * individual connection items, ConnectionTableFormat uses logic that was
 * originally contained in the ConnectionDataLine class.
 */
public class ConnectionTableFormat implements TableFormat<ConnectionItem> {
    /** String for connecting status */
    private static final String CONNECTING_STRING = I18n.tr("Connecting...");
    /** String for outgoing status */
    private static final String OUTGOING_STRING = I18n.tr("Outgoing");
    /** String for incoming status */
    private static final String INCOMING_STRING = I18n.tr("Incoming");
    /** String for 'Connected on' tooltip */
    private static final String CONNECTED_ON = I18n.tr("Connected on");
    
    private static final String LEAF = I18n.tr("Leaf");
    private static final String ULTRAPEER = I18n.tr("Ultrapeer");
    private static final String PEER = I18n.tr("Peer");
    private static final String STANDARD = I18n.tr("Standard");    

    public static final int HOST_IDX = 0;
    public static final int STATUS_IDX = 1;
    public static final int MESSAGES_IDX = 2;
    public static final int MESSAGES_IN_IDX = 3;
    public static final int MESSAGES_OUT_IDX = 4;
    public static final int BANDWIDTH_IDX = 5;
    public static final int BANDWIDTH_IN_IDX = 6;
    public static final int BANDWIDTH_OUT_IDX = 7;
    public static final int DROPPED_IDX = 8;
    public static final int DROPPED_IN_IDX = 9;
    public static final int DROPPED_OUT_IDX = 10;
    public static final int PROTOCOL_IDX = 11;
    public static final int VENDOR_IDX = 12;
    public static final int TIME_IDX = 13;
    public static final int COMPRESSION_IDX = 14;
    public static final int COMPRESSION_IN_IDX = 15;
    public static final int COMPRESSION_OUT_IDX = 16;
    public static final int SSL_IDX = 17;
    public static final int SSL_IN_IDX = 18;
    public static final int SSL_OUT_IDX = 19;
    public static final int QRP_FULL_IDX = 20;
    public static final int QRP_USED_IDX = 21;
    
    private final ConnectionColumn[] columns = {
        new ConnectionColumn(HOST_IDX, "CV_COLUMN_HOST", 
                I18n.tr("Host"), 218, true, String.class),
        new ConnectionColumn(STATUS_IDX, "CV_COLUMN_STATUS", 
                I18n.tr("Status"), 70, true, String.class),
        new ConnectionColumn(MESSAGES_IDX, "CV_COLUMN_MESSAGE", 
                I18n.tr("Messages (I/O)"), 97, true, MessagesValue.class),
        new ConnectionColumn(MESSAGES_IN_IDX, "CV_COLUMN_MESSAGE_IN", 
                I18n.tr("Messages In"), 97, false, Integer.class),
        new ConnectionColumn(MESSAGES_OUT_IDX, "CV_COLUMN_MESSAGE_OUT", 
                I18n.tr("Messages Out"), 97, false, Integer.class),
        new ConnectionColumn(BANDWIDTH_IDX, "CV_COLUMN_BANDWIDTH", 
                I18n.tr("Bandwidth (I/O)"), 115, true, BandwidthValue.class),
        new ConnectionColumn(BANDWIDTH_IN_IDX, "CV_COLUMN_BANDWIDTH_IN", 
                I18n.tr("Bandwidth In"), 115, false, BandwidthValue.class),
        new ConnectionColumn(BANDWIDTH_OUT_IDX, "CV_COLUMN_BANDWIDTH_OUT", 
                I18n.tr("Bandwidth Out"), 115, false, BandwidthValue.class),
        new ConnectionColumn(DROPPED_IDX, "CV_COLUMN_DROPPED", 
                I18n.tr("Dropped (I/O)"), 92, true, DroppedValue.class),
        new ConnectionColumn(DROPPED_IN_IDX, "CV_COLUMN_DROPPED_IN", 
                I18n.tr("Dropped In"), 92, false, DroppedValue.class),
        new ConnectionColumn(DROPPED_OUT_IDX, "CV_COLUMN_DROPPED_OUT", 
                I18n.tr("Dropped Out"), 92, false, DroppedValue.class),
        new ConnectionColumn(PROTOCOL_IDX, "CV_COLUMN_PROTOCOL", 
                I18n.tr("Protocol"), 60, true, ProtocolValue.class),
        new ConnectionColumn(VENDOR_IDX, "CV_COLUMN_VENDOR", 
                I18n.tr("Vendor/Version"), 116, true, String.class),
        new ConnectionColumn(TIME_IDX, "CV_COLUMN_TIME", 
                I18n.tr("Time"), 44, true, TimeRemainingValue.class),
        new ConnectionColumn(COMPRESSION_IDX, "CV_COLUMN_COMPRESSION", 
                I18n.tr("Compressed (I/O)"), 114, false, DroppedValue.class),
        new ConnectionColumn(COMPRESSION_IN_IDX, "CV_COLUMN_COMPRESSION_IN", 
                I18n.tr("Compressed In"), 114, false, DroppedValue.class),
        new ConnectionColumn(COMPRESSION_OUT_IDX, "CV_COLUMN_COMPRESSION_OUT", 
                I18n.tr("Compressed Out"), 114, false, DroppedValue.class),
        new ConnectionColumn(SSL_IDX, "CV_COLUMN_SSL", 
                I18n.tr("SSL Overhead (I/O)"), 100, false, DroppedValue.class),
        new ConnectionColumn(SSL_IN_IDX, "CV_COLUMN_SSL_IN", 
                I18n.tr("SSL Overhead In"), 100, false, DroppedValue.class),
        new ConnectionColumn(SSL_OUT_IDX, "CV_COLUMN_SSL_OUT", 
                I18n.tr("SSL Overhead Out"), 100, false, DroppedValue.class),
        new ConnectionColumn(QRP_FULL_IDX, "CV_COLUMN_QRP_FULL", 
                I18n.tr("QRP (%)"), 70, false, QRPValue.class),
        new ConnectionColumn(QRP_USED_IDX, "CV_COLUMN_QRP_USED", 
                I18n.tr("QRP Empty"), 70, false, String.class)
    };

    /**
     * Returns the number of columns.
     */
    @Override
    public int getColumnCount() {
        return columns.length;
    }
    
    /**
     * Returns the name of the column at the specified index.
     */
    @Override
    public String getColumnName(int column) {
        if (column < columns.length) {
            return columns[column].getName();
        }
        return null;
    }

    /**
     * Returns the column value for the specified connection item and column
     * index. 
     */
    @Override
    public Object getColumnValue(ConnectionItem baseObject, int column) {
        if (column < columns.length) {
            return getValueAt(baseObject, column);
        }
        return null;
    }

    /**
     * Returns the table column for the specified ConnectionItem index.
     */
    public ConnectionColumn getColumn(int index) {
        return columns[index];
    }
    
    /**
     * Generates status text for the specified connection.
     */
    private String getStatusText(ConnectionItem connectionItem) {
        switch (connectionItem.getStatus()) {
        case OUTGOING:
            return OUTGOING_STRING;
        case INCOMING:
            return INCOMING_STRING;
        case CONNECTING:
        default:
            return CONNECTING_STRING;
        }
    }

    /**
     * Returns the tooltip text for the specified connection, which displays
     * more fine-grained connection information.
     */
    public String[] getToolTipArray(ConnectionItem connectionItem) {
        Properties props = connectionItem.getHeaderProperties();
        
        List<String> tips = new ArrayList<String>();
        
        if (props == null) {
            // for the lazy .4 connections (yes, some are still there)
            tips.add(CONNECTED_ON + " " + GuiUtils.msec2DateTime(connectionItem.getTime()));
            
        } else {
            tips.add(CONNECTED_ON + " " + GuiUtils.msec2DateTime(connectionItem.getTime()));
            tips.add("");
            
            String k;
            Enumeration ps = props.propertyNames();
            while (ps.hasMoreElements()) {
                k = (String) ps.nextElement();
                tips.add(k + ": " + props.getProperty(k));
            }
        }
        
        return tips.toArray(new String[tips.size()]);
    }

    /**
     * Returns the value for the specified connection and index.
     * @param connectionItem the ConnectionItem containing data
     * @param index one of the index constants
     */
    private Object getValueAt(ConnectionItem connectionItem, int index) {
        switch (index) {
        case HOST_IDX:
            if (!connectionItem.isAddressResolved() // address not resolved yet
                    && connectionItem.isConnected()     // must be connected
                    && System.currentTimeMillis() - connectionItem.getTime() > 10000
                    && SwingUiSettings.RESOLVE_CONNECTION_HOSTNAMES.getValue()) {
                assignHostName(connectionItem);
            } else if(connectionItem.isAddressResolved()
                    && !SwingUiSettings.RESOLVE_CONNECTION_HOSTNAMES.getValue()) {
                connectionItem.resetHostName();
            }
            return connectionItem.getHostName() + ":" + connectionItem.getPort();

        case STATUS_IDX:
            return getStatusText(connectionItem);
            
        case MESSAGES_IDX:
            if (!connectionItem.isConnected()) return null;
            return new MessagesValue(
                connectionItem.getNumMessagesReceived(),
                connectionItem.getNumMessagesSent()
            );
            
        case MESSAGES_IN_IDX:
            if (!connectionItem.isConnected()) return null;
            return connectionItem.getNumMessagesReceived();
            
        case MESSAGES_OUT_IDX:
            if (!connectionItem.isConnected()) return null;
            return connectionItem.getNumMessagesSent();
            
        case BANDWIDTH_IDX:
            if (!connectionItem.isConnected()) return null;
            return new BandwidthValue(
                connectionItem.getMeasuredDownstreamBandwidth(),
                connectionItem.getMeasuredUpstreamBandwidth()
            );
            
        case BANDWIDTH_IN_IDX:
            if (!connectionItem.isConnected()) return null;
            return new BandwidthValue(connectionItem.getMeasuredDownstreamBandwidth());
            
        case BANDWIDTH_OUT_IDX:
            if (!connectionItem.isConnected()) return null;
            return new BandwidthValue(connectionItem.getMeasuredUpstreamBandwidth());
            
        case DROPPED_IDX:
            if (!connectionItem.isConnected()) return null;
            return new DroppedValue(
                connectionItem.getNumReceivedMessagesDropped() /
                    (connectionItem.getNumMessagesReceived() + 1.0f),
                connectionItem.getNumSentMessagesDropped() /
                    (connectionItem.getNumMessagesSent() + 1.0f)
            );
            
        case DROPPED_IN_IDX:
            if (!connectionItem.isConnected()) return null;
            return new DroppedValue(
                connectionItem.getNumReceivedMessagesDropped() /
                    (connectionItem.getNumMessagesReceived() + 1.0f)
            );
            
        case DROPPED_OUT_IDX:
            if (!connectionItem.isConnected()) return null;
            return new DroppedValue(
                connectionItem.getNumSentMessagesDropped() /
                    (connectionItem.getNumMessagesSent() + 1.0f)
            );
            
        case PROTOCOL_IDX:
            return new ProtocolValue(connectionItem);
        
        case VENDOR_IDX:
            if (!connectionItem.isConnected()) return null;
            String vendor = connectionItem.getUserAgent();
            return (vendor == null) ? "" : vendor;
          
        case TIME_IDX:
            return new TimeRemainingValue((int) 
                    ((System.currentTimeMillis() - connectionItem.getTime()) / 1000));
            
        case COMPRESSION_IDX:
            if (!connectionItem.isConnected()) return null;
            return new DroppedValue(
                    connectionItem.getReadSavedFromCompression(),
                    connectionItem.getSentSavedFromCompression());
            
        case COMPRESSION_IN_IDX:
            if (!connectionItem.isConnected()) return null;
            return new DroppedValue(
                    connectionItem.getReadSavedFromCompression());
            
        case COMPRESSION_OUT_IDX:
            if (!connectionItem.isConnected()) return null;
            return new DroppedValue(
                    connectionItem.getSentSavedFromCompression());
            
        case SSL_IDX:
            return new DroppedValue(
                    connectionItem.getReadLostFromSSL(),
                    connectionItem.getSentLostFromSSL());
            
        case SSL_IN_IDX:
            return new DroppedValue(
                    connectionItem.getReadLostFromSSL());
            
        case SSL_OUT_IDX:
            return new DroppedValue(
                    connectionItem.getSentLostFromSSL());
            
        case QRP_FULL_IDX:
            if (!connectionItem.isConnected()) return null;
            return new QRPValue(
                    connectionItem.getQueryRouteTablePercentFull(),
                    connectionItem.getQueryRouteTableSize());
            
        case QRP_USED_IDX:
            if (!connectionItem.isConnected()) return null;
            int empty = connectionItem.getQueryRouteTableEmptyUnits();
            int inuse = connectionItem.getQueryRouteTableUnitsInUse();
            if (empty == -1 || inuse == -1) {
                return null;
            } else {
                return empty + " / " + inuse;
            }

        default:
            return null;
        }
    }
    
    /**
     * Looks up the host name for the connection.  This method launches a 
     * separate thread to perform the lookup because the task can take 
     * considerable time.
     */
    private void assignHostName(ConnectionItem connectionItem) {
        // Put this outside of the runnable so multiple attempts aren't done.
        connectionItem.setAddressResolved(true);

        // Start task to update host name.
        BackgroundExecutorService.execute(new HostAssigner(connectionItem));
    }
    
    /**
     * Defines a column in the Connection table.
     */
    public static class ConnectionColumn extends TableColumn {
        
        private final String id;
        private final String name;
        private final int width;
        private final boolean visible;
        private final Class<?> columnClass;
        
        public ConnectionColumn(int modelIndex, String id, String name,
                int width, boolean visible, Class<?> columnClass) {
            super(modelIndex);
            this.id = id;
            this.name = name;
            this.width = width;
            this.visible = visible;
            this.columnClass = columnClass;
        }
        
        public Class<?> getColumnClass() {
            return this.columnClass;
        }
        
        public String getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public boolean isVisible() {
            return visible;
        }
        
        @Override
        public int getWidth() {
            return width;
        }
    }
    
    /**
     * Assigns the host name field to the connection item without holding an 
     * explicit reference to it.
     */
    private static class HostAssigner implements Runnable {
        private final WeakReference<ConnectionItem> item;
        
        HostAssigner(ConnectionItem connectionItem) {
            item = new WeakReference<ConnectionItem>(connectionItem);
        }
        
        public void run() {
            ConnectionItem connectionItem = item.get();
            if (connectionItem != null) {
                try {
                    connectionItem.setHostName(InetAddress.getByName(
                            connectionItem.getHostName()).getHostName());
                } catch (UnknownHostException ignored) {}
            }
        }
    }
    
    /**
     * Defines the value object for the Bandwidth I/O field.
     */
    private static class BandwidthValue implements Comparable<BandwidthValue> {
        /** Static number formatter. */
        private final static NumberFormat formatter;
        static {
            formatter = NumberFormat.getNumberInstance();
            formatter.setMinimumFractionDigits(3);
            formatter.setMaximumFractionDigits(3);
        }
        
        private final float down;
        private final float up;
        private final String text;

        /**
         * Constructs a BandwidthValue with the specified upload value.
         */
        public BandwidthValue(float up) {
            this.up = up;
            this.down = 0.0f;
            this.text = I18n.tr(GuiUtils.KBPERSEC_FORMAT, formatter.format(up));
        }
        
        /**
         * Constructs a BandwidthValue with the specified download and upload
         * values.
         */
        public BandwidthValue(float down, float up) {
            this.down = down;
            this.up = up;
            this.text = I18n.tr(GuiUtils.KBPERSEC_FORMAT, formatter.format(down) + " / " + formatter.format(up)); 
        }
        
        /**
         * Compares this object with the specified object by the sum of the 
         * up and down values.
         */
        @Override
        public int compareTo(BandwidthValue other) {
            float me = down + up;
            float you = other.down + other.up;
            if ( me > you ) return 1;
            if ( me < you ) return -1;
            return 0;
        }

        @Override
        public String toString() {
            return text;
        }
    }
    
    /**
     * Defines the value object for the Dropped I/O field.
     */
    private static class DroppedValue implements Comparable<DroppedValue> {
        private final int in;
        private final int out;
        private final String text;

        /**
         * Constructs a DroppedValue with the specified input value.
         */
        public DroppedValue(float in) {
            this.in = Math.min(100, (int)(in * 100));
            this.out = 0;
            this.text = Integer.toString(this.in) + "%";
        }
        
        /**
         * Constructs a DroppedValue with the specified input and output values.
         */
        public DroppedValue(float in, float out) {        
            this.in = Math.min(100, (int)(in * 100));
            this.out = Math.min(100, (int)(out * 100));
            this.text = Integer.toString(this.in) + "% / " + Integer.toString(this.out) + "%";
        }
        
        /**
         * Compares this object with the specified object by the sum of the 
         * input and output values.
         */
        @Override
        public int compareTo(DroppedValue other) {
            return ((in + out) - (other.in + other.out));
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /**
     * Defines the value object for the Messages I/O field.
     */
    private static class MessagesValue implements Comparable<MessagesValue> {
        private final int received;
        private final int sent;
        private final String text;

        public MessagesValue(int received, int sent) {
            this.received = received;
            this.sent = sent;
            this.text = received + " / " + sent; 
        }
        
        /**
         * Compares this object with the specified object by the sum of the 
         * messages received and sent.
         */
        @Override
        public int compareTo(MessagesValue other) {
            return ((received + sent) - (other.received + other.sent));
        }

        @Override
        public String toString() {
            return text;
        }
    }
    
    /**
     * Defines the value object for the Protocol field.  The weight value is
     * calculated using rules from the old ProtocolHolder class.
     */
    private static class ProtocolValue implements Comparable<ProtocolValue> {
        private final int weight;
        private final String text;

        public ProtocolValue(ConnectionItem connectionItem) {
            weight = getWeightHostInfo(connectionItem);
            if (connectionItem.isLeaf()) {
                text = LEAF;
            } else if (connectionItem.isUltrapeer()) {
                text = ULTRAPEER;
            } else if (connectionItem.isPeer()) {
                text = PEER;
            } else {
                text = STANDARD;
            }
        }
        
        private static int getWeightHostInfo(ConnectionItem connectionItem) {
            //Assign weight based on bandwidth:
            //4. ultrapeer->ultrapeer
            //3. old-fashioned (unrouted)
            //2. ultrapeer->leaf
            //1. leaf->ultrapeer
            if (connectionItem.isUltrapeerConnection()) {
                if (connectionItem.isUltrapeer()) {
                    return 1;
                } else {
                    return 4;
                }
            } else if (connectionItem.isLeaf()) {
                return 2;
            }
            return 3;
        }   

        @Override
        public int compareTo(ProtocolValue other) {
            return weight - other.weight;
        }

        @Override
        public String toString() {
            return text;
        }
    }
    
    /**
     * Defines the value object for the Query Route fields.
     */
    private static class QRPValue implements Comparable<QRPValue> {
        /** Format for the double. */
        private final static NumberFormat PERCENT_FORMAT;
        static {
            PERCENT_FORMAT = NumberFormat.getPercentInstance();
            PERCENT_FORMAT.setMaximumFractionDigits(2);
            PERCENT_FORMAT.setMinimumFractionDigits(0);
            PERCENT_FORMAT.setGroupingUsed(false);
        }
        
        /** The percent full of this QRP table. */
        private final float percentFull;
        
        /** The size of this QRP table. */
        private final int size;
        
        /** String representation. */
        private final String text;
        
        public QRPValue(double percentFull, int size) {
            this.percentFull = (float) percentFull;
            this.size = size;
            text = PERCENT_FORMAT.format(percentFull/100) + " / " + 
                GuiUtils.toKilobytes(size);
        }
        
        /**
         * Compares this object with the specified object using the two 
         * component values.
         */
        @Override
        public int compareTo(QRPValue other) {
            if (percentFull != other.percentFull)
                return (percentFull < other.percentFull) ? -1 : 1;
            if (size != other.size)
                return (size < other.size) ? -1 : 1;
            return 0;
        }

        @Override
        public String toString() {
            return text;
        }
    }
    
    /**
     * Defines the value object for time remaining.
     */
    private static class TimeRemainingValue implements Comparable<TimeRemainingValue> {
        private final int timeRemaining;
        
        public TimeRemainingValue(int intValue) {
            timeRemaining = intValue;
        }
        
        @Override
        public int compareTo(TimeRemainingValue o) {
            return o.timeRemaining - timeRemaining;
        }
        
        @Override
        public String toString() {
            return (timeRemaining == 0) ? "" : CommonUtils.seconds2time(timeRemaining);
        }
    }
}
