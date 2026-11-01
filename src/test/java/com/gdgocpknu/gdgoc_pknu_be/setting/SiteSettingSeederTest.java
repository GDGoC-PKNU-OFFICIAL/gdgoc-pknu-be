package com.gdgocpknu.gdgoc_pknu_be.setting;

import static org.assertj.core.api.Assertions.assertThat;

import com.gdgocpknu.gdgoc_pknu_be.setting.domain.SiteSetting;
import com.gdgocpknu.gdgoc_pknu_be.setting.domain.SiteSettingRepository;
import com.gdgocpknu.gdgoc_pknu_be.setting.domain.SiteStat;
import com.gdgocpknu.gdgoc_pknu_be.setting.domain.StatValue;
import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class SiteSettingSeederTest {

    @Autowired
    SiteSettingRepository siteSettingRepository;
    @Autowired
    EntityManager em;

    @Test
    void 행이_없으면_기본_설정과_통계_4개를_만든다() {
        new SiteSettingSeeder(siteSettingRepository).run(null);
        em.flush();
        em.clear();

        SiteSetting setting = siteSettingRepository.findById(SiteSetting.SINGLETON_ID).orElseThrow();
        assertThat(setting.isRecruiting()).isFalse();
        assertThat(setting.getBannerText()).isNotBlank();
        assertThat(setting.getRecruitStart()).isNull();
        assertThat(setting.getStats()).extracting(SiteStat::getLabel)
                .containsExactly("운영 기수", "누적 멤버", "완료 프로젝트", "세션·이벤트");
        assertThat(setting.getStats()).extracting(SiteStat::getSortOrder).containsExactly(1, 2, 3, 4);
    }

    @Test
    void 이미_행이_있으면_관리자가_바꾼_값을_덮어쓰지_않는다() {
        siteSettingRepository.save(SiteSetting.create(true, "관리자가 쓴 배너", null, null, List.of(
                new StatValue("기수", 9, null, null), new StatValue("멤버", 9, null, null),
                new StatValue("프로젝트", 9, null, null), new StatValue("행사", 9, null, null))));
        em.flush();
        em.clear();

        new SiteSettingSeeder(siteSettingRepository).run(null);
        em.flush();
        em.clear();

        SiteSetting setting = siteSettingRepository.findById(SiteSetting.SINGLETON_ID).orElseThrow();
        assertThat(setting.isRecruiting()).isTrue();
        assertThat(setting.getBannerText()).isEqualTo("관리자가 쓴 배너");
        assertThat(setting.getStats()).extracting(SiteStat::getValue).containsExactly(9, 9, 9, 9);
    }

    @Test
    void 두_번_실행해도_행이_하나다() {
        SiteSettingSeeder seeder = new SiteSettingSeeder(siteSettingRepository);
        seeder.run(null);
        em.flush();
        seeder.run(null);
        em.flush();

        assertThat(siteSettingRepository.count()).isEqualTo(1);
    }
}
