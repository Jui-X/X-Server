package impl;

import core.IOProvider;
import Utils.CloseUtils;
import jdk.nashorn.internal.ir.annotations.Ignore;

import java.io.IOException;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-06-02 18:06
 **/
public class IOSelectorProvider implements IOProvider {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    // 是否处于input/ouput注册过程当中
    private final AtomicBoolean inRegisterInput = new AtomicBoolean(false);
    private final AtomicBoolean inRegisterOutput = new AtomicBoolean(false);

    private final Selector readSelector;
    private final Selector writeSelector;

    private final HashMap<SelectionKey, Runnable> inputCallbackMap = new HashMap<>();
    private final HashMap<SelectionKey, Runnable> outputCallbackMap = new HashMap<>();

    private final ExecutorService inputHandlerPool;
    private final ExecutorService outputHandlerPool;

    public IOSelectorProvider() throws IOException {
        readSelector = Selector.open();
        writeSelector = Selector.open();

        inputHandlerPool = new ThreadPoolExecutor(5, 5,
                1000, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(),
                new IOProviderThreadFactory("IOProvider-Input-Thread-"));
        outputHandlerPool = new ThreadPoolExecutor(5, 5,
                1000, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(),
                new IOProviderThreadFactory("IOProvider-Output-Thread-"));

        // 开始输入输出的监听
        startRead();
        startWrite();
    }

    static class SelectorThread extends Thread {
        private final AtomicBoolean isClosed;
        private final AtomicBoolean locker;
        private final Selector selector;
        private final HashMap<SelectionKey, Runnable> map;
        private final ExecutorService pool;
        private final int keyOps;

        SelectorThread(String name, AtomicBoolean isClosed, AtomicBoolean locker, Selector selector, HashMap<SelectionKey, Runnable> map, ExecutorService pool, int keyOps) {
            super(name);
            this.isClosed = isClosed;
            this.locker = locker;
            this.selector = selector;
            this.map = map;
            this.pool = pool;
            this.keyOps = keyOps;
        }

        @Override
        public void run() {
            super.run();
            // 将所有类变量写作成员变量，减少循环过程中对所有成员变量扫描的一个消耗
            AtomicBoolean locker = this.locker;
            AtomicBoolean isClosed = this.isClosed;
            Selector selector = this.selector;
            HashMap<SelectionKey, Runnable> callMap = this.map;
            ExecutorService pool = this.pool;
            int keyOps = this.keyOps;

            while (!isClosed.get()) {
                try {
                    if (selector.select() == 0) {
                        // 判断是否处于input注册的过程
                        // 等待input register的过程结束后，释放锁locker即inRegisterInput
                        waitSelection(locker);
                        continue;
                    } else if (locker.get()) {
                        // 等待select()操作完并将可消费的时间消费掉
                        waitSelection(locker);
                    }

                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    // 使用迭代器防止在循环过程中对SelectionKey进行修改而引发的bug
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        if (key.isValid()) {
                            handleSelection(key, SelectionKey.OP_READ, map, pool, locker);
                        }
                        iterator.remove();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClosedSelectorException ignored) {
                    break;
                }
            }
        }
    }

    private void startRead() {
        Thread thread = new SelectorThread("Xlink IOSelectorProvider ReadSelector Thread",
                isClosed, inRegisterInput, readSelector, inputCallbackMap, inputHandlerPool,
                SelectionKey.OP_READ);
        thread.start();
    }

    private void startWrite() {
        Thread thread = new SelectorThread("Xlink IOSelectorProvider WriteSelector Thread",
                isClosed, inRegisterOutput, writeSelector, outputCallbackMap, outputHandlerPool,
                SelectionKey.OP_WRITE);
        thread.start();
    }

    private static void handleSelection(SelectionKey key, int opRead,
                                        HashMap<SelectionKey, Runnable> map,
                                        ExecutorService pool, AtomicBoolean locker) {
        synchronized (locker) {
            try {
                // 取消对keyOps的监听
                key.interestOps(key.interestOps() & ~opRead);
            } catch (CancelledKeyException e) {
                // 当发现key已经被关闭时，直接返回无需进行后续操作
                return;
            }
        }

        Runnable runnable = null;
        try {
            // 取出Selection Key对应的Runnable任务
            runnable = map.get(key);
        } catch (Exception ignored) {
        }
        if (runnable != null && !pool.isShutdown()) {
            // 异步调度
            pool.execute(runnable);
        }
    }

    private static void waitSelection(final AtomicBoolean locker) {
        synchronized (locker) {
            if (locker.get()) {
                try {
                    // 当registerSelectionKey()中locker设为false且调用notify()时
                    locker.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void register(HandleProvideCallback callback) throws Exception {
        SelectionKey key;
        if (callback.ops == SelectionKey.OP_READ) {
            key = registerSelectionKey(callback.channel, readSelector, SelectionKey.OP_READ, inRegisterInput,
                    inputCallbackMap, callback);
        } else {
            key = registerSelectionKey(callback.channel, writeSelector, SelectionKey.OP_WRITE, inRegisterOutput,
                    outputCallbackMap, callback);
        }
        if (key == null) {
            throw new IOException("Register Error: Channel: " + callback.channel + " ops: " + callback.ops);
        }
    }

    private static SelectionKey registerSelectionKey(SocketChannel channel, Selector selector, int registerOps, AtomicBoolean locker,
                          HashMap<SelectionKey, Runnable> map, Runnable runnable) {

        synchronized (locker) {
            // 设置锁定状态
            // 此时其他线程无法注册
            locker.set(true);

            try {
                // 唤醒当前Selector，让Selector不处于selector()状态
                selector.wakeup();

                SelectionKey key = null;

                if (channel.isRegistered()) {
                    // 检查当前Key是否注册过
                    key = channel.keyFor(selector);
                    if (key != null) {
                        // 将新的状态注册进去
                        key.interestOps(key.readyOps() | registerOps);
                    }
                }

                if (key == null) {
                    // 注册selector，得到key
                    key = channel.register(selector, registerOps);
                    // 注册回调
                    // 将回调函数（inputCallback）注册到map中
                    // 与key所对应
                    map.put(key, runnable);
                }

                return key;
            } catch (ClosedChannelException | CancelledKeyException | ClosedSelectorException e) {
                return null;
            } finally {
                // 解除锁定
                locker.set(false);
                try {
                    locker.notify();
                } catch (Exception ignored) {}
            }
        }

    }

    @Override
    public void unRegister(SocketChannel channel) {
        unregisterSelection(channel, readSelector, inputCallbackMap, inRegisterInput);
        unregisterSelection(channel, writeSelector, outputCallbackMap, inRegisterOutput);
    }

    private static void unregisterSelection(SocketChannel channel, Selector selector,
                                            Map<SelectionKey, Runnable> map, AtomicBoolean locker) {
        synchronized (locker) {
            locker.set(true);
            // 解除阻塞状态，继续下次select操作
            selector.wakeup();
            try {
                if (channel.isRegistered()) {
                    SelectionKey key = channel.keyFor(selector);
                    if (key != null) {
                        // 取消监听
                        // 读和写两种操作分离到两个Selector上
                        // cancel()方法会取消Selector上的所有事件
                        key.cancel();
                        // 移除selection key
                        map.remove(key);
                    }
                }
            } finally {
                locker.set(false);
                try {
                    locker.notifyAll();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            inputHandlerPool.shutdownNow();
            outputHandlerPool.shutdownNow();

            inputCallbackMap.clear();
            outputCallbackMap.clear();

            CloseUtils.close(readSelector);
            CloseUtils.close(writeSelector);
        }
    }

    static class IOProviderThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        IOProviderThreadFactory(String namePrefix) {
            SecurityManager s = System.getSecurityManager();
            this.group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}
