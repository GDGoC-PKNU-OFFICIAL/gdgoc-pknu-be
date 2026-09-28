package com.gdgocpknu.gdgoc_pknu_be.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import java.io.IOException;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 요청 본문의 문자열은 앞뒤 공백을 제거한 뒤 검사한다 (API 명세서 4장).
 * 역직렬화 단계에서 잘라 두면 Bean Validation(`@NotBlank` · `@Size`)과 저장 값이 모두 정리된 값을 본다.
 * 배열 원소(`List<String>`)에도 적용된다.
 *
 * <p>공백을 보존해야 하는 필드(예: 로그인 비밀번호)는 필드에
 * `@JsonDeserialize(using = StringDeserializer.class)`를 붙여 이 규칙에서 뺀다.
 */
@Configuration
public class JacksonConfig {

    @Bean
    Jackson2ObjectMapperBuilderCustomizer trimRequestStrings() {
        return builder -> builder.deserializerByType(String.class, new TrimmingStringDeserializer());
    }

    /** `strip()`은 전각 공백(U+3000) 같은 유니코드 공백도 제거한다. `null`은 그대로 둔다. */
    static final class TrimmingStringDeserializer extends StdScalarDeserializer<String> {

        TrimmingStringDeserializer() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = StringDeserializer.instance.deserialize(p, ctxt);
            return value == null ? null : value.strip();
        }
    }
}
