package BaseChatroom.UDP.Client;

import BaseChatroom.TCP.TCPClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
            TCPClient tcpClient = null;
            try {
                tcpClient = TCPClient.startWith(serverInfo);
                if (tcpClient == null) {
                    return;
                }

                write(tcpClient);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (tcpClient != null) {
                    tcpClient.exit();
                }
            }
        }
    }



    private static void write(TCPClient client) throws IOException {
        // 构建键盘输入流
        InputStream in = System.in;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));

        do {
            // 从键盘读取一行
            String str = bufferedReader.readLine();

            String end = "byebye";
            if (end.equalsIgnoreCase(str)) {
                break;
            }
        } while (true);
    }
}
