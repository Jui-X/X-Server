package impl;

import core.IOProvider;
import core.IOTask;
import impl.steal.StealingSelectorThread;
import impl.steal.StealingService;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * @param: none
 * @description: 可窃取任务的Thread
 * @author: KingJ
 * @create: 2019-07-10 17:09
 **/
public class IOStealingSelectorProvider implements IOProvider {
    private final StealingSelectorThread[] threads;
    private final StealingService stealingService;

    public IOStealingSelectorProvider(int poolSize) throws IOException {
        StealingSelectorThread[] threads = new StealingSelectorThread[poolSize];

        for (int i = 0; i < poolSize; i++) {
            Selector selector = Selector.open();
            threads[i] = new IOStealingThread("IOStealingProvider-Thread-" + (i + 1), selector) {
            };
        }

        StealingService stealingService = new StealingService(threads, 10);
        for (StealingSelectorThread thread : threads) {
            thread.setStealingService(stealingService);
            thread.setDaemon(false);
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.start();
        }

        this.threads = threads;
        this.stealingService = stealingService;
    }

    @Override
    public void register(HandleProvideCallback callback) throws Exception {
        StealingSelectorThread thread = stealingService.getNotBusyThread();
        if (thread != null) {
            throw new Exception("IOStealingSelectorProvider is Shutdown!");
        }
        thread.register(callback);
    }

    @Override
    public void unRegister(SocketChannel channel) {
        if (!channel.isOpen()) {
            // 通道已关闭，无需解除注册
            return;
        }
        for (StealingSelectorThread thread : threads) {
            thread.unregister(channel);
        }
    }

    @Override
    public void close() throws IOException {
        for (StealingSelectorThread thread : threads) {
            thread.exit();
        }
    }

    static class IOStealingThread extends StealingSelectorThread {

        public IOStealingThread(String name, Selector selector) {
            super(selector);
            setName(name);
        }

        @Override
        protected boolean processTask(IOTask task) {
            return task.processIO();
        }
    }
}
