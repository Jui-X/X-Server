package TCPAndUDP.Handler;

import TCPAndUDP.Utils.CloseUtils;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * @param: none
 * @description: 处理客户端请求
 * @author: KingJ
 * @create: 2019-05-27 20:14
 **/
public class ClientHandler {
    private final Socket socket;
    private final ClientReadHandler readHandler;
    private final ClientWriteHandler writeHandler;
    private final CloseNotify closeNotify;

    public ClientHandler(Socket socket, CloseNotify closeNotify) throws IOException {
        this.socket = socket;
        this.readHandler = new ClientReadHandler(socket.getInputStream());
        this.writeHandler = new ClientWriteHandler(socket.getOutputStream());
        this.closeNotify = closeNotify;
        System.out.println("新客户端连接：" + socket.getInetAddress() +
                " P:" + socket.getPort());
    }

    public interface CloseNotify {
        void selfClose(ClientHandler clientHandler);
    }

    /**
     *
     * 负责读取客户端发来的信息
     *
     **/

    class ClientReadHandler extends Thread {
        private boolean done = false;
        private final InputStream inputStream;

        ClientReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            super.run();

            // 客户端请求处理
            try {
                // 得到输入流，用于接收数据
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                do {
                    String str = socketInput.readLine();
                    if (str == null) {
                        System.out.println("TCP Client cannot read...");
                        ClientHandler.this.closeItself();
                        break;
                    }
                    System.out.println(str);
                } while (!done);
            } catch (IOException e) {
                if (!done) {
                    System.out.println("TCP连接异常断开");
                }
            } finally {
                CloseUtils.close(inputStream);
            }
        }

        void exit() {
            done = true;
            CloseUtils.close(inputStream);
        }
    }

    /**
     *
     * 负责写入回送到客户端的信息
     *
     **/
    class ClientWriteHandler extends Thread {
        private boolean done = false;
        private final PrintStream printStream;
        private final ExecutorService executorService;

        ClientWriteHandler(OutputStream outputStream) {
            this.printStream = new PrintStream(outputStream);
            // TODO ThreadExecutorPool创建线程池时自定义ThreadFactory
            this.executorService = new ThreadPoolExecutor(5, 5, 1000
                    , TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());
        }

        class WriteRunnable implements Runnable {
            private final String str;

            WriteRunnable(String str) {
                this.str = str;
            }

            @Override
            public void run() {
                if (ClientWriteHandler.this.done) {
                    return;
                }

                try {
                    ClientWriteHandler.this.printStream.println(str);
                } catch (Exception e) {
                    System.out.println("客户端打印时发生异常");
                }
            }
        }

        void send(String str) {
            executorService.execute(new WriteRunnable(str));
        }

        void exit() {
            done = true;
            CloseUtils.close(printStream);
            executorService.shutdownNow();
        }
    }

    public void sendMsg(String str) {
        writeHandler.send(str);
    }

    public void receiveMsg() {
        readHandler.start();
    }

    private void closeItself() {
        exit();
        closeNotify.selfClose(this);
    }

    public void exit() {
        readHandler.exit();
        writeHandler.exit();
        System.out.println("客户端已退出：" + socket.getInetAddress() +
                " P:" + socket.getPort());
        CloseUtils.close(socket);
    }
}
