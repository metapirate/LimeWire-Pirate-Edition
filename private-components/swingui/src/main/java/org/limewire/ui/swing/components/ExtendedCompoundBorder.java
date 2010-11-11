package org.limewire.ui.swing.components;

import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

/** An extension to CompoundBorder that lets you change the inner & outer borders. */
public class ExtendedCompoundBorder extends CompoundBorder {

    public ExtendedCompoundBorder() {
        super();
    }

    public ExtendedCompoundBorder(Border outsideBorder, Border insideBorder) {
        super(outsideBorder, insideBorder);
    }
    
    public void setOuterBorder(Border outsideBorder) {
        this.outsideBorder = outsideBorder;
    }
    
    public void setInnerBorder(Border insideBorder) {
        this.insideBorder = insideBorder;
    }
    
    

}
