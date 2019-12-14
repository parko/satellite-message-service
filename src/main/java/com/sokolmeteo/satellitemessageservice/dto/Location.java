package com.sokolmeteo.satellitemessageservice.dto;

import com.sokolmeteo.satellitemessageservice.dto.enumerations.CardinalDirections;
import lombok.Data;

@Data
public class Location {
    private CardinalDirections latitudeDirection;
    private CardinalDirections longitudeDirection;
    private double latitude;
    private double longitude;

    public Location(CardinalDirections latitudeDirection,
                    double latitude,
                    CardinalDirections longitudeDirection,
                    double longitude) {
        this.latitudeDirection = latitudeDirection;
        this.longitudeDirection = longitudeDirection;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
