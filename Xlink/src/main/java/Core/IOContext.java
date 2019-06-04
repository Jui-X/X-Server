package Core;

import java.io.IOException;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-06-02 17:35
 **/
public class IOContext {
    private static IOContext INSTANCE;
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
