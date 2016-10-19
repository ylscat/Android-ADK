package com.fangstar.keystore.data;

/**
 * Created at 2016/10/17.
 *
 * @author YinLanShan
 */
public class Response {
    public final byte[] DATA = new byte[128];
    public int length;

    public int push(byte[] buf, int start, int len) {
        int contentLen = -1;

        if (length == 0) {
            if (buf[start] != '#')
                return 0;
            if (len - start > 1) {
                contentLen = buf[start + 1] & 0xFF;
            }
        } else if (length == 1) {
            contentLen = buf[start] & 0xFF;
        } else
            contentLen = DATA[1] & 0xFF;

        int copyLen = 0;
        int end = contentLen + 3; // start '#' + [contentLen] + end'\n' = 3
        if(end > DATA.length)
            return 0;
        if(end <= length + len - start) {
            copyLen = end - length;
            System.arraycopy(buf, start, DATA, length, copyLen);
            length += copyLen;
            return copyLen;
        }
        else {
            copyLen = len - start;
            System.arraycopy(buf, start, DATA, length, copyLen);
            length += copyLen;
            return copyLen;
        }
    }

    public int check() {
        if(length > 2) {
            int contentLen = DATA[1] & 0xFF;
            if(length == contentLen + 3)
                return DATA[length - 1] == '\n' ?
                        1 : -1;
        }
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(length > 0) {
            sb.append("#");
        }
        if(length > 1) {
            int contentLen = DATA[1] & 0xFF;
            for(int i = 1; i < contentLen + 2; i++) {
                if(i == length)
                    break;
                sb.append(String.format(" %02X", DATA[i]));
            }

            if(length == contentLen + 3) {
                sb.append(" \\n");
            }
        }

        return sb.toString();
    }

    public int getCmd() {
        return DATA[2] & 0xFF;
    }

    public int getLength() {
        return DATA[1] & 0xFF;
    }
}
