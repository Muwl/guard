package com.v0357.face.guard.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;


import android_serialport_api.SerialPort;

/**
 * Created by muwenlei on 2018/1/25.
 */

public class SerialPortControl {

    public static final int SERIALPROT_INPUT = 20001;

    protected SerialPort mSerialPort;
    protected InputStream mInputStream;
    private String prot = "ttySAC2";
    private int baudrate = 19200;

    private Context context;
    private Handler handler;


    public SerialPortControl(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;
        openSerialPort();
    }

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
        Thread receiveThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    Log.e("******************", "======");
                    int size;
                    try {
                        Log.e("******************", "222222");
                        byte[] buffer = new byte[1024];
                        if (mInputStream != null) {
                            size = mInputStream.read(buffer);
                            if (size > 0) {
                                String recinfo = new String(buffer, 0,
                                        size);
                                Message message = new Message();
                                message.what = SERIALPROT_INPUT;
                                message.obj = recinfo;
                                handler.sendMessage(message);
//                                working = true;
//                                getUserInfo(1, recinfo);
//                                Log.e("******************", "接收到串口信息:" + recinfo);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        receiveThread.start();
    }
}
