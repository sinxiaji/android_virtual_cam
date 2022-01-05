package com.example.vcam;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.*;
import android.util.Log;
import android.view.Surface;
import de.robv.android.xposed.XposedBridge;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

 //以下代码修改自 https://github.com/zhantong/Android-VideoToImages
 public class VideoToFrames implements Runnable {
    private static final String TAG = "VideoToFrames";
    private static final boolean VERBOSE = false;
    private static final long DEFAULT_TIMEOUT_US = 10000;

    private static final int COLOR_FormatI420 = 1;
    private static final int COLOR_FormatNV21 = 2;


    private final int decodeColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;

    private LinkedBlockingQueue<byte[]> mQueue;
    private OutputImageFormat outputImageFormat;
    private boolean stopDecode = false;

    private String videoFilePath;
    private Throwable throwable;
    private Thread childThread;
    ///播放Surface
    private Surface play_surf;

    private Callback callback;

    public interface Callback {
        void onFinishDecode();

        void onDecodeFrame(int index);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setEnqueue(LinkedBlockingQueue<byte[]> queue) {
        mQueue = queue;
    }

    //设置输出位置，没啥用
    public void setSaveFrames(String dir, OutputImageFormat imageFormat) throws IOException {
        outputImageFormat = imageFormat;

    }

    public void set_surfcae(Surface player_surface) {
        if (player_surface != null) {
            play_surf = player_surface;
        }
    }

    public void stopDecode() {
        stopDecode = true;
    }

    public void decode(String videoFilePath) throws Throwable {
        this.videoFilePath = videoFilePath;
        if (childThread == null) {
            childThread = new Thread(this, "decode");
            childThread.start();
            if (throwable != null) {
                throw throwable;
            }
        }
    }

    public void run() {
        try {
            videoDecode(videoFilePath);
        } catch (Throwable t) {
            throwable = t;
        }
    }

    @SuppressLint("WrongConstant")
    public void videoDecode(String videoFilePath) throws IOException {
        XposedBridge.log("【VCAM】【decoder】开始解码");
        MediaExtractor extractor = null;
        MediaCodec decoder = null;
        try {
//            File videoFile = new File(videoFilePath);
            extractor = new MediaExtractor();
            extractor.setDataSource(videoFilePath);
            int trackIndex = selectTrack(extractor);
            XposedBridge.log("【VCAM】【decoder】trackIndex："+trackIndex);
            if (trackIndex < 0) {
                XposedBridge.log("【VCAM】【decoder】No video track found in " + videoFilePath);
                throw new RuntimeException("No video track found in " + videoFilePath);
            }
            extractor.selectTrack(trackIndex);
            MediaFormat mediaFormat = extractor.getTrackFormat(trackIndex);
            XposedBridge.log("【VCAM】【decoder】mediaFormat" + mediaFormat.toString());
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            decoder = MediaCodec.createDecoderByType(mime);
            showSupportedColorFormat(decoder.getCodecInfo().getCapabilitiesForType(mime));
            XposedBridge.log("【VCAM】【decoder】1");
            if (isColorFormatSupported(decodeColorFormat, decoder.getCodecInfo().getCapabilitiesForType(mime))) {
                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, decodeColorFormat);
                XposedBridge.log("【VCAM】【decoder】2");
                XposedBridge.log("【VCAM】【decoder】set decode color format to type " + decodeColorFormat);
            } else {
                Log.i(TAG, "unable to set decode color format, color format type " + decodeColorFormat + " not supported");
                XposedBridge.log("【VCAM】【decoder】unable to set decode color format, color format type " + decodeColorFormat + " not supported");
            }
            XposedBridge.log("【VCAM】【decoder】3");
            decodeFramesToImage(decoder, extractor, mediaFormat);
            decoder.stop();
            while (!stopDecode) {
                extractor.seekTo(0, 0);
                decodeFramesToImage(decoder, extractor, mediaFormat);
                decoder.stop();
            }
        }
        catch (Exception ex)
        {
            XposedBridge.log("【VCAM】【decoder】解码失败"+ex.getMessage());
        }

        finally {
            if (decoder != null) {
                decoder.stop();
                decoder.release();
                decoder = null;
            }
            if (extractor != null) {
                extractor.release();
                extractor = null;
            }
        }
    }



    public static byte[] NV21_rotate_to_270(byte[] nv21_data, int width, int height)
    {

        byte [] nv21_rotated = new byte[width*width*3/2];

        int y_size = width * height;
        int i = 0;

        // Rotate the Y luma
        for (int x = width - 1; x >= 0; x--)
        {
            int offset = 0;
            for (int y = 0; y < height; y++)
            {
                nv21_rotated[i] = nv21_data[offset + x];
                i++;
                offset += width;
            }
        }

        // Rotate the U and V color components
        i = y_size;
        for (int x = width - 1; x > 0; x = x - 2)
        {
            int offset = y_size;
            for (int y = 0; y < height / 2; y++)
            {
                nv21_rotated[i] = nv21_data[offset + (x - 1)];
                i++;
                nv21_rotated[i] = nv21_data[offset + x];
                i++;
                offset += width;
            }
        }
        return nv21_rotated;
    }

    public static byte[] NV21_rotate_to_180(byte[] nv21_data, int width, int height)
    {
        byte [] nv21_rotated = new byte[width*width*3/2];
        int y_size = width * height;
        int buffser_size = y_size * 3 / 2;
        int i = 0;
        int count = 0;

        for (i = y_size - 1; i >= 0; i--)
        {
            nv21_rotated[count] = nv21_data[i];
            count++;
        }

        for (i = buffser_size - 1; i >= y_size; i -= 2)
        {
            nv21_rotated[count++] = nv21_data[i - 1];
            nv21_rotated[count++] = nv21_data[i];
        }
        return nv21_rotated;
    }

    public static byte[] NV21_rotate_to_90(byte[] nv21_data,int width, int height)
    {
        byte [] nv21_rotated = new byte[width*width*3/2];
        int y_size = width * height;
        int buffser_size = y_size * 3 / 2;

        // Rotate the Y luma
        int i = 0;
        int startPos = (height - 1)*width;
        for (int x = 0; x < width; x++)
        {
            int offset = startPos;
            for (int y = height - 1; y >= 0; y--)
            {
                nv21_rotated[i] = nv21_data[offset + x];
                i++;
                offset -= width;
            }
        }

        // Rotate the U and V color components
        i = buffser_size - 1;
        for (int x = width - 1; x > 0; x = x - 2)
        {
            int offset = y_size;
            for (int y = 0; y < height / 2; y++)
            {
                nv21_rotated[i] = nv21_data[offset + x];
                i--;
                nv21_rotated[i] = nv21_data[offset + (x - 1)];
                i--;
                offset += width;
            }
        }
        return nv21_rotated;
    }

    /**
     * 此处为顺时针旋转旋转90度
     * @param data 旋转前的数据
     * @param imageWidth 旋转前数据的宽
     * @param imageHeight 旋转前数据的高
     * @return 旋转后的数据
     */
    private byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight)
    {
        byte [] yuv = new byte[imageWidth*imageHeight*3/2];
        // Rotate the Y luma
        int i = 0;
        for(int x = 0;x < imageWidth;x++)
        {
            for(int y = imageHeight-1;y >= 0;y--)
            {
                yuv[i] = data[y*imageWidth+x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth*imageHeight*3/2-1;
        for(int x = imageWidth-1;x > 0;x=x-2)
        {
            for(int y = 0;y < imageHeight/2;y++)
            {
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+x];
                i--;
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+(x-1)];
                i--;
            }
        }
        return yuv;
    }

     public static byte[] rotateYUV420SP(byte[] src, int width, int height) {
         byte[] dst = new byte[src.length];
         int wh = width * height;
         //旋转Y
         int k = 0;
         for (int i = 0; i < width; i++) {
             for (int j = height - 1; j >= 0; j--) {
                 dst[k] = src[width * j + i];
                 k++;
             }
         }

         int halfWidth = width / 2;
         int halfHeight = height / 2;
         for (int colIndex = 0; colIndex < halfWidth; colIndex++) {
             for (int rowIndex = halfHeight - 1; rowIndex >= 0; rowIndex--) {
                 int index = (halfWidth * rowIndex + colIndex) * 2;
                 dst[k] = src[wh + index];
                 k++;
                 dst[k] = src[wh + index + 1];
                 k++;
             }
         }
         return dst;
     }

    private void showSupportedColorFormat(MediaCodecInfo.CodecCapabilities caps) {
        System.out.print("supported color format: ");
        for (int c : caps.colorFormats) {
            System.out.print(c + "\t");
        }
        System.out.println();
    }

    private boolean isColorFormatSupported(int colorFormat, MediaCodecInfo.CodecCapabilities caps) {
        for (int c : caps.colorFormats) {
            if (c == colorFormat) {
                return true;
            }
        }
        return false;
    }

    public static byte[] I420Tonv21(byte[] i420bytes, int width, int height) {
        byte[] nv21bytes = new byte[i420bytes.length];
        int total = width * height; //Y数据的长度
        int nLen = total / 4;  //U、V数据的长度
        System.arraycopy(i420bytes, 0, nv21bytes, 0, total);

        for (int i = 0; i < nLen; i++) {
            byte u = i420bytes[total + i];
            byte v = i420bytes[total + nLen + i];

            nv21bytes[total + i * 2] = v;
            nv21bytes[total + i * 2 + 1] = u;
        }
        return nv21bytes;
    }

    private void decodeFramesToImage(MediaCodec decoder, MediaExtractor extractor, MediaFormat mediaFormat) {
        boolean is_first = false;
        long startWhen = 0;
        XposedBridge.log("【VCAM】【decodeFramesToImage】1");
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        XposedBridge.log("【VCAM】【decodeFramesToImage】2");
        decoder.configure(mediaFormat, play_surf, null, 0);
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        decoder.start();
        XposedBridge.log("【VCAM】【decodeFramesToImage】3");
        final int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
        final int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
        XposedBridge.log("【VCAM】【decodeFramesToImage】4  width："+width+"h:"+height);
        int outputFrameCount = 0;
        while (!sawOutputEOS && !stopDecode) {
            if (!sawInputEOS) {
                int inputBufferId = decoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
                if (inputBufferId >= 0) {
                    ByteBuffer inputBuffer = decoder.getInputBuffer(inputBufferId);
                    int sampleSize = extractor.readSampleData(inputBuffer, 0);
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inputBufferId, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        sawInputEOS = true;
                    } else {
                        long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTimeUs, 0);
                        extractor.advance();
                    }
                }
            }
            XposedBridge.log("【VCAM】【decodeFramesToImage】5");
            int outputBufferId = decoder.dequeueOutputBuffer(info, DEFAULT_TIMEOUT_US);
            XposedBridge.log("【VCAM】【decodeFramesToImage】6");
            if (outputBufferId >= 0) {
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    XposedBridge.log("【VCAM】【decodeFramesToImage】 end");
                    sawOutputEOS = true;
                }
                XposedBridge.log("【VCAM】【decodeFramesToImage】7");
                boolean doRender = (info.size != 0);
                if (doRender) {
                    outputFrameCount++;
                    if (callback != null) {
                        callback.onDecodeFrame(outputFrameCount);
                    }
                    if (!is_first) {
                        startWhen = System.currentTimeMillis();
                        is_first = true;
                    }
                    XposedBridge.log("【VCAM】【decodeFramesToImage】8");
                    if (play_surf == null) {
                        XposedBridge.log("【VCAM】【decodeFramesToImage】9");
                        Image image = decoder.getOutputImage(outputBufferId);
                        XposedBridge.log("【VCAM】【decodeFramesToImage】10");
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        XposedBridge.log("【VCAM】【decodeFramesToImage】11");
                        byte[] arr = new byte[buffer.remaining()];
                        XposedBridge.log("【VCAM】【decodeFramesToImage】12 arr"+arr.length);
                        buffer.get(arr);
                        if (mQueue != null) {
                            try {
                                mQueue.put(arr);
                            } catch (InterruptedException e) {
                                XposedBridge.log("【VCAM】" + e.toString());
                            }
                        }
                        XposedBridge.log("【VCAM】【decodeFramesToImage】13 ");
                        if (outputImageFormat != null) {
                            XposedBridge.log("【VCAM】【decodeFramesToImage】14 ");
                            XposedBridge.log("【VCAM】【decodeFramesToImage】14 getFormat()"+image.getFormat());

//                            I420Tonv21

                            byte[] buffer1=getDataFromImage(image, COLOR_FormatI420);
                            XposedBridge.log("【VCAM】【decodeFramesToImage】15 ");
//                            buffer1=rotateYUV420Degree90(buffer1,width,height);
                            XposedBridge.log("【VCAM】【decodeFramesToImage】15-2 ");
                            buffer1=I420Tonv21(buffer1,width,height);
                            XposedBridge.log("【VCAM】【decodeFramesToImage】16 ");
                            if(height>width)
                            {
                                //90度旋转
                                buffer1=rotateYUV420SP(buffer1,width,height);
                            }
                            XposedBridge.log("【VCAM】【decodeFramesToImage】17 ");
                            HookMain.data_buffer = buffer1;
                        }
                        XposedBridge.log("【VCAM】【decodeFramesToImage】18 ");
                        image.close();
                    }
                    long sleepTime = info.presentationTimeUs / 1000 - (System.currentTimeMillis() - startWhen);
                    XposedBridge.log("【VCAM】【decodeFramesToImage】19 ");
                    if (sleepTime > 0) {
                        try {
                            XposedBridge.log("【VCAM】【decodeFramesToImage】20 ");
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            XposedBridge.log("【VCAM】" + e.toString());
                            XposedBridge.log("【VCAM】线程延迟出错");
                        }
                    }
                    XposedBridge.log("【VCAM】【decodeFramesToImage】21 ");
                    decoder.releaseOutputBuffer(outputBufferId, true);
                    XposedBridge.log("【VCAM】【decodeFramesToImage】22 ");
                }
            }
        }
        if (callback != null) {
            callback.onFinishDecode();
        }
    }

    private static int selectTrack(MediaExtractor extractor) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                if (VERBOSE) {
                    Log.d(TAG, "Extractor selected track " + i + " (" + mime + "): " + format);
                }
                return i;
            }
        }
        return -1;
    }

    private static boolean isImageFormatSupported(Image image) {
        int format = image.getFormat();
        switch (format) {
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
                return true;
        }
        return false;
    }

    private static byte[] getDataFromImage(Image image, int colorFormat) {
        if (colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21) {
            throw new IllegalArgumentException("only support COLOR_FormatI420 " + "and COLOR_FormatNV21");
        }
        if (!isImageFormatSupported(image)) {
            throw new RuntimeException("can't convert Image to byte array, format " + image.getFormat());
        }
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        if (VERBOSE) Log.v(TAG, "get data from " + planes.length + " planes");
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = width * height;
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height + 1;
                        outputStride = 2;
                    }
                    break;
                case 2:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = (int) (width * height * 1.25);
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height;
                        outputStride = 2;
                    }
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
            if (VERBOSE) {
                Log.v(TAG, "pixelStride " + pixelStride);
                Log.v(TAG, "rowStride " + rowStride);
                Log.v(TAG, "width " + width);
                Log.v(TAG, "height " + height);
                Log.v(TAG, "buffer size " + buffer.remaining());
            }
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
            if (VERBOSE) Log.v(TAG, "Finished reading data from plane " + i);
        }
        return data;
    }


}

enum OutputImageFormat {
    I420("I420"),
    NV21("NV21"),
    JPEG("JPEG");
    private final String friendlyName;

    OutputImageFormat(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String toString() {
        return friendlyName;
    }
}

