package ex06.pyrmont.startup;

/**
 * 本章会介绍三种生命周期事件监听的工具类 ： Lifecycle，LifecycleEvent、LifecycleListener
 * 还会了解一下LifeecycleSupport，该类提供最简单的方法来触发某个组件的生命周期事件，并对事件监听进行处理
 * 该例子中，会利用Lifecycle接口来管理生命周期
 * 
 * Lifecycle接口：Catania设计成一个组件可以包含其他组件，就是通注册事件监听
 * 				 将所有的组件都置于其父类组件的“监护”之下，这样启动父类组件，所有的子类都会启动
 * 				可以给组件注册多个时间监听器来对发送在该逐渐上的某些事进行监听，
 * 				 该事件的监听器会收到通知
 * LifecycleEvent：生命周期事件是LifecycleEvent的实例
 * LifecycleListener：生命周期的监听器是LifecycleListener接口的实例
 * 					 该接口只有一个方法，lifecycleEvent() ，
 * 					 当某个事件监听器监听到相关事件发生时候，会调用这个方法
 * lifecycleSupporrt：实现了Liftcycle接口，
 * 					   并对某个事件注册了监听器的组件必须拥有的三个方法（添加，查找，移除监听器）
 * 					 接着，会将该组件所需要注册的事件监听器 ，存储到一个数组中，等待被触发数组里的事件
 */
import ex06.pyrmont.core.SimpleContext;
import ex06.pyrmont.core.SimpleContextLifecycleListener;
import ex06.pyrmont.core.SimpleContextMapper;
import ex06.pyrmont.core.SimpleLoader;
import ex06.pyrmont.core.SimpleWrapper;
import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Mapper;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.http.HttpConnector;

public final class Bootstrap {
  public static void main(String[] args) {
    Connector connector = new HttpConnector();
    Wrapper wrapper1 = new SimpleWrapper();
    wrapper1.setName("Primitive");
    wrapper1.setServletClass("PrimitiveServlet");
    Wrapper wrapper2 = new SimpleWrapper();
    wrapper2.setName("Modern");
    wrapper2.setServletClass("ModernServlet");

    Context context = new SimpleContext();
    context.addChild(wrapper1);
    context.addChild(wrapper2);

    Mapper mapper = new SimpleContextMapper();
    mapper.setProtocol("http");
    LifecycleListener listener = new SimpleContextLifecycleListener();
    ((Lifecycle) context).addLifecycleListener(listener);
    context.addMapper(mapper);
    Loader loader = new SimpleLoader();
    context.setLoader(loader);
    // context.addServletMapping(pattern, name);
    context.addServletMapping("/Primitive", "Primitive");
    context.addServletMapping("/Modern", "Modern");
    connector.setContainer(context);
    try {
      connector.initialize();
      ((Lifecycle) connector).start();
      ((Lifecycle) context).start();

      // make the application wait until we press a key.
      System.in.read();
      ((Lifecycle) context).stop();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}