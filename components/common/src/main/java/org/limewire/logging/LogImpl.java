package org.limewire.logging;

import java.text.MessageFormat;

/**
 * Implementation of {@link Log}, delegates to provided 
 * {@link org.apache.commons.logging.Log}.
 */
public class LogImpl implements Log {

    private final org.apache.commons.logging.Log delegate;
    
    public LogImpl(org.apache.commons.logging.Log delegate) {
        this.delegate = delegate;
    }

    public void debug(Object arg0, Throwable arg1) {
        delegate.debug(arg0, arg1);
    }

    public void debug(Object arg0) {
        delegate.debug(arg0);
    }

    public void error(Object arg0, Throwable arg1) {
        delegate.error(arg0, arg1);
    }

    public void error(Object arg0) {
        delegate.error(arg0);
    }

    public void fatal(Object arg0, Throwable arg1) {
        delegate.fatal(arg0, arg1);
    }

    public void fatal(Object arg0) {
        delegate.fatal(arg0);
    }

    public void info(Object arg0, Throwable arg1) {
        delegate.info(arg0, arg1);
    }

    public void info(Object arg0) {
        delegate.info(arg0);
    }

    public boolean isDebugEnabled() {
        return delegate.isDebugEnabled();
    }

    public boolean isErrorEnabled() {
        return delegate.isErrorEnabled();
    }

    public boolean isFatalEnabled() {
        return delegate.isFatalEnabled();
    }

    public boolean isInfoEnabled() {
        return delegate.isInfoEnabled();
    }

    public boolean isTraceEnabled() {
        return delegate.isTraceEnabled();
    }

    public boolean isWarnEnabled() {
        return delegate.isWarnEnabled();
    }

    public void trace(Object arg0, Throwable arg1) {
        delegate.trace(arg0, arg1);
    }

    public void trace(Object arg0) {
        delegate.trace(arg0);
    }

    public void warn(Object arg0, Throwable arg1) {
        delegate.warn(arg0, arg1);
    }

    public void warn(Object arg0) {
        delegate.warn(arg0);
    }

    @Override
    public void debugf(Throwable t, String message, Object... args) {
        if (delegate.isDebugEnabled()) {
            delegate.debug(MessageFormat.format(message, args), t);
        }
    }

    public void debugf(String message, Object... args) {
        if (delegate.isDebugEnabled()) {
            delegate.debug(MessageFormat.format(message, args));
        }
    }
    
    public void debugf(String message, Object arg) {
        if (delegate.isDebugEnabled()) {
            delegate.debug(MessageFormat.format(message, arg));
        }
    }

    public void debugf(String message, Object arg1, Object arg2) {
        if (delegate.isDebugEnabled()) {
            delegate.debug(MessageFormat.format(message, arg1, arg2));
        }
    }

    public void debugf(String message, Object arg1, Object arg2, Object arg3) {
        if (delegate.isDebugEnabled()) {
            delegate.debug(MessageFormat.format(message, arg1, arg2, arg3));
        }
    }

    @Override
    public void infof(Throwable t, String message, Object... args) {
        if (delegate.isInfoEnabled()) {
            delegate.info(MessageFormat.format(message, args), t);
        }
    }

    public void infof(String message, Object... args) {
        if (delegate.isInfoEnabled()) {
            delegate.info(MessageFormat.format(message, args));
        }
    }
    
    public void infof(String message, Object arg) {
        if (delegate.isInfoEnabled()) {
            delegate.info(MessageFormat.format(message, arg));
        }
    }

    public void infof(String message, Object arg1, Object arg2) {
        if (delegate.isInfoEnabled()) {
            delegate.info(MessageFormat.format(message, arg1, arg2));
        }
    }

    public void infof(String message, Object arg1, Object arg2, Object arg3) {
        if (delegate.isInfoEnabled()) {
            delegate.info(MessageFormat.format(message, arg1, arg2, arg3));
        }
    }

    @Override
    public void tracef(Throwable t, String message, Object... args) {
        if (delegate.isTraceEnabled()) {
            delegate.trace(MessageFormat.format(message, args), t);
        }
    }

    public void tracef(String message, Object... args) {
        if (delegate.isTraceEnabled()) {
            delegate.trace(MessageFormat.format(message, args));
        }
    }
    
    public void tracef(String message, Object arg) {
        if (delegate.isTraceEnabled()) {
            delegate.trace(MessageFormat.format(message, arg));
        }
    }

    public void tracef(String message, Object arg1, Object arg2) {
        if (delegate.isTraceEnabled()) {
            delegate.trace(MessageFormat.format(message, arg1, arg2));
        }
    }

    public void tracef(String message, Object arg1, Object arg2, Object arg3) {
        if (delegate.isTraceEnabled()) {
            delegate.trace(MessageFormat.format(message, arg1, arg2, arg3));
        }
    }

    @Override
    public void warnf(Throwable t, String message, Object... args) {
        if (delegate.isWarnEnabled()) {
            delegate.warn(MessageFormat.format(message, args), t);
        }
    }

    public void warnf(String message, Object... args) {
        if (delegate.isWarnEnabled()) {
            delegate.warn(MessageFormat.format(message, args));
        }
    }

    public void warnf(String message, Object arg) {
        if (delegate.isWarnEnabled()) {
            delegate.warn(MessageFormat.format(message, arg));
        }
    }

    public void warnf(String message, Object arg1, Object arg2) {
        if (delegate.isWarnEnabled()) {
            delegate.warn(MessageFormat.format(message, arg1, arg2));
        }
    }

    public void warnf(String message, Object arg1, Object arg2, Object arg3) {
        if (delegate.isWarnEnabled()) {
            delegate.warn(MessageFormat.format(message, arg1, arg2, arg3));
        }
    }

    @Override
    public void errorf(Throwable t, String message, Object... args) {
        if (delegate.isErrorEnabled()) {
            delegate.error(MessageFormat.format(message, args), t);
        }
    }

    public void errorf(String message, Object... args) {
        if (delegate.isErrorEnabled()) {
            delegate.error(MessageFormat.format(message, args));
        }
    }

    public void errorf(String message, Object arg) {
        if (delegate.isErrorEnabled()) {
            delegate.error(MessageFormat.format(message, arg));
        }
    }

    public void errorf(String message, Object arg1, Object arg2) {
        if (delegate.isErrorEnabled()) {
            delegate.error(MessageFormat.format(message, arg1, arg2));
        }
    }

    public void errorf(String message, Object arg1, Object arg2, Object arg3) {
        if (delegate.isErrorEnabled()) {
            delegate.error(MessageFormat.format(message, arg1, arg2, arg3));
        }
    }
}
