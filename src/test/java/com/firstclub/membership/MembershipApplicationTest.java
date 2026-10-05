package com.firstclub.membership;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Clock;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MembershipApplicationTest {

    @Autowired
    private Clock clock;

    @Test
    void contextLoadsWithBusinessTimeZoneClock() {
        assertThat(clock.getZone()).isEqualTo(ZoneId.of("Asia/Kolkata"));
    }
}
