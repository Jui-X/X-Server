package TCPDemo;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-05-20 22:57
 **/
public class TCPServer {
    private static final int PORT = 50000;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket();
        initServerSocket(serverSocket);

        // 绑定到本地50000端口
        serverSocket.bind(new InetSocketAddress(Inet4Address.getLocalHost(), PORT), 50);

        System.out.println("服务器端信息：" + serverSocket.getInetAddress() + " Port:" + serverSocket.getLocalPort());

        for (; ;) {
            Socket client = serverSocket.accept();
            ClientHandler clientHandler = new ClientHandler(client);
            clientHandler.start();
        }
    }

    private static ServerSocket createServerSocket() throws IOException {
        // 创建serverSocket
        ServerSocket serverSocket = new ServerSocket();

        /*// 绑定本地50000端口
        serverSocket = new ServerSocket(PORT);

        // 绑定本地50000端口，并设置当前可允许等待连接的队列为50
        serverSocket = new ServerSocket(PORT, 50);

        serverSocket = new ServerSocket(PORT, 50, Inet4Address.getLocalHost());*/

        return serverSocket;
    }

    private static void initServerSocket(ServerSocket serverSocket) throws SocketException {
        // 是否复用未完全关闭的地址端口
        serverSocket.setReuseAddress(true);

        // 设置接收区缓冲大小
        serverSocket.setReceiveBufferSize(64 * 1024 * 1024);

        // 设置ServerSocket => accept过期时间
//        serverSocket.setSoTimeout(2000);

        // 设置性能参数：短链接，延迟，带宽的相对重要性
        serverSocket.setPerformancePreferences(1, 1 , 1);
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private boolean flag = true;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            super.run();

            System.out.println("新客户端信息：" + socket.getInetAddress() + " Port:" + socket.getPort());

            try {
                // 得到打印流，用于数据输出；服务器回送数据使用
                OutputStream outputStream = socket.getOutputStream();
                InputStream inputStream = socket.getInputStream();

                byte[] buffer = new byte[256];
                int read = inputStream.read(buffer);
                ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, read);

                byte by = byteBuffer.get();

                char c = byteBuffer.getChar();

                int i = byteBuffer.getInt();

                boolean b = byteBuffer.get() == 1;

                long l = byteBuffer.getLong();

                float f = byteBuffer.getFloat();

                double d = byteBuffer.getDouble();

                int pos = byteBuffer.position();
                String str = new String(buffer, pos, read - pos - 1);

                System.out.println("收到数据长度为：" + read + "\n" + ",数据：\n"
                        + by + "\n"
                        + c + "\n"
                        + i + "\n"
                        + b + "\n"
                        + l + "\n"
                        + f + "\n"
                        + d + "\n"
                        + str + "\n"
                );

                outputStream.write(buffer, 0, read);

                outputStream.close();
                inputStream.close();
            } catch (Exception e) {
                System.out.println("连接异常断开");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("客户端已退出：" + socket.getInetAddress() +
                    " P:" + socket.getPort());
        }
    }
}
