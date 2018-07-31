package ex06.pyrmont.core;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
/**
 *	这是一个演示生命周期的监听器的Demo ，继承接口，实现接口的类
 *	lifecycleEvent(LifecycleEvent event)
 *   接收到参数event  ,就可以判断，是什么类型的事件被触发了
 */
public class SimpleContextLifecycleListener implements LifecycleListener {

  public void lifecycleEvent(LifecycleEvent event) {
    Lifecycle lifecycle = event.getLifecycle();
    System.out.println("SimpleContextLifecycleListener's event " +
      event.getType().toString());
    if (Lifecycle.START_EVENT.equals(event.getType())) {
      System.out.println("Starting context.");
    }
    else if (Lifecycle.STOP_EVENT.equals(event.getType())) {
      System.out.println("Stopping context.");
    }
  }
}