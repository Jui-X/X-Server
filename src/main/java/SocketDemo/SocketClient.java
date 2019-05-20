package SocketDemo;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-05-06 23:04
 **/
public class SocketClient {

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket();

        socket.setSoTimeout(3000);

        socket.connect(new InetSocketAddress(Inet4Address.getLocalHost(), 8082), 3000);

        System.out.println("客户端信息：" + socket.getLocalAddress() + " Port:" + socket.getLocalPort());
        System.out.println("服务器端信息：" + socket.getInetAddress() + " Port:" + socket.getPort());

        try {
            toDo(socket);
        } catch (Exception e) {
            System.out.println("Socket异常关闭!");
        }

        // 资源释放
        socket.close();
    }

    private static void toDo(Socket client) throws IOException {
        // 键盘输入流
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        // 得到Socket输出流，并转换为打印流
        OutputStream output = client.getOutputStream();
        PrintStream socketPrintStream = new PrintStream(output);

        // 接收到服务器端的输入流, 并转换为BufferedReader
        InputStream inputStream = client.getInputStream();
        BufferedReader socketBufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        boolean flag = true;
        do {
            // 键盘读取一行并发送到服务器
            String str = input.readLine();
            socketPrintStream.println(str);

            // 从服务器读取一行
            String echo = socketBufferedReader.readLine();
            String bye_msg = "bye";
            if (bye_msg.equalsIgnoreCase(echo)) {
                flag = false;
            }
            else {
                System.out.println(echo);
            }
        } while (flag);

        // 资源释放
        socketPrintStream.close();
        socketBufferedReader.close();
    }
}
