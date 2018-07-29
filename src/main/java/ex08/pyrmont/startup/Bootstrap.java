package ex08.pyrmont.startup;

import ex08.pyrmont.core.SimpleWrapper;
import ex08.pyrmont.core.SimpleContextConfig;
import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.http.HttpConnector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappClassLoader;
import org.apache.catalina.loader.WebappLoader;
import org.apache.naming.resources.ProxyDirContext;
/**
 * 这一章：学习载入器（实现Tomcat中的载入器，就是载入一个所需要的Servlet类）
 * 载入器使用类载入器，后者应用某些规则载入类，类债载入器数Loader 接口的实例
 * 
 * Servlet容器不可以简单的使用系统的类加载器，而是要实现一个自定义的载入器
 * 因为Servlet容器不应该完全信任它正在运行的Servlet类。
 * 如果像前几章，使用系统的类加载器载入某个Servlet类所需要的全部类.
 * 那么这个Servlet类就能访问所有的类，包括当前Java的JVM，和CLASSPATH下的所有类和库
 * 
 * 这是很危险的，Servlet只允许载入WEB-INF/class下，和lib目录载入类。
 * 还有一个原因，为了提供自动重载，就是当WEB-INF/class 和 lib 目录下的类发送变化
 * Web 应用程序（Tomcat）就会自动重新载入这些类，
 * 
 * !!!! 在Tomcat载入器的实现中，类加载器使用额外的线程，不断扫描，检查Servlet类和其他类文件的时间戳
 * 若要支持自动加载的功能，就要实现Reloader接口
 * 
 * respository：仓库表示类载入器会在哪里搜索要载入的类
 * 
 * resource：资源：是一个类载入器的DirContext对象，它的文件路径指的就是上下文的根路径
 * 
 * JAVA 的类载入器：
 * 		每次创建JAVA类的实例时，都必须将类载入到内存中，JVM的类加载器来载入相关的类
 * 		一般，JVM类加载器会在JAVA核心类库，环境变量CLASSPATH指的的目录下查找需要的类
 *		如果找不到相关的类，就会爆（ClassNotFoundException） 	
 *		JVM的三种类加载器：引导类加载器（bootstrap  class loader）
 *					       扩展类加载器（extension class loader）
 *					      系统类加载器 （system class loader）	
 *		三种类加载器是父子关系，其中，引导类加载器是最上层，系统类加载器是最下层
 *		
 *	JVM类加载器的启动过程：
 *		引导类加载器用于引导启动JVM虚拟机，当调用javax.exe程序的时候，就会启动引导类加载器
 *			引导类家暗自气是使用本地代码来实现的。因为它用来载入运行JVM所需要的类，以及所有的Java核心类
 *			核心类包括，java.lang 或者 java.io 包下的类，启动类加载器会在rt.jar和i18n.jar等jar包搜索需要载入的类
 *			引导类加载器会从哪些库中搜索要载入的类，这依赖于JVM和操作系统的版本
 *		扩展类加载器负责载入标准扩展目录中的类，这有利于程序开发，因为程序员只需要将JAR文件复制到
 *			扩展目录中就可以被类加载器搜索到，扩展库依赖于JDK提供商的具体实现。
 *			例如sum公司的JVM的标准扩展目录是  /jdk/lib/ext
 *		系統类加载器是默认的类加载器，它会搜索在环境变量下CLASSPATH中这么的路径和JVR文件
 *			
 *	当需要载入一个类时：
 *		先调用系统类载入器，但是，系统类载入器会将载入任务交给父类载入器，即扩展类加载器，扩展类加载器
 *		又会将类载入任务交给其父类，即引导类加载器。所以，引导类加载器会首先执行载入某个类
 *		如果引导类加载器找不到类，就让扩展类加载器找，还找不到就交给系统类加载器找，再找不到就报错
 *
 *	Tomcat需要使用自定义类加载器有三个原因：
 *		为了在载入类中指定某些规则
 *		为了缓存已经载入的类
 *		为了实现类的预载入，方便使用
 *
 */
public final class Bootstrap {
  public static void main(String[] args) {

    //invoke: http://localhost:8080/Modern or  http://localhost:8080/Primitive

    System.setProperty("catalina.base", System.getProperty("user.dir"));
    Connector connector = new HttpConnector();
    Wrapper wrapper1 = new SimpleWrapper();
    wrapper1.setName("Primitive");
    wrapper1.setServletClass("PrimitiveServlet");
    Wrapper wrapper2 = new SimpleWrapper();
    wrapper2.setName("Modern");
    wrapper2.setServletClass("ModernServlet");

    Context context = new StandardContext();
    // StandardContext's start method adds a default mapper
    context.setPath("/myApp");
    context.setDocBase("myApp");

    context.addChild(wrapper1);
    context.addChild(wrapper2);

    // context.addServletMapping(pattern, name);
    context.addServletMapping("/Primitive", "Primitive");
    context.addServletMapping("/Modern", "Modern");
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

    try {
      connector.initialize();
      ((Lifecycle) connector).start();
      ((Lifecycle) context).start();
      // now we want to know some details about WebappLoader
      WebappClassLoader classLoader = (WebappClassLoader) loader.getClassLoader();
      System.out.println("Resources' docBase: " + ((ProxyDirContext)classLoader.getResources()).getDocBase());
      String[] repositories = classLoader.findRepositories();
      for (int i=0; i<repositories.length; i++) {
        System.out.println("  repository: " + repositories[i]);
      }

      // make the application wait until we press a key.
      System.in.read();
      ((Lifecycle) context).stop();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}