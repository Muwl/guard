package com.v0357.face.guard.utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by muwenlei on 2018/1/25.
 */

public class FileUtils {

    public static File saveBitmap(byte[] data) throws Exception {
        Log.e("==============", "保存图片");
        File f = new File(Environment.getExternalStorageDirectory().getPath() + "/faceImg", "face.jpg");
        File fileParent = f.getParentFile();
        if (f.exists()) {
            f.delete();
        }
        if (!fileParent.exists()) {
            fileParent.mkdirs();
        }
        FileOutputStream out = new FileOutputStream(f);
        out.write(data);
        out.flush();
        out.close();
        return f;
    }

    public static String readToString() {
        String fileName="/sdcard/guardConfig.txt";
        String encoding = "UTF-8";
        File file = new File(fileName);
        Long filelength = file.length();
        byte[] filecontent = new byte[filelength.intValue()];
        try {
            FileInputStream in = new FileInputStream(file);
            in.read(filecontent);
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            return new String(filecontent, encoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

}
