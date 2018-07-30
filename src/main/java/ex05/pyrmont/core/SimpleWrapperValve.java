package ex05.pyrmont.core;

import java.io.IOException;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Valve;
import org.apache.catalina.ValveContext;
import org.apache.catalina.Contained;
import org.apache.catalina.Container;

/**
 *  SimpleWrapperValve是一个基础阀，专门用于出来了对 SimpleWrapper类的请求
 *  最主要的方法：invoke()方法
 *
 */
public class SimpleWrapperValve implements Valve, Contained {

  protected Container container;

  /**
   * 由于SimpleWrapperValve 是一个基础阀
   * 所以，invoke() 方法不需要调用传递给他的ValueContext实例（就是不需要实例的invokeNext()方法）
   * invoke() 方法会调用SimpleWrapper类的allocate() 方法来获取Wrapper实例表示的Servlet实例
   * 让后他调用Servlet实例的Service() 方法。
   * ！！！注意，是Wrapper实例的基础阀调用了Servlet的service()方法，而不是wrapper本身（81页）
   */
  public void invoke(Request request, Response response, ValveContext valveContext)
    throws IOException, ServletException {

    SimpleWrapper wrapper = (SimpleWrapper) getContainer();
    ServletRequest sreq = request.getRequest();
    ServletResponse sres = response.getResponse();
    Servlet servlet = null;
    HttpServletRequest hreq = null;
    if (sreq instanceof HttpServletRequest)
      hreq = (HttpServletRequest) sreq;
    HttpServletResponse hres = null;
    if (sres instanceof HttpServletResponse)
      hres = (HttpServletResponse) sres;

    // Allocate a servlet instance to process this request
    try {
      servlet = wrapper.allocate();
      if (hres!=null && hreq!=null) {
        servlet.service(hreq, hres);
      }
      else {
        servlet.service(sreq, sres);
      }
    }
    catch (ServletException e) {
    }
  }

  public String getInfo() {
    return null;
  }

  public Container getContainer() {
    return container;
  }

  public void setContainer(Container container) {
    this.container = container;
  }
}