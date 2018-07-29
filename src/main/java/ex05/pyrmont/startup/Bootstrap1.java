package ex05.pyrmont.startup;

import ex05.pyrmont.core.SimpleLoader;
import ex05.pyrmont.core.SimpleWrapper;
import ex05.pyrmont.valves.ClientIPLoggerValve;
import ex05.pyrmont.valves.HeaderLoggerValve;
import org.apache.catalina.Loader;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.http.HttpConnector;
/**
 * https://blog.csdn.net/wangchengsi/article/details/4038274
 * 
 * Servlet 容器是用来处理请求Servlet资源，并为 Web 客户端填充 response对象的模块
 * Servlet容器是org.apache.catalina.Container 接口的实例
 * 在Tomcat中，容器有四种：Engine，Host，Context，Wrapper
 * 
 *    engine：表示一整个Catalina Servlet引擎
 *    host：表示一个虚拟主机。什么是虚拟主机可以百度一下“tomcat 虚拟主机配置”
 *    Context：表示一个web app应用，比如你做的一个网站，一个context中可以有多个wrapper；
 *    Wrapper：表示单个Servlet
 *  上面四个概念层级都是catalina的一个接口，它们都位于org.apache.catalina包，都继承Container接口
 *  这四个接口，有四个标准是实现:StandardEngine，StandardHost，StandardContext，StandardWrapper
 *  
 *  
 *  http://www.bitscn.com/pdb/java/200605/23339.html
 *  Container: 当http connector把需求传递给顶级的container: Engin的时候， 我们的视线就应该移动到Container这个层面来了。
 *		　在Container这个层， 我们包含了3种容器： Engin, Host, Context.
 *　		 	Engin: 收到service传递过来的需求， 处理后， 将结果返回给service( service 是通过 connector 这个媒介来和Engin互动的 ).
 *　			Host: Engin收到service传递过来的需求后，不会自己处理， 而是交给合适的Host来处理。
 *　				Host在这里就是虚拟主机的意思， 通常我们都只会使用一个主机，既“localhost”本地机来处理。
 *　			Context: Host接到了从Host传过来的需求后， 也不会自己处理， 而是交给合适的Context来处理。
 *					  表示一个web应用。一个context中可以有多个wrapper；
 *　
 *  
 */
public final class Bootstrap1 {
  public static void main(String[] args) {

/* call by using http://localhost:8080/ModernServlet,
   but could be invoked by any name */

    HttpConnector connector = new HttpConnector();
    Wrapper wrapper = new SimpleWrapper();
    wrapper.setServletClass("ModernServlet");
    Loader loader = new SimpleLoader();
    Valve valve1 = new HeaderLoggerValve();
    Valve valve2 = new ClientIPLoggerValve();// 在这里显示IP

    wrapper.setLoader(loader);
    ((Pipeline) wrapper).addValve(valve1);
    ((Pipeline) wrapper).addValve(valve2);

    connector.setContainer(wrapper);

    try {
      connector.initialize();
      connector.start();

      // make the application wait until we press a key.
      System.in.read();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}