package ex05.pyrmont.core;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import org.apache.catalina.Container;
import org.apache.catalina.Loader;
import org.apache.catalina.DefaultContext;
/**
 * Servlet容器中摘入相关的Servlet类的工作由load完成
 * SimpleLoader 负责完成类的载入工作，它知道Servlet类的位置
 * 
 */
public class SimpleLoader implements Loader {

  public static final String WEB_ROOT =
    System.getProperty("user.dir") + File.separator  + "webroot";//WEB_ROOT指定了Servlet所在的目录

  ClassLoader classLoader = null;
  Container container = null;	// Container指向与该Servlet容器相关的类加载器
  								// 类加载器的细节在第八章讲
  /**
   * 通过getClassLoader() 方法可以返回一个 ClassLoader实例
   * 可以用来搜索Servlet类的位置‘
   * 
   * SimpleLoader函数会初始化类加载器。供 SimpleWrapper实例使用
   */
  public SimpleLoader() {
    try {
      URL[] urls = new URL[1];
      URLStreamHandler streamHandler = null;
      File classPath = new File(WEB_ROOT);
//      初始化仓库
      String repository = (new URL("file", null, classPath.getCanonicalPath() + File.separator)).toString() ;
      urls[0] = new URL(null, repository, streamHandler);
      classLoader = new URLClassLoader(urls);
    }
    catch (IOException e) {
      System.out.println(e.toString() );
    }


  }

  public ClassLoader getClassLoader() {
    return classLoader;
  }

  public Container getContainer() {
    return container;
  }

  public void setContainer(Container container) {
    this.container = container;
  }

  public DefaultContext getDefaultContext() {
    return null;
  }

  public void setDefaultContext(DefaultContext defaultContext) {
  }

  public boolean getDelegate() {
    return false;
  }

  public void setDelegate(boolean delegate) {
  }

  public String getInfo() {
    return "A simple loader";
  }

  public boolean getReloadable() {
    return false;
  }

  public void setReloadable(boolean reloadable) {
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
  }

  public void addRepository(String repository) {
  }

  public String[] findRepositories() {
    return null;
  }

  public boolean modified() {
    return false;
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
  }

}