package joshng.util.retries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: josh
 * Date: 6/26/12
 * Time: 4:27 PM
 */
public class GlobalExclusiveRetryPolicy extends BasicRetryPolicy {
    private final ReentrantLock sessionLock = new ReentrantLock();

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExclusiveRetryPolicy.class);
    
    @Override
    public RetrySession newSession() {
        
        return new BasicRetrySession(this) {

            boolean hasPermission = false;
            @Override
            public boolean canRetry(Exception e) {
                return super.canRetry(e) && obtainPermission();
            }

            public <T> T retry(Callable<T> closure) throws RetryAbortedException {
                try {
                    return super.retry(closure);
                } finally {
                    if (hasPermission) {
                        sessionLock.unlock();
                    }
                }
            }

            private boolean obtainPermission() {
                if(!hasPermission) {
                    sessionLock.lock();
                    hasPermission = true;
                }
                return true;
            }
        };
    }
}
