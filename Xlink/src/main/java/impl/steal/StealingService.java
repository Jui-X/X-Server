package impl.steal;

import core.IOTask;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.IntFunction;

/**
 * @param: none
 * @description: 窃取调度服务
 * @author: KingJ
 * @create: 2019-07-10 20:46
 **/
public class StealingService {
    /**
     * 当任务队列数量低于安全值时，不可窃取
     */
    private final int minSafetyThreshold;
    /**
     * 线程集合
     */
    private final StealingSelectorThread[] threads;
    /**
     * 任务队列
     */
    private final Queue<IOTask>[] queues;
    // 结束标志
    private volatile boolean isTerminated = false;

    public StealingService(StealingSelectorThread[] threads, int minSafetyThreshold) {
        this.minSafetyThreshold = minSafetyThreshold;
        this.threads = threads;
        this.queues = Arrays.stream(threads)
                .map(StealingSelectorThread::getReadyTaskQueue)
                .toArray((IntFunction<Queue<IOTask>[]>) ArrayBlockingQueue[]::new);
    }

    IOTask steal(final Queue<IOTask> excludedQueue) {
        final int minSafetyThreshold = this.minSafetyThreshold;
        final Queue<IOTask>[] queues = this.queues;
        for (Queue<IOTask> queue : queues) {
            if (queue == excludedQueue) {
                continue;
            }

            // TODO 根据队列中任务多少去进行选择窃取

            int size = queue.size();
            if (size > minSafetyThreshold) {
                IOTask poll = queue.poll();
                if (poll != null) {
                    return poll;
                }
            }
        }
        return null;
    }

    /**
     *
     * @return
     */
    public StealingSelectorThread getNotBusyThread() {
       StealingSelectorThread targetThread = null;
       long maxSaturatingCapacity = Long.MAX_VALUE;
        for (StealingSelectorThread thread : threads) {
            long saturatingCapacity = thread.getSaturatingCapacity();
            if (saturatingCapacity != 1 && saturatingCapacity < maxSaturatingCapacity) {
                maxSaturatingCapacity = saturatingCapacity;
                targetThread = thread;
            }
        }
        // 返回饱和度最低的线程
        return targetThread;
    }

    /**
     * 结束操作
     */
    public void shutdown() {
        if (isTerminated) {
            return;
        }
        isTerminated = true;

        for (StealingSelectorThread thread : threads) {
            thread.exit();
        }
    }

    /**
     * 是否已结束
     */
    public boolean isTerminated() {
        return isTerminated;
    }

    /**
     * 执行一个任务
     * @param task
     */
    void execute(IOTask task) {

    }
}
