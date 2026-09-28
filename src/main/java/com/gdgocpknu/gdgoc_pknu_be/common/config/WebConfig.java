package com.gdgocpknu.gdgoc_pknu_be.common.config;

import com.gdgocpknu.gdgoc_pknu_be.common.support.CodedEnum;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * `@RequestParam` · `@PathVariable`의 enum을 코드값으로 바인딩한다.
 * Spring 기본 변환은 enum 이름(`PAST`)만 받으므로, `?status=past`처럼 JSON과 같은 코드값을 쓰려면 이 변환이 필요하다.
 * 없는 코드값은 `MethodArgumentTypeMismatchException` → GlobalExceptionHandler가 400 + fieldErrors로 바꾼다.
 *
 * <p>참고: 이 변환이 실패하면 Spring 바인딩(`TypeConverterDelegate`)이 `Enum.valueOf`로 한 번 더 시도하므로,
 * enum 이름과 정확히 같은 값(`PAST`, `TEAM_PROJECT`)은 통과한다. 코드값도 enum 이름도 아닌 값만 400이 된다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new StringToCodedEnumConverterFactory());
    }

    static final class StringToCodedEnumConverterFactory implements ConverterFactory<String, CodedEnum> {

        @Override
        public <T extends CodedEnum> Converter<String, T> getConverter(Class<T> targetType) {
            return source -> {
                for (T constant : targetType.getEnumConstants()) {
                    if (constant.code().equals(source)) {
                        return constant;
                    }
                }
                throw new IllegalArgumentException("Unknown code for " + targetType.getSimpleName() + ": " + source);
            };
        }
    }
}
