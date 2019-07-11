package impl;

import core.Scheduler;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-08 19:26
 **/
public class ScheduleImpl implements Scheduler {
    private final ScheduledExecutorService scheduledExecutorService;
    private final ExecutorService deliveryPool;

    public ScheduleImpl(int poolSize) {
        this.scheduledExecutorService = Executors.newScheduledThreadPool(poolSize,
                new IOSelectorProvider.IOProviderThreadFactory("Schedule-Thread-"));
        this.deliveryPool = Executors.newFixedThreadPool(4,
                new IOSelectorProvider.IOProviderThreadFactory("Delivery-Thread-"));
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit timeUnit) {
        return scheduledExecutorService.schedule(runnable, delay, timeUnit);
    }

    @Override
    public void delivery(Runnable runnable) {
        deliveryPool.execute(runnable);
    }

    @Override
    public void close() throws IOException {
        scheduledExecutorService.shutdownNow();
        deliveryPool.shutdownNow();
    }
}
