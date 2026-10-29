package com.gdgocpknu.gdgoc_pknu_be.setting;

import com.gdgocpknu.gdgoc_pknu_be.setting.domain.SiteSetting;
import com.gdgocpknu.gdgoc_pknu_be.setting.domain.SiteSettingRepository;
import com.gdgocpknu.gdgoc_pknu_be.setting.domain.StatValue;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사이트 설정의 기본 행을 앱 기동 시 한 번 만든다. 홈 화면이 첫 요청부터 `GET /api/settings`를 부르므로 행이 있어야
 * 하고, 값은 관리자가 설정 화면에서 바꾼다. 마이그레이션(SQL)이 아니라 코드로 두는 이유는 기본 문구 · 숫자를
 * 바꿀 때마다 새 마이그레이션을 만들지 않기 위해서다(관리자 계정 시드와 같은 방식).
 *
 * <p>이미 행이 있으면 절대 건드리지 않는다 — 그렇지 않으면 재배포할 때마다 관리자가 바꾼 값이 되돌아간다.
 * 테스트 프로필에서는 돌리지 않는다: 스키마 제약 테스트가 `id = 1` 행을 직접 넣는다.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class SiteSettingSeeder implements ApplicationRunner {

    private static final String DEFAULT_BANNER_TEXT = "모집 일정이 정해지면 안내드릴게요.";

    private final SiteSettingRepository siteSettingRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (siteSettingRepository.existsById(SiteSetting.SINGLETON_ID)) {
            return;
        }
        siteSettingRepository.save(SiteSetting.create(false, DEFAULT_BANNER_TEXT, null, null, defaultStats()));
        log.info("사이트 설정 기본 행을 생성했습니다. 관리자 화면에서 값을 수정하세요.");
    }

    /** 홈의 활동 통계 4칸(PRD 3-4). 숫자는 자리 표시용이며 관리자가 실제 값으로 바꾼다. */
    static List<StatValue> defaultStats() {
        return List.of(
                new StatValue("운영 기수", 1, null, null),
                new StatValue("누적 멤버", 0, null, null),
                new StatValue("완료 프로젝트", 0, null, null),
                new StatValue("세션·이벤트", 0, null, null));
    }
}
