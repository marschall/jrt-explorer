package com.github.marschall.jrtexplorer;


import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

class ReusableBufferedInputStream extends InputStream {

  private final byte[] buffer;

  private int position;
  private int end;

  private InputStream delegate;

  ReusableBufferedInputStream(int bufferSize) {
    this.buffer = new byte[bufferSize];
    this.position = 0;
    this.end = 0;
  }

  ReusableBufferedInputStream() {
    this(8192);
  }

  void setInputStream(InputStream delegate) {
    this.delegate = delegate;
    this.position = 0;
    this.end = 0;
  }

  private void ensureBuffer() throws IOException {
    if (this.end - this.position > 0) {
      return;
    }
    this.position = 0;
    this.end = this.delegate.read(this.buffer, 0, this.buffer.length);
    if (this.end == -1) {
      throw new EOFException("no more data");
    }
  }

  @Override
  public int read() throws IOException {
    this.ensureBuffer();
    return this.buffer[this.position++];
  }

  @Override
  public int read(byte b[], int off, int len) throws IOException {
    this.ensureBuffer();
    int actual = Math.min(this.available(), len);
    System.arraycopy(this.buffer, this.position, b, off, actual);
    this.position += actual;
    return actual;
  }

  @Override
  public int available() throws IOException {
    return this.end - this.position;
  }

  @Override
  public boolean markSupported() {
    return false;
  }

  @Override
  public synchronized void mark(int readlimit) {
    throw new UnsupportedOperationException("mark not supported");
  }

  @Override
  public synchronized void reset() {
    throw new UnsupportedOperationException("mark not supported");
  }
}
