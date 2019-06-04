package BaseChatroom.UDP.Server;

import BaseChatroom.TCP.TCPServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static BaseChatroom.UDP.Enum.Enum.TCP_PORT;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-05-25 22:27
 **/
public class UDPServer {

    public static void main(String[] args) throws IOException {
        // 根据端口构建TCP Server
        TCPServer tcpServer = new TCPServer(TCP_PORT.getValue());
        boolean isSucceed = tcpServer.start();
        if (!isSucceed) {
            System.out.println("TCP Server start failed...");
            return;
        }
        // 将自己服务器的TCP端口，通过UDP发送到UDP广播的服务器
        UDPServerProvider.start(TCP_PORT.getValue());

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String str;
        String end = "byebye";
        do {
            // 从键盘读取输入并将输入广播到所有客户端
            str = bufferedReader.readLine();
            // 调用WriteHandler将收到的信息广播出去
            tcpServer.broadcast(str);
        } while (!end.equalsIgnoreCase(str));

        UDPServerProvider.stop();
        tcpServer.stop();
    }
}
