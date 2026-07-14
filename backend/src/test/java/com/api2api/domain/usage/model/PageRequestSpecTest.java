package com.api2api.domain.usage.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class PageRequestSpecTest {

    @Test
    void test_acceptsTwentyRecords_when_dashboardRecentCallsUseCompactPage() {
        PageRequestSpec pageRequest = PageRequestSpec.of(1, 20);

        assertThat(pageRequest.getSize()).isEqualTo(20);
    }

    @Test
    void test_rejectsUnsupportedPageSize_when_sizeIsNotAllowed() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PageRequestSpec.of(1, 30))
                .withMessage("Page size must be one of 20, 50, 100 or 200");
    }
}
