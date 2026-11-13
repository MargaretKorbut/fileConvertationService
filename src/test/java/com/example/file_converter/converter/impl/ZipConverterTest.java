package com.example.file_converter.converter.impl;

import com.example.file_converter.converter.ConversionService;
import com.example.file_converter.exception.ConversionException;
import com.example.file_converter.exception.UnsupportedFileTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.example.file_converter.support.TestFiles.isPdf;
import static com.example.file_converter.support.TestFiles.pageCount;
import static com.example.file_converter.support.TestFiles.png;
import static com.example.file_converter.support.TestFiles.txt;
import static com.example.file_converter.support.TestFiles.zip;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ZipConverterTest {

    private ConversionService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<ConversionService> provider = mock(ObjectProvider.class);
        ZipConverter zipConverter = new ZipConverter(provider);
        service = new ConversionService(List.of(new TxtConverter(), new ImageConverter(), zipConverter));
        when(provider.getObject()).thenReturn(service);
    }

    @Test
    void mergesAllFilesIntoOnePdf() throws Exception {
        byte[] archive = zip(Map.of(
                "a.txt", txt("one"),
                "b.txt", txt("two"),
                "c.png", png()));

        byte[] pdf = service.convert("test.zip", archive);

        assertThat(isPdf(pdf)).isTrue();
        assertThat(pageCount(pdf)).isEqualTo(3);
    }

    @Test
    void processesNestedArchivesRecursively() throws Exception {
        byte[] inner = zip(Map.of("a.txt", txt("x"), "b.txt", txt("y")));
        byte[] outer = zip(Map.of("inner.zip", inner));

        byte[] pdf = service.convert("outer.zip", outer);

        assertThat(pageCount(pdf)).isEqualTo(2);
    }

    @Test
    void skipsServiceFiles() throws Exception {
        byte[] archive = zip(Map.of(
                "__MACOSX/._a.txt", txt("junk"),
                "a.txt", txt("ok")));

        byte[] pdf = service.convert("test.zip", archive);

        assertThat(pageCount(pdf)).isEqualTo(1);
    }

    @Test
    void failsWhenArchiveContainsUnsupportedFile() throws Exception {
        byte[] archive = zip(Map.of(
                "a.txt", txt("ok"),
                "doc.pdf", new byte[]{1}));

        assertThatThrownBy(() -> service.convert("test.zip", archive))
                .isInstanceOf(UnsupportedFileTypeException.class);
    }

    @Test
    void failsOnEmptyArchive() throws Exception {
        byte[] archive = zip(Map.of());

        assertThatThrownBy(() -> service.convert("test.zip", archive))
                .isInstanceOf(ConversionException.class);
    }

    @Test
    void readsFileNamesInCp866() throws Exception {
        // так проводник Windows записывает русские имена в архив
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(out, Charset.forName("Cp866"))) {
            zos.putNextEntry(new ZipEntry("привет.txt"));
            zos.write(txt("текст"));
            zos.closeEntry();
        }

        byte[] pdf = service.convert("test.zip", out.toByteArray());

        assertThat(isPdf(pdf)).isTrue();
    }
}