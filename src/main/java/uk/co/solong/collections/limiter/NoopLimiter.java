package uk.co.solong.collections.limiter;

public class NoopLimiter implements Limiter {

    /**
     * Deliberately performs no-operation
     */
    @Override
    public void clockIn() throws InterruptedException {
    }

}
