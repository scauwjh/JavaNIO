package com.java.nio.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Scanner;

import com.java.nio.beans.DataPacket;
import com.java.nio.util.DebugUtil;
import com.java.nio.util.StringUtil;

public class NIOClient implements Runnable {
	
	public static Integer errCount = 0;
	
	public static void client() throws Exception {
		SocketAddress address = null;
		SocketChannel client = null;
		ByteBuffer buffer = null;
		String fromUser = StringUtil.randString(32);
		address = new InetSocketAddress("219.239.95.15", 9999);
		client = SocketChannel.open(address);
		client.configureBlocking(false);
		buffer = ByteBuffer.allocate(1024);
		
		Scanner input = new Scanner(System.in);
		
		Long count = 0L;
		while(true) {
			if (count > 500)
				break;
			Integer sleep = (int) (Math.random() * 10) + 10;
			Thread.sleep(sleep);
			buffer.clear();
			String send = "hello server"; //input.next();
			DataPacket dp = new DataPacket();
			dp.setId(count++);
			dp.setContent(send);
			dp.setSendTime(new Date());
			dp.setFromUser(fromUser);
			dp.setToUser("server");
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ObjectOutputStream obj = new ObjectOutputStream(out);
			obj.writeObject(dp);
			buffer.put(out.toByteArray());
			buffer.flip();
			client.write(buffer);
			obj.close();
			out.close();
			
			
			buffer.clear();
			int len;
			while (true) {
				len = client.read(buffer);
				if (len > 0) {
					buffer.flip();
					len = buffer.limit();
					byte b[] = new byte[len];
					buffer.get(b, buffer.position(), len);
					// 字节转类
					ByteArrayInputStream in = new ByteArrayInputStream(b);
					ObjectInputStream objIn = new ObjectInputStream(in);
					dp = (DataPacket) objIn.readObject();
					in.close();
					objIn.close();
					if (!dp.getToUser().equals(fromUser)) {
						errCount ++;
						System.err.println("data is error");
					}
//					System.out.println("data id: " + dp.getId());
//					System.out.println("received from client: " + dp.getContent());
//					System.out.println("received time: " + dp.getSendTime());
					break;
				}
			}
		}
		client.close();
		System.out.println("error data count: " + errCount);
	}

	@Override
	public void run() {
		try {
			client();
		} catch (Exception e) {
			DebugUtil.printStackTrace(e);
		}
	}

	
	public static void main(String[] args) throws InterruptedException {
		NIOClient client = new NIOClient();
		for (int i = 0; i < 300; i++) {
			new Thread(client).start();
			Thread.sleep(10);
		}
	}
}
