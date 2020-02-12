package com.sokolmeteo.satellitemessageservice.dto;

import com.sokolmeteo.satellitemessageservice.dto.enums.CardinalDirections;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
public class IridiumMessage {
    @Id
    @GeneratedValue
    private Long id;
    private Integer overallLength;
    private Integer headerLength;
    private Integer cdrReference;
    private String imei;
    private Integer sessionStatus;
    private Integer momsn;
    private Integer mtmsn;
    private Date received;
    private Date eventDate;
    private Integer locationLength;
    private CardinalDirections latitudeDirection;
    private CardinalDirections longitudeDirection;
    private Double latitude;
    private Double longitude;
    private Integer height;
    private Integer cepRadius;
    private Integer payloadLength;
    private String payload;
    private int errorCounter;
    private boolean sent = false;
}
