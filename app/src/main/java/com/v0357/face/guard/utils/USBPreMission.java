package com.v0357.face.guard.utils;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.IUsbManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

/**
 * Created by muwenlei on 2018/1/25.
 */

public class USBPreMission {

    private UsbManager localUsbManager;
    USBPreMission usbPreMissionUtils;

    private Context context;

    public USBPreMission(Context context) {
        this.context = context;
    }

    public void  initUSBPre() {
        localUsbManager = (UsbManager)context.getSystemService("usb");
        initUsb();
    }

    @SuppressLint("NewApi")
    private void initUsb(){
//		PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent("com.empia.USB_PERMISSION"), 0);
        Intent intent = new Intent();
        intent.setAction("com.empia.USB_PERMISSION");
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.empia.USB_PERMISSION");
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(mReceiver, filter);
        // Request permission
        for (UsbDevice device : localUsbManager.getDeviceList().values()) {
            intent.putExtra(UsbManager.EXTRA_DEVICE, device);
            intent.putExtra(UsbManager.EXTRA_PERMISSION_GRANTED, true);

            final PackageManager pm = context.getPackageManager();
            try {
                ApplicationInfo aInfo = pm.getApplicationInfo(context.getPackageName(),
                        0);
                try {
                    IBinder b = ServiceManager.getService(context.USB_SERVICE);
                    IUsbManager service = IUsbManager.Stub.asInterface(b);
                    service.grantDevicePermission(device, aInfo.uid);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } catch (PackageManager.NameNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            context.getApplicationContext().sendBroadcast(intent);
            Log.i("bofan","UsbManager.EXTRA_DEVICE =="  + localUsbManager.openDevice(device));
        }

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.empia.USB_PERMISSION".equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent
                            .getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    Log.e("=====","UsbManager.EXTRA_DEVICE 22222222222222222 ========"
                            + intent.getParcelableExtra(UsbManager.EXTRA_DEVICE));
                    Log.e("=====","是否有权限了？？？？？？   " + localUsbManager.hasPermission(device));
                    if (intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            // Open reader
                            Log.e("=====","Opening reader: " + device.getDeviceName()
                                    + "...");
                        }
                    } else {
                        if (device != null) {
                            Log.e("=====","Permission no EXTRA_PERMISSION_GRANTED for device "
                                    + device.getDeviceName());
                        }

                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                synchronized (this) {
                }
            }
        }
    };
}
