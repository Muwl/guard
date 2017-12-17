package com.v0357.face.guard.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.arcsoft.facetracking.AFT_FSDKVersion;
import com.guo.android_extend.java.ExtByteArrayOutputStream;
import com.guo.android_extend.tools.CameraHelper;
import com.guo.android_extend.widget.CameraFrameData;
import com.guo.android_extend.widget.CameraGLSurfaceView;
import com.guo.android_extend.widget.CameraSurfaceView;
import com.guo.android_extend.widget.CameraSurfaceView.OnCameraListener;
import com.v0357.face.guard.R;
import com.v0357.face.guard.face.FaceDB;
import com.v0357.face.guard.view.ProgressDialog;

import org.xutils.http.RequestParams;
import org.xutils.x;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android_serialport_api.SerialPort;

/**
 * Created by gqj3375 on 2017/4/28.
 */

public class DetecterActivityOld extends Activity implements OnCameraListener, View.OnTouchListener, Camera.AutoFocusCallback, Runnable {
    private final String TAG = this.getClass().getSimpleName();
    private ProgressDialog mProgressDialog;
    private int mWidth, mHeight, mFormat;
    private CameraSurfaceView mSurfaceView;
    private CameraGLSurfaceView mGLSurfaceView;
    private Camera mCamera;

    AFT_FSDKVersion version = new AFT_FSDKVersion();
    AFT_FSDKEngine engine = new AFT_FSDKEngine();
    List<AFT_FSDKFace> result = new ArrayList<>();

    int mCameraID;
    int mCameraRotate;
    boolean mCameraMirror;
    static byte[] mImageNV21 = null;
    static boolean working = false;
    AFT_FSDKFace mAFT_FSDKFace = null;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 10001) {
                File f = (File) msg.obj;
                verifyFace(f);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        mCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;
        mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
        mCameraMirror = true;
        mCameraRotate = 0;
        mWidth = 640;
        mHeight = 480;
        mFormat = ImageFormat.NV21;
        setContentView(R.layout.activity_camera);
        mGLSurfaceView = (CameraGLSurfaceView) findViewById(R.id.glsurfaceView);
        mGLSurfaceView.setOnTouchListener(this);
        mSurfaceView = (CameraSurfaceView) findViewById(R.id.surfaceView);
        mSurfaceView.setOnCameraListener(this);
        mSurfaceView.setupGLSurafceView(mGLSurfaceView, true, mCameraMirror, mCameraRotate);
        mSurfaceView.debug_print_fps(true, false);

        AFT_FSDKError err = engine.AFT_FSDK_InitialFaceEngine(FaceDB.appid, FaceDB.ft_key, AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 5);
        Log.d(TAG, "AFT_FSDK_InitialFaceEngine =" + err.getCode());
        err = engine.AFT_FSDK_GetVersion(version);
        Log.d(TAG, "AFT_FSDK_GetVersion:" + version.toString() + "," + err.getCode());
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public Camera setupCamera() {
        // TODO Auto-generated method stub
        mCamera = Camera.open(mCameraID);
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mWidth, mHeight);
            parameters.setPreviewFormat(mFormat);
            for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
                Log.d(TAG, "SIZE:" + size.width + "x" + size.height);
            }
            for (Integer format : parameters.getSupportedPreviewFormats()) {
                Log.d(TAG, "FORMAT:" + format);
            }
            List<int[]> fps = parameters.getSupportedPreviewFpsRange();
            for (int[] count : fps) {
                Log.d(TAG, "T:");
                for (int data : count) {
                    Log.d(TAG, "V=" + data);
                }
            }
            //parameters.setPreviewFpsRange(15000, 30000);
            //parameters.setExposureCompensation(parameters.getMaxExposureCompensation());
            //parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            //parameters.setAntibanding(Camera.Parameters.ANTIBANDING_AUTO);
            //parmeters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            //parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
            //parameters.setColorEffect(Camera.Parameters.EFFECT_NONE);
            mCamera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mCamera != null) {
            mWidth = mCamera.getParameters().getPreviewSize().width;
            mHeight = mCamera.getParameters().getPreviewSize().height;
        }
        return mCamera;
    }

    @Override
    public Object onPreview(byte[] data, int width, int height, int format, long timestamp) {
        AFT_FSDKError err = engine.AFT_FSDK_FaceFeatureDetect(data, width, height, AFT_FSDKEngine.CP_PAF_NV21, result);
        Log.d(TAG, "AFT_FSDK_FaceFeatureDetect =" + err.getCode());
        Log.d(TAG, "Face=" + result.size());
        for (AFT_FSDKFace face : result) {
            Log.d(TAG, "Face:" + face.toString());
        }
        if (mImageNV21 == null) {
            if (!result.isEmpty()) {
                Log.e("==============", "重新来-===");
                mAFT_FSDKFace = result.get(0).clone();
                mImageNV21 = data.clone();
            }
        }
        //copy rects
        Rect[] rects = new Rect[result.size()];
        for (int i = 0; i < result.size(); i++) {
            rects[i] = new Rect(result.get(i).getRect());
        }
        result.clear();
        return rects;
    }

    @Override
    public void run() {
        while (true) {
            synchronized (this) {
                if (mImageNV21 != null && !working) {
                    working = true;
                    byte[] data = mImageNV21;
                    YuvImage yuv = new YuvImage(data, ImageFormat.NV21, mWidth, mHeight, null);
                    ExtByteArrayOutputStream ops = new ExtByteArrayOutputStream();
                    yuv.compressToJpeg(mAFT_FSDKFace.getRect(), 80, ops);
                    final Bitmap bmp = BitmapFactory.decodeByteArray(ops.getByteArray(), 0, ops.getByteArray().length);
                    try {
                        ops.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    saveBitmap(bmp);
                }
            }
        }
    }

    public void saveBitmap(Bitmap bmp) {
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
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, out);
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

    private void verifyFace(File file) {
        String url = "https://api-cn.faceplusplus.com/facepp/v3/search";
        RequestParams params = new RequestParams(url);
        params.addBodyParameter("api_key", "tNr9XrJWaRY3gNojNK6Xz77XE_d4Ik65");
        params.addBodyParameter("api_secret", "-42yGXbbquYeD_xSM2HMiEwPMMeiT2sT");
        params.addBodyParameter("image_file", file);
        params.addBodyParameter("faceset_token", "54825fa4e1d077935aacaf0c9fccc719");
        try {
            String res = x.http().postSync(params, String.class);
            Log.e("=============", res);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            Log.e("==============", "失败");
        }
        working = false;
        mImageNV21 = null;
    }

    protected void showProgressDialog() {
        mProgressDialog = new ProgressDialog(this).createDialog(this);
        mProgressDialog.setMessage("正在识别");
        mProgressDialog.show();
    }

    protected void dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onBeforeRender(CameraFrameData data) {

    }

    @Override
    public void onAfterRender(CameraFrameData data) {
        mGLSurfaceView.getGLES2Render().draw_rect((Rect[]) data.getParams(), Color.GREEN, 2);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        CameraHelper.touchFocus(mCamera, event, v, this);
        return false;
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        if (success) {
            Log.d(TAG, "Camera Focus SUCCESS!");
        }
    }

    @Override
    public void setupChanged(int format, int width, int height) {

    }

    @Override
    public boolean startPreviewLater() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        AFT_FSDKError err = engine.AFT_FSDK_UninitialFaceEngine();
        Log.d(TAG, "AFT_FSDK_UninitialFaceEngine =" + err.getCode());
    }

    /***************************************串口*****************************************/
    protected SerialPort mSerialPort;
    protected InputStream mInputStream;
    private String prot = "ttySAC2";
    private int baudrate = 9600;
    private Thread receiveThread;

    private void openSerialPort() {
        try {
            mSerialPort = new SerialPort(new File("/dev/" + prot), baudrate,
                    0);
            mInputStream = mSerialPort.getInputStream();
            receiveThread();
            Log.e("=============", "打开成功");
        } catch (SecurityException e) {
            Log.e("=============", "打开失败");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("=============", "打开失败");
            e.printStackTrace();
        }
    }

    private void sendSerialPort(String str){
        try {
            SerialPort sendSerialPort=new SerialPort(new File("/dev/" + prot), baudrate,
                    0);
            OutputStream mOutputStream = mSerialPort.getOutputStream();
            mOutputStream.write((str).getBytes());
            Log.e("=============", "发送成功");
        } catch (IOException e) {
            Log.e("=============", "发送失败");
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
                        byte[] buffer = new byte[1024];
                        if (mInputStream == null)
                            return;
                        size = mInputStream.read(buffer);
                        if (size > 0) {
                            String recinfo = new String(buffer, 0,
                                    size);
                            Log.e("============", "接收到串口信息:" + recinfo);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        receiveThread.start();
    }

}
