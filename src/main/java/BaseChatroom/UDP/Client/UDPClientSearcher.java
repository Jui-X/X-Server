package BaseChatroom.UDP.Client;

import BaseChatroom.Utils.ByteUtils;
import BaseChatroom.UDP.Enum.Constant;
import BaseChatroom.UDP.Enum.Enum;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-05-25 22:27
 **/
public class UDPClientSearcher {
    private static final int RESPONSE_PORT = Enum.UDP_RESPONSE_PORT.getValue();

    private static class Listener extends Thread {
        private final int responsePort;
        private final CountDownLatch startDownLatch;
        private final CountDownLatch receiveDownLatch;
        private final List<ServerInfo> serverInfoList = new ArrayList<ServerInfo>();
        private final byte[] buffer = new byte[256];
        private final int minLen = Constant.HEADER.length + Enum.SHORT_LENGTH.getValue() + Enum.INT_LENGTH.getValue();
        private boolean done = true;
        private DatagramSocket datagramSocket = null;

        private Listener(int resPort, CountDownLatch startDownLatch, CountDownLatch receiveDownLatch) {
            this.responsePort = resPort;
            this.startDownLatch = startDownLatch;
            this.receiveDownLatch = receiveDownLatch;
        }

        @Override
        public void run() {
            super.run();

            startDownLatch.countDown();

            try {
                datagramSocket = new DatagramSocket(responsePort);
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

                while (done) {
                    datagramSocket.receive(receivePacket);

                    String ip = receivePacket.getAddress().getHostAddress();
                    int port = receivePacket.getPort();
                    int dataSize = receivePacket.getLength();
                    // 根据收到的byte数组进行decode构建字符串
                    byte[] data = receivePacket.getData();
                    boolean isValid = dataSize >= minLen && ByteUtils.startsWith(data, Constant.HEADER);
                    System.out.println("UDP Searcher received from ip " + ip +
                            "\tport = " + port + "\tdataValid: " + isValid);

                    if (!isValid) {
                        continue;
                    }

                    ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, Constant.HEADER.length, dataSize);
                    final short cmd = byteBuffer.getShort();
                    final int tcpPort = byteBuffer.getInt();
                    if (cmd != 2 || tcpPort <= 0) {
                        System.out.printf("UDP Searcher receive cmd:" + cmd + "\ttcpPort:" + tcpPort);
                        continue;
                    }

                    String server_index = new String(buffer, minLen, dataSize - minLen);
                    ServerInfo info = new ServerInfo(ip, tcpPort, server_index);
                    serverInfoList.add(info);
                    // 成功接收到服务器信息
                    receiveDownLatch.countDown();
                }
            } catch (IOException e) {

            } finally {
                close();
                System.out.println("UDP Searcher listener finished.");
            }
        }

        private void close() {
            if (datagramSocket != null) {
                datagramSocket.close();
                datagramSocket = null;
            }
        }

        List<ServerInfo> getServerInfoAndClose() {
            done = false;
            close();
            return serverInfoList;
        }
    }

    public static ServerInfo searchServer(int timeout) {
        System.out.println("UDP Searcher start...");

        // 成功收到回送的栅栏
        CountDownLatch receiveDownLatch = new CountDownLatch(1);
        Listener listener = null;

        try {
            listener = listen(receiveDownLatch);
            broadcast();
            receiveDownLatch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("UDP Searcher end.");

        if (listener == null) {
            return null;
        }

        List<ServerInfo> serverList = listener.getServerInfoAndClose();
        if (serverList != null && serverList.size() != 0) {
            return serverList.get(0);
        }
        return null;
    }

    private static Listener listen(CountDownLatch receiveDownLatch) throws InterruptedException {
        System.out.println("UDP Searcher listen...");

        CountDownLatch startDownLatch = new CountDownLatch(1);
        Listener listener = new Listener(RESPONSE_PORT, startDownLatch, receiveDownLatch);
        listener.start();

        startDownLatch.await();

        return listener;
    }

    private static void broadcast() throws IOException {
        System.out.println("UDP Searcher Broadcast start...");

        // 作为发送方（搜索方），让系统指定端口
        // DatagramSocket用于发送或接收UDP报文
        DatagramSocket ds = new DatagramSocket();

        // 构建一份请求数据
        ByteBuffer byteBuffer = ByteBuffer.allocate(256);
        // 头部
        byteBuffer.put(Constant.HEADER);
        // cmd命令
        byteBuffer.putShort((short) Enum.CMD_RECEIVE.getValue());
        // 回送端口
        byteBuffer.putInt(RESPONSE_PORT);
        // 直接构建Packet
        DatagramPacket requestPacket = new DatagramPacket(byteBuffer.array(), byteBuffer.position() + 1);
        // 广播地址
        requestPacket.setAddress(InetAddress.getByName("255.255.255.255"));
        // 设置服务器接收UDP请求的端口
        requestPacket.setPort(Enum.UDP_PORT.getValue());

        // 发送广播
        ds.send(requestPacket);
        ds.close();

        System.out.println("UDP Searcher Broadcast finished.");
    }
}
