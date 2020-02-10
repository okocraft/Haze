package net.okocraft.haze.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Utilities {

    public static List<Object> readByteArray(byte[] message) {
        
        List<Object> result = new ArrayList<>();
        
        try (
            ByteArrayInputStream byteOut = new ByteArrayInputStream(message);
            DataInputStream out = new DataInputStream(byteOut);
        ) {

            while (true) {
                String className = out.readUTF();

                if (className.equals(Boolean.class.getSimpleName())) {
                    result.add(out.readBoolean());
                } else if (className.equals(Character.class.getSimpleName())) {
                    result.add(out.readChar());
                } else if (className.equals(Short.class.getSimpleName())) {
                    result.add(out.readShort());
                } else if (className.equals(Integer.class.getSimpleName())) {
                    result.add(out.readInt());
                } else if (className.equals(Long.class.getSimpleName())) {
                    result.add(out.readLong());
                } else if (className.equals(Float.class.getSimpleName())) {
                    result.add(out.readFloat());
                } else if (className.equals(Double.class.getSimpleName())) {
                    result.add(out.readDouble());
                } else if (className.equals(String.class.getSimpleName())) {
                    result.add(out.readUTF());
                } else {
                    break;
                }
            }

        } catch (EOFException ignore) {
        } catch (IOException e) {
            e.printStackTrace();
        }

        result.forEach(e -> {
            System.out.println(e.getClass().getSimpleName());
        });

        return result;
    }

    public static byte[] createByteArray(Object... elements) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);
        try {

            for (Object element : elements) {
                if (element instanceof Boolean) {
                    out.writeUTF(Boolean.class.getSimpleName());
                    out.writeBoolean((Boolean) element);
                } else if (element instanceof Character) {
                    out.writeUTF(Character.class.getSimpleName());
                    out.writeChar((Character) element);
                } else if (element instanceof Short) {
                    out.writeUTF(Short.class.getSimpleName());
                    out.writeShort((Short) element);
                } else if (element instanceof Integer) {
                    out.writeUTF(Integer.class.getSimpleName());
                    out.writeInt((Integer) element);
                } else if (element instanceof Long) {
                    out.writeUTF(Long.class.getSimpleName());
                    out.writeLong((Long) element);
                } else if (element instanceof Float) {
                    out.writeUTF(Float.class.getSimpleName());
                    out.writeFloat((Float) element);
                } else if (element instanceof Double) {
                    out.writeUTF(Double.class.getSimpleName());
                    out.writeDouble((Double) element);
                } else if (element instanceof String) {
                    out.writeUTF(String.class.getSimpleName());
                    out.writeUTF((String) element);
                } else {
                    throw new IOException("The type " + element.getClass().getSimpleName()
                            + " is not supported. Type must be primitive wrapper class (like Integer) or String.");
                }
            }

            out.writeUTF("end message");

            out.close();
            byteOut.close();

            return byteOut.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    
    public static Object[] addFirst(Object add, Object[] array) {
        Object[] result = new Object[array.length + 1];
        result[0] = add;
        System.arraycopy(array, 0, result, 1, array.length);
        return result;
    }
}