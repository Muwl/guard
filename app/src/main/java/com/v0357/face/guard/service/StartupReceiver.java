package com.v0357.face.guard.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.v0357.face.guard.ui.USBCameraActivity;

public class StartupReceiver extends BroadcastReceiver {

	static final String action_boot="android.intent.action.BOOT_COMPLETED";
	 
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub

		 if (intent.getAction().equals(action_boot)){
	            Intent ootStartIntent=new Intent(context,USBCameraActivity.class);
	            ootStartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	            context.startActivity(ootStartIntent);
	        }

       
	}

}
