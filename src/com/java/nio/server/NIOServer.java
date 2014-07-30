package com.java.nio.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;

import com.java.nio.beans.DataPacket;
import com.java.nio.util.DebugUtil;

public class NIOServer implements Runnable {
	
	protected ServerSocketChannel serverChannel;
	protected Selector selector;
	protected ServerSocket serverSocket;
	protected ByteBuffer buffer;
	protected static int PORT;
	protected static int BUFFER_SIZE;
	protected static final int TIMEOUT = 3000;
	
	public Boolean init;
	public static Integer linkCount = 0;
	
	public NIOServer(int port, int bufferSize) {
		init = true;
		PORT = port;
		BUFFER_SIZE = bufferSize;
		buffer = ByteBuffer.allocate(bufferSize);
		try {
			selector = Selector.open(); // 打开选择器
			serverChannel = ServerSocketChannel.open(); // 打开服务通道
			serverChannel.configureBlocking(false); // 设置为非阻塞
			serverSocket = serverChannel.socket(); // 创建服务器socket
			serverSocket.bind(new InetSocketAddress(PORT)); // socket绑定端口
			serverChannel.register(selector, SelectionKey.OP_ACCEPT); //注册连接事件
			DebugUtil.println("init server successed!");
		} catch (IOException e) {
			DebugUtil.printStackTrace(e);
			init = false;
			DebugUtil.println("init server failed!");
		}
	}

	public void startListen() {
		SelectionKey key = null;
		Iterator<SelectionKey> iter = null;
		DebugUtil.println("start listening");
		while(true) {
			try {
				if (selector.select(TIMEOUT) <= 0)
					continue;
			} catch (IOException e1) {
				DebugUtil.printStackTrace(e1);
				continue;
			}
			iter = selector.selectedKeys().iterator();
			while(iter.hasNext()) {
				key = iter.next();
				iter.remove();
				handleKey(key);
			}
		}
	}
	
	public void handleKey(SelectionKey key) {
		if (key.isValid() && key.isAcceptable()) {
			// 连接事件
			acceptEvent();
		}
		else if (key.isValid() && key.isReadable()) {
			// 读事件
			readEvent(key);
		}
		else if (key.isValid() && key.isWritable()) {
			//写事件
			writeEvent(key);
		}
	}
	
	public void closeClient(SocketChannel client) {
		try {
			DebugUtil.println("count of links now: " + (--linkCount));
			client.socket().close();
			client.close();
		} catch (IOException e) {
			DebugUtil.println("close client exception");
			DebugUtil.printStackTrace(e);
		}
	}
	
	public void acceptEvent() {
		SocketChannel client = null;
		try {
			client = serverChannel.accept(); // 连接客户端socket
			client.configureBlocking(false); // 设置为非阻塞
			client.register(selector, SelectionKey.OP_READ);
			DebugUtil.println("a new link is accepted");
			DebugUtil.println("count of links now: " + (++linkCount));
		} catch (IOException e) {
			DebugUtil.println("accept exception");
			DebugUtil.printStackTrace(e);
			closeClient(client);
		}
	}
	
	public void readEvent(SelectionKey key) {
		SocketChannel client = null;
		
		client = (SocketChannel) key.channel();
		buffer.clear();
		int len = 0;
		try {
			len = client.read(buffer);
			if (len < 0) 
				closeClient(client);
			try {
				if (len > 0) {
					buffer.flip();
					len = buffer.limit();
					byte b[] = new byte[len];
					buffer.get(b, buffer.position(), len);
					// 字节转类
					ByteArrayInputStream in = new ByteArrayInputStream(b);
					ObjectInputStream objIn = new ObjectInputStream(in);
					DataPacket dp = (DataPacket) objIn.readObject();
					in.close();
					objIn.close();
//					System.out.println("data id: " + dp.getId());
//					System.out.println("received from client: " + dp.getContent());
//					System.out.println("received time: " + dp.getSendTime());

					// data for sending to client
					dp.setContent("hello client");
					dp.setSendTime(new Date());
					dp.setToUser(dp.getFromUser());
					dp.setFromUser("server");
					key.attach(dp);
					key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
				}
			} catch (Exception e) {
				DebugUtil.println("read exception");
				DebugUtil.printStackTrace(e);
			}
		} catch (IOException e) {
			DebugUtil.println("client is lost when read");
			closeClient(client);
		}
	}
	
	public void writeEvent(SelectionKey key) {
		DataPacket received = (DataPacket) key.attachment();
		SocketChannel client = null;
		client = (SocketChannel) key.channel();
		buffer.clear();
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ObjectOutputStream obj = new ObjectOutputStream(out);
			obj.writeObject(received);
			buffer.put(out.toByteArray());
			buffer.flip();
			obj.close();
			out.close();
			try {
				client.write(buffer);
				key.interestOps(SelectionKey.OP_READ);
			} catch (IOException e) {
				DebugUtil.println("client is lost when write");
				closeClient(client);
			}
		} catch (Exception e) {
			DebugUtil.println("write exception");
			DebugUtil.printStackTrace(e);
		}
	}
	
	
	@Override
	public void run() {
		startListen();
	}
	
	public static void main(String[] args) {
		NIOServer server = new NIOServer(9999, 1024);
		new Thread(server).start();
		
	}
}
