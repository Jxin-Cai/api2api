package com.api2api.ohs.http.admin.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.admin.dto.AdminUpsertChannelEvaluationScheduleRequest;
import java.time.DateTimeException;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;

class ChannelEvaluationHttpConverterTest {

    private final ChannelEvaluationHttpConverter converter = Mappers.getMapper(ChannelEvaluationHttpConverter.class);

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "  ")
    void test_defaultsToUtc_when_zoneIsMissing(String zone) {
        // Arrange
        var request = scheduleRequest(zone);

        // Act
        var command = converter.toUpsertScheduleCommand(request, UserAccountId.of(1L), ProviderChannelId.of(1L));

        // Assert
        assertThat(command.getCron().zoneId()).isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    void test_trimsZone_when_zoneHasSurroundingWhitespace() {
        // Arrange
        var request = scheduleRequest(" Asia/Shanghai ");

        // Act
        var command = converter.toUpsertScheduleCommand(request, UserAccountId.of(1L), ProviderChannelId.of(1L));

        // Assert
        assertThat(command.getCron().zoneId()).isEqualTo(ZoneId.of("Asia/Shanghai"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Unknown/Zone", "UTC+bad"})
    void test_rejectsInvalidZone_when_zoneCannotBeParsed(String zone) {
        // Arrange
        var request = scheduleRequest(zone);

        // Act / Assert
        assertThatThrownBy(() -> converter.toUpsertScheduleCommand(request, UserAccountId.of(1L), ProviderChannelId.of(1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Evaluation cron zone is invalid: " + zone)
                .hasCauseInstanceOf(DateTimeException.class);
    }

    private AdminUpsertChannelEvaluationScheduleRequest scheduleRequest(String zone) {
        return AdminUpsertChannelEvaluationScheduleRequest.builder()
                .cronExpression("0 0 * * * *").zoneId(zone).build();
    }
}
