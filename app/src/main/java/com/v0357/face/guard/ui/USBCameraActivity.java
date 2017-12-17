package com.v0357.face.guard.ui;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.arcsoft.facetracking.AFT_FSDKVersion;
import com.friendlyarm.AndroidSDK.GPIOEnum;
import com.friendlyarm.AndroidSDK.HardwareControler;
import com.guo.android_extend.java.ExtByteArrayOutputStream;
import com.jiangdg.usbcamera.Constants;
import com.jiangdg.usbcamera.USBCameraManager;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.widget.CameraViewInterface;
import com.serenegiant.usb.widget.UVCCameraTextureView;
import com.v0357.face.guard.R;
import com.v0357.face.guard.face.FaceDB;
import com.v0357.face.guard.view.ProgressDialog;


import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android_serialport_api.SerialPort;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * AndroidUSBCamera引擎使用Demo
 * <p>
 * Created by jiangdongguo on 2017/9/30.
 */

public class USBCameraActivity extends AppCompatActivity implements CameraDialog.CameraDialogParent, Runnable {
    @BindView(R.id.camera_view)
    public View mTextureView;
    private USBCameraManager mUSBManager;
    private CameraViewInterface mUVCCameraView;

    private boolean isRequest;
    private boolean isPreview;
    private int previewWidth= Constants.PREVIEWWIDTH;
    private int previewHeight=Constants.PREVIEWHEIGHT;

    private ProgressDialog mProgressDialog;
    AFT_FSDKVersion version = new AFT_FSDKVersion();
    AFT_FSDKEngine engine = new AFT_FSDKEngine();
    List<AFT_FSDKFace> result = new ArrayList<>();
    static byte[] mImageNV21 = null;
    static boolean working = false;
    AFT_FSDKFace mAFT_FSDKFace = null;

    private final Handler mHandler = new Handler();
    private int pinPort = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_usbcamera);
        ButterKnife.bind(this);
        initGPIO();
        openSerialPort();
        mUVCCameraView = (CameraViewInterface) mTextureView;
        mUVCCameraView.setCallback(new CameraViewInterface.Callback() {
            @Override
            public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
                if (!isPreview && mUSBManager.isCameraOpened()) {
                    mUSBManager.startPreview(mUVCCameraView, new AbstractUVCCameraHandler.OnPreViewResultListener() {
                        @Override
                        public void onPreviewResult(boolean result) {

                        }
                    });
                    isPreview = true;
                }
            }

            @Override
            public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {

            }

            @Override
            public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
                if (isPreview && mUSBManager.isCameraOpened()) {
                    mUSBManager.stopPreview();
                    isPreview = false;
                }
            }
        });
        // 初始化引擎
        mUSBManager = USBCameraManager.getInstance();
        mUSBManager.initUSBMonitor(this, listener);
        mUSBManager.createUVCCamera(mUVCCameraView);
        AFT_FSDKError err = engine.AFT_FSDK_InitialFaceEngine(FaceDB.appid, FaceDB.ft_key, AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 5);
        Log.d("========", "AFT_FSDK_InitialFaceEngine =" + err.getCode());
        err = engine.AFT_FSDK_GetVersion(version);
        Log.d("========", "AFT_FSDK_GetVersion:" + version.toString() + "," + err.getCode());
        Thread thread = new Thread(this);
        thread.start();

        mUSBManager.setmOnPreViewDateLister(new AbstractUVCCameraHandler.OnPreViewDateLister() {
            @Override
            public void onPreviewFrame(byte[] data) {
                if (!working) {
                    result.clear();
                    AFT_FSDKError err = engine.AFT_FSDK_FaceFeatureDetect(data, previewWidth, previewHeight, AFT_FSDKEngine.CP_PAF_NV21, result);
                    if (mImageNV21 == null) {
                        if (!result.isEmpty()) {
                            mImageNV21 = data;
                        }
                    }
                }
            }
        });


    }

    @Override
    public void run() {
        while (true) {
            synchronized (this) {
                if (mImageNV21 != null && !working) {
                    working = true;
                    byte[] data = mImageNV21;
                    if (result == null || result.size() == 0 || data == null) {
                        return;
                    }
                    mAFT_FSDKFace = result.get(0).clone();
                    Mirror(data,previewWidth,previewHeight);
                    YuvImage yuv = new YuvImage(data, ImageFormat.NV21, previewWidth, previewHeight, null);
                    ExtByteArrayOutputStream ops = new ExtByteArrayOutputStream();
                    Rect rect = new Rect();
                    rect.top = (int) (mAFT_FSDKFace.getRect().top * 0.80);
                    rect.left = previewWidth-(int) (mAFT_FSDKFace.getRect().right * 1.1);
                    rect.right =previewWidth- (int) (mAFT_FSDKFace.getRect().left * 0.9);
                    rect.bottom = (int) (mAFT_FSDKFace.getRect().bottom * 1.15);
                    if (rect.left<0){
                        rect.left=0;
                    }
                    if (rect.right > previewWidth) {
                        rect.right = previewWidth;
                    }
                    if (rect.bottom > previewHeight) {
                        rect.bottom = previewHeight;
                    }
                    yuv.compressToJpeg(rect, 90, ops);
                    saveBitmap(ops.getByteArray());
                    result.clear();
                    try {
                        ops.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    private void Mirror(byte[] src, int w, int h) { //src是原始yuv数组
        int i;
        int index;
        byte temp;
        int a, b;
        //mirror y
        for (i = 0; i < h; i++) {
            a = i * w;
            b = (i + 1) * w - 1;
            while (a < b) {
                temp = src[a];
                src[a] = src[b];
                src[b] = temp;
                a++;
                b--;
            }
        }

        // mirror u and v
        index = w * h;
        for (i = 0; i < h / 2; i++) {
            a = i * w;
            b = (i + 1) * w - 2;
            while (a < b) {
                temp = src[a + index];
                src[a + index] = src[b + index];
                src[b + index] = temp;

                temp = src[a + index + 1];
                src[a + index + 1] = src[b + index + 1];
                src[b + index + 1] = temp;
                a+=2;
                b-=2;
            }
        }
    }

    public void saveBitmap(byte[] data) {
        Log.e("==============", "保存图片");
        File f = new File(Environment.getExternalStorageDirectory().getPath() + "/faceImg", "face.jpg");
        File fileParent = f.getParentFile();
        if (f.exists()) {
            f.delete();
        }
        if (!fileParent.exists()) {
            fileParent.mkdirs();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            out.write(data);
//            bmp.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.flush();
            out.close();
            verifyFace(f);
        } catch (FileNotFoundException e) {
            working = false;
            mImageNV21 = null;
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            working = false;
            mImageNV21 = null;
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * face++图片验证
     *
     * @param file
     */
    private void verifyFace(File file) {
        String url = "https://api-cn.faceplusplus.com/facepp/v3/search";
        RequestParams params = new RequestParams(url);
        params.addBodyParameter("api_key", "tNr9XrJWaRY3gNojNK6Xz77XE_d4Ik65");
        params.addBodyParameter("api_secret", "-42yGXbbquYeD_xSM2HMiEwPMMeiT2sT");
        params.addBodyParameter("image_file", file);
        params.addBodyParameter("faceset_token", "edc942ea21072ac4a9ca5cb09a1eca49");
        try {
            String res = x.http().postSync(params, String.class);
            JSONObject jsonObject = JSONObject.parseObject(res);
            if (!TextUtils.isEmpty(jsonObject.getString("request_id"))) {
                JSONArray resArr = jsonObject.getJSONArray("results");
                if (resArr != null && resArr.size() != 0) {
                    float confide = resArr.getJSONObject(0).getFloat("confidence");
                    if (confide >= 80) {
                        getUserInfo(0, resArr.getJSONObject(0).getString("face_token"));
                    } else {
                        setStartWork();
                    }
                } else {
                    setStartWork();
                }
            } else {
                setStartWork();
            }

            Log.e("=============", res);
        } catch (Throwable throwable) {
            setStartWork();
            throwable.printStackTrace();
            Log.e("==============", "失败");
        }
    }

    private void setStartWork() {
        working = false;
        mImageNV21 = null;
    }


    /***************************************串口*****************************************/
    protected SerialPort mSerialPort;
    protected InputStream mInputStream;
    private String prot = "ttySAC0";
    private int baudrate = 115200;
    private Thread receiveThread;

    private void openSerialPort() {
        try {
            mSerialPort = new SerialPort(new File("/dev/" + prot), baudrate,
                    0);
            mInputStream = mSerialPort.getInputStream();
            receiveThread();
            Log.e("******************", "打开成功");
        } catch (SecurityException e) {
            Log.e("******************", "打开失败");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("******************", "打开失败");
            e.printStackTrace();
        }
    }

    private void receiveThread() {
        // 接收
        receiveThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    int size;
                    try {
                        if (working || mImageNV21 != null) {
                            return;
                        }
                        byte[] buffer = new byte[1024];
                        if (mInputStream == null)
                            return;
                        size = mInputStream.read(buffer);
                        if (size > 0) {
                            String recinfo = new String(buffer, 0,
                                    size);
                            working = true;
                            getUserInfo(1, recinfo);
                            Log.e("******************", "接收到串口信息:" + recinfo);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        receiveThread.start();
    }

    /**
     * @param flag 0代表人脸识别  1代表扫描登录
     * @param tag  唯一标示
     */
    private void getUserInfo(int flag, String tag) {
        String url = "";
        RequestParams params = new RequestParams(url);
        params.addQueryStringParameter("tag", "tag");
        x.http().post(params, new Callback.CommonCallback<String>() {
            @Override
            public void onSuccess(String result) {
                Toast.makeText(x.app(), result, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
                Toast.makeText(x.app(), ex.getMessage(), Toast.LENGTH_LONG).show();
                setStartWork();
            }

            @Override
            public void onCancelled(CancelledException cex) {
                Toast.makeText(x.app(), "cancelled", Toast.LENGTH_LONG).show();
                setStartWork();
            }

            @Override
            public void onFinished() {

            }
        });
    }

    /**
     * 开门 3秒后自动关门
     */
    private void openDoor() {
        setGPIOValue(GPIOEnum.LOW);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                colseDoor();
            }
        }, 3000);
    }

    /**
     * 关门
     */
    private void colseDoor() {
        setGPIOValue(GPIOEnum.HIGH);
        setStartWork();
    }

    //*******************************GPIO*****************************************************************

    private Timer timer = new Timer();
    static int STEP_INIT_GPIO_DIRECTION = 1;
    static int STEP_CLOSE_ALL_LED = 2;
    static int STEP_INIT_VIEW = 3;
    private int step = 0;

    private void initGPIO() {
        if (HardwareControler.exportGPIOPin(pinPort) == 0) {
            timer.schedule(init_task, 100, 100);
        } else {
            Log.e("===========", "GPIO export error!");
        }
    }

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    timer.cancel();
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private TimerTask init_task = new TimerTask() {
        public void run() {
            if (step == STEP_INIT_GPIO_DIRECTION) {
                if (HardwareControler.setGPIODirection(pinPort, GPIOEnum.OUT) == 0) {
                } else {
                    Log.v("TimerTask", "setGPIODirection failed");
                }
                step++;
            } else if (step == STEP_CLOSE_ALL_LED) {
                if (HardwareControler.setGPIOValue(pinPort, GPIOEnum.HIGH) == 0) {
                } else {
                    Log.v("TimerTask", "setGPIODirection failed");
                }
                step++;
            } else if (step == STEP_INIT_VIEW) {
                Message message = new Message();
                message.what = 1;
                handler.sendMessage(message);
            }
        }
    };

    /**
     * 设置GPIO的高低电位
     *
     * @param enumvalue GPIOEnum.LOW  低电位  GPIOEnum.HIGH 高电位
     */
    private void setGPIOValue(int enumvalue) {
        int res = HardwareControler.setGPIOValue(pinPort, enumvalue);
        if (res != 0) {
            Log.e("=========", "setGPIOValue failed");
        }
    }


    //*********************************************************************************************************
    @Override
    protected void onStart() {
        super.onStart();
        if (mUSBManager == null)
            return;
        // 注册USB事件广播监听器
        mUSBManager.registerUSB();
        mUVCCameraView.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 注销USB事件广播监听器
        if (mUSBManager != null) {
            mUSBManager.unregisterUSB();
        }
        mUVCCameraView.onPause();

    }

    @OnClick(R.id.camera_view)
    public void onViewClick(View view) {
        int vId = view.getId();
        switch (vId) {
            // 点击后自动对焦
            case R.id.camera_view:
                if (mUSBManager == null)
                    return;
                mUSBManager.startCameraFoucs();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUSBManager != null) {
            mUSBManager.release();
        }
        timer.cancel();
        setStartWork();
        AFT_FSDKError err = engine.AFT_FSDK_UninitialFaceEngine();
        Log.d("=====", "AFT_FSDK_UninitialFaceEngine =" + err.getCode());
    }


    /**
     * USB设备事件监听器
     */
    private USBCameraManager.OnMyDevConnectListener listener = new USBCameraManager.OnMyDevConnectListener() {
        // 插入USB设备
        @Override
        public void onAttachDev(UsbDevice device) {
            if (mUSBManager == null || mUSBManager.getUsbDeviceCount() == 0) {
                showShortMsg("未检测到USB摄像头设备");
                return;
            }
            // 请求打开摄像头
            if (!isRequest) {
                isRequest = true;
                if (mUSBManager != null) {
                    mUSBManager.requestPermission(0);
                }
            }
        }

        // 拔出USB设备
        @Override
        public void onDettachDev(UsbDevice device) {
            if (isRequest) {
                // 关闭摄像头
                isRequest = false;
                mUSBManager.closeCamera();
                showShortMsg(device.getDeviceName() + "已拨出");
            }
        }

        // 连接USB设备成功
        @Override
        public void onConnectDev(UsbDevice device, boolean isConnected) {
            if (!isConnected) {
                showShortMsg("连接失败，请检查分辨率参数是否正确");
                isPreview = false;
            } else {
                isPreview = true;
            }
        }

        // 与USB设备断开连接
        @Override
        public void onDisConnectDev(UsbDevice device) {
            showShortMsg("连接失败");
        }
    };


    private void showShortMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBManager.getUSBMonitor();
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (canceled) {
            showShortMsg("取消操作");
        }
    }

    public boolean isCameraOpened() {
        return mUSBManager.isCameraOpened();
    }


}
