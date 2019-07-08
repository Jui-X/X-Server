package core;

import java.io.Closeable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public interface Scheduler extends Closeable {
    /**
     * 调度一份延迟任务
     *
     * @param runnable 任务Runnable
     * @param delay    延迟时间
     * @param timeUnit 延迟时间单位
     * @return 返回调度任务的控制Future
     */
    ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit timeUnit);
}
