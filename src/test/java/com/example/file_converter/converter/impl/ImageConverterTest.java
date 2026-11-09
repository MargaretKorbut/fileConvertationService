package com.example.file_converter.converter.impl;

import com.example.file_converter.converter.FileType;
import org.junit.jupiter.api.Test;

import static com.example.file_converter.support.TestFiles.isPdf;
import static com.example.file_converter.support.TestFiles.jpg;
import static com.example.file_converter.support.TestFiles.pageCount;
import static com.example.file_converter.support.TestFiles.png;
import static org.assertj.core.api.Assertions.assertThat;

class ImageConverterTest {

    private final ImageConverter converter = new ImageConverter();

    @Test
    void supportsPngAndJpgOnly() {
        assertThat(converter.supports(FileType.PNG)).isTrue();
        assertThat(converter.supports(FileType.JPG)).isTrue();
        assertThat(converter.supports(FileType.TXT)).isFalse();
    }

    @Test
    void convertsPngToPdf() throws Exception {
        byte[] pdf = converter.toPdf("a.png", png());

        assertThat(isPdf(pdf)).isTrue();
        assertThat(pageCount(pdf)).isEqualTo(1);
    }

    @Test
    void convertsJpgToPdf() throws Exception {
        byte[] pdf = converter.toPdf("a.jpg", jpg());

        assertThat(isPdf(pdf)).isTrue();
        assertThat(pageCount(pdf)).isEqualTo(1);
    }
}