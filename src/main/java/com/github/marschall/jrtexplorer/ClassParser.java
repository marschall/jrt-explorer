package com.github.marschall.jrtexplorer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

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

    private final int ACC_PUBLIC = 0x0001;

    private DataInput input;

    private int u1() throws IOException {
        return this.input.readUnsignedByte();
    }

    private int u2() throws IOException {
        return this.input.readUnsignedShort();
    }

    private int u4() throws IOException {
        return this.input.readInt();
    }

    ParseResult parse(InputStream stream, Path path) throws IOException {
        this.input = new DataInputStream(stream);

        this.readMagic();
        this.readMinor();
        this.readMayor();

        int constantPoolCount = readConstantPoolCount();
        ConstantPool constantPool = this.readConstantPool(constantPoolCount, path);

        int accessFlags = this.readAccessFlags();
        int thisClass = readThisClass();
        int classNameIndex = constantPool.classNameIndices[thisClass - 1];
        String className = new String(constantPool.strings[classNameIndex - 1], StandardCharsets.UTF_8);
        return new ParseResult(className, this.isPublic(accessFlags));
    }

    private void readMagic() throws IOException {
        int magic = this.u4();
        if (magic != 0xCAFEBABE) {
            throw new IllegalStateException("unexpected magic: " + Integer.toHexString(magic));
        }
    }

    private void readMinor() throws IOException {
        this.u2();
    }

    private void readMayor() throws IOException {
        this.u2();
    }

    private int readConstantPoolCount() throws IOException {
        return this.u2();
    }

    private ConstantPool readConstantPool(int constantPoolCount, Path path) throws IOException {
        ConstantPool constantPool = new ConstantPool(constantPoolCount);
        for (int i = 0; i < constantPoolCount - 1; ++i) {
            readConstantPoolEntry(i, constantPool, path);
        }
        return constantPool;
    }

    static final class ConstantPool {
        final byte[][] strings;
        final int[] classNameIndices;

        ConstantPool(int constantPoolCount) {
            this.strings = new byte[constantPoolCount - 1][];
            this.classNameIndices = new int[constantPoolCount - 1];
        }
    }

    private void readConstantPoolEntry(int index, ConstantPool parsed, Path path) throws IOException {
        int tag = this.readConstantPoolTag();
        switch (tag) {
            case CONSTANT_Class:
                int nameIndex = this.u2();
                parsed.classNameIndices[index] = nameIndex;
                break;
            case CONSTANT_Fieldref:
                int classIndex = this.u2();
                int nameAndTypeIndex = this.u2();
                break;
            case CONSTANT_Methodref:
                classIndex = this.u2();
                nameAndTypeIndex = this.u2();
                break;
            case CONSTANT_InterfaceMethodref:
                classIndex = this.u2();
                nameAndTypeIndex = this.u2();
                break;
            case CONSTANT_String:
                int stringIndex = this.u2();
                break;
            case CONSTANT_Integer:
                int bytes = this.u4();
                break;
            case CONSTANT_Float:
                bytes = this.u4();
                break;
            case CONSTANT_Long:
                int highBytes = this.u4();
                int lowBytes = this.u4();
                break;
            case CONSTANT_Double:
                highBytes = this.u4();
                lowBytes = this.u4();
                break;
            case CONSTANT_NameAndType:
                nameIndex = this.u2();
                int descriptorIndex = this.u2();
                break;
            case CONSTANT_Utf8:
                int length = this.u2();
                byte[] utf8 = new byte[length];
                this.input.readFully(utf8, 0, length);
                parsed.strings[index] = utf8;
                break;
            case CONSTANT_MethodHandle:
                int referenceKind = this.u2();
                int referenceIndex = this.u2();
                break;
            case CONSTANT_MethodType:
                descriptorIndex = this.u2();
                break;
            case CONSTANT_InvokeDynamic:
                int bootstrapMethodAttrIndex = this.u2();
                int name_and_type_index = this.u2();
                break;
            default:
                throw new IllegalStateException("unexpected tag: " + tag + " at index: " + index + " for path: " + path);
        }
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

    private boolean isPublic(int accessFlags) {
        return (accessFlags & ACC_PUBLIC) != 0;
    }
}