package BaseChatroom.TCP;

import BaseChatroom.Handler.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-05-27 16:39
 **/
public class TCPServer implements ClientHandler.ClientHandlerCallBack{
    private final int port;
    private ClientListener myClientListener;
    private List<ClientHandler> clientHandlerList = new ArrayList<ClientHandler>();
    private final ExecutorService forwardThreadPoolExecutor;

    public TCPServer(int port) {
        this.port = port;
        this.forwardThreadPoolExecutor = new ThreadPoolExecutor(5, 5,
                2000, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());
    }

    public boolean start() {
        try {
            ClientListener clientListener = new ClientListener(port);
            myClientListener = clientListener;
            // 服务端开始监听客户端过来的消息
            clientListener.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void stop() {
        if (myClientListener != null) {
            myClientListener.exit();
        }

        for (ClientHandler clientHandler : clientHandlerList) {
            clientHandler.exit();
        }

        clientHandlerList.clear();
    }

    public void broadcast(String str) {
        for (ClientHandler clientHandler : clientHandlerList) {
            clientHandler.sendMsg(str);
        }
    }

    @Override
    public void selfClose(ClientHandler handler) {
        clientHandlerList.remove(handler);
    }

    @Override
    public void onNewMessageArrived(ClientHandler clientHandler, String msg) {
        System.out.println("Received from " + clientHandler.getClientInfo());

        // 线程池异步提交转发任务
        forwardThreadPoolExecutor.execute(() -> {
            clientHandlerList.stream().filter(h -> !h.equals(clientHandler))
                    .forEach(h -> h.sendMsg(msg));
        });
    }

    private class ClientListener extends Thread {
        private ServerSocket server;
        private boolean done = true;

        private ClientListener(int port) throws IOException {
            server = new ServerSocket(port);
            System.out.println("服务器端信息：" + server.getInetAddress() + " Port:" + server.getLocalPort());
        }

        @Override
        public void run() {
            super.run();

            System.out.println("TCP Server is ready.");
            // 等待客户端连接
            do {
                Socket client;
                try {
                    // 监听消息，发生阻塞
                    client = server.accept();
                } catch (IOException e) {
                    continue;
                }
                // 服务端异步构建线程处理请求
                try {
                    // 构建新的ClientHandler线程处理客户端请求
                    ClientHandler clientHandler = new ClientHandler(client, TCPServer.this);
                    clientHandler.receiveMsg();
                    clientHandlerList.add(clientHandler);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("TCP Client Error. " + e.getMessage());
                }
            } while (done);
            System.out.println("TCP Server exit.");
        }

        void exit() {
            done = true;
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}