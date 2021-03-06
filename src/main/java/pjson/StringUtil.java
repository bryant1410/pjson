package pjson;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.charset.Charset;

/**
 * Utility functions for converting bytes to String.
 */
public final class StringUtil {

    public static final Unsafe UNSAFE;
    public static final long STRING_VALUE_FIELD_OFFSET;
    public static final long STRING_OFFSET_FIELD_OFFSET;
    public static final long STRING_COUNT_FIELD_OFFSET;
    public static final boolean ENABLED;


    public static final char[] EMPTY_CHARS = new char[0];
    public static final String EMPTY_STRING = "";

    private static final boolean WRITE_TO_FINAL_FIELDS = Boolean.parseBoolean(System.getProperty("org.boon.write.to.final.string.fields", "true"));
    private static final boolean DISABLE = Boolean.parseBoolean(System.getProperty("org.boon.faststringutils", "false"));

    private static Unsafe loadUnsafe() {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            return (Unsafe) unsafeField.get(null);

        } catch (NoSuchFieldException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    static {
        UNSAFE = DISABLE ? null : loadUnsafe();
        ENABLED = UNSAFE != null;
    }

    /**
     * Uses the ISO-8859-1 charset
     */
    public static final Charset DEFAULT_CHAR_SET = Charset.forName("iso-8859-1");
    private static final byte STR_SPACE = (byte) ' ';

    public static final String toString(final char[] bts, final int start, final int len) {
        return noCopyStringFromCharsNoCheck(bts, start, len);
    }


    public static final String fastToString(final char[] bts, final int start, final int len) {
        return noCopyStringFromCharsNoCheck(bts, start, len);
    }



    private static long getFieldOffset(String fieldName) {
        if (ENABLED) {
            try {
                return UNSAFE.objectFieldOffset(String.class.getDeclaredField(fieldName));
            } catch (NoSuchFieldException e) {
                // field undefined
            }
        }
        return -1L;
    }

    static {
        STRING_VALUE_FIELD_OFFSET = getFieldOffset("value");
        STRING_OFFSET_FIELD_OFFSET = getFieldOffset("offset");
        STRING_COUNT_FIELD_OFFSET = getFieldOffset("count");
    }

    private enum StringImplementation {
        DIRECT_CHARS {
            @Override
            public char[] toCharArray(String string) {
                return (char[]) UNSAFE.getObject(string, STRING_VALUE_FIELD_OFFSET);
            }

            @Override
            public String noCopyStringFromChars(char[] chars) {
                if (WRITE_TO_FINAL_FIELDS) {
                    String string = new String();
                    UNSAFE.putObject(string, STRING_VALUE_FIELD_OFFSET, chars);
                    return string;
                } else {
                    return new String(chars);
                }
            }
        },
        OFFSET {
            @Override
            public char[] toCharArray(String string) {
                char[] value = (char[]) UNSAFE.getObject(string, STRING_VALUE_FIELD_OFFSET);
                int offset = UNSAFE.getInt(string, STRING_OFFSET_FIELD_OFFSET);
                int count = UNSAFE.getInt(string, STRING_COUNT_FIELD_OFFSET);
                if (offset == 0 && count == value.length)
                    // no need to copy
                    return value;
                else
                    return string.toCharArray();
            }

            @Override
            public String noCopyStringFromChars(char[] chars) {
                if (WRITE_TO_FINAL_FIELDS) {
                    String string = new String();
                    UNSAFE.putObject(string, STRING_VALUE_FIELD_OFFSET, chars);
                    UNSAFE.putInt(string, STRING_COUNT_FIELD_OFFSET, chars.length);
                    return string;
                } else {
                    return new String(chars);
                }
            }
        },
        UNKNOWN {
            @Override
            public char[] toCharArray(String string) {
                return string.toCharArray();
            }

            @Override
            public String noCopyStringFromChars(char[] chars) {
                return new String(chars);
            }
        };

        public abstract char[] toCharArray(String string);

        public abstract String noCopyStringFromChars(char[] chars);
    }

    public static StringImplementation STRING_IMPLEMENTATION = computeStringImplementation();

    private static StringImplementation computeStringImplementation() {

        if (STRING_VALUE_FIELD_OFFSET != -1L) {
            if (STRING_OFFSET_FIELD_OFFSET != -1L && STRING_COUNT_FIELD_OFFSET != -1L) {
                return StringImplementation.OFFSET;

            } else if (STRING_OFFSET_FIELD_OFFSET == -1L && STRING_COUNT_FIELD_OFFSET == -1L) {
                return StringImplementation.DIRECT_CHARS;
            } else {
                // WTF
                return StringImplementation.UNKNOWN;
            }
        } else {
            return StringImplementation.UNKNOWN;
        }
    }

    public static boolean hasUnsafe() {
        return ENABLED;
    }

    public static char[] toCharArray(final String string) {
        if (string == null) return EMPTY_CHARS;
        return STRING_IMPLEMENTATION.toCharArray(string);

    }

    public static char[] toCharArrayNoCheck(final CharSequence charSequence) {
        return toCharArray(charSequence.toString());
    }

    public static char[] toCharArray(final CharSequence charSequence) {
        if (charSequence == null) return EMPTY_CHARS;
        return toCharArray(charSequence.toString());
    }

    public static char[] toCharArrayFromBytes(final byte[] bytes, Charset charset, int start, int len) {
        return toCharArray(new String(bytes, start, len, charset != null ? charset : DEFAULT_CHAR_SET));
    }

    public static String noCopyStringFromChars(final char[] chars) {
        if (chars==null) return EMPTY_STRING;
        return STRING_IMPLEMENTATION.noCopyStringFromChars(chars);
    }


    public static String noCopyStringFromCharsNoCheck(final char[] chars, int len) {
        char[] newChars = new char[len];
        CharArrayTool.copy(chars, 0, newChars, 0, len);
        return STRING_IMPLEMENTATION.noCopyStringFromChars(newChars);
    }

    public static String noCopyStringFromCharsNoCheck(final char[] chars, int start, int len) {
        char[] newChars = new char[len];
        CharArrayTool.copy(chars, start, newChars, 0, len);
        return STRING_IMPLEMENTATION.noCopyStringFromChars(newChars);
    }

    public static String noCopyStringFromCharsNoCheck(final char[] chars) {
        return STRING_IMPLEMENTATION.noCopyStringFromChars(chars);
    }

    public static char[] decode(final byte[] src, final int from, final int len) {
        final int end = from + len;
        final char[] dst = new char[len];

        for(int i = from; i < end; i++)
            dst[i] = (char)(src[i] & 0xff);

        return dst;
    }
}
