package ex03.pyrmont.connector.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.apache.catalina.util.Enumerator;
import org.apache.catalina.util.ParameterMap;
import org.apache.catalina.util.RequestUtil;

/** this class copies methods from org.apache.catalina.connector.HttpRequestBase
 *  and org.apache.catalina.connector.http.HttpRequestImpl.
 *  The HttpRequestImpl class employs a pool of HttpHeader objects for performance
 *  These two classes will be explained in Chapter 4.
 *  
 *  此类从org.apache.catalina.connector.HttpRequestBase复制方法
 *  和org.apache.catalina.connector.http.HttpRequestImpl。
 *  HttpRequestImpl类使用HttpHeader对象池来提高性能
 *   这两个类将在第4章中解释。
 */
import ex03.pyrmont.connector.RequestStream;

/**
 * 这是HttpRequest请求信息的pojo类。带有请求信息的所有属性
 * 被外部调用通过set设置pojo的属性
 * 
 * 这里大部分都是空方法，在第四章会有完整的实现。
 * 
 * 这个类主要的工作是解析HTTP请求并填充HttpRequest对象
 * 对每个请求头和Cookie，在HttpRequest类里都提供了addHeaders()和addCookie()方法来填充信息
 * 这两个方法，会在HttpProcessor类的parseHeaders()方法中进行调用。
 * 当只有真正需要到请求参数时候，才会使用HttpRequest的parseParameters()方法解析请求参数
 */
public class HttpRequest implements HttpServletRequest {

  private String contentType;
  private int contentLength;
  private InetAddress inetAddress;
  private InputStream input;
  private String method;
  private String protocol;
  private String queryString;
  private String requestURI;
  private String serverName;
  private int serverPort;
  private Socket socket;
  private boolean requestedSessionCookie;
  private String requestedSessionId;
  private boolean requestedSessionURL;

  /**
   * The request attributes for this request.
   */
  protected HashMap attributes = new HashMap();
  /**
   * The authorization credentials sent with this Request.
   */
  protected String authorization = null;
  /**
   * The context path for this request.
   */
  protected String contextPath = "";
  /**
   * The set of cookies associated with this Request.
   */
  protected ArrayList cookies = new ArrayList();
  /**
   * An empty collection to use for returning empty Enumerations.  Do not
   * add any elements to this collection!
   */
  protected static ArrayList empty = new ArrayList();
  /**
   * The set of SimpleDateFormat formats to use in getDateHeader().
   */
  protected SimpleDateFormat formats[] = {
    new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
    new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
    new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US)
  };

  /**
   * The HTTP headers associated with this Request, keyed by name.  The
   * values are ArrayLists of the corresponding header values.
   */
  protected HashMap headers = new HashMap();
  /**
   * The parsed parameters for this request.  This is populated only if
   * parameter information is requested via one of the
   * <code>getParameter()</code> family of method calls.  The key is the
   * parameter name, while the value is a String array of values for this
   * parameter.
   * <p>
   * <strong>IMPLEMENTATION NOTE</strong> - Once the parameters for a
   * particular request are parsed and stored here, they are not modified.
   * Therefore, application level access to the parameters need not be
   * synchronized.
   */
  protected ParameterMap parameters = null;

  /**
   * Have the parameters for this request been parsed yet?
   */
  protected boolean parsed = false;
  protected String pathInfo = null;

  /**
   * The reader that has been returned by <code>getReader</code>, if any.
   */
  protected BufferedReader reader = null;

  /**
   * The ServletInputStream that has been returned by
   * <code>getInputStream()</code>, if any.
   */
  protected ServletInputStream stream = null;

  public HttpRequest(InputStream input) {
    this.input = input;
  }

  public void addHeader(String name, String value) {
    name = name.toLowerCase();
    synchronized (headers) {
      ArrayList values = (ArrayList) headers.get(name);
      if (values == null) {
        values = new ArrayList();
        headers.put(name, values);
      }
      values.add(value);
    }
  }

  /**
   * Parse the parameters of this request, if it has not already occurred.
   * If parameters are present in both the query string and the request
   * content, they are merged.
   */
  /**
   * 在调用HTTPServletRequest的 getParameter()  getParamaterMap()   getParameterNames()
   * 或者 getParameterValues() 这四个方法之前，我们是不需要解析请求体的参数的
   * 所以，在执行者四个方法之前，都会先调用parseParameter() 方法
   * 且有且只会调用一次
   */
  protected void parseParameters() {	//打断点，查看如何获取请求体参数
    if (parsed)							// 标识是否已经完成对请求体的解析
      return;
    //ParameterMap存储所有的键值对，但是，Servlet程序员不可以对ParameterMap里的数值
    // ParameterMap 是一个特殊的HashMap,里面有一个名为locked的变量   ,只有当locked是flase的时候
    // 才可以对ParameterMap里的数值进行修改    ，否则会抛出IllegalStateException 异常
    ParameterMap results = parameters;	// 可能其他操作已经赋值给parameters
    if (results == null)
      results = new ParameterMap();		// 如果没有任何操作过parameter，自己创新创建新的
    results.setLocked(false);			// 打开ParameterMap 的锁，使其可以写
    
    String encoding = getCharacterEncoding();	// 查看请求头的编码类型，若没有，使用默认
    if (encoding == null)
      encoding = "ISO-8859-1";

    // Parse any parameters specified in the query string
//    对参数进行解析，解析工作会调用 
//    org.apache.Catalina.util.RequestUtil类的parseParameters()来完成解析 
    String queryString = getQueryString();
    try {
      RequestUtil.parseParameters(results, queryString, encoding);
    }
    catch (UnsupportedEncodingException e) {
      ;
    }

    // Parse any parameters specified in the input stream
//    解析，parseParameters() 方法会检查HTTP请求体是否包含请求 参数
//    如果请求体有参数，则请求头的“content-length”的值就会大于 0 
//    下面的代码用于解析请求体
    String contentType = getContentType();
    if (contentType == null)
      contentType = "";
    int semicolon = contentType.indexOf(';');
    if (semicolon >= 0) {
      contentType = contentType.substring(0, semicolon).trim();
    }
    else {
      contentType = contentType.trim();
    }
    if ("POST".equals(getMethod()) && (getContentLength() > 0)
      && "application/x-www-form-urlencoded".equals(contentType)) {
      try {
        int max = getContentLength();
        int len = 0;
        byte buf[] = new byte[getContentLength()];
        ServletInputStream is = getInputStream();
        while (len < max) {
          int next = is.read(buf, len, max - len);
          if (next < 0 ) {
            break;
          }
          len += next;
        }
        is.close();
        if (len < max) {
          throw new RuntimeException("Content length mismatch");
        }
        RequestUtil.parseParameters(results, buf, encoding);
      }
      catch (UnsupportedEncodingException ue) {
        ;
      }
      catch (IOException e) {
        throw new RuntimeException("Content read fail");
      }
    }

    // Store the final results
    results.setLocked(true);
    parsed = true;
    parameters = results;
  }

  public void addCookie(Cookie cookie) {
    synchronized (cookies) {
      cookies.add(cookie);
    }
  }

  /**
   * Create and return a ServletInputStream to read the content
   * associated with this Request.  The default implementation creates an
   * instance of RequestStream associated with this request, but this can
   * be overridden if necessary.
   *
   * @exception IOException if an input/output error occurs
   */
  public ServletInputStream createInputStream() throws IOException {
    return (new RequestStream(this));
  }

  public InputStream getStream() {
    return input;
  }
  public void setContentLength(int length) {
    this.contentLength = length;
  }

  public void setContentType(String type) {
    this.contentType = type;
  }

  public void setInet(InetAddress inetAddress) {
    this.inetAddress = inetAddress;
  }

  public void setContextPath(String path) {
    if (path == null)
      this.contextPath = "";
    else
      this.contextPath = path;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public void setPathInfo(String path) {
    this.pathInfo = path;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public void setQueryString(String queryString) {
    this.queryString = queryString;
  }

  public void setRequestURI(String requestURI) {
    this.requestURI = requestURI;
  }
  /**
   * Set the name of the server (virtual host) to process this request.
   *
   * @param name The server name
   */
  public void setServerName(String name) {
    this.serverName = name;
  }
  /**
   * Set the port number of the server to process this request.
   *
   * @param port The server port
   */
  public void setServerPort(int port) {
    this.serverPort = port;
  }

  public void setSocket(Socket socket) {
    this.socket = socket;
  }

  /**
   * Set a flag indicating whether or not the requested session ID for this
   * request came in through a cookie.  This is normally called by the
   * HTTP Connector, when it parses the request headers.
   *
   * @param flag The new flag
   */
  public void setRequestedSessionCookie(boolean flag) {
    this.requestedSessionCookie = flag;
  }

  public void setRequestedSessionId(String requestedSessionId) {
    this.requestedSessionId = requestedSessionId;
  }

  public void setRequestedSessionURL(boolean flag) {
    requestedSessionURL = flag;
  }

  /* implementation of the HttpServletRequest*/
  public Object getAttribute(String name) {
    synchronized (attributes) {
      return (attributes.get(name));
    }
  }

  public Enumeration getAttributeNames() {
    synchronized (attributes) {
      return (new Enumerator(attributes.keySet()));
    }
  }

  public String getAuthType() {
    return null;
  }

  public String getCharacterEncoding() {
    return null;
  }

  public int getContentLength() {
    return contentLength ;
  }

  public String getContentType() {
    return contentType;
  }

  public String getContextPath() {
    return contextPath;
  }

  public Cookie[] getCookies() {
    synchronized (cookies) {
      if (cookies.size() < 1)
        return (null);
      Cookie results[] = new Cookie[cookies.size()];
      return ((Cookie[]) cookies.toArray(results));
    }
  }

  public long getDateHeader(String name) {
    String value = getHeader(name);
    if (value == null)
      return (-1L);

    // Work around a bug in SimpleDateFormat in pre-JDK1.2b4
    // (Bug Parade bug #4106807)
    value += " ";

    // Attempt to convert the date header in a variety of formats
    for (int i = 0; i < formats.length; i++) {
      try {
        Date date = formats[i].parse(value);
        return (date.getTime());
      }
      catch (ParseException e) {
        ;
      }
    }
    throw new IllegalArgumentException(value);
  }

  public String getHeader(String name) {
    name = name.toLowerCase();
    synchronized (headers) {
      ArrayList values = (ArrayList) headers.get(name);
      if (values != null)
        return ((String) values.get(0));
      else
        return null;
    }
  }

  public Enumeration getHeaderNames() {
    synchronized (headers) {
      return (new Enumerator(headers.keySet()));
    }
  }

  public Enumeration getHeaders(String name) {
    name = name.toLowerCase();
    synchronized (headers) {
      ArrayList values = (ArrayList) headers.get(name);
      if (values != null)
        return (new Enumerator(values));
      else
        return (new Enumerator(empty));
    }
  }

  public ServletInputStream getInputStream() throws IOException {
    if (reader != null)
      throw new IllegalStateException("getInputStream has been called");

    if (stream == null)
      stream = createInputStream();
    return (stream);
  }

  public int getIntHeader(String name) {
    String value = getHeader(name);
    if (value == null)
      return (-1);
    else
      return (Integer.parseInt(value));
  }

  public Locale getLocale() {
    return null;
  }

  public Enumeration getLocales() {
    return null;
  }

  public String getMethod() {
    return method;
  }

  public String getParameter(String name) {
    parseParameters();
    String values[] = (String[]) parameters.get(name);
    if (values != null)
      return (values[0]);
    else
      return (null);
  }

  public Map getParameterMap() {
    parseParameters();
    return (this.parameters);
  }

  public Enumeration getParameterNames() {
    parseParameters();
    return (new Enumerator(parameters.keySet()));
  }

  public String[] getParameterValues(String name) {
    parseParameters();
    String values[] = (String[]) parameters.get(name);
    if (values != null)
      return (values);
    else
      return null;
  }

  public String getPathInfo() {
    return pathInfo;
  }

  public String getPathTranslated() {
    return null;
  }

  public String getProtocol() {
    return protocol;
  }

  public String getQueryString() {
    return queryString;
  }

  public BufferedReader getReader() throws IOException {
    if (stream != null)
      throw new IllegalStateException("getInputStream has been called.");
    if (reader == null) {
      String encoding = getCharacterEncoding();
      if (encoding == null)
        encoding = "ISO-8859-1";
      InputStreamReader isr =
        new InputStreamReader(createInputStream(), encoding);
        reader = new BufferedReader(isr);
    }
    return (reader);
  }

  public String getRealPath(String path) {
    return null;
  }

  public String getRemoteAddr() {
    return null;
  }

  public String getRemoteHost() {
    return null;
  }

  public String getRemoteUser() {
    return null;
  }

  public RequestDispatcher getRequestDispatcher(String path) {
    return null;
  }

  public String getScheme() {
   return null;
  }

  public String getServerName() {
    return null;
  }

  public int getServerPort() {
    return 0;
  }

  public String getRequestedSessionId() {
    return null;
  }

  public String getRequestURI() {
    return requestURI;
  }

  public StringBuffer getRequestURL() {
    return null;
  }

  public HttpSession getSession() {
    return null;
  }

  public HttpSession getSession(boolean create) {
    return null;
  }

  public String getServletPath() {
    return null;
  }

  public Principal getUserPrincipal() {
    return null;
  }

  public boolean isRequestedSessionIdFromCookie() {
    return false;
  }

  public boolean isRequestedSessionIdFromUrl() {
    return isRequestedSessionIdFromURL();
  }

  public boolean isRequestedSessionIdFromURL() {
    return false;
  }

  public boolean isRequestedSessionIdValid() {
    return false;
  }

  public boolean isSecure() {
    return false;
  }

  public boolean isUserInRole(String role) {
    return false;
  }

  public void removeAttribute(String attribute) {
  }

  public void setAttribute(String key, Object value) {
  }

  /**
   * Set the authorization credentials sent with this request.
   *
   * @param authorization The new authorization credentials
   */
  public void setAuthorization(String authorization) {
    this.authorization = authorization;
  }

  public void setCharacterEncoding(String encoding) throws UnsupportedEncodingException {
  }

public long getContentLengthLong() {
	// TODO Auto-generated method stub
	return 0;
}

public int getRemotePort() {
	// TODO Auto-generated method stub
	return 0;
}

public String getLocalName() {
	// TODO Auto-generated method stub
	return null;
}

public String getLocalAddr() {
	// TODO Auto-generated method stub
	return null;
}

public int getLocalPort() {
	// TODO Auto-generated method stub
	return 0;
}

public ServletContext getServletContext() {
	// TODO Auto-generated method stub
	return null;
}

public AsyncContext startAsync() throws IllegalStateException {
	// TODO Auto-generated method stub
	return null;
}

public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
		throws IllegalStateException {
	// TODO Auto-generated method stub
	return null;
}

public boolean isAsyncStarted() {
	// TODO Auto-generated method stub
	return false;
}

public boolean isAsyncSupported() {
	// TODO Auto-generated method stub
	return false;
}

public AsyncContext getAsyncContext() {
	// TODO Auto-generated method stub
	return null;
}

public DispatcherType getDispatcherType() {
	// TODO Auto-generated method stub
	return null;
}

public String changeSessionId() {
	// TODO Auto-generated method stub
	return null;
}

public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
	// TODO Auto-generated method stub
	return false;
}

public void login(String username, String password) throws ServletException {
	// TODO Auto-generated method stub
	
}

public void logout() throws ServletException {
	// TODO Auto-generated method stub
	
}

public Collection<Part> getParts() throws IOException, ServletException {
	// TODO Auto-generated method stub
	return null;
}

public Part getPart(String name) throws IOException, ServletException {
	// TODO Auto-generated method stub
	return null;
}

public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
	// TODO Auto-generated method stub
	return null;
}
}
