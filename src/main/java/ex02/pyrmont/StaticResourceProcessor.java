package ex02.pyrmont;

import java.io.IOException;
/**
 * 用于 处理静态资源请求
 */
public class StaticResourceProcessor {

  public void process(Request request, Response response) {
    try {
      response.sendStaticResource();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }
}