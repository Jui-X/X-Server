package core;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;

public interface IOProvider extends Closeable {
    /**
     *
     * 用于观察Channel的状态，当Channel可读时，通过HandleInputCallback进行回调
     *
     **/
    // boolean registerInput(SocketChannel channel, HandleProvideCallback callback) throws IOException;

    /**
     *
     * 用于观察Channel的状态，当Channel可读时，通过HandleInputCallback进行回调
     *
     **/
    // boolean registerOutput(SocketChannel channel, HandleProvideCallback callback);

    // void unregisterInput(SocketChannel channel);

    // void unregisterOutput(SocketChannel channel);

    void register(HandleProvideCallback callback) throws Exception;

    void unRegister(SocketChannel channel);

    /**
     *
     * 回调发生时嗲用provideParameter()，表明当前Channel能够提供输出/输出
     *
     **/
    abstract class HandleProvideCallback extends IOTask implements Runnable {
        private final IOProvider provider;
        /**
         * 附加本次未完全消费完成的IoArgs，然后进行自循环
         */
        protected volatile IOParameter attach;

        public HandleProvideCallback(IOProvider provider, SocketChannel channel, int ops) {
            super(channel, ops);
            this.provider = provider;
        }

        @Override
        public final void run() {
            final IOParameter attach = this.attach;
            this.attach = null;

            if (provideParameter(attach)) {
                try {
                    provider.register(this);
                } catch (Exception e) {
                    fireThrowable(e);
                }
            }
        }

        @Override
        public final boolean processIO() {
            final IOParameter attach = this.attach;
            this.attach = null;

            return provideParameter(attach);
        }

        /**
         * 可以进行接收或者发送时的回调
         *
         * @param parameter 携带之前的附加值
         */
        protected abstract boolean provideParameter(IOParameter parameter);

        /**
         * 检查当前的附加值是否为null，如果处于自循环时当前附加值attach不为null，
         * 此时如果外层有调度注册异步发送或者接收是错误的
         */
        public void checkAttachNull() {
            if (attach != null) {
                throw new IllegalStateException("Current attach is not empty!");
            }
        }
    }
}
