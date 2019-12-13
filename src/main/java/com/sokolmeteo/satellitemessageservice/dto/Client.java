package com.sokolmeteo.satellitemessageservice.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class Client {
    private UUID id = UUID.randomUUID();
    private Object credentials = null;
}
