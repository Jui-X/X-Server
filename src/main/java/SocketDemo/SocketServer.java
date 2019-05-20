package SocketDemo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-05-06 23:04
 **/
public class SocketServer {

    public static void main(String[] args) throws IOException {
        // 服务器端会在所有可用的IP地址的端口上进行监听
        ServerSocket server = new ServerSocket(8082);

        System.out.println("服务器端信息：" + server.getInetAddress() + " Port:" + server.getLocalPort());

        // 等待客户端连接
        while (true) {
            // 得到客户端
            Socket client = server.accept();
            // 客户端构建异步线程
            ClientHandler clientHandler = new ClientHandler(client);
            // 启动线程
            clientHandler.start();
        }
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
                PrintStream socketOutput = new PrintStream(socket.getOutputStream());
                // 得到输入流，用于接收客户端数据
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                do {
                    // 从客户端拿到一条数据
                    String str = socketInput.readLine();
                    String bye_msg = "bye";
                    if (bye_msg.equalsIgnoreCase(str)) {
                        flag = false;
                        socketOutput.println("bye");
                    }
                    else {
                        // 打印到屏幕并回送数据长度
                        System.out.println(str);
                        socketOutput.println("回送：" + str.length());
                    }
                } while (flag);

                socketInput.close();
                socketOutput.close();
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
