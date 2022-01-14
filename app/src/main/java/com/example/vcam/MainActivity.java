package com.example.vcam;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

import java.io.*;
import java.lang.reflect.Method;
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
        String txt="";
        if(txt=="")
        {
           String imei= VccHelper.getIMEI(this);
           txt=imei;
            VccHelper.saveDeviceCode(imei);
        }
        editText.setText(txt);
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
        try
        {
            String video_path = Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/";
            File uni_DCIM_path = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/");
            if (uni_DCIM_path.canWrite()) {
                File uni_Camera1_path = new File(video_path);
                if (!uni_Camera1_path.exists()) {
                    uni_Camera1_path.mkdir();
                }
            }
        }catch (Exception ex)
        {
            ex.printStackTrace();
        }


        ///创建虚拟文件确保播放可用
        File file = new File(VccHelper.fileUrl);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        final Switch aSwitch = (Switch) findViewById(R.id.switch1);
        File fileDis = new File(VccHelper.disable_file);
        if (!fileDis.exists())
        {
            aSwitch.setChecked(true);
        }

        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b)
                {
                    File file = new File(VccHelper.disable_file);
                    if (file.exists()) {

                        file.delete();
                    }
                    textView.setText("1");
                }else
                {
                    File file = new File(VccHelper.disable_file);
                    if (!file.exists()) {
                        try {
                            file.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    textView.setText("0");
                }
            }
        });

        final Switch aSwitch2 = (Switch) findViewById(R.id.switch2);
        File file2 = new File(VccHelper.no_silent_file);
        if (file2.exists())
        {
            aSwitch2.setChecked(true);
        }
        aSwitch2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b)
                {
                    File file = new File(VccHelper.no_silent_file);
                    if (!file.exists()) {
                        try {
                            file.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    textView.setText("1");

                }else
                {
                    File file = new File(VccHelper.no_silent_file);
                    if (file.exists()) {
                        file.delete();
                    }
                    textView.setText("0");
                }
            }
        });


        textView.setText("播放地址:"+VccHelper.GetPalyUrl(txt));
    }








    private void  saveDeviceCode(String deviceCod)
    {
        String fileName=VccHelper.video_path+"deviceCode.conf";
        String saveinfo = deviceCod.trim();
        FileOutputStream fos;
        try {
            fos = openFileOutput(fileName, MODE_WORLD_READABLE + MODE_WORLD_WRITEABLE);
            fos.write(saveinfo.getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(MainActivity.this, "数据保存成功", Toast.LENGTH_LONG).
                show();
    }

    private String  loadDeviceCode()
    {
        String get = "";
        try {
            String fileName=VccHelper.video_path+"deviceCode.conf";
            FileInputStream fis = openFileInput(fileName);
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            get = new String(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(MainActivity.this, "保存的数据是" + get,
                Toast.LENGTH_LONG).show();
        return  get;
    }

}


