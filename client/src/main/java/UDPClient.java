import box.FileSendPacket;
import core.IOContext;
import impl.IOSelectorProvider;

import java.io.*;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-05-25 22:27
 **/
public class UDPClient {

    public static void main(String[] args) throws IOException {
        // 缓存文件目录
        File cachePath = Xyz.getCacheDir("client");

        IOContext.setup()
                .ioProvider(new IOSelectorProvider())
                .start();

        // 超时时间设置为10s
        ServerInfo serverInfo = UDPClientSearcher.searchServer(5000);
        System.out.println("UDPClient => TCP Client \t ip: " + serverInfo.getAddress() + "\tport: " + serverInfo.getPort()
                + "\tserver_index: " + serverInfo.getServer_index());

        if (serverInfo != null) {
            TCPClient tcpClient = null;
            try {
                tcpClient = TCPClient.startWith(serverInfo, cachePath);
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

        IOContext.close();
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

            if (str.startsWith("-f")) {
                String[] array = str.split(" ");
                if (str.length() >= 2) {
                    String filePath = array[1];
                    File file = new File(filePath);
                    if (file.exists() && file.isFile()) {
                        FileSendPacket packet = new FileSendPacket(file);
                        client.send(packet);
                        continue;
                    }
                }
            }

            // 发送到服务器
            client.send("1: " + str);
            client.send("2: " + str);
            client.send("3: " + str);
            client.send("4: " + str);

        } while (true);
    }
}
