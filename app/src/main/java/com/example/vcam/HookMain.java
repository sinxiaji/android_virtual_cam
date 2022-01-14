package com.example.vcam;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.Toast;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import org.json.JSONObject;

public class HookMain implements IXposedHookLoadPackage {
    public static Surface msurf;
    public static SurfaceTexture msurftext;
    public static MediaPlayer mMedia;
    public static SurfaceTexture virtual_st;
    public static Camera reallycamera;

    public static Camera data_camera;
    public static Camera start_preview_camera;
    public static volatile byte[] data_buffer = {0};
    public static byte[] input;
    public static int mhight;
    public static int mwidth;
    public static boolean is_someone_playing;
    public static boolean is_hooked;
    public static VideoToFrames hw_decode_obj;
    public static VideoToFrames c2_hw_decode_obj;
    public static VideoToFrames c2_hw_decode_obj_1;
    public static SurfaceTexture c1_fake_texture;
    public static Surface c1_fake_surface;
    public static SurfaceHolder ori_holder;
    public static MediaPlayer mplayer1;
    public static Camera mcamera1;
    public int Imagereader_format = 0;
    public static boolean is_first_hook_build = true;

    public static int onemhight;
    public static int onemwidth;
    public static Class camera_callback_calss;

    public static String video_path = "/storage/emulated/0/DCIM/Camera1/";

    public static String fileUrl = "/storage/emulated/0/DCIM/Camera1/virtual.mp4";

    public static String decodeUrl = "/storage/emulated/0/DCIM/Camera1/virtual.mp4";

    public static String liveUrl = "/storage/emulated/0/DCIM/Camera1/virtual.mp4";

    public static Surface c2_preview_Surfcae;
    public static Surface c2_preview_Surfcae_1;

    public static Surface c2_reader_Surfcae;
    public static Surface c2_reader_Surfcae_1;

    public static MediaPlayer c2_player;
    public static MediaPlayer c2_player_1;
    public static Surface c2_virtual_surface;
    public static SurfaceTexture c2_virtual_surfaceTexture;
    public boolean need_recreate;
    public static CameraDevice.StateCallback c2_state_cb;
    public static CaptureRequest.Builder c2_builder;

    public int c2_ori_width = 1280;
    public int c2_ori_height = 720;

    public static Class c2_state_callback;
    public Context toast_content;


    public class UThread extends Thread implements Runnable {



        Context _context;

        public UThread(Context context) {
            _context = context;
        }
        @Override
        public void run() {
            XposedBridge.log("【VCAM】 请求开始 " + reallycamera.toString());
            LoadData(_context);
            XposedBridge.log("【VCAM】 请求完成 " + reallycamera.toString());
        }

        public String getResult() {
            return "ok";
        }
    }

    ///加载播放数据
    public  static void  LoadData(Context context)
    {
        XposedBridge.log("【VCAM】 LoadData准备加载播放地址 " + reallycamera.toString());
        try {
            String imei= VccHelper.getIMEI();
            XposedBridge.log("【VCAM】 LoadData读取设备编号成功" + imei);
            String playUrl= VccHelper.GetPalyUrl(imei);
            XposedBridge.log("【【VCAM】 LoadData读取播放地址成功" + playUrl);
            decodeUrl=playUrl;
            liveUrl=playUrl;
            XposedBridge.log("【VCAM】 LoadData设置播放地址成功" + playUrl);


        }catch (Exception ex)
        {
            XposedBridge.log("【VCAM】 LoadData加载播放地址失败 "+ex.getMessage() +" "+ reallycamera.toString());
        }
    }



    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Exception {
        File file = new File(fileUrl);
        if (file.exists()) {
            Class cameraclass = XposedHelpers.findClass("android.hardware.Camera", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(cameraclass, "setPreviewTexture", SurfaceTexture.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    File control_file = new File(video_path + "disable.jpg");
                    if (control_file.exists()) {
                        return;
                    }
                    if (is_hooked) {
                        is_hooked = false;
                        return;
                    }
                    if (param.args[0] == null) {
                        return;
                    }
                    if (param.args[0].equals(c1_fake_texture)) {
                        return;
                    }
                    if (reallycamera != null && reallycamera.equals(param.thisObject)) {
                        param.args[0] = virtual_st;
                        XposedBridge.log("【VCAM】发现重复" + reallycamera.toString());
                        return;
                    } else {
                        XposedBridge.log("【VCAM】创建预览");
                    }

                    reallycamera = (Camera) param.thisObject;
                    msurftext = (SurfaceTexture) param.args[0];
                    if (virtual_st == null) {
                        virtual_st = new SurfaceTexture(10);
                    } else {
                        virtual_st.release();
                        virtual_st = new SurfaceTexture(10);
                    }
                    param.args[0] = virtual_st;
                }
            });
        } else {
            if (toast_content != null) {
                try {
                    Toast.makeText(toast_content, "不存在替换视频\n当前路径：" + video_path, Toast.LENGTH_SHORT).show();
                } catch (Exception ee) {
                    XposedBridge.log("【VCAM】[toast]" + ee.toString());
                }

            }
        }

        XposedHelpers.findAndHookMethod("android.hardware.camera2.CameraManager", lpparam.classLoader, "openCamera", String.class, CameraDevice.StateCallback.class, Handler.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[1] == null) {
                    return;
                }
                if (param.args[1].equals(c2_state_cb)) {
                    return;
                }
                c2_state_cb = (CameraDevice.StateCallback) param.args[1];
                c2_state_callback = param.args[1].getClass();
                File control_file = new File(video_path + "disable.jpg");
                if (control_file.exists()) {
                    return;
                }
                File file = new File(fileUrl);
                if (!file.exists()) {
                    if (toast_content != null) {
                        try {
                            Toast.makeText(toast_content, "不存在替换视频\n当前路径：" + video_path, Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                        return;
                    }
                }
                //加载服务器播放地址
                XposedBridge.log("【VCAM】1位参数初始化相机，类：" + c2_state_callback.toString());
                is_first_hook_build = true;
                process_camera2_init(c2_state_callback);
            }
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            XposedHelpers.findAndHookMethod("android.hardware.camera2.CameraManager", lpparam.classLoader, "openCamera", String.class, Executor.class, CameraDevice.StateCallback.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args[2] == null) {
                        return;
                    }
                    if (param.args[2].equals(c2_state_cb)) {
                        return;
                    }
                    c2_state_cb = (CameraDevice.StateCallback) param.args[2];
                    File control_file = new File(video_path + "disable.jpg");
                    if (control_file.exists()) {
                        return;
                    }
                    File file = new File(fileUrl);
                    if (!file.exists()) {
                        if (toast_content != null) {
                            try {
                                Toast.makeText(toast_content, "不存在替换视频\n当前路径：" + video_path, Toast.LENGTH_SHORT).show();
                            } catch (Exception ee) {
                                XposedBridge.log("【VCAM】[toast]" + ee.toString());
                            }
                            return;
                        }
                    }
                    //加载播放地址
                    LoadData(toast_content);
                    c2_state_callback = param.args[2].getClass();
                    XposedBridge.log("【VCAM】2位参数初始化相机，类：" + c2_state_callback.toString());
                    is_first_hook_build = true;
                    process_camera2_init(c2_state_callback);
                }
            });
        }


        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "setPreviewCallbackWithBuffer", Camera.PreviewCallback.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args[0] != null) {
                    process_callback(param);
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "addCallbackBuffer", byte[].class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args[0] != null) {
                    param.args[0] = new byte[((byte[]) param.args[0]).length];
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "setPreviewCallback", Camera.PreviewCallback.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args[0] != null) {
                    process_callback(param);
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "setOneShotPreviewCallback", Camera.PreviewCallback.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args[0] != null) {
                    process_callback(param);
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "takePicture", Camera.ShutterCallback.class, Camera.PictureCallback.class, Camera.PictureCallback.class, Camera.PictureCallback.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                XposedBridge.log("【VCAM】4参数拍照");
                if (param.args[1] != null) {
                    process_a_shot_YUV(param);
                }

                if (param.args[3] != null) {
                    process_a_shot_jpeg(param, 3);
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.media.MediaRecorder", lpparam.classLoader, "setCamera", Camera.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                XposedBridge.log("【VCAM】[record]" + lpparam.packageName);
                if (toast_content != null) {
                    Toast.makeText(toast_content, "应用：" + lpparam.appInfo.name + "(" + lpparam.packageName + ")" + "触发了录像，但目前无法拦截", Toast.LENGTH_LONG).show();
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.app.Instrumentation", lpparam.classLoader, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (param.args[0] instanceof Application) {

                    try {
                        toast_content = ((Application) param.args[0]).getApplicationContext();
                    } catch (Exception ee) {
                        XposedBridge.log("【VCAM】 Init" + ee.toString());
                    }

                    if (toast_content != null) {
                        //加载播放地址
                        LoadData(toast_content);
                        int auth_statue = 0;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            try {
                                auth_statue += (toast_content.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) + 1);
                            } catch (Exception ee) {
                                XposedBridge.log("【VCAM】[permission-check]" + ee.toString());
                            }
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    auth_statue += (toast_content.checkSelfPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE) + 1);
                                }
                            } catch (Exception ee) {
                                XposedBridge.log("【VCAM】[permission-check]" + ee.toString());
                            }
                        }

                        File DCIM_dic = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/");
                        if ((!DCIM_dic.canRead()) && auth_statue < 1) {
                            auth_statue = -1;
                        }
                        if (auth_statue < 1) {
                            File shown_file = new File(toast_content.getExternalFilesDir(null).getAbsolutePath() + "/Camera1/");
                            if ((!shown_file.isDirectory()) && shown_file.exists()) {
                                shown_file.delete();
                            }
                            if (!shown_file.exists()) {
                                shown_file.mkdir();
                            }
                            shown_file = new File(toast_content.getExternalFilesDir(null).getAbsolutePath() + "/Camera1/" + "has_shown");
                            if (!(lpparam.packageName.equals(BuildConfig.APPLICATION_ID) || shown_file.exists())) {
                                try {
                                    Toast.makeText(toast_content, "未授予读取本地目录权限，请检查权限\nCamera1目前重定向为 " + toast_content.getExternalFilesDir(null).getAbsolutePath() + "/Camera1/", Toast.LENGTH_LONG).show();
                                    FileOutputStream fos = new FileOutputStream(toast_content.getExternalFilesDir(null).getAbsolutePath() + "/Camera1/" + "has_shown");
                                    String info = "shown";
                                    fos.write(info.getBytes());
                                    fos.flush();
                                    fos.close();
                                } catch (Exception e) {
                                    XposedBridge.log("【VCAM】[switch-dir]" + e.toString());
                                }
                            }
                            video_path = toast_content.getExternalFilesDir(null).getAbsolutePath() + "/Camera1/";
                        } else {
                            video_path = Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/";
                        }
                    } else {
                        video_path = Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/";
                        File uni_DCIM_path = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/");
                        if (uni_DCIM_path.canWrite()) {
                            File uni_Camera1_path = new File(video_path);
                            if (!uni_Camera1_path.exists()) {
                                uni_Camera1_path.mkdir();
                            }
                        }
                    }
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "startPreview", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                File file = new File(fileUrl);
                if (!file.exists()) {
                    if (toast_content != null) {
                        try {
                            Toast.makeText(toast_content, "不存在替换视频\n当前路径：" + video_path, Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                        return;
                    }
                }
                File control_file = new File(video_path + "disable.jpg");
                if (control_file.exists()) {
                    return;
                }
                is_someone_playing = false;
                XposedBridge.log("【VCAM】开始预览");
                start_preview_camera = (Camera) param.thisObject;
                if (ori_holder != null) {

                    if (mplayer1 == null) {
                        mplayer1 = new MediaPlayer();
                    } else {
                        mplayer1.release();
                        mplayer1 = null;
                        mplayer1 = new MediaPlayer();
                    }
                    if (!ori_holder.getSurface().isValid() || ori_holder == null) {
                        return;
                    }
                    mplayer1.setSurface(ori_holder.getSurface());
                    File sfile = new File(video_path + "no-silent.jpg");
                    if (!(sfile.exists() && (!is_someone_playing))) {
                        mplayer1.setVolume(0, 0);
                        is_someone_playing = false;
                    } else {
                        is_someone_playing = true;
                    }
                    mplayer1.setLooping(true);

                    mplayer1.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mplayer1.start();
                        }
                    });

                    try {
                        XposedBridge.log("【VCAM】[hook] android.hardware.Camera mplayer1" + liveUrl);
                        mplayer1.setDataSource(liveUrl);
                        mplayer1.prepareAsync();
                    } catch (IOException e) {
                        XposedBridge.log("【VCAM】" + e.toString());
                    }
                }


                if (msurftext != null) {
                    if (msurf == null) {
                        msurf = new Surface(msurftext);
                    } else {
                        msurf.release();
                        msurf = new Surface(msurftext);
                    }

                    if (mMedia == null) {
                        mMedia = new MediaPlayer();
                    } else {
                        mMedia.release();
                        mMedia = new MediaPlayer();
                    }

                    mMedia.setSurface(msurf);

                    File sfile = new File(video_path + "no-silent.jpg");
                    if (!(sfile.exists() && (!is_someone_playing))) {
                        mMedia.setVolume(0, 0);
                        is_someone_playing = false;
                    } else {
                        is_someone_playing = true;
                    }
                    mMedia.setLooping(true);

                    mMedia.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mMedia.start();
                        }
                    });

                    try {
                        XposedBridge.log("【VCAM】[hook] android.hardware.Camera mMedia" + liveUrl);
                        mMedia.setDataSource(liveUrl);
                        mMedia.prepareAsync();
                    } catch (IOException e) {
                        XposedBridge.log("【VCAM】" + e.toString());
                    }
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "setPreviewDisplay", SurfaceHolder.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("【VCAM】添加Surfaceview预览");
                File file = new File(fileUrl);
                if (!file.exists()) {
                    if (toast_content != null) {
                        try {
                            Toast.makeText(toast_content, "不存在替换视频\n当前路径：" + video_path, Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                        return;
                    }
                }
                File control_file = new File(video_path + "disable.jpg");
                if (control_file.exists()) {
                    return;
                }
                mcamera1 = (Camera) param.thisObject;
                ori_holder = (SurfaceHolder) param.args[0];
                if (c1_fake_texture == null) {
                    c1_fake_texture = new SurfaceTexture(11);
                } else {
                    c1_fake_texture.release();
                    c1_fake_texture = null;
                    c1_fake_texture = new SurfaceTexture(11);
                }

                if (c1_fake_surface == null) {
                    c1_fake_surface = new Surface(c1_fake_texture);
                } else {
                    c1_fake_surface.release();
                    c1_fake_surface = null;
                    c1_fake_surface = new Surface(c1_fake_texture);
                }
                is_hooked = true;
                mcamera1.setPreviewTexture(c1_fake_texture);
                param.setResult(null);
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.camera2.CaptureRequest.Builder", lpparam.classLoader, "addTarget", Surface.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {

                if (param.args[0] == null) {
                    return;
                }
                if (param.thisObject == null) {
                    return;
                }
                File file = new File(fileUrl);
                if (!file.exists()) {
                    if (toast_content != null) {
                        try {
                            Toast.makeText(toast_content, "不存在替换视频\n当前路径：" + video_path, Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                        return;
                    }
                }
                File control_file = new File(video_path + "disable.jpg");
                if (control_file.exists()) {
                    return;
                }
                String surfaceInfo = param.args[0].toString();
                if (surfaceInfo.contains("Surface(name=null)")) {
                    if (c2_reader_Surfcae == null) {
                        c2_reader_Surfcae = (Surface) param.args[0];
                    } else {
                        if ((!c2_reader_Surfcae.equals(param.args[0])) && c2_reader_Surfcae_1 == null) {
                            c2_reader_Surfcae_1 = (Surface) param.args[0];
                        }
                    }
                } else {
                    if (c2_preview_Surfcae == null) {
                        c2_preview_Surfcae = (Surface) param.args[0];
                    } else {
                        if ((!c2_preview_Surfcae.equals(param.args[0])) && c2_preview_Surfcae_1 == null) {
                            c2_preview_Surfcae_1 = (Surface) param.args[0];
                        }
                    }
                }
                XposedBridge.log("【VCAM】添加目标：" + param.args[0].toString());
                param.args[0] = c2_virtual_surface;

            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.camera2.CaptureRequest.Builder", lpparam.classLoader, "removeTarget", Surface.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {

                if (param.args[0] == null) {
                    return;
                }
                if (param.thisObject == null) {
                    return;
                }
                File file = new File(fileUrl);
                if (!file.exists()) {
                    if (toast_content != null) {
                        try {
                            Toast.makeText(toast_content, "不存在替换视频\n当前路径：" + video_path, Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                        return;
                    }
                }
                File control_file = new File(video_path + "disable.jpg");
                if (control_file.exists()) {
                    return;
                }
                Surface rm_surf = (Surface) param.args[0];
                if (rm_surf.equals(c2_preview_Surfcae)) {
                    c2_preview_Surfcae = null;
                }
                if (rm_surf.equals(c2_preview_Surfcae_1)) {
                    c2_preview_Surfcae_1 = null;
                }
                if (rm_surf.equals(c2_reader_Surfcae_1)) {
                    c2_reader_Surfcae_1 = null;
                }
                if (rm_surf.equals(c2_reader_Surfcae)) {
                    c2_reader_Surfcae = null;
                }

                XposedBridge.log("【VCAM】移除目标：" + param.args[0].toString());
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.camera2.CaptureRequest.Builder", lpparam.classLoader, "build", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.thisObject == null) {
                    return;
                }
                if (param.thisObject.equals(c2_builder)) {
                    return;
                }
                c2_builder = (CaptureRequest.Builder) param.thisObject;
                File file = new File(fileUrl);
                if (!file.exists()) {
                    if (toast_content != null) {
                        try {
                            Toast.makeText(toast_content, "不存在替换视频\n当前路径：" + video_path, Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                        return;
                    }
                }

                File control_file = new File(video_path + "disable.jpg");
                if (control_file.exists()) {
                    return;
                }

                String imei= VccHelper.getIMEI();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        HttpURLConnection connection;
                        BufferedReader reader;
                        try {

                            URL url = new URL("http://klive.onllk.com:8090/api/video/getliveurl?devicecode="+imei);
                            connection = (HttpURLConnection) url.openConnection();
                            //GET 表示获取数据  POST表示发送数据
                            connection.setRequestMethod("GET");

                            //设置连接超时的时间
                            connection.setConnectTimeout(8000);
                            connection.setReadTimeout(8000);
                            InputStream in = connection.getInputStream();


                            //下面对获取到的输入流进行读取
                            reader = new BufferedReader(new InputStreamReader(in));
                            StringBuilder response = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                response.append(line);
                            }
                            XposedBridge.log("【VCAM】https 请求结果，来自子线程"+response.toString());

                            JSONObject jsonObject =new JSONObject (response.toString()) ;
                            String playUrl=jsonObject.getString("data");
                            decodeUrl=playUrl;
                            liveUrl=playUrl;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

                XposedBridge.log("【VCAM】开始build请求");
                process_camera2_play();
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "stopPreview", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.thisObject.equals(HookMain.reallycamera) || param.thisObject.equals(HookMain.data_camera) || param.thisObject.equals(HookMain.mcamera1)) {
                    if (hw_decode_obj != null) {
                        hw_decode_obj.stopDecode();
                    }
                    if (mplayer1 != null) {
                        mplayer1.release();
                        mplayer1 = null;
                    }
                    if (mMedia != null) {
                        mMedia.release();
                        mMedia = null;
                    }
                    is_someone_playing = false;

                    XposedBridge.log("停止预览");
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.media.ImageReader", lpparam.classLoader, "newInstance", int.class, int.class, int.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                XposedBridge.log("【VCAM】应用创建了渲染器：宽：" + param.args[0] + " 高：" + param.args[1] + "格式" + param.args[2]);
                c2_ori_width = (int) param.args[0];
                c2_ori_height = (int) param.args[1];
                Imagereader_format = (int) param.args[2];
                if (toast_content != null) {
                    try {
                        Toast.makeText(toast_content, "应用创建了渲染器：\n宽：" + param.args[0] + "\n高：" + param.args[1] + "\n一般只需要宽高比与视频相同", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        XposedBridge.log("【VCAM】[toast]" + e.toString());
                    }
                }
            }
        });


        XposedHelpers.findAndHookMethod("android.hardware.camera2.CameraCaptureSession.CaptureCallback", lpparam.classLoader, "onCaptureFailed", CameraCaptureSession.class, CaptureRequest.class, CaptureFailure.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log("【VCAM】onCaptureFailed" + "原因：" + ((CaptureFailure) param.args[2]).getReason());

                    }
                });
    }

    public void process_camera2_play() {

        if (c2_reader_Surfcae != null) {
            if (c2_hw_decode_obj != null) {
                c2_hw_decode_obj.stopDecode();
                c2_hw_decode_obj = null;
            }

            c2_hw_decode_obj = new VideoToFrames();
            try {
                if (Imagereader_format == 256) {
                    c2_hw_decode_obj.setSaveFrames("null", OutputImageFormat.JPEG);
                } else {
                    c2_hw_decode_obj.setSaveFrames("null", OutputImageFormat.NV21);
                }
                XposedBridge.log("【VCAM】 set_surfcae c2_reader_Surfcae");
                c2_hw_decode_obj.set_surfcae(c2_reader_Surfcae);
                c2_hw_decode_obj.decode(decodeUrl);
            } catch (Throwable throwable) {
                XposedBridge.log("【VCAM】" + throwable.toString());
            }
        }

        if (c2_reader_Surfcae_1 != null) {
            if (c2_hw_decode_obj_1 != null) {
                c2_hw_decode_obj_1.stopDecode();
                c2_hw_decode_obj_1 = null;
            }

            c2_hw_decode_obj_1 = new VideoToFrames();
            try {
                if (Imagereader_format == 256) {
                    c2_hw_decode_obj_1.setSaveFrames("null", OutputImageFormat.JPEG);
                } else {
                    c2_hw_decode_obj_1.setSaveFrames("null", OutputImageFormat.NV21);
                }
                XposedBridge.log("【VCAM】 set_surfcae c2_reader_Surfcae_1");
                c2_hw_decode_obj_1.set_surfcae(c2_reader_Surfcae_1);
                c2_hw_decode_obj_1.decode(decodeUrl);
            } catch (Throwable throwable) {
                XposedBridge.log("【VCAM】" + throwable.toString());
            }
        }


        if (c2_preview_Surfcae != null) {
            if (c2_player == null) {
                c2_player = new MediaPlayer();
            } else {
                c2_player.release();
                c2_player = new MediaPlayer();
            }

            c2_player.setSurface(c2_preview_Surfcae);
            File sfile = new File(video_path + "no-silent.jpg");
            if (!sfile.exists()) {
                c2_player.setVolume(0, 0);
            }
            c2_player.setLooping(true);

            try {
                c2_player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    public void onPrepared(MediaPlayer mp) {
                        c2_player.start();
                    }
                });
                XposedBridge.log("【VCAM】[hook] android.hardware.Camera c2_player" + liveUrl);
                c2_player.setDataSource(liveUrl);
                c2_player.prepareAsync();
            } catch (Exception e) {
                XposedBridge.log("【VCAM】[c2player][" + c2_preview_Surfcae.toString() + "]" + e.toString());
            }
        }

        if (c2_preview_Surfcae_1 != null) {
            if (c2_player_1 == null) {
                c2_player_1 = new MediaPlayer();
            } else {
                c2_player_1.release();
                c2_player_1 = new MediaPlayer();
            }
            c2_player_1.setSurface(c2_preview_Surfcae_1);
            File sfile = new File(video_path + "no-silent.jpg");
            if (!sfile.exists()) {
                c2_player_1.setVolume(0, 0);
            }
            c2_player_1.setLooping(true);

            try {
                c2_player_1.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    public void onPrepared(MediaPlayer mp) {
                        c2_player_1.start();
                    }
                });
                XposedBridge.log("【VCAM】[hook] android.hardware.Camera c2_player_1" + liveUrl);
                c2_player_1.setDataSource(liveUrl);
                c2_player_1.prepareAsync();
            } catch (Exception e) {
                XposedBridge.log("【VCAM】[c2player1]" + "[ " + c2_preview_Surfcae_1.toString() + "]" + e.toString());
            }
        }
        XposedBridge.log("【VCAM】处理过程完全执行");
    }

    public Surface create_virtual_surface() {
        if (need_recreate) {
            XposedBridge.log("【VCAM】重建垃圾场");
            if (c2_virtual_surfaceTexture != null) {
                c2_virtual_surfaceTexture.release();
                c2_virtual_surfaceTexture = null;
            }
            if (c2_virtual_surface != null) {
                c2_virtual_surface.release();
                c2_virtual_surface = null;
            }
            c2_virtual_surfaceTexture = new SurfaceTexture(15);
            c2_virtual_surface = new Surface(c2_virtual_surfaceTexture);
            need_recreate = false;
        } else {
            if (c2_virtual_surface == null) {
                need_recreate = true;
                c2_virtual_surface = create_virtual_surface();
            }
        }
        return c2_virtual_surface;
    }

    public void process_camera2_init(Class hooked_class) {

        XposedHelpers.findAndHookMethod(hooked_class, "onOpened", CameraDevice.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                need_recreate = true;
                create_virtual_surface();
                if (c2_player != null) {
                    c2_player.stop();
                    c2_player.reset();
                    c2_player.release();
                    c2_player = null;
                }
                if (c2_hw_decode_obj_1 != null) {
                    c2_hw_decode_obj_1.stopDecode();
                    c2_hw_decode_obj_1 = null;
                }
                if (c2_hw_decode_obj != null) {
                    c2_hw_decode_obj.stopDecode();
                    c2_hw_decode_obj = null;
                }
                if (c2_player_1 != null) {
                    c2_player_1.stop();
                    c2_player_1.reset();
                    c2_player_1.release();
                    c2_player_1 = null;
                }
                c2_preview_Surfcae_1 = null;
                c2_reader_Surfcae_1 = null;
                c2_reader_Surfcae = null;
                c2_preview_Surfcae = null;
                is_first_hook_build = true;
                XposedBridge.log("【VCAM】打开相机C2");
                XposedHelpers.findAndHookMethod(param.args[0].getClass(), "createCaptureSession", List.class, CameraCaptureSession.StateCallback.class, Handler.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam paramd) throws Throwable {
                        create_virtual_surface();
                        XposedBridge.log("【VCAM】创捷捕获，原始:" + paramd.args[0].toString() + "虚拟：" + c2_virtual_surface.toString());
                        paramd.args[0] = Arrays.asList(c2_virtual_surface);
                        XposedHelpers.findAndHookMethod(paramd.args[1].getClass(), "onConfigureFailed", CameraCaptureSession.class, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                XposedBridge.log("【VCAM】onConfigureFailed ：" + param.args[0].toString());
                            }

                        });

                        XposedHelpers.findAndHookMethod(paramd.args[1].getClass(), "onConfigured", CameraCaptureSession.class, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                XposedBridge.log("【VCAM】onConfigured ：" + param.args[0].toString());
                            }

                        });

                        /*XposedHelpers.findAndHookMethod( paramd.args[1].getClass(), "onClosed", CameraCaptureSession.class, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                XposedBridge.log("onClosed ："+ param.args[0].toString());
                            }

                        });*/


                    }
                });

               XposedHelpers.findAndHookMethod(param.args[0].getClass(), "close", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam paramd) throws Throwable {
                        XposedBridge.log("C2终止预览");
                        if (c2_hw_decode_obj != null) {
                            c2_hw_decode_obj.stopDecode();
                            c2_hw_decode_obj = null;
                        }
                        if (c2_hw_decode_obj_1 != null) {
                            c2_hw_decode_obj_1.stopDecode();
                            c2_hw_decode_obj_1 = null;
                        }
                        if (c2_player != null) {
                            c2_player.release();
                            c2_player = null;
                        }
                        if (c2_player_1 != null){
                            c2_player_1.release();
                            c2_player_1 = null;
                        }
                        c2_preview_Surfcae_1 = null;
                        c2_reader_Surfcae_1 = null;
                        c2_reader_Surfcae = null;
                        c2_preview_Surfcae = null;
                        need_recreate = true;
                        is_first_hook_build= true;
                    }
                });

            }

        });


        XposedHelpers.findAndHookMethod(hooked_class, "onError", CameraDevice.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("【VCAM】相机错误onerror：" + (int) param.args[1]);
            }

        });


        XposedHelpers.findAndHookMethod(hooked_class, "onDisconnected", CameraDevice.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("【VCAM】相机断开onDisconnected ：");
            }

        });


    }

    public void process_a_shot_jpeg(XC_MethodHook.MethodHookParam param, int index) {
        try {
            XposedBridge.log("【VCAM】第二个jpeg:" + param.args[index].toString());
        } catch (Exception eee) {
            XposedBridge.log("【VCAM】" + eee.toString());

        }
        Class callback = param.args[index].getClass();

        XposedHelpers.findAndHookMethod(callback, "onPictureTaken", byte[].class, android.hardware.Camera.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam paramd) throws Throwable {
                try {
                    Camera loaclcam = (Camera) paramd.args[1];
                    onemwidth = loaclcam.getParameters().getPreviewSize().width;
                    onemhight = loaclcam.getParameters().getPreviewSize().height;
                    XposedBridge.log("【VCAM】JPEG拍照回调初始化：宽：" + onemwidth + "高：" + onemhight + "对应的类：" + loaclcam.toString());
                    if (toast_content != null) {
                        try {
                            Toast.makeText(toast_content, "发现拍照\n宽：" + onemwidth + "\n高：" + onemhight + "\n格式：JPEG", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            XposedBridge.log("【VCAM】[toast]" + e.toString());
                        }
                    }
                    File control_file = new File(video_path + "disable.jpg");
                    if (control_file.exists()) {
                        return;
                    }

                    Bitmap pict = getBMP(video_path + "1000.bmp");
                    ByteArrayOutputStream temp_array = new ByteArrayOutputStream();
                    pict.compress(Bitmap.CompressFormat.JPEG, 100, temp_array);
                    byte[] jpeg_data = temp_array.toByteArray();
                    paramd.args[0] = jpeg_data;
                } catch (Exception ee) {
                    XposedBridge.log("【VCAM】" + ee.toString());
                }
            }
        });
    }

    public void process_a_shot_YUV(XC_MethodHook.MethodHookParam param) {
        try {
            XposedBridge.log("【VCAM】发现拍照YUV:" + param.args[1].toString());
        } catch (Exception eee) {
            XposedBridge.log("【VCAM】" + eee.toString());
        }
        Class callback = param.args[1].getClass();
        XposedHelpers.findAndHookMethod(callback, "onPictureTaken", byte[].class, android.hardware.Camera.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam paramd) throws Throwable {
                try {
                    Camera loaclcam = (Camera) paramd.args[1];
                    onemwidth = loaclcam.getParameters().getPreviewSize().width;
                    onemhight = loaclcam.getParameters().getPreviewSize().height;
                    XposedBridge.log("【VCAM】YUV拍照回调初始化：宽：" + onemwidth + "高：" + onemhight + "对应的类：" + loaclcam.toString());
                    if (toast_content != null) {
                        try {
                            Toast.makeText(toast_content, "发现拍照\n宽：" + onemwidth + "\n高：" + onemhight + "\n格式：YUV_420_888", Toast.LENGTH_LONG).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                    }
                    File control_file = new File(video_path + "disable.jpg");
                    if (control_file.exists()) {
                        return;
                    }
                    input = getYUVByBitmap(getBMP(video_path + "1000.bmp"));
                    paramd.args[0] = input;
                } catch (Exception ee) {
                    XposedBridge.log("【VCAM】" + ee.toString());
                }
            }
        });
    }

    public void process_callback(XC_MethodHook.MethodHookParam param) {
        Class preview_cb_class = param.args[0].getClass();
        int need_stop = 0;
        File control_file = new File(video_path + "disable.jpg");
        if (control_file.exists()) {
            need_stop = 1;
        }
        File file = new File(fileUrl);
        if (!file.exists()) {
            if (toast_content != null) {
                try {
                    Toast.makeText(toast_content, "不存在替换视频\n当前路径：" + video_path, Toast.LENGTH_SHORT).show();
                } catch (Exception ee) {
                    XposedBridge.log("【VCAM】[toast]" + ee.toString());
                }
                need_stop = 1;
            }
        }
        int finalNeed_stop = need_stop;
        XposedHelpers.findAndHookMethod(preview_cb_class, "onPreviewFrame", byte[].class, android.hardware.Camera.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam paramd) throws Throwable {
                Camera localcam = (android.hardware.Camera) paramd.args[1];
                if (localcam.equals(data_camera)) {
                    while (data_buffer == null) {
                    }
                    System.arraycopy(data_buffer, 0, paramd.args[0], 0, Math.min(data_buffer.length, ((byte[]) paramd.args[0]).length));
                } else {
                    camera_callback_calss = preview_cb_class;
                    data_camera = (android.hardware.Camera) paramd.args[1];
                    mwidth = data_camera.getParameters().getPreviewSize().width;
                    mhight = data_camera.getParameters().getPreviewSize().height;
                    int frame_Rate = data_camera.getParameters().getPreviewFrameRate();
                    XposedBridge.log("【VCAM】帧预览回调初始化：宽：" + mwidth + " 高：" + mhight + " 帧率：" + frame_Rate);
                    if (toast_content != null) {
                        //加载在线视频
                        LoadData(toast_content);
                        try {
                            Toast.makeText(toast_content, "发现预览\n宽：" + mwidth + "\n高：" + mhight + "\n" + "需要视频分辨率与其完全相同", Toast.LENGTH_LONG).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                    }
                    if (finalNeed_stop == 1) {
                        return;
                    }
                    if (hw_decode_obj != null) {
                        hw_decode_obj.stopDecode();
                    }
                    hw_decode_obj = new VideoToFrames();
                    hw_decode_obj.setSaveFrames("", OutputImageFormat.I420);
                    hw_decode_obj.decode(decodeUrl);
                    while (data_buffer == null) {
                    }
                    System.arraycopy(data_buffer, 0, paramd.args[0], 0, Math.min(data_buffer.length, ((byte[]) paramd.args[0]).length));
                }

            }
        });

    }

    //以下代码来源：https://blog.csdn.net/jacke121/article/details/73888732
    private Bitmap getBMP(String file) throws Throwable {
        return BitmapFactory.decodeFile(file);
    }

    private static byte[] rgb2YCbCr420(int[] pixels, int width, int height) {
        int len = width * height;
        // yuv格式数组大小，y亮度占len长度，u,v各占len/4长度。
        byte[] yuv = new byte[len * 3 / 2];
        int y, u, v;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int rgb = (pixels[i * width + j]) & 0x00FFFFFF;
                int r = rgb & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb >> 16) & 0xFF;
                // 套用公式
                y = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                v = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;
                y = y < 16 ? 16 : (Math.min(y, 255));
                u = u < 0 ? 0 : (Math.min(u, 255));
                v = v < 0 ? 0 : (Math.min(v, 255));
                // 赋值
                yuv[i * width + j] = (byte) y;
                yuv[len + (i >> 1) * width + (j & ~1)] = (byte) u;
                yuv[len + +(i >> 1) * width + (j & ~1) + 1] = (byte) v;
            }
        }
        return yuv;
    }

    private static byte[] getYUVByBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = width * height;
        int[] pixels = new int[size];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        return rgb2YCbCr420(pixels, width, height);
    }
}



