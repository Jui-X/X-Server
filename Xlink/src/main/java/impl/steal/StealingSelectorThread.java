package impl.steal;

import Utils.CloseUtils;
import core.IOProvider;
import core.IOTask;

import java.io.IOException;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-10 17:14
 **/
public abstract class StealingSelectorThread extends Thread {
    private static final int MAX_ONCE_READ_TASK = 128;
    private static final int MAX_ONCE_WRITE_TASK = 128;
    private static final int MAX_ONCE_RUN_TASK = MAX_ONCE_READ_TASK + MAX_ONCE_WRITE_TASK;
    // 允许的操作
    private static final int VALID_OPS = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
    private final Selector selector;
    // 是否还处于运行中
    private volatile boolean isRunning = true;
    // 已就绪任务队列
    private final BlockingQueue<IOTask> readyTaskQueue = new ArrayBlockingQueue<>(MAX_ONCE_RUN_TASK);
    // 待注册的任务队列
    private final ConcurrentLinkedQueue<IOTask> registerTaskQueue = new ConcurrentLinkedQueue<>();
    // 任务饱和度度量
    private final AtomicLong saturatingCapacity = new AtomicLong();
    // 用于多线程协同的任务窃取Service
    private volatile StealingService stealingService;

    private final AtomicBoolean unRegisterLocker = new AtomicBoolean(false);

    public StealingSelectorThread(Selector selector) {
        this.selector = selector;
    }

    @Override
    public void run() {
        super.run();

        final Selector selector = this.selector;
        final BlockingQueue<IOTask> readyTaskQueue = this.readyTaskQueue;
        final ConcurrentLinkedQueue<IOTask> registerTaskQueue = this.registerTaskQueue;
        final AtomicBoolean unregisterLocker = this.unRegisterLocker;
        // 单次就绪的任务缓存，随后一次行加入到就绪队列中
        final List<IOTask> onceReadyReadTaskCache = new ArrayList<>(MAX_ONCE_READ_TASK);
        final List<IOTask> onceReadyWriteTaskCache = new ArrayList<>(MAX_ONCE_WRITE_TASK);

        try {
            while (isRunning) {
                // 加入待注册的通道
                consumeRegisterTodoTasks(registerTaskQueue);

                int count = selector.select();

                // 立即检查
                if (selector.selectNow() == 0) {
                    // 当前没有就绪任务，则将CPU使用权让出
                    Thread.yield();
                    continue;
                }

                while (unRegisterLocker.get()) {
                    Thread.yield();
                }

                if (count == 0) {
                    continue;
                }

                // 处理已就绪的通道
                Set<SelectionKey> selectionKey = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKey.iterator();

                int onceReadyTaskCount = MAX_ONCE_READ_TASK;
                int onceWriteTaskCount = MAX_ONCE_WRITE_TASK;

                // 迭代已就绪的任务
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    Object attachmentObj = key.attachment();
                    //
                    if (key.isValid() && attachmentObj instanceof KeyAttachment) {
                        final KeyAttachment attachment = (KeyAttachment) attachmentObj;
                        try {
                            final int readyOps = key.readyOps();
                            int interestOps = key.interestOps();

                            // 是否可读
                            if ((readyOps & SelectionKey.OP_READ) != 0 && onceReadyTaskCount-- > 0) {
                                onceReadyReadTaskCache.add(attachment.taskForReadable);
                                interestOps = interestOps & ~SelectionKey.OP_READ;
                            }
                            // 是否可写
                            if ((readyOps & SelectionKey.OP_WRITE) != 0 && onceWriteTaskCount-- > 0) {
                                onceReadyWriteTaskCache.add(attachment.taskForWritable);
                                interestOps = interestOps & ~SelectionKey.OP_WRITE;
                            }

                            // 取消已就绪的关注
                            key.interestOps(interestOps);
                        } catch (CancelledKeyException igonred) {
                            // 当前连接被取消，断开时直接移除相关任务
                            if (attachment.taskForReadable != null) {
                                onceReadyReadTaskCache.remove(attachment.taskForReadable);
                            }
                            if (attachment.taskForWritable != null) {
                                onceReadyWriteTaskCache.remove(attachment.taskForWritable);
                            }
                        }
                    }
                    iterator.remove();
                }

                // 判断缓存中是否有待执行的任务
                if (!onceReadyWriteTaskCache.isEmpty()) {
                    // 将缓存中的任务加入到就绪队列中
                    joinTaskQueue(readyTaskQueue, onceReadyWriteTaskCache);
                    onceReadyWriteTaskCache.clear();
                }

                // 判断缓存中是否有待执行的任务
                if (!onceReadyReadTaskCache.isEmpty()) {
                    // 将缓存中的任务加入到就绪队列中
                    joinTaskQueue(readyTaskQueue, onceReadyReadTaskCache);
                    onceReadyReadTaskCache.clear();
                }

                // 消费就绪队列任务
                consumeTodoTasks(readyTaskQueue, registerTaskQueue);
            }
        } catch (ClosedSelectorException ignored) {
        } catch (IOException e) {
            CloseUtils.close(selector);
        } finally {
            readyTaskQueue.clear();
            registerTaskQueue.clear();
        }
    }

    /**
     * 将通道注册到当前的Selector中
     *
     * @param
     * @return 是否注册成功
     */
    public void register(IOTask task) {
        if ((task.ops & ~VALID_OPS) == 0) {
            throw new UnsupportedOperationException("");
        }
        registerTaskQueue.offer(task);
        selector.wakeup();
    }

    /**
     * 取消注册，原理类似于注册操作在队列中添加一份取消注册的任务，并将副本变量清空
     *
     * @param channel
     */
    public void unregister(SocketChannel channel) {
        // 通过Selector去扫描是否有已经注册过的SelectionKey
        SelectionKey selectionKey = channel.keyFor(selector);
        if (selectionKey != null && selectionKey.attachment() != null) {
            // 关闭前可使用Attach简单判断是否已处于队列中
            selectionKey.attach(null);

            if (Thread.currentThread() == this) {
                selectionKey.cancel();
            } else {
                synchronized (unRegisterLocker) {
                    unRegisterLocker.set(true);
                    selector.wakeup();
                    selectionKey.cancel();
                    unRegisterLocker.set(false);
                }
            }
        }
    }

    private void consumeRegisterTodoTasks(final ConcurrentLinkedQueue<IOTask> registerTaskQueue) {
        final Selector selector = this.selector;

        IOTask registerTask = registerTaskQueue.poll();
        while (registerTask != null) {
            try {
                final SocketChannel channel = registerTask.channel;
                int ops = registerTask.ops;
                SelectionKey key = channel.keyFor(selector);
                if (key == null) {
                    key = channel.register(selector, ops, new KeyAttachment());
                } else {
                    key.interestOps(key.interestOps() | ops);
                }

                Object attachment = key.attachment();
                if (attachment instanceof KeyAttachment) {
                    ((KeyAttachment) attachment).attach(ops, registerTask);
                } else {
                    // 外部关闭，直接取消
                    key.cancel();
                }
            } catch (Exception e) {
                registerTask.fireThrowable(e);
            }
        }
    }

    /**
     * 将单次就绪的任务缓存加入到总队列中去
     *
     * @param readyTaskQueue     总任务队列
     * @param onceReadyTaskCache 单次待执行的任务
     */
    private void joinTaskQueue(final Queue<IOTask> readyTaskQueue, final List<IOTask> onceReadyTaskCache) {
        // TODO 多任务之间可按优先级等进行排序
        readyTaskQueue.addAll(onceReadyTaskCache);
    }

    /**
     * 消费待完成的任务
     */
    private void consumeTodoTasks(final Queue<IOTask> readyTaskQueue, ConcurrentLinkedQueue<IOTask> registerTaskQueue) {
        // 循环执行所有任务
        IOTask doTask = readyTaskQueue.poll();
        while (doTask != null) {
            // 任务完成即增加饱和度
            saturatingCapacity.incrementAndGet();
            // 执行任务
            // 如果执行完之后仍需关注当前任务类型，则直接注册至注册队列当中去
            if (processTask(doTask)) {
                // 执行完成后添加待注册的列表
                registerTaskQueue.offer(doTask);
            }
            // 下一个任务
            doTask = readyTaskQueue.poll();

            //
            final StealingService stealingService = this.stealingService;
            if (stealingService != null) {
                doTask = stealingService.steal(readyTaskQueue);
                while (doTask != null) {
                    saturatingCapacity.incrementAndGet();
                    if (processTask(doTask)) {
                        registerTaskQueue.offer(doTask);
                    }
                    doTask = stealingService.steal(readyTaskQueue);
                }
            }
        }
    }

    /**
     * 线程退出操作
     */
    public void exit() {
        isRunning = false;
        CloseUtils.close(selector);
        interrupt();
    }

    /**
     * 调用子类的执行任务操作实现
     *
     * @param task 任务
     * @return 执行任务后是否需要再次添加该任务
     */
    protected abstract boolean processTask(IOTask task);

    /**
     * 用于注册时添加的附件
     */
    static class KeyAttachment {
        // 可读时执行的任务
        IOTask taskForReadable;
        // 可写时执行的任务
        IOTask taskForWritable;

        /**
         * 附加任务
         *
         * @param ops  任务关注的事件类型
         * @param task 任务
         */
        void attach(int ops, IOTask task) {
            if (ops == SelectionKey.OP_READ) {
                taskForReadable = task;
            } else {
                taskForWritable = task;
            }
        }
    }

    /**
     * 获取内部的任务队列
     *
     * @return 任务队列
     */
    Queue<IOTask> getReadyTaskQueue() {
        return readyTaskQueue;
    }

    /**
     * 绑定StealingService
     *
     * @param stealingService
     */
    public void setStealingService(StealingService stealingService) {
        this.stealingService = stealingService;
    }

    /**
     * 获取饱和程度
     * 暂时的饱和度量是由任务执行的次数来决定的
     *
     * @return -1 已失效
     */
    long getSaturatingCapacity() {
        if (selector.isOpen()) {
            return saturatingCapacity.get();
        } else {
            return -1;
        }
    }
}
