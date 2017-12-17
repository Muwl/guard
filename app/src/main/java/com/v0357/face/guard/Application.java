package com.v0357.face.guard;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;

import com.v0357.face.guard.face.FaceDB;

import org.xutils.x;

/**
 * Created by gqj3375 on 2017/4/28.
 */

public class Application extends android.app.Application {
	private final String TAG = this.getClass().toString();
	FaceDB mFaceDB;
	Uri mImage;

	@Override
	public void onCreate() {
		super.onCreate();
		mFaceDB = new FaceDB(this.getExternalCacheDir().getPath());
		x.Ext.init(this);
		x.Ext.setDebug(BuildConfig.DEBUG);
		mImage = null;
	}
}
