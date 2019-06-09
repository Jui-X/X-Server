package xlink.impl;



import common.utils.CloseUtils;
import xlink.core.IOProvider;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
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

    private void startRead() {
        Thread thread = new Thread("Xlink IOSelectorProvider ReadSelector Thread") {
            @Override
            public void run() {
                while (!isClosed.get()) {
                    try {
                        if (readSelector.select() == 0) {
                            // 判断是否处于注册的过程
                            waitSelection(inRegisterInput);
                            continue;
                        }

                        Set<SelectionKey> selectionKeys = readSelector.selectedKeys();
                        for (SelectionKey selectionKey : selectionKeys) {
                            if (selectionKey.isValid()) {
                                handleSelection(selectionKey, SelectionKey.OP_READ, inputCallbackMap,
                                        inputHandlerPool);
                            }
                        }
                        selectionKeys.clear();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    private void startWrite() {
        Thread thread = new Thread("Xlink IOSelectorProvider WriteSelector Thread") {
            @Override
            public void run() {
                while (!isClosed.get()) {
                    try {
                        if (writeSelector.select() == 0) {
                            waitSelection(inRegisterOutput);
                            continue;
                        }

                        Set<SelectionKey> selectionKeys = writeSelector.selectedKeys();
                        for (SelectionKey selectionKey : selectionKeys) {
                            if (selectionKey.isValid()) {
                                handleSelection(selectionKey, SelectionKey.OP_WRITE, outputCallbackMap,
                                        outputHandlerPool);
                            }
                        }
                        selectionKeys.clear();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    private static void handleSelection(SelectionKey key, int opRead,
                                        HashMap<SelectionKey, Runnable> map,
                                        ExecutorService pool) {
        // 取消对keyOps的监听
        key.interestOps(key.readyOps() & ~opRead);

        Runnable runnable = null;
        try {
            runnable = map.get(key);
        } catch (Exception ignored) {}

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
    public boolean registerInput(SocketChannel channel, HandleInputCallback callback) throws IOException {

        return registerSelectionKey(channel, readSelector, SelectionKey.OP_READ, inRegisterInput,
                inputCallbackMap, inputHandlerPool, callback) != null;
    }

    @Override
    public boolean registerOutput(SocketChannel channel, HandleOutputCallback callback) {

        return registerSelectionKey(channel, writeSelector, SelectionKey.OP_WRITE, inRegisterOutput,
                outputCallbackMap, outputHandlerPool, callback) != null;
    }

    private static SelectionKey registerSelectionKey(SocketChannel channel, Selector selector, int registerOps, AtomicBoolean locker,
                          HashMap<SelectionKey, Runnable> map, ExecutorService pool, Runnable runnable) {

        synchronized (locker) {
            // 设置锁定状态
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
                    map.put(key, runnable);
                }

                return key;
            } catch (ClosedChannelException e) {
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
    public void unregisterInput(SocketChannel channel) {
        unregisterSelection(channel, readSelector, inputCallbackMap);
    }

    @Override
    public void unregisterOutput(SocketChannel channel) {
        unregisterSelection(channel, writeSelector, outputCallbackMap);
    }

    private static void unregisterSelection(SocketChannel channel, Selector selector,
                                            Map<SelectionKey, Runnable> map) {
        if (channel.isRegistered()) {
            SelectionKey key = channel.keyFor(selector);
            if (key != null) {
                key.cancel();
                map.remove(key);
                selector.wakeup();
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

            readSelector.wakeup();
            writeSelector.wakeup();

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
