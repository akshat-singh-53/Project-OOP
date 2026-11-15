package com.itsm.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ThirdPartySmsAdapterTest {

    @Test
    @DisplayName("TC24 SMS adapter converts local numbers to the vendor's international format")
    void msisdnConversion() {
        assertThat(ThirdPartySmsAdapter.toMsisdn("98200 00001")).isEqualTo("+919820000001");
        assertThat(ThirdPartySmsAdapter.toMsisdn("+44 20 7946 0958")).isEqualTo("+442079460958");
        assertThatCode(() -> new ThirdPartySmsAdapter().sendSms("9820000001", "x".repeat(300)))
                .doesNotThrowAnyException();
    }
}
