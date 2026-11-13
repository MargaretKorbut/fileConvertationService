package com.example.file_converter.converter;

import com.example.file_converter.exception.UnsupportedFileTypeException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileTypeTest {

    @Test
    void definesTypeByExtensionIgnoringCase() {
        assertThat(FileType.fromFileName("a.txt")).isEqualTo(FileType.TXT);
        assertThat(FileType.fromFileName("A.TXT")).isEqualTo(FileType.TXT);
        assertThat(FileType.fromFileName("a.png")).isEqualTo(FileType.PNG);
        assertThat(FileType.fromFileName("a.jpg")).isEqualTo(FileType.JPG);
        assertThat(FileType.fromFileName("a.jpeg")).isEqualTo(FileType.JPG);
        assertThat(FileType.fromFileName("a.zip")).isEqualTo(FileType.ZIP);
    }

    @Test
    void failsOnUnknownExtension() {
        assertThatThrownBy(() -> FileType.fromFileName("a.docx"))
                .isInstanceOf(UnsupportedFileTypeException.class)
                .hasMessageContaining("docx");
    }

    @Test
    void failsWhenThereIsNoExtension() {
        assertThatThrownBy(() -> FileType.fromFileName("README"))
                .isInstanceOf(UnsupportedFileTypeException.class);
        assertThatThrownBy(() -> FileType.fromFileName("name."))
                .isInstanceOf(UnsupportedFileTypeException.class);
    }
}