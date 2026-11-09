package com.example.file_converter.converter;

import com.example.file_converter.exception.ConversionException;
import com.example.file_converter.exception.UnsupportedFileTypeException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversionServiceTest {

    @Test
    void usesConverterThatSupportsFileType() throws Exception {
        Converter txt = mock(Converter.class);
        Converter image = mock(Converter.class);
        when(txt.supports(FileType.TXT)).thenReturn(true);
        byte[] content = {1, 2, 3};
        byte[] pdf = {9};
        when(txt.toPdf("a.txt", content)).thenReturn(pdf);

        ConversionService service = new ConversionService(List.of(image, txt));

        assertThat(service.convert("a.txt", content)).isSameAs(pdf);
        verify(image, never()).toPdf(any(), any());
    }

    @Test
    void failsWhenNoConverterForType() {
        ConversionService service = new ConversionService(List.of());

        assertThatThrownBy(() -> service.convert("a.txt", new byte[0]))
                .isInstanceOf(UnsupportedFileTypeException.class);
    }

    @Test
    void failsOnUnknownExtension() {
        ConversionService service = new ConversionService(List.of());

        assertThatThrownBy(() -> service.convert("a.docx", new byte[0]))
                .isInstanceOf(UnsupportedFileTypeException.class);
    }

    @Test
    void wrapsUnexpectedConverterErrors() throws Exception {
        Converter converter = mock(Converter.class);
        when(converter.supports(FileType.TXT)).thenReturn(true);
        when(converter.toPdf(any(), any())).thenThrow(new IOException("boom"));
        ConversionService service = new ConversionService(List.of(converter));

        assertThatThrownBy(() -> service.convert("a.txt", new byte[0]))
                .isInstanceOf(ConversionException.class)
                .isNotInstanceOf(UnsupportedFileTypeException.class)
                .hasMessageContaining("boom");
    }

    @Test
    void keepsBusinessErrorsAsIs() throws Exception {
        Converter converter = mock(Converter.class);
        when(converter.supports(FileType.TXT)).thenReturn(true);
        when(converter.toPdf(any(), any())).thenThrow(new ConversionException("bad file"));
        ConversionService service = new ConversionService(List.of(converter));

        assertThatThrownBy(() -> service.convert("a.txt", new byte[0]))
                .isInstanceOf(ConversionException.class)
                .hasMessage("bad file");
    }
}