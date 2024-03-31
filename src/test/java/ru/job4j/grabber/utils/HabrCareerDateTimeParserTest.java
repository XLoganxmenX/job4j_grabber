package ru.job4j.grabber.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class HabrCareerDateTimeParserTest {

    @Test
    public void whenParseDateTimeThenDateTimeWithoutUTC() {
        DateTimeParser dateTimeParser = new HabrCareerDateTimeParser();
        String dateTime = "2024-03-25T18:21:50+03:00";
        String result = "2024-03-25T18:21:50";

        assertThat(dateTimeParser.parse(dateTime).toString()).isEqualTo(result);
    }

    @Test
    public void whenParseDateTimeWithoutUTCThenDateTimeEqual() {
        DateTimeParser dateTimeParser = new HabrCareerDateTimeParser();
        String dateTime = "2024-03-25T18:21:50";
        String result = "2024-03-25T18:21:50";

        assertThat(dateTimeParser.parse(dateTime).toString()).isEqualTo(result);
    }
}