package com.example.file_converter.storage;

import com.example.file_converter.exception.SourceNotFoundException;
import com.example.file_converter.exception.StorageException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
public class StorageService {

    private final MinioClient minioClient;

    public StorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public byte[] download(String bucket, String key) {
        try (InputStream in = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            return in.readAllBytes();
        } catch (ErrorResponseException e) {
            String code = e.errorResponse().code();
            if ("NoSuchKey".equals(code) || "NoSuchBucket".equals(code)) {
                // файла нет: повторять бесполезно, это бизнес-ошибка
                throw new SourceNotFoundException("Файл не найден: " + bucket + "/" + key);
            }
            throw new StorageException("Ошибка MinIO при скачивании " + key, e);
        } catch (Exception e) {
            throw new StorageException("Ошибка MinIO при скачивании " + key, e);
        }
    }

    public void upload(String bucket, String key, byte[] data, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(new ByteArrayInputStream(data), data.length, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Ошибка MinIO при загрузке " + key, e);
        }
    }
}
