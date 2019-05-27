package TCPAndUDP.UDP.Server;

import TCPAndUDP.UDP.Enum.Enum;

import java.io.IOException;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-05-25 22:27
 **/
public class Server {

    public static void main(String[] args) {
        // 将自己服务器的TCP端口，通过UDP发送到UDP广播的服务器
        ServerProvider.start(Enum.TCP_PORT.getValue());

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ServerProvider.stop();
    }
}
