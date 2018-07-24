package ex01.pyrmont;

import java.io.OutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;

/*
  HTTP Response = Status-Line
    *(( general-header | response-header | entity-header ) CRLF)
    CRLF
    [ message-body ]
    Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
*/

public class Response {

  private static final int BUFFER_SIZE = 1024;
  Request request;
  OutputStream output;

  /***
   *	从HttPServer 中 接收一个 OutputStream 对象
   *	用传入的这个OutputStream输出流，来响应响应体
   */
  public Response(OutputStream output) {
    this.output = output;
  }


  public void setRequest(Request request) {
    this.request = request;
  }

  /**
   *	用于发送一个静态资源到浏览器，如HTML
   */
  public void sendStaticResource() throws IOException {
    byte[] bytes = new byte[BUFFER_SIZE];
    FileInputStream fis = null;
    try {
      // 通过URI，拼接WEB_ROOT，找到我们要回传的资源    
     // 例如访问index.html : D:\workspace\MavenDemo01\webroot\  index.html
      File file = new File(HttpServer.WEB_ROOT, request.getUri());
      if (file.exists()) {
        fis = new FileInputStream(file);
        int ch = fis.read(bytes, 0, BUFFER_SIZE);// 从文件流中读取文件到byte缓冲数组中
        while (ch!=-1) {						// 循环，直到文件全输出完
          output.write(bytes, 0, ch);			// 输出
          ch = fis.read(bytes, 0, BUFFER_SIZE); // 读文件
        }
      }
      else {
        // file not found
        String errorMessage = "HTTP/1.1 404 File Not Found\r\n" +
          "Content-Type: text/html\r\n" +
          "Content-Length: 23\r\n" +
          "\r\n" +
          "<h1>File Not Found</h1>";
        output.write(errorMessage.getBytes());
      }
    }
    catch (Exception e) {
      // thrown if cannot instantiate a File object
      System.out.println(e.toString() );
    }
    finally {
      if (fis!=null)
        fis.close();
    }
  }
}