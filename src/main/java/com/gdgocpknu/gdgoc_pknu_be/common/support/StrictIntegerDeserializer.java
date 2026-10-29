package com.gdgocpknu.gdgoc_pknu_be.common.support;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

/**
 * JSON 정수(`7`)만 받는다. Jackson 기본 동작은 `1.5`를 `1`로 자르고 `"7"`을 `7`로 바꿔 받아서, "정수"라고 명세한 값이
 * 조용히 다른 값으로 저장된다. 정수가 아닌 값(소수 · 문자열 · 불리언)과 `int` 범위를 넘는 값은 역직렬화 단계에서
 * 400(`fieldErrors`)이 된다. `null`은 이 역직렬화기를 거치지 않고 그대로 `null`이므로 `@NotNull`이 잡는다.
 *
 * <p>정수여야 하는 필드에 `@JsonDeserialize(using = StrictIntegerDeserializer.class)`로 붙인다.
 */
public class StrictIntegerDeserializer extends JsonDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT) {
            return (Integer) context.handleUnexpectedToken(Integer.class, parser);
        }
        return parser.getIntValue();
    }
}
