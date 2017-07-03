package com.github.marschall.jrtexplorer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Supplier;

class ClassParser {
    private final int CONSTANT_Class = 7;
    private final int CONSTANT_Fieldref = 9;
    private final int CONSTANT_Methodref = 10;
    private final int CONSTANT_InterfaceMethodref = 11;
    private final int CONSTANT_String = 8;
    private final int CONSTANT_Integer = 3;
    private final int CONSTANT_Float = 4;
    private final int CONSTANT_Long = 5;
    private final int CONSTANT_Double = 6;
    private final int CONSTANT_NameAndType = 12;
    private final int CONSTANT_Utf8 = 1;
    private final int CONSTANT_MethodHandle = 15;
    private final int CONSTANT_MethodType = 16;
    private final int CONSTANT_InvokeDynamic = 18;

    private static final int ACC_PUBLIC = 0x0001;
    private static final int ACC_ANNOTATION = 0x2000;

    private ReusableBufferedInputStream input;
    private int position;

    ClassParser() {
        this.input = new ReusableBufferedInputStream();
    }

    private int u1() throws IOException {
        int ch = this.input.read();
        if (ch < 0) {
            throw new EOFException();
        }
        this.position += 1;
        return ch;
    }

    private int u2() throws IOException {
        int ch1 = this.input.read();
        int ch2 = this.input.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        this.position += 2;
        return (ch1 << 8) + (ch2 << 0);
    }

    private int u4() throws IOException {
        int ch1 = this.input.read();
        int ch2 = this.input.read();
        int ch3 = this.input.read();
        int ch4 = this.input.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }
        this.position += 4;
        return (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0);
    }

    private void readFully(byte b[], int off, int len) throws IOException {
        if (len < 0) {
            throw new IndexOutOfBoundsException();
        }
        int n = 0;
        while (n < len) {
            int count = this.input.read(b, off + n, len - n);
            if (count < 0) {
                throw new EOFException();
            }
            n += count;
        }
        this.position += len;
    }

  private static void skipFully(InputStream in, long len) throws IOException {
    long skipped = 0;
    while (skipped != len) {
      skipped += in.skip(len - skipped);
    }
  }

    ParseResult parse(Supplier<InputStream> streamSupplier, Path path) throws IOException {
      ConstantPool constantPool;
      int accessFlags;
      int thisClass;
      try (InputStream stream = streamSupplier.get()) {
        this.input.setInputStream(stream);
        this.position = 0;

        this.readMagic();
        int minor = this.readMinor();
        int major = this.readMayor();

        int constantPoolCount = readConstantPoolCount();
        constantPool = this.readConstantPool(constantPoolCount, path);

        accessFlags = this.readAccessFlags();
        thisClass = readThisClass();
      }

        int classNameIndex = constantPool.classNameIndices[thisClass];
      Utf8Info info = constantPool.strings[classNameIndex];
        String className = readUtf8(info, streamSupplier);
        return new ParseResult(className, isPublic(accessFlags), isAnnotation(accessFlags));
    }

  private String readUtf8(Utf8Info info, Supplier<InputStream> streamSupplier) throws IOException {
    int length = info.length;
    byte[] buffer = new byte[length];
    try (InputStream in = streamSupplier.get()) {
      skipFully(in, info.position);
      int read = in.read(buffer, 0, length);
      while (read < length) {
        read += in.read(buffer, read, length - read);
      }
    }
    for (int i = 0; i < length; ++i) {
      byte b = buffer[i];
      if (b == '/') {
        buffer[i] = '.';
      }
    }
    return new String(buffer, StandardCharsets.UTF_8);
  }

    private void readMagic() throws IOException {
        int magic = this.u4();
        if (magic != 0xCAFEBABE) {
            throw new IllegalStateException("unexpected magic: " + Integer.toHexString(magic));
        }
    }

    private int readMinor() throws IOException {
        return this.u2();
    }

    private int readMayor() throws IOException {
        return this.u2();
    }

    private int readConstantPoolCount() throws IOException {
        return this.u2();
    }

  private int readAccessFlags() throws IOException {
    return this.u2();
  }

  private int readThisClass() throws IOException {
    return this.u2();
  }

  private int readConstantPoolTag() throws IOException {
    return this.u1();
  }

  private static boolean isPublic(int accessFlags) {
    return (accessFlags & ACC_PUBLIC) != 0;
  }

  private static boolean isAnnotation(int accessFlags) {
    return (accessFlags & ACC_ANNOTATION) != 0;
  }

    private ConstantPool readConstantPool(int constantPoolCount, Path path) throws IOException {
        ConstantPool constantPool = new ConstantPool(constantPoolCount);
        for (int i = 0; i < constantPoolCount - 1; ) {
            i += readConstantPoolEntry(i, constantPool, path);
        }
        return constantPool;
    }

    private int readConstantPoolEntry(int index, ConstantPool parsed, Path path) throws IOException {
        int tag = this.readConstantPoolTag();
        switch (tag) {
            case CONSTANT_Class:
                int nameIndex = this.u2();
                parsed.classNameIndices[index + 1] = nameIndex;
                return 1;
            case CONSTANT_Fieldref:
                int classIndex = this.u2();
                int nameAndTypeIndex = this.u2();
                return 1;
            case CONSTANT_Methodref:
                classIndex = this.u2();
                nameAndTypeIndex = this.u2();
                return 1;
            case CONSTANT_InterfaceMethodref:
                classIndex = this.u2();
                nameAndTypeIndex = this.u2();
                return 1;
            case CONSTANT_String:
                int stringIndex = this.u2();
                return 1;
            case CONSTANT_Integer:
                int bytes = this.u4();
                return 1;
            case CONSTANT_Float:
                bytes = this.u4();
                return 1;
            case CONSTANT_Long:
                int highBytes = this.u4();
                int lowBytes = this.u4();
                return 2;
            case CONSTANT_Double:
                highBytes = this.u4();
                lowBytes = this.u4();
                return 2;
            case CONSTANT_NameAndType:
                nameIndex = this.u2();
                int descriptorIndex = this.u2();
                return 1;
            case CONSTANT_Utf8:
                int length = this.u2();
              /*
                byte[] utf8 = new byte[length];
                this.readFully(utf8, 0, length);
                parsed.strings[index + 1] = utf8;
                */
              parsed.strings[index + 1] = new Utf8Info(this.position, length);
              skipFully(this.input, length);
              this.position += length;
                return 1;
            case CONSTANT_MethodHandle:
                int referenceKind = this.u1();
                int referenceIndex = this.u2();
                return 1;
            case CONSTANT_MethodType:
                descriptorIndex = this.u2();
                return 1;
            case CONSTANT_InvokeDynamic:
                int bootstrapMethodAttrIndex = this.u2();
                int name_and_type_index = this.u2();
                return 1;
            // FIXME value 20 missing for module-info.java
            default:
                throw new IllegalStateException("unexpected tag: " + tag + " at offset: " + this.position + " at index: " + index + " for path: " + path);
        }
    }

  static final class Utf8Info {

    final int position;
    final int length;

    Utf8Info(int position, int length) {
      this.position = position;
      this.length = length;
    }
  }

  static final class ConstantPool {
    final Utf8Info[] strings;
    final int[] classNameIndices;

    ConstantPool(int constantPoolCount) {
      this.strings = new Utf8Info[constantPoolCount];
      this.classNameIndices = new int[constantPoolCount];
    }
  }
}
