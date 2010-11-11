package com.limegroup.gnutella.templates;

import java.text.ParseException;
import java.util.Map;

/**
 * A generic Template Processor. Given a template, and a mapping of template values to substitution
 * values, attempts to fill the template with the substituted values. If the template is not 
 * valid, an IllegalTemplateException will be thrown. Otherwise a successful template will be
 * created.
 */
public abstract class StoreTemplateProcessor {

    /**
     * States of the substitution.
     */
    protected static enum States {
        LOOKING_FOR_START_DELIM,
        INSIDE_START_DELIM;
    }
    
    /**
     * Substitutable values must be wrapped in a START_DELIM value END_DELIM
     * ex. <artist>
     */
    protected final static char START_DELIM = '<';
    protected final static char END_DELIM = '>';
    
    protected String performSubstitution(final String template, final Map<String,String> substitutions) 
            throws IllegalTemplateException {
        
         final StringBuilder outputBuffer = new StringBuilder();
         StringBuilder subBuffer = new StringBuilder();
         States state = States.LOOKING_FOR_START_DELIM;
         for (int i=0; i<template.length(); i++) {
             final char c = template.charAt(i);

             if(state == States.LOOKING_FOR_START_DELIM ) {
                 if(c == START_DELIM) {
                     state = States.INSIDE_START_DELIM;
                     subBuffer = new StringBuilder();
                 }
                 else {
                     outputBuffer.append(c);
                 }
             } else if( state == States.INSIDE_START_DELIM ) {
                 if( c == END_DELIM ) {
                     final String variable = subBuffer.toString().replaceAll("\\s", "");
                     String replacement = substitutions.get(variable);
                     if (replacement == null) {
                         throw new IllegalTemplateException(i,TEMPLATE_PROCESSOR_UNKNOWN_REPLACEMENT, template);
                     }
                     outputBuffer.append(replacement);
                     state = States.LOOKING_FOR_START_DELIM;
                 } else {
                     subBuffer.append(c);
                 }
             }
         }
         // if still inside a delim, template must be wrong, throw an exception
         if (state == States.INSIDE_START_DELIM) { 
             throw new IllegalTemplateException(template.length(),TEMPLATE_PROCESSOR_UNCLOSED_VARIABLE, template);
         }
         
         outputBuffer.trimToSize();
         return outputBuffer.toString();
    }
    
    /*
     * Illegal Argument Descriptions
     * TODO: need to add a translator for each of these, currently no code in the UI cares about the actual
     *       message so no rush to get this done currently
     */
    final static int TEMPLATE_PROCESSOR_ILLEGAL_CHARACTER = 0;   //Illegal character in template
    final static int TEMPLATE_PROCESSOR_MISSING_DELIMETER = 1;   //Missing delimeter < or >
    final static int TEMPLATE_PROCESSOR_UNKNOWN_REPLACEMENT = 2; //Unknown replacement character
    final static int TEMPLATE_PROCESSOR_UNCLOSED_VARIABLE = 3;   //Unenclosed variable
    
    
    /**
     * Thrown for an invalid template.
     */
    @SuppressWarnings("serial")
    public final static class IllegalTemplateException extends ParseException {
        
        private final int messageType;
        
        private final String template;
                
        public IllegalTemplateException(int pos, final int msgType, final String template) {
            super("",pos); 
            this.messageType = msgType;
            this.template = template;
        }
        
        @Override
        public String getMessage() {
            final StringBuilder sb = new StringBuilder();
            //
            // This is used for testing
            //
            String s = null;
            try {
                s = super.getMessage();
            } catch(Exception e){
                s = e.getLocalizedMessage();
            }
            sb.append(s);
            sb.append(System.getProperty("line.separator"));
            sb.append(template);
            sb.append(System.getProperty("line.separator"));
            for (int i=0, N=getErrorOffset(); i<N; i++) sb.append(' ');
            sb.append('^');
            return sb.toString();
        }
        
        public String getTemplate(){
            return template;
        }
        
        public int getMessageType(){
            return messageType;
        }

    }
}
