# 目录
- [目录](#目录)
  - [学习笔记Week1](#学习笔记week1)
    - [窗口通知（Notification）](#窗口通知notification)
      - [代码实现](#代码实现)
    - [服务（Service）](#服务service)
      - [Android 多线程](#android-多线程)


## 学习笔记Week1

###       窗口通知（Notification）

​			**窗口通知**是安卓开发中最为常用的一个部分，最常见的如微信、qq的消息通知。下面就来学习一下如何在app中实现这个功能吧。

#### 		代码实现

​			由于安卓系统版本较多，每一版都在不断更新。而通知的创建以Android8为分界岭，8以上的系统通知创建较8以下的更为复杂。需要额外用到NotificationChannel。公用部分是Notification 、NotificationManager、NotificaitonCompat。因此在创建通知时要**向前兼容**

```java
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import androidx.core.app.NotificationCompat;

String ChannelID = "1";
String ChannelName = "channel";
int importanceLevel = NotificationManager.IMPORTANCE_DEFAULT;	//通知优先级
Notification notification;
NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {	
    //Android版本大于等于8时必需要创建通知频道
    NotificationChannel channel = new NotificationChannel(ChannelID, ChannelName, importanceLevel);
    manager.createNotificationChannel(channel);
       //不同处，new Builder时需要传入ChannelID(同一个类别的通知在创建channel时传入的ID必须与Builder中的一致)
        notification = new NotificationCompat.Builder(this, ChannelID)
        .setContentTitle(title)
        .setContentText(text)
        .setSmallIcon(icon)
	//.... 设置其他一些可选属性
        .build();
} else{
    //Android版本小于8时无需创建NotificationChannel
    notification = new NotificationCompat.Builder(this)
        .setContentTitle(title)
        .setContentText(text)
        .setSmallIcon(icon)
	//.... 设置其他一些可选属性
        .build();
}
 //必备操作，与安卓系统版本无关，通过NotificationManager的notify唤醒通知
manager.notify(1, notification); //这里的1指通知的唯一id，相当于通知在状态栏的位置标识
/*
	notify(int id, Notification notification) 函数详解:
	举例说明：
	假如现在notification1正在通知栏显示通知（其通过notify(1, notification1)唤醒），
	若现在notification2也通过notify(1, notification2)唤醒，则原本notification1的位置会被notification2替代
	但若notification2通过notify(12, notification2)唤醒，则两条通知会在不同的地方显示，不会影响对方
	总结：notify的第一个参数指定通知的显示位置，id相同则显示位置相同，不同则显示互不影响
*/

/* 
	NotificationChannel(String id, CharSequence channelName, @Importance int importance)
	函数详解：id为通知的标识，方便区分，不同包的id可以重复，但同个包中的id必须唯一
	         channelName为通知频道名字，方便将各种通知归类，如消息频道、语音通话频道
	         importance是通知显示的重要性，是弹出通知还是静默通知等
*/

```





### 	服务（Service）		

作为四大组件之一的服务，一直在app中扮演着非常重要的角色，文件下载、微信消息等等都可见服务的身影。而要想熟练掌握服务，就必须从Android多线程入手。

#### 		Android 多线程

​	 对于一些耗时的任务，如音乐播放，如果将其放在app主线程中运行，会造成大量资源消耗，进而可能导致程序崩溃。因此我们需要创建子线程来运行那些费时、费内存的任务。下面是几种创建子线程的方式。

* ##### Runnable接口实现

    与Java原生多线程操作一致，这里不重复叙述

* ##### Handler、Message、MessageQueue、Looper

    这是Android提供的异步消息处理机制，可以很好地解决子线程无法更新UI的问题（Android的UI线程不安全，必须在主线程中执行UI更新）

    ```java
    import android.os.Handler;
    import android.os.Message;
    import android.os.Looper;
    
    final int UPDATE_UI = 1; 
    
    //传入Looper新建handler对象，重写handleMessage
    Handler handler = new Handler(Looper.getMainLooper()) {
    	@Override
    	public void handleMessage(@NonNull Message msg) { //用于处理各种消息
            switch(msg.what) {
                case 1:
                    //情况1的操作
                    break;
                case 2:
                    //情况2的操作
                    break;
                //其他情况UI更新操作
            }
        }
    };
    
    new Thread(new Runnable() {
        @Override
        public void run() {
    		Message message = new Message();
    		message.what = UPDATE_UI;
    		handler.sendMessage(message);//发出消息
        }
    }).start();
    
    
    ```

    

    创建Message对象后，指定消息内容（即what域），接着利用handler将消息发送出去。之后这条消息会被发送到MessageQueue中进行排队等待，当排到时，Looper会将这条消息发回handleMessage中，这样一来UI的更新操作就是在主线程中的handler中执行的，因此不会造成任何问题。而在这过程中Looper会一直查看MessageQueue中是否有新消息，是一个死循环，除非手动调用quit()方法会结束该循环，同时也会无法取出新消息。

    需要说明，当一个Activity启动时会自动创建Looper，因此主线程中的Looper启动与停止不需要我们担心。 但如果是自己定义的 Looper ，则要执行quit()操作，否则这个子线程就会一直处于等待状态，容易造成内存泄漏 。
    
    当然，如果只是想更新UI，可以直接调用封装好的runOnUiThread()，这样可以省去很多操作。



