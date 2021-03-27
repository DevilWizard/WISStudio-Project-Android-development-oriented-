# 目录
- [学习笔记Week1](#学习笔记week1)
  - [窗口通知](#窗口通知)
    - [代码实现](#代码实现)


## 学习笔记Week1

###       窗口通知

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

​			
