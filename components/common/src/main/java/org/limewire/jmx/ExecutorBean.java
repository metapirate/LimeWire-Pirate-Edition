package org.limewire.jmx;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.management.MXBean;

import org.limewire.util.Objects;

/**
 * A JMX Bean for {@link Executor}s
 * 
 * @see Executor
 * @see ExecutorService
 * @see ThreadPoolExecutor
 */
@MXBean
public interface ExecutorBean {
    
    /**
     * Returns the type of the underlying {@link Executor}
     */
    public String getType();
    
    /**
     * @see ExecutorService
     */
    public boolean isShutdown();
    
    /**
     * @see ExecutorService
     */
    public boolean isTerminated();
    
    /**
     * @see ExecutorService
     */
    public void shutdown();
    
    /**
     * @see ExecutorService
     */
    public void shutdownNow();
    
    /**
     * @see ThreadPoolExecutor
     */
    public int getActiveCount();
    
    /**
     * @see ThreadPoolExecutor
     */
    public long getCompletedTaskCount();
    
    /**
     * @see ThreadPoolExecutor
     */
    public int getCorePoolSize();
    
    /**
     * @see ThreadPoolExecutor
     */
    public int getLargestPoolSize();
    
    /**
     * @see ThreadPoolExecutor
     */
    public int getMaximumPoolSize();
    
    /**
     * @see ThreadPoolExecutor
     */
    public int getPoolSize();
    
    /**
     * @see ThreadPoolExecutor
     */
    public int getQueueSize();
    
    /**
     * @see ThreadPoolExecutor
     */
    public long getTaskCount();
    
    /**
     * @see ThreadPoolExecutor
     */
    public boolean isTerminating();
    
    /**
     * @see ThreadPoolExecutor
     */
    public void purge();
    
    /**
     * @see ThreadPoolExecutor
     */
    public void setCorePoolSize(int corePoolSize);
    
    /**
     * @see ThreadPoolExecutor
     */
    public void setKeepAliveTimeInMillis(long keepAliveTime);
    
    /**
     * @see ThreadPoolExecutor
     */
    public long getKeepAliveTimeInMillis();
    
    /**
     * @see ThreadPoolExecutor
     */
    public void setMaximumPoolSize(int maximumPoolSize);
    
    /**
     * @see ThreadPoolExecutor
     */
    public String getThreadFactory();
    
    /**
     * The default implementation of {@link ExecutorBean}
     */
    public static class Impl implements ExecutorBean {
        
        private final Executor executor;
        
        public Impl(Executor executor) {
            this.executor = Objects.nonNull(executor, "executor");
        }

        @Override
        public String getType() {
            return executor.getClass().getName();
        }

        @Override
        public boolean isShutdown() {
            if (executor instanceof ExecutorService) {
                return ((ExecutorService)executor).isShutdown();
            }
            return false;
        }

        @Override
        public boolean isTerminated() {
            if (executor instanceof ExecutorService) {
                return ((ExecutorService)executor).isTerminated();
            }
            return false;
        }

        @Override
        public void shutdown() {
            if (executor instanceof ExecutorService) {
                ((ExecutorService)executor).shutdown();
            }
        }

        @Override
        public void shutdownNow() {
            if (executor instanceof ExecutorService) {
                ((ExecutorService)executor).shutdownNow();
            }
        }
        
        @Override
        public int getActiveCount() {
            if (executor instanceof ThreadPoolExecutor) {
                return ((ThreadPoolExecutor)executor).getActiveCount();
            }
            return -1;
        }
        
        @Override
        public long getCompletedTaskCount() {
            if (executor instanceof ThreadPoolExecutor) {
                return ((ThreadPoolExecutor)executor).getCompletedTaskCount();
            }
            return -1;
        }
        
        @Override
        public int getCorePoolSize() {
            if (executor instanceof ThreadPoolExecutor) {
                return ((ThreadPoolExecutor)executor).getCorePoolSize();
            }
            return -1;
        }
        
        @Override
        public int getLargestPoolSize() {
            if (executor instanceof ThreadPoolExecutor) {
                return ((ThreadPoolExecutor)executor).getLargestPoolSize();
            }
            return -1;
        }
        
        @Override
        public int getMaximumPoolSize() {
            if (executor instanceof ThreadPoolExecutor) {
                return ((ThreadPoolExecutor)executor).getMaximumPoolSize();
            }
            return -1;
        }
        
        @Override
        public int getPoolSize() {
            if (executor instanceof ThreadPoolExecutor) {
                return ((ThreadPoolExecutor)executor).getPoolSize();
            }
            return -1;
        }
        
        @Override
        public int getQueueSize() {
            if (executor instanceof ThreadPoolExecutor) {
                return ((ThreadPoolExecutor)executor).getQueue().size();
            }
            return -1;
        }
        
        @Override
        public long getTaskCount() {
            if (executor instanceof ThreadPoolExecutor) {
                return ((ThreadPoolExecutor)executor).getTaskCount();
            }
            return -1;
        }
        
        @Override
        public boolean isTerminating() {
            if (executor instanceof ThreadPoolExecutor) {
                return ((ThreadPoolExecutor)executor).isTerminating();
            }
            return false;
        }
        
        @Override
        public void purge() {
            if (executor instanceof ThreadPoolExecutor) {
                ((ThreadPoolExecutor)executor).purge();
            }
        }
        
        @Override
        public void setCorePoolSize(int corePoolSize) {
            if (executor instanceof ThreadPoolExecutor) {
                ((ThreadPoolExecutor)executor).setCorePoolSize(corePoolSize);
            }
        }
        
        @Override
        public void setKeepAliveTimeInMillis(long keepAliveTime) {
            if (executor instanceof ThreadPoolExecutor) {
                ((ThreadPoolExecutor)executor).setKeepAliveTime(
                        keepAliveTime, TimeUnit.MILLISECONDS);
            }
        }
        
        @Override
        public long getKeepAliveTimeInMillis() {
            if (executor instanceof ThreadPoolExecutor) {
                return ((ThreadPoolExecutor)executor).getKeepAliveTime(TimeUnit.MILLISECONDS);
            }
            return -1;
        }
        
        @Override
        public void setMaximumPoolSize(int maximumPoolSize) {
            if (executor instanceof ThreadPoolExecutor) {
                ((ThreadPoolExecutor)executor).setMaximumPoolSize(maximumPoolSize);
            }
        }

        @Override
        public String getThreadFactory() {
            if (executor instanceof ThreadPoolExecutor) {
                return ((ThreadPoolExecutor)executor)
                    .getThreadFactory().getClass().getName();
            }
            return null;
        }
    }
}
