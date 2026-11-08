package com.gdgocpknu.gdgoc_pknu_be.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/** 객체 메타데이터만 메모리에 두는 가짜 S3. 실제 서명 · HTTP 동작은 R2ClientCompatibilityTest가 S3 호환 서버로 확인한다. */
public class FakeS3Client implements S3Client {

    private record StoredObject(long size, String contentType) {
    }

    private final Map<String, StoredObject> objects = new HashMap<>();
    private final List<String> deletedKeys = new ArrayList<>();
    private RuntimeException headFailure;
    private RuntimeException deleteFailure;

    public void put(String key, long size, String contentType) {
        objects.put(key, new StoredObject(size, contentType));
    }

    public void failHeadWith(RuntimeException failure) {
        headFailure = failure;
    }

    public void failDeleteWith(RuntimeException failure) {
        deleteFailure = failure;
    }

    public boolean exists(String key) {
        return objects.containsKey(key);
    }

    public List<String> deletedKeys() {
        return List.copyOf(deletedKeys);
    }

    public void reset() {
        objects.clear();
        deletedKeys.clear();
        headFailure = null;
        deleteFailure = null;
    }

    @Override
    public HeadObjectResponse headObject(HeadObjectRequest request) {
        if (headFailure != null) {
            throw headFailure;
        }
        StoredObject object = objects.get(request.key());
        if (object == null) {
            throw NoSuchKeyException.builder().statusCode(404).build();
        }
        return HeadObjectResponse.builder().contentLength(object.size()).contentType(object.contentType()).build();
    }

    @Override
    public DeleteObjectResponse deleteObject(DeleteObjectRequest request) {
        if (deleteFailure != null) {
            throw deleteFailure;
        }
        objects.remove(request.key());
        deletedKeys.add(request.key());
        return DeleteObjectResponse.builder().build();
    }

    @Override
    public String serviceName() {
        return SERVICE_NAME;
    }

    @Override
    public void close() {
    }
}
