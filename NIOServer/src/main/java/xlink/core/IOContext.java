package xlink.core;

import java.io.IOException;

/**
 * @param: none
 * @description: IO上下文中提供IOProvider这样一个调度者
 * @author: KingJ
 * @create: 2019-06-02 17:35
 **/
public class IOContext {
    private static IOContext INSTANCE;
    // 针对所有连接，都可以通过IOProvider进行注册与解除注册
    private final IOProvider IOProvider;

    public IOContext(IOProvider IOProvider) {
        this.IOProvider = IOProvider;
    }

    public IOProvider getProvider() {
        return this.IOProvider;
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
    }

    public static class StartBoot {
        private IOProvider ioProvider;

        private StartBoot() {}

        public StartBoot ioProvider(IOProvider ioProvider) {
            this.ioProvider = ioProvider;
            return this;
        }

        public IOContext start() {
            INSTANCE = new IOContext(ioProvider);
            return INSTANCE;
        }
    }
}
