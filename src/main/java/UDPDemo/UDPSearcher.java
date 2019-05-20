package UDPDemo;

import java.io.IOException;
import java.net.*;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-05-20 11:29
 **/
public class UDPSearcher {

    public static void main(String[] args) throws IOException {
        System.out.println("UDP Searcher Started...");

        // 作为发送方（搜索方），让系统指定端口
        // DatagramSocket用于发送或接收UDP报文
        DatagramSocket ds = new DatagramSocket();

        // 构建发送数据
        String requestData = "Hello World!";
        byte[] requestDataBytes = requestData.getBytes();
        DatagramPacket requestPacket = new DatagramPacket(requestDataBytes,
                requestDataBytes.length);
        // UDP 服务方的端口为本机的20000端口
        requestPacket.setAddress(InetAddress.getLocalHost());
        requestPacket.setPort(20000);

        ds.send(requestPacket);

        final byte[] buf = new byte[256];
        // DatagramPacket用于处理报文
        DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

        // 接收
        ds.receive(receivePacket);

        // 打印发送者信息
        // 发送者ip地址
        String ip = receivePacket.getAddress().getHostAddress();
        int port = receivePacket.getPort();
        int dataSize = receivePacket.getLength();
        // 根据收到的byte数组进行decode构建字符串
        String data = new String(receivePacket.getData(), 0, dataSize);
        System.out.println("UDP Provider received from ip " + ip +
                "\tport = " + port + "\tdata: " + data);

        // 结束并关闭socket
        System.out.println("UDP Searcher finished");
        ds.close();
    }
}
