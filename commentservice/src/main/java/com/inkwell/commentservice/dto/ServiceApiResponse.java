package com.inkwell.commentservice.dto;

import lombok.Data;

@Data
public class ServiceApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
}
