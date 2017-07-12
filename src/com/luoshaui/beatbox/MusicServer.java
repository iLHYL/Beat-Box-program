package com.luoshaui.beatbox;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

public class MusicServer {
	ArrayList<ObjectOutputStream> clientOutputStream;
	
	public class ClientHandler implements Runnable{
		ObjectInputStream in;
		Socket clientSocket;
		public ClientHandler(Socket socket) {
			// TODO Auto-generated constructor stub
			try{
				clientSocket = socket;
				in = new ObjectInputStream(socket.getInputStream());
			}catch(Exception e){e.printStackTrace();}
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Object o1 = null;
			Object o2 = null;
			try{
				
				while ((o1 = in.readObject())!=null){
					o2 = in.readObject();
					System.out.println("read two Object");
					tellEveryone(o1,o2);
				}
			}catch(Exception e){e.printStackTrace();}
		}
	}//close inner class
	
	public void tellEveryone(Object one,Object two){
		Iterator<ObjectOutputStream> it = clientOutputStream.iterator();
		while(it.hasNext()){
			try{
				ObjectOutputStream out = (ObjectOutputStream)it.next();
				out.writeObject(one);
				out.writeObject(two);
			}catch(Exception e){e.printStackTrace();}
		}
	}
	
	public void go(){
		clientOutputStream = new ArrayList<>();
		
		try{
			@SuppressWarnings("resource")
			ServerSocket serverSock = new ServerSocket(4244);
			
			while(true){
				Socket clientSock = serverSock.accept();
				ObjectOutputStream out = new ObjectOutputStream(clientSock.getOutputStream());
				clientOutputStream.add(out);
				
				Thread t  = new Thread(new ClientHandler(clientSock));
				t.start();
				System.out.println("got a connection");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
