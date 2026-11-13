package com.example.file_converter.converter.impl;

import com.example.file_converter.converter.FileType;
import org.junit.jupiter.api.Test;

import static com.example.file_converter.support.TestFiles.isPdf;
import static com.example.file_converter.support.TestFiles.pageCount;
import static com.example.file_converter.support.TestFiles.txt;
import static org.assertj.core.api.Assertions.assertThat;

class TxtConverterTest {

    private final TxtConverter converter = new TxtConverter();

    @Test
    void supportsOnlyTxt() {
        assertThat(converter.supports(FileType.TXT)).isTrue();
        assertThat(converter.supports(FileType.PNG)).isFalse();
        assertThat(converter.supports(FileType.ZIP)).isFalse();
    }

    @Test
    void convertsRussianAndEnglishTextToPdf() throws Exception {
        byte[] pdf = converter.toPdf("a.txt", txt("Привет, мир! Hello, world!"));

        assertThat(isPdf(pdf)).isTrue();
        assertThat(pageCount(pdf)).isEqualTo(1);
    }

    @Test
    void emptyFileGivesOnePage() throws Exception {
        byte[] pdf = converter.toPdf("a.txt", new byte[0]);

        assertThat(isPdf(pdf)).isTrue();
        assertThat(pageCount(pdf)).isEqualTo(1);
    }

    @Test
    void longTextTakesSeveralPages() throws Exception {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            text.append("Строка номер ").append(i).append('\n');
        }

        byte[] pdf = converter.toPdf("a.txt", txt(text.toString()));

        assertThat(pageCount(pdf)).isGreaterThan(1);
    }
}