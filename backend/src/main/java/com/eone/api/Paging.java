package com.eone.api;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
public final class Paging {
    private Paging() {}
    public static PageRequest of(int page, int size, String field) {
        if (page < 0 || page > 100000 || size < 1 || size > 100)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Use page 0–100000 and size 1–100");
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, field, "id"));
    }
}
