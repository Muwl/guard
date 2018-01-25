package com.v0357.face.guard.utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by muwenlei on 2018/1/25.
 */

public class FileUtils {

    public static void saveBitmap(byte[] data) throws Exception {
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
    }
}
