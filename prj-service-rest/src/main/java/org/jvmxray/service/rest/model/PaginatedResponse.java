package org.jvmxray.service.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Generic paginated response wrapper.
 *
 * @param <T> The type of data in the response
 * @author Milton Smith
 */
public class PaginatedResponse<T> {

    @JsonProperty("data")
    private List<T> data;

    @JsonProperty("pagination")
    private PaginationInfo pagination;

    @JsonProperty("metadata")
    private ResponseMetadata metadata;

    // Constructors
    public PaginatedResponse() {
    }

    public PaginatedResponse(List<T> data, PaginationInfo pagination, ResponseMetadata metadata) {
        this.data = data;
        this.pagination = pagination;
        this.metadata = metadata;
    }

    // Inner class for pagination information
    public static class PaginationInfo {
        @JsonProperty("page")
        private int page;

        @JsonProperty("size")
        private int size;

        @JsonProperty("total_elements")
        private long totalElements;

        @JsonProperty("total_pages")
        private int totalPages;

        @JsonProperty("has_next")
        private boolean hasNext;

        @JsonProperty("has_previous")
        private boolean hasPrevious;

        public PaginationInfo() {
        }

        public PaginationInfo(int page, int size, long totalElements) {
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = (int) Math.ceil((double) totalElements / size);
            this.hasNext = page < totalPages - 1;
            this.hasPrevious = page > 0;
        }

        // Getters and Setters
        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(long totalElements) {
            this.totalElements = totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }

        public boolean isHasNext() {
            return hasNext;
        }

        public void setHasNext(boolean hasNext) {
            this.hasNext = hasNext;
        }

        public boolean isHasPrevious() {
            return hasPrevious;
        }

        public void setHasPrevious(boolean hasPrevious) {
            this.hasPrevious = hasPrevious;
        }
    }

    // Inner class for response metadata
    public static class ResponseMetadata {
        @JsonProperty("query_time")
        private long queryTime;

        @JsonProperty("result_truncated")
        private boolean resultTruncated;

        public ResponseMetadata() {
        }

        public ResponseMetadata(long queryTime, boolean resultTruncated) {
            this.queryTime = queryTime;
            this.resultTruncated = resultTruncated;
        }

        // Getters and Setters
        public long getQueryTime() {
            return queryTime;
        }

        public void setQueryTime(long queryTime) {
            this.queryTime = queryTime;
        }

        public boolean isResultTruncated() {
            return resultTruncated;
        }

        public void setResultTruncated(boolean resultTruncated) {
            this.resultTruncated = resultTruncated;
        }
    }

    // Getters and Setters
    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public PaginationInfo getPagination() {
        return pagination;
    }

    public void setPagination(PaginationInfo pagination) {
        this.pagination = pagination;
    }

    public ResponseMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ResponseMetadata metadata) {
        this.metadata = metadata;
    }
}