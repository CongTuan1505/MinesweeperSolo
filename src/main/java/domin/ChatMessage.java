package domin;

import java.nio.*;
import java.nio.charset.*;
import java.util.Base64;

/** Base64 keeps text, Vietnamese, newlines and protocol delimiters in a single wire field. */
public final class ChatMessage {
    public static final int MAX_LENGTH = 500;
    private ChatMessage() {}
    public static String encode(String text) {
        return Base64.getEncoder().encodeToString(validate(text).getBytes(StandardCharsets.UTF_8));
    }
    public static String decode(String payload) {
        if(payload == null || payload.length() > MAX_LENGTH * 8)
            throw new IllegalArgumentException("Tin nhắn quá dài");
        try {
            byte[] bytes = Base64.getDecoder().decode(payload);
            String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            return validate(text);
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Tin nhắn không đúng UTF-8",e);
        }
    }
    private static String validate(String text) {
        if(text==null || text.isBlank())throw new IllegalArgumentException("Tin nhắn không được để trống");
        text=text.strip();
        if(text.length()>MAX_LENGTH)throw new IllegalArgumentException("Tin nhắn tối đa 500 ký tự");
        for(int i=0;i<text.length();i++)
            if(Character.isISOControl(text.charAt(i)) && text.charAt(i)!='\n' && text.charAt(i)!='\t')
                throw new IllegalArgumentException("Tin nhắn chứa ký tự không hợp lệ");
        return text;
    }
}
