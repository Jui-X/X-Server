package UDPDemo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-05-20 20:46
 **/
public class UDPBroadcastSearcher {
    private static final int LISTEN_PORT = 50001;

    public static void main(String[] args) throws InterruptedException, IOException {
        System.out.println("UDP Searcher start...");

        Listener listener = listen();
        broadcast();

        // 读取任意键盘信息后可以退出
        System.in.read();

        List<Device> devices = listener.getDeviceAndClose();

        for (Device device: devices) {
            System.out.println("Device: " + device.toString());
        }

        System.out.println("UDP Searcher end.");
    }

    private static class Listener extends Thread {
        private final int listenPort;
        private final CountDownLatch countDownLatch;
        private final List<Device> devices = new ArrayList<>();
        private boolean done = false;
        private DatagramSocket datagramSocket = null;

        public Listener(int port, CountDownLatch countDownLatch) {
            super();
            this.listenPort = port;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            super.run();

            countDownLatch.countDown();

            try {
                // 监听回送端口
                datagramSocket = new DatagramSocket(listenPort);

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
                    System.out.println("UDP Searcher received from ip " + ip +
                            "\tport = " + port + "\tdata: " + data);

                    // 解析服务端序列号
                    String index = MessageBuilder.parseServer(data);
                    if (index != null) {
                        Device device = new Device(port, ip, index);
                        devices.add(device);
                    }
                }
            } catch (Exception e) {

            } finally {
                UDPClose();
                System.out.println("UDPSearcher listener finished.");
            }
        }

        List<Device> getDeviceAndClose() {
            done = true;
            UDPClose();
            return devices;
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
    }

    private static class Device {
        final int port;
        final String ip;
        final String index;

        public Device(int port, String ip, String index) {
            this.port = port;
            this.ip = ip;
            this.index = index;
        }

        @Override
        public String toString() {
            return "Device {" +
                    "port = " + port +
                    ", ip = " + ip +
                    ", index = " + index
                    + "}";
        }
    }

    private static Listener listen() throws InterruptedException {
        System.out.println("UDP Searcher listen...");
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Listener listener = new Listener(LISTEN_PORT, countDownLatch);
        listener.start();

        // 等待
        countDownLatch.await();
        return listener;
    }

    private static void broadcast() throws IOException {
        System.out.println("UDP Searcher Broadcast start...");

        // 作为发送方（搜索方），让系统指定端口
        // DatagramSocket用于发送或接收UDP报文
        DatagramSocket ds = new DatagramSocket();

        // 构建一份请求数据
        String requestData = MessageBuilder.buildPort(LISTEN_PORT);
        byte[] requestDataBytes = requestData.getBytes();
        // 构建DatagramPacket
        DatagramPacket requestPacket = new DatagramPacket(requestDataBytes, requestDataBytes.length);
        // 以广播形式，发送到服务方50000端口
        requestPacket.setAddress(InetAddress.getByName("255.255.255.255"));
        requestPacket.setPort(50000);

        // 发送
        ds.send(requestPacket);
        ds.close();

        System.out.println("UDP Searcher Broadcast finished.");
    }
}
