package com.github.marschall.jrtexplorer;

import java.io.*;
import java.nio.charset.StandardCharsets;

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

    void parse(InputStream stream) throws IOException {
        this.input = new DataInputStream(stream);

        this.readMagic();
        this.readMinor();
        this.readMayor();

        int constantPoolCount = readConstantPoolCount();
        System.out.println("constant pool count: " + constantPoolCount);
        byte[][] constantPool = this.readConstantPool(constantPoolCount);

        int accessFlags = this.readAccessFlags();
        System.out.println("public: " + this.isPublic(accessFlags));
        int thisClass = readThisClass();
        System.out.println("class name: " + thisClass);

        int i = 0;
        for (byte[] utf8 : constantPool) {
            if (utf8 != null) {
                System.out.println("  " + i + ": " + new String(utf8, StandardCharsets.UTF_8));
            }
            i += 1;
        }
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

    private byte[][] readConstantPool(int constantPoolCount) throws IOException {
        byte[][] constantPool = new byte[constantPoolCount - 1][];
        for (int i = 0; i < constantPoolCount - 1; ++i) {
            readConstantPoolEntry(i, constantPool);
        }
        return constantPool;
    }

    private void readConstantPoolEntry(int index, byte[][] constantPool) throws IOException {
        int tag = this.readConstantPoolTag();
        if (index == 17 || index == 78) {
            System.out.println("index: " + index + " tag: " + tag);
        }
        switch (tag) {
            case CONSTANT_Class:
                int nameIndex = this.u2();
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
                constantPool[index] = utf8;
                if (index == 78) {
                    System.out.println("index: " + index + " length: " + length);
                    System.out.println("   " + new String(utf8, StandardCharsets.UTF_8));
                }
                /*
                int terminator = this.u1();
                if (terminator != 0) {
                    throw new IllegalStateException("unexpected terminator: " + terminator + " at index: " + index);
                }
                */
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
                if (tag == 0 && index == 0) {
                    // skip
                    return;
                }
                throw new IllegalStateException("unexpected tag: " + tag + " at index: " + index);
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
