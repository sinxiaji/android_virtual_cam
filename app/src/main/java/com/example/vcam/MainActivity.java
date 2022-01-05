package com.example.vcam;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends Activity {

    @SuppressLint("WorldReadableFiles")
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }


        TextView textView = findViewById(R.id.textView3);

        Button repo_button = findViewById(R.id.button);

        repo_button.setOnClickListener(new View.OnClickListener() {

            @Override

            public void onClick(View v) {

                Uri uri = Uri.parse("http://klive.onllk.com:8090/");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        final EditText editText = (EditText) findViewById(R.id.editText1);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = editText.getText().toString();
                textView.setText(text);
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });


        final Switch aSwitch = (Switch) findViewById(R.id.switch1);
        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b)
                {
                    textView.setText("1");

                }else
                {
                    textView.setText("0");
                }
            }
        });

        String httpUrl="http://klive.onllk.com:8090/api/video/getliveurl?devicecode=121";
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
                    textView.setText(resultData) ;
                } else {
                    textView.setText("Sorry,the content is null");//获取到的数据为空时显示
                }
            } catch (IOException e) {
                textView.setText (e.getMessage());
                //出现异常时，打印异常信息
            }
        } else {
            textView.setText ("url is null"); //当url为空时输出
        }
    }
}


