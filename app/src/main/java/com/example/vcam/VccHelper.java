package com.example.vcam;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class VccHelper {

    public  static String GetPalyUrl(String deviceCode)
    {
        String playUrl="";
        String httpUrl="http://klive.onllk.com:8090/api/video/getliveurl?devicecode="+deviceCode;
        String resultData="";//定义一个resultData用于存储获得的数据
        URL url=null; //定义URL对象
        try {
            url=new URL (httpUrl); //构造一个URL对象时需要使用异常处理
        } catch (MalformedURLException e) {
            System.out.println (e.getMessage ());//打印出异常信息
        }
        if (url !=null) {//如果URL不为空时
            try{
                //有关网络操作时，需要使用异常处理
                HttpURLConnection urlConn= (HttpURLConnection)url.openConnection (); //使用HttpURLConnection打开连接
                InputStreamReader in=new InputStreamReader (urlConn.getInputStream());//得到读取的内容
                BufferedReader buffer=new BufferedReader (in);//为输出创建BufferedReader
                String inputLine=null;
                while (((inputLine=buffer.readLine()) !=null)) {
                    // 读取获得的数据
                    resultData+=inputLine+"\n"; // 加上"\n"实现换行
                }
                in.close();//关闭InputStreamReader
                urlConn.disconnect(); //关闭HTTP连接
                if (resultData !=null) {//如果获取到的数据不为空
//                    textView.setText(resultData) ; 不为空需要解析出播放地址
                    playUrl=resultData;
                    JSONObject jsonObject =new JSONObject (resultData) ;
                    playUrl=jsonObject.getString("data");

                } else {
                    Log.i("VccHelper","Sorry,the content is null");
//                    textView.setText("Sorry,the content is null");//获取到的数据为空时显示
                }
            } catch (IOException e) {
//                textView.setText (e.getMessage());
                Log.e("VccHelper",e.getMessage());
                //出现异常时，打印异常信息
            } catch (JSONException e) {
                Log.e("VccHelper",e.getMessage());
            }
        } else {
            Log.i("VccHelper","url is null");
//            textView.setText ("url is null"); //当url为空时输出
        }
        return  playUrl;
    }


    public Context toast_content;

    public static String video_path = "/storage/emulated/0/DCIM/Camera1/";

    public static String fileUrl = "/storage/emulated/0/DCIM/Camera1/virtual.mp4";

    public static String disable_file = "/storage/emulated/0/DCIM/Camera1/disable.jpg";

    public static String no_silent_file = "/storage/emulated/0/DCIM/Camera1/no-silent.jpg";

    public static String decodeUrl = "/storage/emulated/0/DCIM/Camera1/virtual.mp4";

    public static String liveUrl = "/storage/emulated/0/DCIM/Camera1/virtual.mp4";


    public static   String getIMEI(Context context){
        String imei = "";
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE);
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P)
            {
                imei = Settings.System.getString(
                        context.getContentResolver(), Settings.Secure.ANDROID_ID);//10.0以后获取不到UUID，用androidId来代表唯一性

            }
            else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
                imei = tm.getDeviceId();
            }
            else {
                Method method = tm.getClass().getMethod("getImei");
                imei = (String) method.invoke(tm);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imei;
    }

}
