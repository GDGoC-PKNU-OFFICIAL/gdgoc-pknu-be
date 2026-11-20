package com.gdgocpknu.gdgoc_pknu_be.common.config;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
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

    /**
     * 요청 하나가 메모리를 쓸 수 있는 크기의 상한. Jackson 기본값은 문자열 하나가 2천만 자까지 허용돼 로그인처럼
     * 인증 없이 부르는 API에 큰 본문을 동시에 보내면 힙(무료 티어 약 384MB)이 바닥난다. 검증(`@Size`)은 문자열을
     * 다 읽은 뒤에 돌므로 이 단계에서 막아야 메모리를 지킨다.
     * 정상 요청의 가장 큰 값은 소개 문단 1천 자, 본문 전체 2만 자 안팎이라 아래 값은 그보다 훨씬 넉넉하다.
     */
    static final int MAX_STRING_LENGTH = 10_000;
    static final int MAX_DOCUMENT_LENGTH = 200_000;
    static final int MAX_NESTING_DEPTH = 50;

    @Bean
    Jackson2ObjectMapperBuilderCustomizer limitRequestSize() {
        StreamReadConstraints constraints = StreamReadConstraints.builder()
                .maxStringLength(MAX_STRING_LENGTH)
                .maxDocumentLength(MAX_DOCUMENT_LENGTH)
                .maxNestingDepth(MAX_NESTING_DEPTH)
                .build();
        return builder -> builder.factory(JsonFactory.builder().streamReadConstraints(constraints).build());
    }

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
