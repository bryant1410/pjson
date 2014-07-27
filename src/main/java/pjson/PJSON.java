package pjson;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Fast simple json parser.<br/>
 * Note that numbers are not converted to any Java number and are returned as java Strings.<br/>
 * This improves performs but has the side effect that you need to parse any number data before treating it as an actual number.
 */
public final class PJSON {


    private static final byte OBJECT_START = (byte)'{';
    private static final byte OBJECT_END = (byte)'}';
    private static final byte ARR_START = (byte)'[';
    private static final byte ARR_END = (byte)']';
    private static final byte STR_MARK = (byte)'\"';
    private static final byte STR_COMMA = (byte)',';
    private static final byte STR_ESCAPE = (byte)'\\';

    private static final byte STR_COL = (byte)':';

    public static final void parse(final byte[] bts, final int start, final int len, final JSONListener events){
        parse(bts, start, len, false, events);
    }

    /**
     * Parses the byte array for json data.
     * @param bts byte array
     * @param start start offset
     * @param len number of bytes to parse
     * @param events JSONListener, all events will be sent to this instance.
     */
    public static final void parse(final byte[] bts, final int start, final int len, boolean parseNumbers, final JSONListener events){
        final int btsLen = start + len;
        boolean inString = false;
        int strStartIndex = 0;
        byte prevByte = -1;
        byte bt = -1;
        boolean inNonStrObj = false;

        for(int i = start; i < btsLen; i++){
            prevByte = bt;
            bt = bts[i];
            switch (bt){
                case STR_MARK:
                    if(prevByte != STR_ESCAPE) {
                        if (inString) {
                            events.string(StringUtil.toString(bts, strStartIndex, i - strStartIndex));
                        } else
                            strStartIndex = i+1;

                        inString ^= true;
                    }
                    inNonStrObj = false;
                    break;
                case STR_COL:
                    inNonStrObj = true;
                    strStartIndex = i+1;
                    break;
                case STR_COMMA:
                    if(inNonStrObj) {
                        parseNumber(events, bts, strStartIndex, i - strStartIndex, parseNumbers);
                        inNonStrObj = false;
                    }
                    break;
                case OBJECT_START:
                    if(!inString)
                     events.objectStart();
                    break;
                case OBJECT_END:
                    if(!inString){
                        if(inNonStrObj){
                            parseNumber(events, bts, strStartIndex, i - strStartIndex, parseNumbers);
                            inNonStrObj = false;
                        }
                        events.objectEnd();
                    }
                    break;
                case ARR_START:
                    if(!inString)
                        events.arrStart();

                    break;
                case ARR_END:
                    if(!inString){
                        if(inNonStrObj){
                            parseNumber(events, bts, strStartIndex, i - strStartIndex, parseNumbers);
                            inNonStrObj = false;
                        }
                        events.arrEnd();
                    }
                    break;

            }

        }
    }

    /**
     * Returns a String from a number.
     * @param events
     * @param bts
     * @param i
     * @param len
     */
    private static final void parseNumber(JSONListener events, byte[] bts, int i, int len, boolean parseNumbers){
        if(parseNumbers){
            final String str = StringUtil.fastToString(bts, i, len);
            if(str.equals("null"))
                events.string("null");
            else if(str.length() < 10)
                events.number(Integer.valueOf(str));
            else
                events.number(Long.valueOf(str));

        }else
            events.string(StringUtil.fastToString(bts, i, len));
    }

    public static final Object defaultParse(final byte[] bts){
        return defaultParse(bts, true);
    }

    /**
     * Parse the whole byte array bts.
     * @param bts
     * @return Object Map or Collection.
     */
    public static final Object defaultParse(final byte[] bts, boolean parseNumbers){
        final DefaultListener list = new DefaultListener();
        parse(bts, 0, bts.length, parseNumbers, list);
        return list.getValue();
    }

    public static final Object defaulParse(final byte[] bts, final int start, final int len){
        return defaulParse(bts, start, len, true);
    }
    /**
     * Parse the bts byte array from offset start and to < len.
     * @param bts
     * @param start
     * @param len
     * @return Object Map or Collection
     */
    public static final Object defaulParse(final byte[] bts, final int start, final int len, boolean parseNumbers){
        final DefaultListener list = new DefaultListener();
        parse(bts, start, len, parseNumbers, list);
        return list.getValue();
    }

}