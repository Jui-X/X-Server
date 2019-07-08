package core;

import java.io.IOException;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-06-02 17:35
 **/
public class IOContext {
    private static IOContext INSTANCE;
    // 针对所有连接，都可以通过IOProvider进行注册与解除注册
    private final IOProvider IOProvider;
    private final Scheduler scheduler;

    public IOContext(IOProvider IOProvider, Scheduler scheduler) {
        this.IOProvider = IOProvider;
        this.scheduler = scheduler;
    }

    public IOProvider getProvider() {
        return this.IOProvider;
    }

    public Scheduler getScheduler() {
        return this.scheduler;
    }

    public static IOContext getInstance() {
        return INSTANCE;
    }

    public static StartBoot setup() {
        return new StartBoot();
    }

    public static void close() throws IOException {
        if (INSTANCE != null) {
            INSTANCE.closeItself();
        }
    }

    private void closeItself() throws IOException {
        IOProvider.close();
        scheduler.close();
    }

    public static class StartBoot {
        private IOProvider ioProvider;
        private Scheduler scheduler;

        private StartBoot() {}

        public StartBoot ioProvider(IOProvider ioProvider) {
            this.ioProvider = ioProvider;
            return this;
        }

        public StartBoot scheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public IOContext start() {
            INSTANCE = new IOContext(ioProvider, scheduler);
            return INSTANCE;
        }
    }
}
