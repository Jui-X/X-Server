package core.schedule;

import core.Connector;
import core.ScheduleJob;

import java.util.concurrent.TimeUnit;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-08 19:47
 **/
public class IdleTimeoutScheduleJob extends ScheduleJob {
    public IdleTimeoutScheduleJob(long idleTimeout, TimeUnit timeUnit, Connector connector) {
        super(idleTimeout, timeUnit, connector);
    }

    @Override
    public void run() {
        long lastActiveTime = connector.getLastActiveTime();
        long idleTimeoutMilliseconds = this.idleTimeoutMilliseconds;

        long nextDelay = idleTimeoutMilliseconds - (System.currentTimeMillis() - lastActiveTime);

        if (nextDelay <= 0) {
            schedule(idleTimeoutMilliseconds);

            try {
                connector.fireIdleTimeoutEvent();
            } catch (Throwable throwable) {
                connector.fireExceptionCaught();
            }
        } else {
            schedule(nextDelay);
        }
    }
}
