package ex03.pyrmont.connector.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.util.StringManager;

import ex03.pyrmont.ServletProcessor;
import ex03.pyrmont.StaticResourceProcessor;

/**
 * 连接器的支持类 ，会调用HttpRequest生成实例
 * HttpProcessor使用其parse()方法解析HTTP请求中的请求行和请求头信息，解析完通过set存在 HttpRequest实体中
 * 
 * HttpRequest会被转换成 HTTPServletRequest类型，传入被调用的  Servlet实例 的service方法
 * 所以这个类，必须正确解析设置每个成员变量，以供Servlet使用，比如
 * URI、查询字符串、参数、Cookie，请求头信息等，但是不会解析任何请求参数
 * 因为Servlet不需要用到请求参数，不必要浪费CPU做无谓的解析（ServletProcessor调用servlet的service）
 * 
 * 该类的process防范，接收来自传入 HTTP请求的额接字，对每个HTTP请求，都要
 * 创建一个HttpRequest对象
 * 创建 一个HTTPResponse对象
 * 解析HTTP请求的第一行内容和请求头信息，填充HttpRequest对象
 * 将HttpRequest对象和HTTPResponse对象传递给ServletProcess或者staticResourcesProcess的process方法
 * 
 * parseHeaders()有知识点
 * parseRequest() 有笔记
 */
/* this class used to be called HttpServer */
public class HttpProcessor {

  public HttpProcessor(HttpConnector connector) {
    this.connector = connector;
  }
  /**
   * The HttpConnector with which this processor is associated.
   */
  private HttpConnector connector = null;
  private HttpRequest request;//表示HTTP请求对象
  private HttpRequestLine requestLine = new HttpRequestLine();
  private HttpResponse response;

  protected String method = null;
  protected String queryString = null;

  /**
   * The string manager for this package.
   */	
  protected StringManager sm =
    StringManager.getManager("ex03.pyrmont.connector.http");

  public void process(Socket socket) {
    SocketInputStream input = null;
    OutputStream output = null;
    try {
      input = new SocketInputStream(socket.getInputStream(), 2048);
      output = socket.getOutputStream();

      // create HttpRequest object and parse
      request = new HttpRequest(input);

      // create HttpResponse object
      response = new HttpResponse(output);
      response.setRequest(request);

      response.setHeader("Server", "Pyrmont Servlet Container");

      parseRequest(input, output);
      parseHeaders(input);

      //check if this is a request for a servlet or a static resource
      //a request for a servlet begins with "/servlet/"
      if (request.getRequestURI().startsWith("/servlet/")) {
        ServletProcessor processor = new ServletProcessor();
        processor.process(request, response);
      }
      else {
        StaticResourceProcessor processor = new StaticResourceProcessor();
        processor.process(request, response);
      }

      // Close the socket
      socket.close();
      // no shutdown for this application
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * This method is the simplified version of the similar method in
   * org.apache.catalina.connector.http.HttpProcessor.
   * However, this method only parses some "easy" headers, such as
   * "cookie", "content-length", and "content-type", and ignore other headers.
   * @param input The input stream connected to our socket
   *
   * @exception IOException if an input/output error occurs
   * @exception ServletException if a parsing error occurs
   */
  /**
   * 解析请求头 
   * SocketInputStream提供两个重要的方法。分别是readRequestLine和readHeader
   * readRequestLine：返回HTTP 请求的第一行，包括URI，请求方法和HTTP本版本
   * HttpHeader：返回HttpHead对象
   */
  private void parseHeaders(SocketInputStream input)
    throws IOException, ServletException {
/**
 * 注意！！！parseHeaders的方法体，是一个 while() 循环，不断地从SocketInput读取请求头
 * 直到全部读完，循环开始时，会创建HttpHeader对象，然后将对象传递给SocketInputStream的readHeader()方法
 */
	  while (true) {
      HttpHeader header = new HttpHeader();;

      // Read the next header
//创建HttpHeader实例之后，可以将其传递个SocketInputStream的readHeader()
//      若有请求头信息可以读取，readHead()方法会相应的填充HttpHeader对象
//      若没有都东西可以读取  nameEnd  和 valueEnd 都会是  0
      
// 可以通过检查HttpHeader实例的namedEnd和valueEnd字段来怕近端是否已经读取所有的请求头信息
      input.readHeader(header);
      if (header.nameEnd == 0) {
        if (header.valueEnd == 0) {
          return;
        }
        else {
          throw new ServletException
            (sm.getString("httpProcessor.parseHeaders.colon"));
        }
      }

//      若还要请求头没有读取，可以使用下面的方法获取请求头的名称和值
      String name = new String(header.name, 0, header.nameEnd);
      String value = new String(header.value, 0, header.valueEnd);
      request.addHeader(name, value);
      
      // do something for some headers, ignore others.
//      请求头包含一些属性设置的信息。例如，我们通过API：javax.servlet.ServletRequest类
//      的getContentLength() 方法时，应该返回 请求头的 "content-lenght"的值
//      还有请求头的"Cookie"，需要我们设置Cookie的集合，以下是处理过程
      if (name.equals("cookie")) {
        Cookie cookies[] = RequestUtil.parseCookieHeader(value);
        for (int i = 0; i < cookies.length; i++) {
          if (cookies[i].getName().equals("jsessionid")) {
            // Override anything requested in the URL
//        	  覆盖URL中请求的任何内容，因为之前解析URI的时候，有一次判断jessionid
            if (!request.isRequestedSessionIdFromCookie()) {
              // Accept only the first session id cookie
//            	既然请求头里有jessionid，我们就要覆盖之前的设置
              request.setRequestedSessionId(cookies[i].getValue());
              request.setRequestedSessionCookie(true);
              request.setRequestedSessionURL(false);
            }
          }
          request.addCookie(cookies[i]);
        }
      }
      else if (name.equals("content-length")) {
        int n = -1;
        try {
          n = Integer.parseInt(value);
        }
        catch (Exception e) {
          throw new ServletException(sm.getString("httpProcessor.parseHeaders.contentLength"));
        }
        request.setContentLength(n);
      }
      else if (name.equals("content-type")) {
        request.setContentType(value);
      }
    } //end while
  }

/**
 * parseRequest会解析请求行，并给HttpRequest实体进行赋值
 */
  private void parseRequest(SocketInputStream input, OutputStream output)
    throws IOException, ServletException {

    // Parse the incoming request line
//	  获取请求的方法，URI和协议版本
    input.readRequestLine(requestLine);
    String method =
      new String(requestLine.method, 0, requestLine.methodEnd);
    String uri = null;
    String protocol = new String(requestLine.protocol, 0, requestLine.protocolEnd);

    // Validate the incoming request line
    if (method.length() < 1) {
      throw new ServletException("Missing HTTP request method");
    }
    else if (requestLine.uriEnd < 1) {
      throw new ServletException("Missing HTTP request URI");
    }
    
    // Parse any query parameters out of the request URI
//  但是，在URI后面可能有一个查询字符串，就是？后面的查询内容
//    parseRequest() 方法会先调用HttpRequest类的setQueryString() 方法来回去字符串
//    并填充到HttpRequest对象中。
    int question = requestLine.indexOf("?");
    if (question >= 0) {
    	/**
    	 * 分配一个新的String，其中包含字符数组参数的子数组中的字符。
    	 *  offset参数是子数组的第一个字符的索引，count参数指定子数组的长度。 
    	 *  复制子阵列的内容; 后续修改字符数组不会影响新创建的字符串。
    	 *  参数：value作为characters、offset源数组(offset)的初始、count长度
    	 */
      request.setQueryString(new String(requestLine.uri, question + 1,
        requestLine.uriEnd - question - 1));
      uri = new String(requestLine.uri, 0, question);
    }
    else {
      request.setQueryString(null);
      uri = new String(requestLine.uri, 0, requestLine.uriEnd);
    }


    // Checking for an absolute URI (with the HTTP protocol)
//    但是，绝大多数的URI都是指向一个相对路径中的资源
    if (!uri.startsWith("/")) {
      int pos = uri.indexOf("://");
      // Parsing out protocol and host name   解析协议和主机名
      if (pos != -1) {
        pos = uri.indexOf('/', pos + 3);
        if (pos == -1) {
          uri = "";
        }
        else {
          uri = uri.substring(pos);
        }
      }
    }

    // Parse any requested session ID out of the request URI
//    接着，查询会话标识符、从请求URI中解析任何请求的会话ID
//    若存在jessionid，则表明会话标识符在查询字符串中，而不在cookie中
//    为此，需要调用setRequestSessionURL() 方法传入true值，表明session在URL中
    String match = ";jsessionid=";
    int semicolon = uri.indexOf(match);
    if (semicolon >= 0) {
      String rest = uri.substring(semicolon + match.length());
      int semicolon2 = rest.indexOf(';');
      if (semicolon2 >= 0) {
        request.setRequestedSessionId(rest.substring(0, semicolon2));
        rest = rest.substring(semicolon2);
      }
      else {
        request.setRequestedSessionId(rest);
        rest = "";
      }
      request.setRequestedSessionURL(true);
      uri = uri.substring(0, semicolon) + rest;
    }
    else {
      request.setRequestedSessionId(null);
      request.setRequestedSessionURL(false);//打断点，看jessionid解析
    }

    // Normalize URI (using String operations at the moment)
//    normalize()方法会将非正常的URL进行修正。比如  \  换成  /  
//    如果URI无法被修正，则会认为它是一个无效的请求，normalize方法则会返回 null
//    这时，parseRequest()方法则会在末尾抛出异常
    String normalizedUri = normalize(uri);// 打断点，看非法URI更正

    // Set the corresponding request properties
    ((HttpRequest) request).setMethod(method);
    request.setProtocol(protocol);
    if (normalizedUri != null) {		//为空抛出异常
      ((HttpRequest) request).setRequestURI(normalizedUri);
    }
    else {
      ((HttpRequest) request).setRequestURI(uri);
    }

    if (normalizedUri == null) {
      throw new ServletException("Invalid URI: " + uri + "'");
    }
  }

  /**
   * Return a context-relative path, beginning with a "/", that represents
   * the canonical version of the specified path after ".." and "." elements
   * are resolved out.  If the specified path attempts to go outside the
   * boundaries of the current context (i.e. too many ".." path elements
   * are present), return <code>null</code> instead.
   *
   * @param path Path to be normalized
   */
  protected String normalize(String path) {
    if (path == null)
      return null;
    // Create a place for the normalized path
    String normalized = path;

    // Normalize "/%7E" and "/%7e" at the beginning to "/~"
    if (normalized.startsWith("/%7E") || normalized.startsWith("/%7e"))
      normalized = "/~" + normalized.substring(4);

    // Prevent encoding '%', '/', '.' and '\', which are special reserved
    // characters
    if ((normalized.indexOf("%25") >= 0)
      || (normalized.indexOf("%2F") >= 0)
      || (normalized.indexOf("%2E") >= 0)
      || (normalized.indexOf("%5C") >= 0)
      || (normalized.indexOf("%2f") >= 0)
      || (normalized.indexOf("%2e") >= 0)
      || (normalized.indexOf("%5c") >= 0)) {
      return null;
    }

    if (normalized.equals("/."))
      return "/";

    // Normalize the slashes and add leading slash if necessary
    if (normalized.indexOf('\\') >= 0)
      normalized = normalized.replace('\\', '/');
    if (!normalized.startsWith("/"))
      normalized = "/" + normalized;

    // Resolve occurrences of "//" in the normalized path
    while (true) {
      int index = normalized.indexOf("//");
      if (index < 0)
        break;
      normalized = normalized.substring(0, index) +
        normalized.substring(index + 1);
    }

    // Resolve occurrences of "/./" in the normalized path
    while (true) {
      int index = normalized.indexOf("/./");
      if (index < 0)
        break;
      normalized = normalized.substring(0, index) +
        normalized.substring(index + 2);
    }

    // Resolve occurrences of "/../" in the normalized path
    while (true) {
      int index = normalized.indexOf("/../");
      if (index < 0)
        break;
      if (index == 0)
        return (null);  // Trying to go outside our context
      int index2 = normalized.lastIndexOf('/', index - 1);
      normalized = normalized.substring(0, index2) +
        normalized.substring(index + 3);
    }

    // Declare occurrences of "/..." (three or more dots) to be invalid
    // (on some Windows platforms this walks the directory tree!!!)
    if (normalized.indexOf("/...") >= 0)
      return (null);

    // Return the normalized path that we have completed
    return (normalized);

  }

}
