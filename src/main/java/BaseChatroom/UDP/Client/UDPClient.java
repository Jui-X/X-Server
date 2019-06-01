package BaseChatroom.UDP.Client;

import BaseChatroom.TCP.TCPClient;

import java.io.IOException;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-05-25 22:27
 **/
public class UDPClient {

    public static void main(String[] args) {
        // 超时时间设置为10s
        ServerInfo serverInfo = UDPClientSearcher.searchServer(5000);
        System.out.println("TCP Client \t ip: " + serverInfo.getAddress() + "\tport: " + serverInfo.getPort()
                + "\tserver_index: " + serverInfo.getServer_index());

        if (serverInfo != null) {
            try {
                TCPClient.linkWith(serverInfo);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
