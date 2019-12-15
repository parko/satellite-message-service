package com.sokolmeteo.satellitemessageservice.dto;

import lombok.Data;

@Data
public class Payload {
    private String date;
    private String time;
    private int errors;
    private int count;
    private float voltage1;
    private float voltage2;
    private float temperature;
    private float pressure;
    private int moisture;
    private float windSpeed;
    private int windDirection;
    private float windFlaw;
    private float precipitation;
    private float solarRadiation;
    private int snowDepth;
    private int soilMoisture;
    private float soilTemperature;
}
