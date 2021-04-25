Activity的生命周期

今天调试程序的时候遇到个严重的问题。当我通过导航栏的返回键退出应用后再次进入，会出现空指针异常导致应用崩溃，而我若通过上滑退出应用（点击导航栏的最右端的任务栏，然后将应用上滑移出，就是我们一般清后台的操作）再次进入应用则不会有这样的问题。最后发现，我是在activity中的onDestroy方法里将那些用过的资源都置了空，所以导致了空指针报错。看来，返回键退出应用和手动杀应用还是有区别的。

调试发现，按返回键后，再启动应用，activity的onCreate方法并没有执行，因为按返回键后执行onDstroy的过程中把那些资源全都置空了，所以再次点开应用，被置空的资源没有在onCreate中重新初始化，这也就不奇怪会出现空指针的异常了。能初步看出，返回键退出应用是“假杀后台”，而通过上滑将应用清后台则是真正的清后台。

在网上找到如下解决方法

```java
@Override
public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
         moveTaskToBack(true);
    }
    return super.onKeyDown(keyCode, event);
}
```

但这部分需要深入了解安卓的生命周期，暂时就先放着把

细解Context

Return the context of the single, global Application object of the current process. This generally should only be used if you need a Context whose lifecycle is separate from the current context, that is tied to the lifetime of the process rather than the current component.

Consider for example how this interacts with `registerReceiver(android.content.BroadcastReceiver, android.content.IntentFilter)`:

- If used from an Activity context, the receiver is being registered within that activity. This means that you are expected to unregister before the activity is done being destroyed; in fact if you do not do so, the framework will clean up your leaked registration as it removes the activity and log an error. Thus, if you use the Activity context to register a receiver that is static (global to the process, not associated with an Activity instance) then that registration will be removed on you at whatever point the activity you used is destroyed.
- If used from the Context returned here, the receiver is being registered with the global state associated with your application. Thus it will never be unregistered for you. This is necessary if the receiver is associated with static data, not a particular component. However using the ApplicationContext elsewhere can easily lead to serious leaks if you forget to unregister, unbind, etc.