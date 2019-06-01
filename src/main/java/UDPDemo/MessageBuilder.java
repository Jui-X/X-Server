package UDPDemo;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-05-20 20:46
 **/
public class MessageBuilder {
    private static final String SERVER_HEADER = "Receive Message and This is TCPClient ";
    private static final String PORT_HEADER = "Please reply port ";

    public static String buildPort(int port) {
        return PORT_HEADER + port;
    }

    public static int parsePort(String data) {
        if (data.startsWith(PORT_HEADER)) {
            return Integer.parseInt(data.substring(PORT_HEADER.length()));
        }
        return -1;
    }

    public static String buildSerever(String index) {
        return SERVER_HEADER + index;
    }

    public static String parseServer(String data) {
        if (data.startsWith(SERVER_HEADER)) {
            return data.substring(SERVER_HEADER.length());
        }
        return null;
    }
}
