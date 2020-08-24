package com.example.myapplication;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SocketClientManager {

  private static SocketClientManager instance;
  private ProcessResposer mProcessReponser;

  private SocketClientManager() {
  }

  public static SocketClientManager shareManager(ProcessResposer processResposer) {
    if (instance == null) {
      synchronized (SocketServerManager.class) {
        if (instance == null) {
          instance = new SocketClientManager();
        }
      }
    }
    instance.mProcessReponser = processResposer;
    return instance;
  }

  //发送消息
  public void sendMsg(final String ip, final int port, final String msg) {
    new Thread(new Runnable() {
      @RequiresApi(api = Build.VERSION_CODES.KITKAT)
      @Override
      public void run() {

        Socket socket = null;
        try {
          //                    String[] ipAndPort = ip.split(":");
          socket = new Socket(ip, port);
          socket.setSoTimeout(2000);
          OutputStream outputStream = socket.getOutputStream();
          //                    String msg = "客户端在发送Socket消息!";
          outputStream.write(msg.getBytes(StandardCharsets.UTF_8));
          if (mProcessReponser != null) {
            mProcessReponser.requested(msg);
          }
          socket.shutdownOutput();

          InputStream inputStream = socket.getInputStream();
          byte[] bytes = new byte[1024];
          int len;
          StringBuilder sb = new StringBuilder();
          while ((len = inputStream.read(bytes)) != -1) {
            Log.v("-----", "====len===" + len);
            //注意指定编码格式，发送方和接收方一定要统一，建议使用UTF-8
            sb.append(new String(bytes, 0, len, StandardCharsets.UTF_8));
          }
          if (mProcessReponser != null) {
            mProcessReponser.getRespose(sb.toString());
          }
          System.out.println("get message from server: " + sb);

          //                    socket.shutdownInput();
          socket.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }).start();
  }

  public interface ProcessResposer {
    void requested(String msg);

    void getRespose(String response);
  }
}
