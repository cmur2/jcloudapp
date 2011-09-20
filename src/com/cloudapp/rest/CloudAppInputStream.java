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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.http.entity.mime.content.InputStreamBody;

public class CloudAppInputStream extends InputStreamBody {

  private long length;

  public CloudAppInputStream(InputStream in, String mimeType, String filename, long length) {
    super(in, mimeType, filename);
    this.length = length;
  }
  
  public CloudAppInputStream(InputStream in, String filename, long length) {
    super(in, filename);
    this.length = length;
  }

  protected CloudAppInputStream(File file) throws FileNotFoundException {
    this(new FileInputStream(file), file.getName(), file.length());
  }

  @Override
  public long getContentLength() {
    return length;
  }

}
