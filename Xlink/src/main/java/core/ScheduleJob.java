package core;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-08 19:34
 **/
public abstract class ScheduleJob implements Runnable {
    protected final long idleTimeoutMilliseconds;
    protected final Connector connector;

    private volatile Scheduler scheduler;
    private volatile ScheduledFuture scheduledFuture;

    protected ScheduleJob(long idleTimeout, TimeUnit timeUnit, Connector connector) {
        this.idleTimeoutMilliseconds = timeUnit.toMillis(idleTimeout);
        this.connector = connector;
    }

    /**
     * 调度当前任务
     * 供外部调用
     * @param scheduler 调度器
     */
    synchronized void schedule(Scheduler scheduler) {
        this.scheduler = scheduler;
        schedule(idleTimeoutMilliseconds);
    }

    synchronized void unSchedule() {
        if (scheduler != null) {
            scheduler = null;
        }
        if (scheduledFuture != null) {
            // 允许中断操作
            scheduledFuture.cancel(true);
        }
    }

    /**
     * 调度当前任务的具体实现
     * 供持有Scheduler的当前类以及子类直接调用
     * @param timeoutMilliseconds 等待毫秒
     */
    protected void schedule(long timeoutMilliseconds) {
        if (scheduler != null) {
            scheduledFuture = scheduler.schedule(this, timeoutMilliseconds, TimeUnit.MILLISECONDS);
        }
    }
}
