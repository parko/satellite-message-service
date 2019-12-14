package com.sokolmeteo.satellitemessageservice.service;

import com.sokolmeteo.satellitemessageservice.dto.enumerations.CardinalDirections;
import com.sokolmeteo.satellitemessageservice.dto.Location;
import com.sokolmeteo.satellitemessageservice.server.TCPServerUtils;
import org.springframework.stereotype.Service;

import java.util.BitSet;

@Service
public class LocationService {

    public Location decryptLocation(byte[] bytes, int cursor) {
        byte[] b = new byte[1];
        System.arraycopy(bytes, cursor++, b, 0, 1);
        BitSet direction = BitSet.valueOf(b);
        CardinalDirections latitudeDirection = direction.get(1) ? CardinalDirections.SOUTH : CardinalDirections.NORTH;
        CardinalDirections longitudeDirection = direction.get(0) ? CardinalDirections.WEST : CardinalDirections.EAST;
        double latitude = toDecimalDegrees(bytes, cursor);
        cursor += 3;
        double longitude = toDecimalDegrees(bytes, cursor);

        return new Location(latitudeDirection, latitude, longitudeDirection, longitude);
    }

    private double toDecimalDegrees(byte[] bytes, int cursor) {
        double result = 0D;
        result += TCPServerUtils.byteArrayToInt(bytes, cursor++, 1) * 100D;
        result += (double) TCPServerUtils.byteArrayToLong(bytes, cursor, 2) / 1000D;
        return result;
    }
}
