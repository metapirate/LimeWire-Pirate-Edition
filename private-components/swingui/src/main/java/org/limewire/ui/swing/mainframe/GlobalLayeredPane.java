package org.limewire.ui.swing.mainframe;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import com.google.inject.BindingAnnotation;

/**
 * Annotation allowing injection of the global layered pane for limewire. This
 * panel can be injected into classes so that they can add themselves to the
 * layered panel.
 */
@BindingAnnotation
@Target( { FIELD, PARAMETER, METHOD })
@Retention(RUNTIME)
public @interface GlobalLayeredPane {

}
