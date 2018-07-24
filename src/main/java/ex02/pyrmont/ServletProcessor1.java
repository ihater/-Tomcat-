package ex02.pyrmont;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;

import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class ServletProcessor1 {

  public void process(Request request, Response response) {

    String uri = request.getUri();//Uri的格式大概 类似：/servilet/servletName资源名
    String servletName = uri.substring(uri.lastIndexOf("/") + 1);//载入Servlet
    
    //URLCLassLoader为ClassLoader的直接子类
    //用它的loadClass（）方法来载入servlet类
    URLClassLoader loader = null;

    try {
      // create a URLClassLoader
      URL[] urls = new URL[1];
      URLStreamHandler streamHandler = null;
      File classPath = new File(Constants.WEB_ROOT);
      // the forming of repository is taken from the createClassLoader method in
      // org.apache.catalina.startup.ClassLoaderFactory
      // 生成仓库
      String repository = (new URL("file", null, classPath.getCanonicalPath() + File.separator)).toString() ;
      System.out.println("servlet的目录repository："+repository);
    
      // the code for forming the URL is taken from the addRepository method in
      // org.apache.catalina.loader.StandardClassLoader class.
      //  在Servlet容器中，类载入器查找Servlet类的目录被称为仓库Repository
      urls[0] = new URL(null, repository, streamHandler);
      loader = new URLClassLoader(urls);
    }
    catch (IOException e) {
      System.out.println(e.toString() );
    }
    Class myClass = null;
    try {
    	 System.out.println("需要载入的 servletName："+servletName);
      myClass = loader.loadClass(servletName);
    }
    catch (ClassNotFoundException e) {
      System.out.println(e.toString());
    }

    Servlet servlet = null;

    try {
      servlet = (Servlet) myClass.newInstance();
      /**
       * 据说这里这么写是不好的：因为：
       * request上转型为ServletRequest，那么Servlet的service依旧可以下转型为Request类型
       * 这样，在Servlet的service 方法里面，依旧可以调用Request里的独有的公有方法（比如parse）
       * 但是，这样是不可以的，我们既然上转型了，就是希望，只给service调用ServletRequest有的方法
       * 解决方法是创建Request和Response的外观类（外观类和原类实现同一个接口，在外观类中创建私有接口对象用原类进行赋值即可）。
       * 而在调用接口对象时使用外观类就不会导致原类的方法泄露
       */
      servlet.service((ServletRequest) request, (ServletResponse) response);
    }
    catch (Exception e) {
      System.out.println(e.toString());
    }
    catch (Throwable e) {
      System.out.println(e.toString());
    }

  }
}