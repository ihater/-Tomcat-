package ex05.pyrmont.core;

import java.io.IOException;
import javax.servlet.ServletException;
import org.apache.catalina.Contained;
import org.apache.catalina.Container;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Valve;
import org.apache.catalina.ValveContext;
/**
 * 熟悉Servlet的人一定接触过Servlet filter，
 * 在Servlet处理请求之前，先会由filter“过滤”一下。tomcat内部同样也有类似的东西，那就是Valve——阀门
 *
 * 管道包含该Servlet容器将要调用的任务，一个阀表示一个具体的任务（73页）
 * 可以通过配置Server.xml配置文件，来动态地添加阀
 * 
 * 怎么调用管道的阀：SimpleContext的620行,当前的62行
 * Connector调用容器的invoke方法，把Request给容器，容器再把Request对象给自身的pipeline
 * 
 * 在pipeline内部有个内部类ValveContext，它来管理所有的Valve
 */
public class SimplePipeline implements Pipeline {

  public SimplePipeline(Container container) {
    setContainer(container);
  }

  // The basic Valve (if any) associated with this Pipeline.
  protected Valve basic = null;
  // The Container with which this Pipeline is associated.
  protected Container container = null;
  // the array of Valves
  protected Valve valves[] = new Valve[0];

  public void setContainer(Container container) {
    this.container = container;
  }

  public Valve getBasic() {
    return basic;
  }

  public void setBasic(Valve valve) {
    this.basic = valve;
    ((Contained) valve).setContainer(container);
  }

  public void addValve(Valve valve) {
    if (valve instanceof Contained)
      ((Contained) valve).setContainer(this.container);

    synchronized (valves) {
      Valve results[] = new Valve[valves.length +1];
      System.arraycopy(valves, 0, results, 0, valves.length);
      results[valves.length] = valve;
      valves = results;
    }
  }

  public Valve[] getValves() {
    return valves;
  }

  /**
   *  Connector调用容器的invoke方法，把Request给容器，容器再把Request对象给自身的pipeline
   */
  public void invoke(Request request, Response response)
    throws IOException, ServletException {
    // Invoke the first Valve in this pipeline for this request
//	  然后，在pipeline内部有个内部类ValveContext，它来管理所有的Valve。
//	  pipeline一般会调用ValveContext的invokeNext
    (new SimplePipelineValveContext()).invokeNext(request, response);
  }

  public void removeValve(Valve valve) {
  }

  // this class is copied from org.apache.catalina.core.StandardPipeline class's
  // StandardPipelineValveContext inner class.
  /**
   * Valve的遍历：
   * 理论上，可以用for循环来完成 阀值 的 遍历的，但是Tomcat的设计者，用了ValveContext来遍历阀值，其实也是遍历
   * 当连接器 调用 容器的 invoke() 方法，就完成了阀值的遍历
   * ValveContext可以保证阀值只被调用一次
   */
  protected class SimplePipelineValveContext implements ValveContext {

    protected int stage = 0;// 当前正在调用的阀值，ValveContext会循环遍历时叠加

    public String getInfo() {
      return null;		//重写返回阀值的信息
    }

    // 重写阀值的遍历
    public void invokeNext(Request request, Response response)
      throws IOException, ServletException {
      int subscript = stage;
      stage = stage + 1;
      // Invoke the requested Valve for the current request thread
      if (subscript < valves.length) {
        valves[subscript].invoke(request, response, this);
      }
      else if ((subscript == valves.length) && (basic != null)) {
        basic.invoke(request, response, this);
      }
      else {
        throw new ServletException("No valve");
      }
    }
  } // end of inner class

}