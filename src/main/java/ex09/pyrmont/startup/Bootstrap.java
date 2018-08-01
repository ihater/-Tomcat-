package ex09.pyrmont.startup;

import ex09.pyrmont.core.SimpleWrapper;
import ex09.pyrmont.core.SimpleContextConfig;
import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Manager;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.http.HttpConnector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.session.StandardManager;
/**
 *	Catania通过session管理器的组件来管理建立的session
 *	该组件由Manager 接口表示 ，session管理器需要与一个Context 容器关联，
 *	相比其他组件 , session 管理器负责创建，更新，销毁session对象
 *	当有请求到达，要返回一个有有效的session对象
 *
 *	默认情况下，session会将所有创建的session对象放到内存中
 *	但是，在Tomcat管理器中，可以实现session的持久化，存储到文件存储器通过JDBC写入到数据库中
 *	在Catania中，session包下游一些与Session对象和session对象管理相关的类
 */
public final class Bootstrap {
  public static void main(String[] args) {

    //invoke: http://localhost:8080/myApp/Session

    System.setProperty("catalina.base", System.getProperty("user.dir"));
    Connector connector = new HttpConnector();
    Wrapper wrapper1 = new SimpleWrapper();
    wrapper1.setName("Session");
    wrapper1.setServletClass("SessionServlet");

    Context context = new StandardContext();
    // StandardContext's start method adds a default mapper
    context.setPath("/myApp");
    context.setDocBase("myApp");

    context.addChild(wrapper1);

    // context.addServletMapping(pattern, name);
    // note that we must use /myApp/Session, not just /Session
    // because the /myApp section must be the same as the path, so the cookie will
    // be sent back.
    context.addServletMapping("/myApp/Session", "Session");
    // add ContextConfig. This listener is important because it configures
    // StandardContext (sets configured to true), otherwise StandardContext
    // won't start
    LifecycleListener listener = new SimpleContextConfig();
    ((Lifecycle) context).addLifecycleListener(listener);

    // here is our loader
    Loader loader = new WebappLoader();
    // associate the loader with the Context
    context.setLoader(loader);

    connector.setContainer(context);

    // add a Manager
    Manager manager = new StandardManager();
    context.setManager(manager);

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