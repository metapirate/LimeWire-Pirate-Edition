package com.limegroup.gnutella.tigertree.dime;

class TigerDimeUtils {

    static final String SERIALIZED_TREE_TYPE = "http://open-content.net/spec/thex/breadthfirst";

    static final String DIGEST = "http://open-content.net/spec/digest/tiger";

    static final String DTD_PUBLIC_ID = "-//NET//OPEN-CONTENT//THEX 02//EN";

    static final String DTD_SYSTEM_ID = "http://open-content.net/spec/thex/thex.dtd";

    static final String DTD_ENTITY = "<!ELEMENT hashtree (file,digest,serializedtree)>"
            + "<!ELEMENT file EMPTY>" + "<!ATTLIST file size CDATA #REQUIRED>"
            + "<!ATTLIST file segmentsize CDATA #REQUIRED>" + "<!ELEMENT digest EMPTY>"
            + "<!ATTLIST digest algorithm CDATA #REQUIRED>"
            + "<!ATTLIST digest outputsize CDATA #REQUIRED>" + "<!ELEMENT serializedtree EMPTY>"
            + "<!ATTLIST serializedtree depth CDATA #REQUIRED>"
            + "<!ATTLIST serializedtree type CDATA #REQUIRED>"
            + "<!ATTLIST serializedtree uri CDATA #REQUIRED>";

    static final String SYSTEM_STRING = "SYSTEM";

    static int HASH_SIZE = 24;

}
