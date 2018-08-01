package ex07.pyrmont.startup;

import ex07.pyrmont.core.SimpleContext;
import ex07.pyrmont.core.SimpleContextLifecycleListener;
import ex07.pyrmont.core.SimpleContextMapper;
import ex07.pyrmont.core.SimpleLoader;
import ex07.pyrmont.core.SimpleWrapper;
import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.logger.FileLogger;
import org.apache.catalina.Mapper;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.http.HttpConnector;
/**
 * 日志记录器：
 * 必须实现Logger，这个demo相比demo6，就改变了两个雷，SimpleContext和BootStrap 
 * 
 * Logger接口定义了5种日志级别：FATAL,ERROR,WARING,INFORMATION,DEBUG
 * 可以用 getVerbosity() 和 setVerbosity() 来获取，和设置日志级别
 * 
 * Logger还可以通过 接口 的 getContainer() 和  setContainer() 来设置某个日志记录器和
 * 某个Servlet容器相关联。addPropertyChangeListener() 和 removeProperttyChangeListener()
 * 可以添加移除 ProperChangeListener实例
 * 
 * Tomcta提供三种日志记录器 ： FileLogger，SystemOutLogger，SysErrorLogger
 * 均继承自LoggerBase，Tomcat5的日志记录，还继承了Lifecycle接口和MBeanRegistration接口
 * 
 */
public final class Bootstrap {
  public static void main(String[] args) {
    Connector connector = new HttpConnector();
    Wrapper wrapper1 = new SimpleWrapper();
    wrapper1.setName("Primitive");
    wrapper1.setServletClass("PrimitiveServlet");
    Wrapper wrapper2 = new SimpleWrapper();
    wrapper2.setName("Modern");
    wrapper2.setServletClass("ModernServlet");
    Loader loader = new SimpleLoader();

    Context context = new SimpleContext();
    context.addChild(wrapper1);
    context.addChild(wrapper2);

    Mapper mapper = new SimpleContextMapper();
    mapper.setProtocol("http");
    LifecycleListener listener = new SimpleContextLifecycleListener();
    ((Lifecycle) context).addLifecycleListener(listener);
    context.addMapper(mapper);
    context.setLoader(loader);
    // context.addServletMapping(pattern, name);
    context.addServletMapping("/Primitive", "Primitive");
    context.addServletMapping("/Modern", "Modern");

    // ------ add logger --------
    System.setProperty("catalina.base", System.getProperty("user.dir"));
    FileLogger logger = new FileLogger();
    logger.setPrefix("FileLog_");
    logger.setSuffix(".txt");
    logger.setTimestamp(true);
    logger.setDirectory("webroot");
    context.setLogger(logger);

    //---------------------------

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