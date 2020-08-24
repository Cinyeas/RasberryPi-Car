package com.example.myapplication;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServerManager {

  private static SocketServerManager instance;
  private ServerSocket mServer;
  private Thread mAcceptThread;
  private ExecutorService mExecutorService;
  private ProcessRequester mProcessRequester;
  private final int PORT = 55533;

  private SocketServerManager() {
    try {
      mExecutorService = Executors.newCachedThreadPool();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static SocketServerManager shareManager(ProcessRequester processRequest) {
    if (instance == null) {
      synchronized (SocketServerManager.class) {
        if (instance == null) {
          instance = new SocketServerManager();
        }
      }
    }
    instance.mProcessRequester = processRequest;
    return instance;
  }

  //开启服务
  public void startServer() {
    try {
      mServer = new ServerSocket(PORT);
      mAcceptThread = new Thread() {
        @Override
        public void run() {
          while (!isInterrupted()) {
            Log.v("-----","等待连接过来");

            try {
              Socket socket = mServer.accept();
              mExecutorService.execute(new SocketTask(socket));
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
      };
      mAcceptThread.start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  //关闭服务器
  public void stopServer() {
    try {
      mServer.close();
      mAcceptThread.interrupt();
      mExecutorService.shutdownNow();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public class SocketTask implements Runnable {

    private final Socket mSocket;

    public SocketTask(Socket socket) {
      mSocket = socket;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void run() {
      try {

        while(true){
          InputStream inputStream = mSocket.getInputStream();
          int BUFFSIZE = 1024;
          byte[] bytes = new byte[BUFFSIZE];
          int len;
          StringBuilder sb = new StringBuilder();
          len = inputStream.read(bytes);
          if (len > 0) {
            sb.append(new String(bytes, 0, len, StandardCharsets.UTF_8));
          }
          Log.v("-----", "客户端提交的内容为："+sb);
          try {
            OutputStream outputStream = mSocket.getOutputStream();
            String resposeStr = sb.toString().toUpperCase();
            outputStream.write(resposeStr.getBytes());
          }catch (IOException e){
            e.printStackTrace();
          }
        }

      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public interface ProcessRequester {
    void getRequest(String string);
  }
}
