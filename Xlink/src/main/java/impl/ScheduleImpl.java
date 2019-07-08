package impl;

import core.Scheduler;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-08 19:26
 **/
public class ScheduleImpl implements Scheduler {
    private final ScheduledExecutorService scheduledExecutorService;

    public ScheduleImpl(int poolSize) {
        this.scheduledExecutorService = Executors.newScheduledThreadPool(poolSize,
                new IOSelectorProvider.IOProviderThreadFactory("Schedule-Thread-"));
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit timeUnit) {
        return scheduledExecutorService.schedule(runnable, delay, timeUnit);
    }

    @Override
    public void close() throws IOException {
        scheduledExecutorService.shutdownNow();
    }
}
