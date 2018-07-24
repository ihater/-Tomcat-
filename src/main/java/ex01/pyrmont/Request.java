package ex01.pyrmont;

import java.io.InputStream;
import java.io.IOException;

public class Request {

  private InputStream input;
  private String uri;

  public Request(InputStream input) {
    this.input = input;
  }

  /**
   *  parse()方法用于解析HTTP请求中的原始数据
   *  其中会调用getUri()方法，解析HTTP请求，返回URI存到uri
   */
  public void parse() {
    // Read a set of characters from the socket
    StringBuffer request = new StringBuffer(2048);
    int i;
    byte[] buffer = new byte[2048];
    try {
      i = input.read(buffer);//将从传入的InputStream中读取整个字节流，存到buffer缓冲区
    }
    catch (IOException e) {
      e.printStackTrace();
      i = -1;
    }
    for (int j=0; j<i; j++) {
      request.append((char) buffer[j]);//将缓冲器中的HTTP请求存到 StringBuffer
    }
    System.out.print(request.toString());
    uri = parseUri(request.toString());
  }

  /**
   *  parseUri是用来解析HTTP请求的URI的
   *  返回的是 请求 的 URI 
   *  具体实现是，该方法在请求中搜索第一个和第二个空格，从中找出URI，为什么是第一个和第二个空格呢？？？
   *  因为请求头的第一行，请求方式   资源  HTTP版本，这三个字段用 空格 隔开
   *  GET /index.html HTTP/1.1
   */
  private String parseUri(String requestString) {
    int index1, index2;
    index1 = requestString.indexOf(' ');
    if (index1 != -1) {
      index2 = requestString.indexOf(' ', index1 + 1);
      if (index2 > index1)
        return requestString.substring(index1 + 1, index2);
    }
    return null;
  }

  /**
   * 外部调用getUri 会放回HTTP 请求的URI 
   */
  public String getUri() {
    return uri;
  }

}