package com.v0357.face.guard.utils;

import android.util.Log;

/**
 * Created by Administrator on 2017/12/24.
 */

public class ScriptUtils {

    public static void startScript(){
        try {
            //通过挂在到linux的方式，修改文件的操作权限
            Process su = Runtime.getRuntime().exec("/system/xbin/sh");
            String cmd = "/data/local/ngrok -config=/data/local/boot/ngrok.conf start ssh  " + "\n" + "exit\n";
            su.getOutputStream().write(cmd.getBytes());
            Log.e("***--","11111111");
            if ((su.waitFor() != 0) ) {
                Log.e("***--","2222222222");
                throw new SecurityException();
            }
        } catch (Exception e) {
            Log.e("***--","3333333333");
            e.printStackTrace();
            throw new SecurityException();
        }
    }
}
