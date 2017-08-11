package demo.com.rounter;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import demo.com.rounter.bean.SelectActivity;
import demo.com.rounter.location.GPSServer;
import demo.com.rounter.qr.CaptureActivity;
import demo.com.rounter.utils.Https;

import static android.R.attr.animation;
import static android.R.attr.path;
import static demo.com.rounter.R.id.toast_img;
import static demo.com.rounter.R.id.toptitle;


/**
 * Created by ff on 2017/2/28.
 */

public class WebActivity extends Activity {

    public static final int SELECT_PHOTO = 3;
    public static final int TAKE_PHOTO = 1;
    public static final int QRCODE = 4;
//        public static String server = "http://192.168.1.155:88/";
    public static String server = "http://proxy.dotwintech.com:9000/";
    private WebView webView;
    private Context context;
    private String callBack;
    private  Uri imageUri;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    callBack = (String) msg.obj;
                    //开启二维码扫描
                    startActivityForResult(new Intent(context, CaptureActivity.class), QRCODE);
                    break;
                case 2:
                    callBack = (String) msg.obj;
                    //选择图片
                    startActivityForResult(new Intent(context, SelectActivity.class),SELECT_PHOTO);
                    break;
                case 3:
                    Log.d("qr","拍照");
                    callBack = (String) msg.obj;
                    File outPutImg = new File(getExternalCacheDir(),"outPutImg.jpg");
                    if (outPutImg.exists()){
                        outPutImg.delete();
                    }
                    try {
                        outPutImg.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    imageUri = Uri.fromFile(outPutImg);
                    //启动相机程序
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(intent, TAKE_PHOTO);
                    break;
                case 4:
                    startService(new Intent(context,GPSServer.class));
                    break;
                case 5:
                    String path=null;
                    String str = (String) msg.obj;
                    Log.e("path",str);
                    try {
                        if(str != null) {
                            JSONObject json = new JSONObject(str);
                            if (json.getInt("result") == 1) {
                                path = json.getString("object");
                            }else{
                                path="error";
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    webView.loadUrl("javascript:"+callBack+"(\""+path+"\")");
                    break;
            }
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_web);
        //		初始化
        webView = (WebView) findViewById(R.id.webView1);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webSettings.setDefaultTextEncodingName("utf-8");
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(false);
        WebView.setWebContentsDebuggingEnabled(true);
        //5.0以上 webview 需要自己同步cookie
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView,true);

        webView.setWebChromeClient(new WebChromeClient() {
            //读取html的title并显示
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
            }


        });
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return true;
            }
        });
        //提供js方法调用
        webView.addJavascriptInterface(new Jsoperation(context, handler), "client");
        //		获取传入参数，初始化页面
        webView.loadUrl("file:///android_asset/index.html");
        //开启gps位置监听
        startService(new Intent(context,GPSServer.class));

        progress("Progress","test");
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.loadUrl("javascript:onResume()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.exit(0);
    }

    public void TopBack(View view) {
        if (webView.canGoBack()) {
            webView.goBack(); // 后退
        } else {
            this.finish();
            overridePendingTransition(R.anim.right_in, R.anim.left_out);
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == QRCODE && resultCode == RESULT_OK) {  //    获取扫码的二维码中的信息
            String qrinfo = data.getStringExtra(CaptureActivity.EXTRA_RESULT);
            Log.e("qr", "扫描二维码" + callBack + ":" + qrinfo);
            webView.loadUrl("javascript:"+callBack+"(\""+qrinfo+"\")");
        }else if(requestCode == SELECT_PHOTO && resultCode == RESULT_OK){//选择图片
            final String select = data.getStringExtra(SelectActivity.EXTRA_RESULT);
            new Thread(){
                @Override
                public void run() {
                    String result = Https.postFile(server+"router_stat/api/file/uploadImg",select);
                    Message msg = new Message();
                    msg.obj = result;
                    msg.what = 5;
                    handler.sendMessage(msg);
                }
            }.start();
        }else if(requestCode == TAKE_PHOTO && resultCode == RESULT_OK){
                try {
                 //文件为cache目录下的imguri,可以得到图片并且显示
                    new Thread(){
                        @Override
                        public void run() {
                            String result = Https.postFile(server+"router_stat/api/file/uploadImg",getExternalCacheDir()+"/outPutImg.jpg");
                            if (result == null){
                                result = "{\"result\":0}";
                            }
                            Message msg = new Message();
                            msg.obj = result;
                            msg.what = 5;
                            handler.sendMessage(msg);
                        }
                    }.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
    }

    private long mPressedTime = 0;
    /**
     * 点击返回，返回上一个网页
     *
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack(); // 后退
            return true;
        } else {
            long mNowTime = System.currentTimeMillis();//获取第一次按键时间
            if((mNowTime - mPressedTime) > 2000){
                Toast.makeText(this,"再按一次退出",Toast.LENGTH_SHORT).show();
                mPressedTime = mNowTime;
            }else{
//                this.finish();
                onDestroy();

            }
            return true;
        }
    }


private Dialog dialog;
    private void progress(String type,String msg){
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.toast_1,null);
        TextView textView = (TextView) view.findViewById(R.id.toast_text);
        ImageView imageView = (ImageView) view.findViewById(R.id.toast_img);
        textView.setText(msg);
        if (type.equals("Progress")){
            dialog = new Dialog(this);
            dialog.setCancelable(true);
            dialog.setContentView(view);
            imageView.setImageResource(R.drawable.loading1);
            Animation animation = AnimationUtils.loadAnimation(this,R.anim.loading_animation);
            imageView.startAnimation(animation);
            dialog.show();
        }
    }
}
