package org.limewire.ui.swing.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.ParseException;

import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.Timer;

/**
 * An implementation of {@link KeyListener} that will attempt to validate, and if needed,
 *  reset invalid input of a {@link JSpinner} between pauses in typing.
 */
public class PeriodicFieldValidator implements KeyListener {

        private final Timer timer = new Timer(600, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int position = field.getCaretPosition();
                String orignalString = field.getText();
                
                if (!"".equals(field.getText())) {
                    try {
                        field.commitEdit();
                        field.setText(orignalString);
                        field.setCaretPosition(position);
                    } catch (ParseException e1) {
                        field.setValue(field.getValue());
                        field.setCaretPosition(field.getText().length());
                    }
                }
            }
        });
        
        private final JFormattedTextField field;
        
        public PeriodicFieldValidator(JFormattedTextField field) {
            this.field = field;
            
            timer.setRepeats(false);
        }
        
        @Override
        public void keyTyped(KeyEvent e) {
            timer.stop();
            timer.start();
        }
        @Override
        public void keyPressed(KeyEvent e) {
        }
        @Override
        public void keyReleased(KeyEvent e) {
        }
    }