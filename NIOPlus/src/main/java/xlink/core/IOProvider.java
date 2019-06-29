package xlink.core;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;

public interface IOProvider extends Closeable {
    /**
     *
     * 用于观察Channel的状态，当Channel可读时，通过HandleInputCallback进行回调
     *
     **/
    boolean registerInput(SocketChannel channel, HandleInputCallback callback) throws IOException;

    /**
     *
     * 用于观察Channel的状态，当Channel可读时，通过HandleInputCallback进行回调
     *
     **/
    boolean registerOutput(SocketChannel channel, HandleOutputCallback callback);

    void unregisterInput(SocketChannel channel);

    void unregisterOutput(SocketChannel channel);

    /**
     *
     * 回调发生时调用provideInput()，表明当前Channel能够提供输入
     *
     **/
    abstract class HandleInputCallback implements Runnable {
        @Override
        public final void run() {
            provideInput();
        }

        protected abstract void provideInput();
    }

    /**
     *
     * 回调发生时嗲用provideOutput()，表明当前Channel能够提供输出
     *
     **/
    abstract class HandleOutputCallback implements Runnable {
        private Object attach;

        @Override
        public final void run() {
            provideOutput(attach);
        }

        public final <T> T getAttach() {
            T attach = (T) this.attach;
            return attach;
        }

        public final void setAttach(Object attach) {
            this.attach = attach;
        }

        protected abstract void provideOutput(Object attach);
    }
}
