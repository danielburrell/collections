package uk.co.solong.collections.limiter;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleLimiter implements Limiter {

    private static final Logger logger = LoggerFactory.getLogger(SimpleLimiter.class);

    private final BlockingQueue<CallDamElement> queue;
    private final Duration period;
    private final Runnable purge;

    public SimpleLimiter(final int upperLimit, final Duration duration, final long purgePeriod, final TimeUnit purgeUnit) {
        this.period = duration;
        this.queue = new ArrayBlockingQueue<CallDamElement>(upperLimit);
        this.purge = new Runnable() {

            @Override
            public void run() {
                try {
                    while (isTooOld(queue.peek())) {
                        queue.remove();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            private boolean isTooOld(CallDamElement callDamElement) {
                if (callDamElement == null) {
                    return false;
                } else {
                    DateTime timeAdded = callDamElement.getDateTimeAdded();
                    DateTime now = new DateTime();
                    if (timeAdded == null || timeAdded.isAfter(now)) {
                        return false;
                    }
                    Duration difference = new Interval(timeAdded, now).toDuration();
                    return (difference.isLongerThan(period));
                }

            }

        };

        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(purge, 0, purgePeriod, purgeUnit);
        logger.info("Scheduling CallDam purge every {} {}", purgePeriod, purgeUnit.toString());

        if (period.getStandardDays() < 1) {
            if (period.getStandardHours() < 1) {
                if (period.getStandardMinutes() < 1) {
                    logger.info("Call Dam limit set to {} calls per {} seconds", upperLimit, period.getStandardSeconds());
                } else {
                    logger.info("Call Dam limit set to {} calls per {} minutes", upperLimit, period.getStandardMinutes());
                }
            } else {
                logger.info("Call Dam limit set to {} calls per {} hours", upperLimit, period.getStandardHours());
            }
        }
    }

    public SimpleLimiter(final int upperLimit, final Duration duration) {
        this(upperLimit, duration, 1, TimeUnit.MINUTES);
    }

    @Override
    public void clockIn() throws InterruptedException {
        CallDamElement element = new CallDamElement();
        queue.put(element);
        element.initialize();
    }

    private static class CallDamElement {
        private DateTime dateTimeAdded;

        public void initialize() {
            getDateTimeAdded();
        }

        public synchronized DateTime getDateTimeAdded() {
            // by adding this null check, we ensure that if any read attempt
            // made before the element has been initialised will result in a
            // non-null value.
            if (dateTimeAdded == null) {
                dateTimeAdded = new DateTime();
            }
            return dateTimeAdded;
        }
    }

}
