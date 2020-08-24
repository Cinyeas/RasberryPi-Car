package com.example.myapplication;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;

import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,OnTouchListener,SocketClientManager.ProcessResposer {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String FORWARD = "cmd_start";
    private static final String BACK = "cmd_back";
    private static final String LEFT = "cmd_left";
    private static final String RIGHT = "cmd_right";
    private static final String STOP = "cmd_stop";
    private static final String LATERAL_LEFT = "cmd_side_left";
    private static final String LATERAL_RIGHT = "cmd_side_right";

    private static final String HIGH_SPEED = "cmd_speed_high";
    private static final String MID_SPEED = "cmd_speed_mid";
    private static final String LOW_SPEED = "cmd_speed_low";

    private boolean is_touch = false;

    private String mServiceIp;
    private int mServicePort;

    private SocketClientManager mSocketClientManager;
    private WebView myWebView;

    private String  ip_addr = "192.168.1.101";    //树莓派ip地址
    private String port = "8080";   //端口号，默认为8080，不用改

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageButton btnForward = findViewById(R.id.forward);
        ImageButton btnBack = findViewById(R.id.back);
        ImageButton btnLeft = findViewById(R.id.left);
        ImageButton btnRight = findViewById(R.id.right);
        Button btnHs = findViewById(R.id.hs_btn);
        Button btnMs = findViewById(R.id.ms_btn);
        Button btnLs = findViewById(R.id.ls_btn);
        TextView stop_tv = findViewById(R.id.stop_btn);
        ImageButton side_left = findViewById(R.id.side_left_btn);
        ImageButton side_right = findViewById(R.id.side_right_btn);

        btnForward.setOnTouchListener(this);
        btnBack.setOnTouchListener(this);
        btnLeft.setOnTouchListener(this);
        btnRight.setOnTouchListener(this);

        side_left.setOnTouchListener(this);
        side_right.setOnTouchListener(this);

        btnHs.setOnClickListener(this);
        btnMs.setOnClickListener(this);
        btnLs.setOnClickListener(this);
        stop_tv.setOnClickListener(this);




        stop_tv.setOnClickListener(this);

        Init(ip_addr, port);

        mSocketClientManager = SocketClientManager.shareManager(MainActivity.this);
    }


    /*
    * 右上角配置小车的ip地址和端口号
    * 这里是socket使用的端口号，与摄像头的端口号需要区分开
    *
    **/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.id_cfg_item:
                final QMUIDialog.EditTextDialogBuilder ip_builder =
                        new QMUIDialog.EditTextDialogBuilder(MainActivity.this);
                ip_builder.setTitle("配置IP")
                        .setPlaceholder("如：192.168.0.1")
                        .setInputType(InputType.TYPE_CLASS_TEXT)
                        .addAction("取消", new QMUIDialogAction.ActionListener() {
                            @Override
                            public void onClick(QMUIDialog dialog, int index) {
                                dialog.dismiss();
                            }
                        })
                        .addAction("确定", new QMUIDialogAction.ActionListener() {
                            @Override
                            public void onClick(QMUIDialog dialog, int index) {
                                CharSequence text = ip_builder.getEditText().getText();
                                if (text != null && text.length() > 0) {
                                    PreferencesUtils.putString(MainActivity.this, SPKeys.SERVICEIP,
                                            text.toString());
                                    Toast.makeText(MainActivity.this,"配置成功",Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                } else {
                                    Toast.makeText(MainActivity.this,"请填写IP" ,Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .create(com.qmuiteam.qmui.R.style.QMUI_Dialog).show();
                break;

            case R.id.id_port_item:
                final QMUIDialog.EditTextDialogBuilder port_builder =
                        new QMUIDialog.EditTextDialogBuilder(MainActivity.this);
                port_builder.setTitle("配置端口号")
                        .setPlaceholder("如：55533")
                        .setInputType(InputType.TYPE_CLASS_TEXT)
                        .addAction("取消", new QMUIDialogAction.ActionListener() {
                            @Override
                            public void onClick(QMUIDialog dialog, int index) {
                                dialog.dismiss();
                            }
                        })
                        .addAction("确定", new QMUIDialogAction.ActionListener() {
                            @Override
                            public void onClick(QMUIDialog dialog, int index) {
                                CharSequence text = port_builder.getEditText().getText();
                                if (text != null && text.length() > 0) {
                                    PreferencesUtils.putString(MainActivity.this, SPKeys.SERVICEPORT,
                                            text.toString());
                                    Toast.makeText(MainActivity.this,"配置成功",Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                } else {
                                    Toast.makeText(MainActivity.this,"请填写IP" ,Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .create(com.qmuiteam.qmui.R.style.QMUI_Dialog).show();
                break;
            default:
                break;
        }

        return true;
    }
    java.util.Timer timer = new java.util.Timer(true);

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        v.performClick();
        mServiceIp = PreferencesUtils.getString(MainActivity.this, SPKeys.SERVICEIP);
        mServicePort = Integer.parseInt(PreferencesUtils.getString(MainActivity.this, SPKeys.SERVICEPORT));
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            is_touch = true;
            switch (v.getId()) {
                case R.id.forward:
                    Thread for_thread = new Thread(){
                        @Override
                        public void run() {
                            super.run();
                            while (is_touch){
                                send(mServiceIp, mServicePort, FORWARD);
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };
                    for_thread.start();
                    break;
                case R.id.back:
                    Thread back_thread = new Thread(){
                        @Override
                        public void run() {
                            super.run();
                            while (is_touch){
                                send(mServiceIp, mServicePort, BACK);
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };
                    back_thread.start();
                    break;
                case R.id.left:
                    Thread left_thread = new Thread(){
                        @Override
                        public void run() {
                            super.run();
                            while (is_touch){
                                send(mServiceIp, mServicePort, LEFT);
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };
                    left_thread.start();
                    break;
                case R.id.right:
                    Thread right_thread = new Thread(){
                        @Override
                        public void run() {
                            super.run();
                            while (is_touch){
                                send(mServiceIp, mServicePort, RIGHT);
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };
                    right_thread.start();
                    break;
                case R.id.side_left_btn:
                    Thread sl_thread = new Thread(){
                        @Override
                        public void run() {
                            super.run();
                            while (is_touch){
                                send(mServiceIp, mServicePort, LATERAL_LEFT);
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };
                    sl_thread.start();
                    break;

                case R.id.side_right_btn:
                    Thread rl_thread = new Thread(){
                        @Override
                        public void run() {
                            super.run();
                            while (is_touch){
                                send(mServiceIp, mServicePort, LATERAL_RIGHT);
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };
                    rl_thread.start();
                    break;

                default:
                    break;
            }
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            is_touch = false;
            send(mServiceIp, mServicePort, STOP);
            return true;
        } else {
            return false;
        }
    }

    private void send(String serviceIp, int mServicePort, String command) {
        mSocketClientManager.sendMsg(this.mServiceIp,this.mServicePort,command);
        Log.v("socket","Send:"+command);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void requested(String msg) {

    }

    @Override
    public void getRespose(String response) {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.hs_btn:
                send(mServiceIp, mServicePort, HIGH_SPEED);
                break;
            case R.id.ms_btn:
                send(mServiceIp, mServicePort, MID_SPEED);
                break;
            case R.id.ls_btn:
                send(mServiceIp, mServicePort, LOW_SPEED);
                break;
            case R.id.stop_btn:
                send(mServiceIp, mServicePort, STOP);
            default:
                break;
        }
    }

    public void Init(String ip_addr,String port){

        myWebView = (WebView) findViewById(R.id.web_camera);//获取view

        WebSettings WebSet = myWebView.getSettings();    //获取webview设置
        WebSet.setJavaScriptEnabled(true);              //设置JavaScript支持

        WebSet.setSupportZoom(true);            // 设置可以支持缩放

        WebSet.setBuiltInZoomControls(true);    // 设置出现缩放工具

        WebSet.setUseWideViewPort(true);        //扩大比例的缩放

        WebSet.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);   //自适应屏幕
        WebSet.setLoadWithOverviewMode(true);

        myWebView.loadUrl("http://"+ip_addr+":"+port+"/?action=stream");

        myWebView.setWebViewClient(new WebViewClient(){  //设置不适用第三方浏览器打开网页
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                view.loadUrl(url);
                return true;
            }
        });
    }
    @Override
    public void onBackPressed() {

        if (myWebView.canGoBack()) {
            myWebView.goBack();//返回上一页面
        } else {
            System.exit(0);//退出程序
        }
    }
}
