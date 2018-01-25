package com.v0357.face.guard.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.friendlyarm.AndroidSDK.GPIOEnum;
import com.friendlyarm.AndroidSDK.HardwareControler;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by muwenlei on 2018/1/25.
 */

public class GPIOControl {
    public static final int IRDAG1_PASS=10001;
    public static final int IRDAG2_PASS=10002;
    private int irDAGpio1 = 0;
    private int irDAGpio2 = 0;
    private int doorGpio1 = 0;
    private int doorGpio2 = 0;
    private int ledGpio = 0;

    private Timer timer = new Timer();
    static int STEP_INIT_GPIO_DIRECTION = 1;
    static int STEP_CLOSE_ALL_LED = 2;
    static int STEP_INIT_VIEW = 3;
    private int step = 0;
    private boolean irDAG1Flag=false;
    private boolean irDAG2Flag=false;

    private Context context;
    private Handler handler;


    public GPIOControl(Context context, Handler handler) {
        this.context = context;
        this.handler=handler;
        initGPIO();
    }

    /**
     * 初始化5个GPIO
     */
    public void initGPIO() {
        exportGPIO();
        timer.schedule(init_task, 100, 100);
    }

    /**
     * 导出5个GPIO引脚内存
     */
    private void exportGPIO() {
        if (HardwareControler.exportGPIOPin(irDAGpio1) != 0) {
            Toast.makeText(context,"红外1导出失败!",Toast.LENGTH_SHORT);
        }
        if (HardwareControler.exportGPIOPin(irDAGpio2) != 0) {
            Toast.makeText(context,"红外2导出失败!",Toast.LENGTH_SHORT);
        }
        if (HardwareControler.exportGPIOPin(doorGpio1) != 0) {
            Toast.makeText(context,"门闸1导出失败!",Toast.LENGTH_SHORT);
        }
        if (HardwareControler.exportGPIOPin(doorGpio2) != 0) {
            Toast.makeText(context,"门闸2导出失败!",Toast.LENGTH_SHORT);
        }
        if (HardwareControler.exportGPIOPin(ledGpio) != 0) {
            Toast.makeText(context,"指示灯导出失败!",Toast.LENGTH_SHORT);
        }
    }

    /**
     * 初始化设置GPIO的输入或者输出，以及默认电平
     */
    private TimerTask init_task = new TimerTask() {
        public void run() {
            if (step == STEP_INIT_GPIO_DIRECTION) {
                if (HardwareControler.setGPIODirection(irDAGpio1, GPIOEnum.IN) != 0) {
                    Toast.makeText(context,"红外1设置方向失败!",Toast.LENGTH_SHORT);
                }
                if (HardwareControler.setGPIODirection(irDAGpio2, GPIOEnum.IN) != 0) {
                    Toast.makeText(context,"红外2设置方向失败!",Toast.LENGTH_SHORT);
                }
                if (HardwareControler.setGPIODirection(doorGpio1, GPIOEnum.OUT) != 0) {
                    Toast.makeText(context,"门闸1设置方向失败!",Toast.LENGTH_SHORT);
                }
                if (HardwareControler.setGPIODirection(doorGpio2, GPIOEnum.OUT) != 0) {
                    Toast.makeText(context,"门闸2设置方向失败!",Toast.LENGTH_SHORT);
                }
                if (HardwareControler.setGPIODirection(ledGpio, GPIOEnum.OUT) != 0) {
                    Toast.makeText(context,"指示灯设置方向失败!",Toast.LENGTH_SHORT);
                }
                step++;
            } else if (step == STEP_CLOSE_ALL_LED) {
                if (HardwareControler.setGPIOValue(doorGpio1, GPIOEnum.HIGH) != 0) {
                    Toast.makeText(context,"门闸1设置高电平失败!",Toast.LENGTH_SHORT);
                }
                if (HardwareControler.setGPIOValue(doorGpio2, GPIOEnum.HIGH) != 0) {
                    Toast.makeText(context,"门闸2设置高电平失败!",Toast.LENGTH_SHORT);
                }
                if (HardwareControler.setGPIOValue(ledGpio, GPIOEnum.HIGH) != 0) {
                    Toast.makeText(context,"指示灯设置高电平失败!",Toast.LENGTH_SHORT);
                }
                step++;
            } else if (step == STEP_INIT_VIEW) {
                timer.cancel();
            }
        }
    };

    /**
     * 红外1检测
     */
    Thread irDAG1Thread=new Thread(new Runnable() {
        @Override
        public void run() {
            while (irDAG1Flag){
                try {
                    if (HardwareControler.getGPIOValue(irDAGpio1)==GPIOEnum.LOW){
                        handler.sendEmptyMessage(IRDAG1_PASS);
                    }
                    irDAG1Flag=false;
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    });

    /**
     * 红外2检测
     */
    Thread irDAG2Thread=new Thread(new Runnable() {
        @Override
        public void run() {
            while (irDAG2Flag){
                try {
                    if (HardwareControler.getGPIOValue(irDAGpio2)==GPIOEnum.LOW){
                        handler.sendEmptyMessage(IRDAG2_PASS);
                    }
                    irDAG2Flag=false;
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    });

    /***********************************对外暴露方法*****************************************/

    /**
     * 开启门闸1
     */
    public void openDoor1(){
        if (HardwareControler.setGPIOValue(doorGpio1, GPIOEnum.LOW)!=0){
            Toast.makeText(context,"门闸1开启失败",Toast.LENGTH_SHORT);
        }
    }
    /**
     * 关闭门闸1
     */
    public void closeDoor1(){
        if (HardwareControler.setGPIOValue(doorGpio1, GPIOEnum.HIGH)!=0){
            Toast.makeText(context,"门闸1关闭失败",Toast.LENGTH_SHORT);
        }
    }
    /**
     * 开启门闸2
     */
    public void openDoor2(){
        if (HardwareControler.setGPIOValue(doorGpio2, GPIOEnum.LOW)!=0){
            Toast.makeText(context,"门闸2开启失败",Toast.LENGTH_SHORT);
        }
    }
    /**
     * 关闭门闸1
     */
    public void closeDoor2(){
        if (HardwareControler.setGPIOValue(doorGpio2, GPIOEnum.HIGH)!=0){
            Toast.makeText(context,"门闸2关闭失败",Toast.LENGTH_SHORT);
        }
    }

    /**
     * 开启第一个红外检测
     */
    public void startIrDAG1Monitor(){
        irDAG1Flag=true;
        irDAG1Thread.start();
    }

    /**
     * 关闭第一个红外检测
     */
    public void closeIrDAG1Monitor(){
        irDAG1Flag=false;
    }

    /**
     * 开启第二个红外检测
     */
    public void startIrDAG2Monitor(){
        irDAG2Flag=true;
        irDAG2Thread.start();
    }

    /**
     * 关闭第二个红外检测
     */
    public void closeIrDAG2Monitor(){
        irDAG2Flag=false;
    }


    public void onDestory(){
        if (timer!=null){
            timer.cancel();
        }

    }


}
