package UDPDemo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * @param: none
 * @description: 用于监听UDP消息，提供服务
 * @author: KingJ
 * @create: 2019-05-20 11:29
 **/
public class UDPProvider {

    public static void main(String[] args) throws IOException {
        System.out.println("UDP Provider Started...");

        // 作为接收者，指定一个端口进行接收消息
        // DatagramSocket用于发送或接收UDP报文
        DatagramSocket datagramSocket = new DatagramSocket(20000);

        // 构建接收实体
        final byte[] buf = new byte[256];
        // DatagramPacket用于处理报文
        DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

        // 接收
        datagramSocket.receive(receivePacket);

        // 打印发送者信息
        // 发送者ip地址
        String ip = receivePacket.getAddress().getHostAddress();
        int port = receivePacket.getPort();
        int dataSize = receivePacket.getLength();
        // 根据收到的byte数组进行decode构建字符串
        String data = new String(receivePacket.getData(), 0, dataSize);
        System.out.println("UDP Provider received from ip " + ip +
                "\tport = " + port + "\tdata: " + data);


        // 构建回送消息
        String responseData = "Received data with length: " + dataSize;
        byte[] responseDataBytes = responseData.getBytes();
        // 根据回送消息构建一份回送信息DatagramPacket
        DatagramPacket responsePacket = new DatagramPacket(responseDataBytes,
                responseDataBytes.length, receivePacket.getAddress(),
                receivePacket.getPort());

        datagramSocket.send(responsePacket);

        // 结束并关闭socket
        System.out.println("UDP Provider finished");
        datagramSocket.close();
    }
}
