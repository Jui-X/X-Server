package UDPDemo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.UUID;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-05-20 20:46
 **/
public class UDPBroadcastProvider {

    public static void main(String[] args) throws IOException {
        // 生成唯一ID
        String index = UUID.randomUUID().toString();
        Provider provider = new Provider(index);
        provider.start();

        // 读取任意输出后服务关闭
        System.in.read();
        provider.exit();
    }

    private static class Provider extends Thread {
        private final String server_idx;
        private boolean done = false;
        private DatagramSocket datagramSocket = null;

        public Provider(String index) {
            super();
            this.server_idx = index;
        }

        @Override
        public void run() {
            super.run();

            System.out.println("UDP Provider Broadcast start...");

            try {
                // 服务方监听自身5000端口
                datagramSocket = new DatagramSocket(50000);

                while (!done) {
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

                    // 解析端口号
                    int resPort = MessageBuilder.parsePort(data);
                    if (resPort != -1) {
                        // 构建回送消息
                        String responseData = MessageBuilder.buildSerever(server_idx);
                        byte[] responseDataBytes = responseData.getBytes();
                        // 根据回送消息构建一份回送信息DatagramPacket
                        DatagramPacket responsePacket = new DatagramPacket(responseDataBytes,
                                responseDataBytes.length, receivePacket.getAddress(),
                                resPort);

                        datagramSocket.send(responsePacket);
                    }
                }
            } catch (IOException e) {

            } finally {
                UDPClose();
                System.out.printf("UDP Provider finished.");
            }
        }

        /**
         * 关闭连接并删除
         */
        private void UDPClose() {
            if (datagramSocket != null) {
                datagramSocket.close();
            }
            datagramSocket.close();
        }

        /**
         * 终止循环
         * 关闭连接
         */
        void exit() {
            done = true;
            UDPClose();
        }
    }

}
