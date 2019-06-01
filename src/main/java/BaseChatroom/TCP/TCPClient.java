package BaseChatroom.TCP;

import BaseChatroom.UDP.Client.ServerInfo;
import BaseChatroom.Utils.CloseUtils;

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

    public static void linkWith(ServerInfo info) throws IOException {
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

            try {
                // 发送接收数据
                write(client);
            } catch (Exception e) {
                System.out.println("Socket通信异常关闭");
            }

            readHandler.exit();
        } catch (Exception e) {
            System.out.println("异常关闭");
        }

        client.close();
        System.out.println("TCP Client exit...");
    }

    private static void write(Socket client) throws IOException {
        // 构建键盘输入流
        InputStream in = System.in;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));

        // 得到Socket输出流，并转换成打印流
        OutputStream outputStream = client.getOutputStream();
        PrintStream socketPrintSteam = new PrintStream(outputStream);

        do {
            // 从键盘读取一行
            String str = bufferedReader.readLine();
            // 发送到服务器
            socketPrintSteam.println(str);

            String end = "byebye";
            if (end.equalsIgnoreCase(str)) {
                break;
            }
        } while (true);

        // 关闭资源
        socketPrintSteam.close();
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
