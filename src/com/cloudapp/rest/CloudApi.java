/*
 *  Copyright (c) 2010 Simon Gaeremynck <gaeremyncks@gmail.com>
 *  
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */
package com.cloudapp.rest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.FileNameMap;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CloudApi {

  private static final Logger LOGGER = Logger.getLogger("CloudApi");

  private URI serviceUrl;
  private DefaultHttpClient client;

  public CloudApi(String mail, String pw, URI serviceUrl) {
    this.serviceUrl = serviceUrl;
    
    // SSL sucks
    SchemeRegistry schemeRegistry = SchemeRegistryFactory.createDefault();
    try {
      TrustManager tm = new X509TrustManager() { 
        public X509Certificate[] getAcceptedIssuers() { return null; }
        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
      };
      SSLContext sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, new TrustManager[] { tm }, null);
      SSLSocketFactory ssf = new SSLSocketFactory(sslContext);
      ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
      schemeRegistry.register(new Scheme("https", 443, ssf));
    } catch(NoSuchAlgorithmException ex) {
    } catch(KeyManagementException ex) {
    }
    
    client = new DefaultHttpClient(new SingleClientConnManager(schemeRegistry));
    client.setReuseStrategy(new DefaultConnectionReuseStrategy());
    HttpProtocolParams.setUserAgent(client.getParams(), "Wolke/7.0");

    // Try to authenticate.
    AuthScope scope = new AuthScope(serviceUrl.getHost(), serviceUrl.getPort());
    client.getCredentialsProvider().setCredentials(scope,
        new UsernamePasswordCredentials(mail, pw));
  }

  /**
   * Create a bookmark.
   * 
   * @param name
   *          The name of the bookmark.
   * @param url
   *          The URL of the bookmark
   * @return a JSONObject with the info that was return by the CloudApp servers.
   * @throws CloudApiException
   */
  public JSONObject createBookmark(String name, String url) throws CloudApiException {
    HttpPost request = null;
    try {
      // Apparently we have to post a JSONObject ..
      JSONObject item = new JSONObject();
      item.put("name", name);
      item.put("redirect_url", url);
      JSONObject bodyObj = new JSONObject();
      bodyObj.put("item", item);
      String body = bodyObj.toString(2);

      // Prepare the request.
      URI uri = URIUtils.createURI(serviceUrl.getScheme(),
          serviceUrl.getHost(), serviceUrl.getPort(), "/items", null, null);
      request = new HttpPost(uri);
      request.setHeader("Accept", "application/json");
      request.setEntity(new StringEntity(body));

      // Fire it
      HttpResponse response = client.execute(request);
      int status = response.getStatusLine().getStatusCode();
      body = EntityUtils.toString(response.getEntity());

      // Only 200 means success.
      if (status == 200) {
        return new JSONObject(body);
      }

      throw new CloudApiException(status, body, null);
    } catch (JSONException e) {
      LOGGER.log(Level.WARNING, "Error when trying to convert the return output to JSON.", e);
      throw new CloudApiException(500, e.getMessage(), e);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Unable to create bookmark.", e);
      throw new CloudApiException(500, e.getMessage(), e);
    } catch (URISyntaxException e) {
      LOGGER.log(Level.WARNING, "Unable to create bookmark.", e);
      throw new CloudApiException(500, e.getMessage(), e);
    } finally {
      if (request != null) {
        request.abort();
      }
    }
  }

  /**
   * Upload a file to the CloudApp servers.
   * 
   * @param file
   *          The file that should be stored.
   * @return A JSONObject with the returned output from the CloudApp servers.
   * @throws CloudApiException
   */
  public JSONObject uploadFile(File file) throws CloudApiException {
    try {
      FileNameMap map = URLConnection.getFileNameMap();
      String type = map.getContentTypeFor(file.getName());
      if (type == null) {
    	type = "application/octet-stream";
      }
      CloudAppInputStream input = new CloudAppInputStream(file, type);
      return uploadFile(input);
    } catch (FileNotFoundException e) {
      LOGGER.log(Level.WARNING, "The provided file could not be found.", e);
      throw new CloudApiException(500, e.getMessage(), e);
    }
  }

  /**
   * Upload a file to the CloudApp servers.
   * 
   * @param input
   *          The inputstream that holds the content that should be stored on the server.
   * @param filename
   *          The name of this file. i.e.: README.txt
   * @return A JSONObject with the returned output from the CloudApp servers.
   * @throws CloudApiException
   */
  @SuppressWarnings("rawtypes")
  public JSONObject uploadFile(CloudAppInputStream stream) throws CloudApiException {
    HttpGet keyRequest = null;
    HttpPost uploadRequest = null;
    try {
      // Get a key for the file first.
      keyRequest = new HttpGet(URIUtils.resolve(serviceUrl, "/items/new"));
      keyRequest.addHeader("Accept", "application/json");

      // Execute the request.
      HttpResponse response = client.execute(keyRequest);
      int status = response.getStatusLine().getStatusCode();
      if (status == 200) {
        String body = EntityUtils.toString(response.getEntity());
        JSONObject json = new JSONObject(body);

        // If this is a free account, we could exceed the daily amount.
        if (json.has("uploads_remaining") && json.getInt("uploads_remaining") == 0) {
          throw new CloudApiException(403, "You have exceeded your number of uploads.",
              null);
        }

        // From the API docs
        // Use this response to construct the upload. Each item in params becomes a
        // separate parameter you'll need to post to url. Send the file as the parameter
        // file.
        String url = json.getString("url");
        JSONObject params = json.getJSONObject("params");
        MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        // Add all the plain parameters.
        Iterator keys = params.keys();
        while (keys.hasNext()) {
          String key = (String) keys.next();
          entity.addPart(key, new StringBody(params.getString(key)));
        }

        // Add the actual file.
        // We have to use the 'file' parameter for the S3 storage.
        entity.addPart("file", stream);

        uploadRequest = new HttpPost(url);
        uploadRequest.addHeader("Accept", "application/json");
        uploadRequest.setEntity(entity);

        // Perform the actual upload.
        // uploadMethod.setFollowRedirects(true);
        response = client.execute(uploadRequest);
        status = response.getStatusLine().getStatusCode();
        body = EntityUtils.toString(response.getEntity());
        if (status == 200) {
          return new JSONObject(body);
        }
        throw new CloudApiException(status, "Was unable to upload the file:\n" + body, null);

      }
      throw new CloudApiException(500,
          "Was unable to retrieve a key from CloudApp to upload a file.", null);

    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Error when trying to upload a file.", e);
      throw new CloudApiException(500, e.getMessage(), e);
    } catch (JSONException e) {
      LOGGER.log(Level.WARNING, "Error when trying to convert the return output to JSON.", e);
      throw new CloudApiException(500, e.getMessage(), e);
    } finally {
      if (keyRequest != null) {
        keyRequest.abort();
      }
      if (uploadRequest != null) {
        uploadRequest.abort();
      }
    }
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see com.cloudapp.rest.CloudApi#getItems()
   */
  public JSONArray getItems(int page, int itemsPerPage, String type, boolean showDeleted)
      throws CloudApiException {
    HttpGet request = null;
    try {
      // Sanitize the parameters
      if (page < 1) {
        page = 1;
      }
      if (itemsPerPage < 1) {
        itemsPerPage = 1;
      }

      // Prepare the list of parameters.
      List<NameValuePair> nvps = new ArrayList<NameValuePair>();
      nvps.add(new BasicNameValuePair("page", "" + page));
      nvps.add(new BasicNameValuePair("per_page", "" + itemsPerPage));
      if (type != null) {
        nvps.add(new BasicNameValuePair("type", type));
      }
      nvps.add(new BasicNameValuePair("deleted", (showDeleted) ? "true" : "false"));

      // Prepare the URI (the host and querystring.)
      URI uri = URIUtils.createURI(serviceUrl.getScheme(),
          serviceUrl.getHost(), serviceUrl.getPort(), "/items",
          URLEncodedUtils.format(nvps, "UTF-8"), null);

      // Prepare the request.
      request = new HttpGet(uri);
      request.addHeader("Accept", "application/json");

      // Perform the request.
      HttpResponse response = client.execute(request);
      response.addHeader("Accept", "application/json");
      int status = response.getStatusLine().getStatusCode();
      String body = EntityUtils.toString(response.getEntity(), "UTF-8");

      // We always need 200 for items retrieval.
      if (status == 200) {
        return new JSONArray(body);
      }

      // Anything else is a failure.
      throw new CloudApiException(status, body, null);

    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Error when trying to retrieve the items.", e);
      throw new CloudApiException(500, e.getMessage(), e);
    } catch (JSONException e) {
      LOGGER.log(Level.WARNING, "Error when trying to parse the response as JSON.", e);
      throw new CloudApiException(500, e.getMessage(), e);
    } catch (URISyntaxException e) {
      LOGGER.log(Level.WARNING, "Error when trying to retrieve the items.", e);
      throw new CloudApiException(500, e.getMessage(), e);
    } finally {
      if (request != null) {
        request.abort();
      }
    }
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see com.cloudapp.rest.CloudApi#getItem(java.lang.String)
   */
  public JSONObject getItem(String url) throws CloudApiException {
    // No need to be authenticated to retrieve this item.
    HttpGet request = new HttpGet(url);
    request.addHeader("Accept", "application/json");

    try {
      // Perform the actual GET.
      HttpResponse response = client.execute(request);
      int status = response.getStatusLine().getStatusCode();
      String body = EntityUtils.toString(response.getEntity(), "UTF-8");

      // We're really only interested in 200 responses.
      if (status == 200) {
        return new JSONObject(body);
      }

      // If the status is not 200, that means there is a failure.
      throw new CloudApiException(status, body, null);

    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Error when trying to retrieve an item.", e);
      throw new CloudApiException(500, e.getMessage(), e);
    } catch (JSONException e) {
      LOGGER.log(Level.WARNING, "Error when trying to parse the response as JSON.", e);
      throw new CloudApiException(500, e.getMessage(), e);
    } finally {
      if (request != null) {
        request.abort();
      }
    }

  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see com.cloudapp.rest.CloudApi#deleteItem(java.lang.String)
   */
  public void deleteItem(String href) throws CloudApiException {
    HttpDelete request = null;
    try {
      // To delete an item we just have to a a DELETE request to http//my.cl.ly/id.
      request = new HttpDelete(href);
      HttpResponse response = client.execute(request);
      int status = response.getStatusLine().getStatusCode();

      // If it isn't a 302 it failed.
      if (status != 302) {
        String body = EntityUtils.toString(response.getEntity());
        throw new CloudApiException(status, body, null);
      }
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Error when trying to delete an item.", e);
      throw new CloudApiException(500, e.getMessage(), null);
    } finally {
      if (request != null) {
        request.abort();
      }
    }

  }

}
