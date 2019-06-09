package client;


import common.utils.CloseUtils;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-05-27 16:39
 **/
public class TCPClient {
    private final Socket socket;
    private final ClientReadHandler clientReadHandler;
    private final PrintStream printStream;

    public TCPClient(Socket socket, ClientReadHandler clientReadHandler) throws IOException {
        this.socket = socket;
        this.clientReadHandler = clientReadHandler;
        this.printStream = new PrintStream(socket.getOutputStream());
    }

    public static TCPClient startWith(ServerInfo info) throws IOException {
        Socket client = new Socket();
        // 设置read的阻塞时间为3s
        client.setSoTimeout(3000);
        // 连接到TCPServer指定的端口
        client.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()), 3000);

        System.out.println("客户端信息：" + client.getLocalAddress() + " Port:" + client.getLocalPort());
        System.out.println("服务器端信息：" + client.getInetAddress() + " Port:" + client.getPort());

        try {
            ClientReadHandler readHandler = new ClientReadHandler(client.getInputStream());
            readHandler.start();

            return new TCPClient(client, readHandler);
        } catch (Exception e) {
            System.out.println("连接异常关闭");
            CloseUtils.close(client);
        }

        client.close();
        System.out.println("TCP Client exit...");

        return null;
    }

    public void sendMsg(String msg) {
        printStream.println(msg);
    }

    public void exit() {
        clientReadHandler.exit();
        CloseUtils.close(printStream);
        CloseUtils.close(socket);
    }

    /**
     *
     * 负责读取信息
     *
     **/

    static class ClientReadHandler extends Thread {
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
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(inputStream));

                do {
                    String str;
                    try {
                        str = socketInput.readLine();
                    } catch (SocketTimeoutException e) {
                        continue;
                    }

                    if (str == null) {
                        System.out.println("连接超时，连接已关闭，无法读取数据");
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
}
