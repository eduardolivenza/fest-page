package com.festdescans.api.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PageTest {

    @Test
    void capsThePageSizeAndClampsNonPositiveValues() {
        assertThat(PageRequest.of(1, 500, 50).pageSize()).isEqualTo(50);
        assertThat(new PageRequest(0, 0)).isEqualTo(new PageRequest(1, 1));
        assertThat(new PageRequest(3, 20).offset()).isEqualTo(40);
    }

    @Test
    void computesTotalPagesRoundingUp() {
        assertThat(new Page<>(List.of(), 0, 1, 12).totalPages()).isZero();
        assertThat(new Page<>(List.of(), 12, 1, 12).totalPages()).isEqualTo(1);
        assertThat(new Page<>(List.of(), 13, 1, 12).totalPages()).isEqualTo(2);
    }
}
