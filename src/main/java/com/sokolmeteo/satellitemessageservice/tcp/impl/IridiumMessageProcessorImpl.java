package com.sokolmeteo.satellitemessageservice.tcp.impl;

import com.sokolmeteo.satellitemessageservice.dto.IridiumMessage;
import com.sokolmeteo.satellitemessageservice.dto.Location;
import com.sokolmeteo.satellitemessageservice.dto.enums.CardinalDirections;
import com.sokolmeteo.satellitemessageservice.repo.IridiumMessageRepository;
import com.sokolmeteo.satellitemessageservice.tcp.TCPServerMessageProcessor;
import com.sokolmeteo.satellitemessageservice.tcp.TCPServerUtils;

import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Date;

public class IridiumMessageProcessorImpl implements TCPServerMessageProcessor {
    private final String END_LINE = "\r\n";
    private final IridiumMessageRepository iridiumMessageRepository;

    public IridiumMessageProcessorImpl(IridiumMessageRepository iridiumMessageRepository) {
        this.iridiumMessageRepository = iridiumMessageRepository;
    }

    @Override
    public void processMessage(byte[] bytes) {
        IridiumMessage iridiumMessage = getIridiumMessage(bytes);
        iridiumMessageRepository.save(iridiumMessage);
        System.out.println(String.format("[%d] - message received", iridiumMessage.getId()));
    }

    private IridiumMessage getIridiumMessage(byte[] message) {
        IridiumMessage iridiumMessage = new IridiumMessage();
        int cursor = 0;
        int error = 0;

        if (message[cursor++] == 0x01) {
            iridiumMessage.setReceived(new Date());
            iridiumMessage.setOverallLength(TCPServerUtils.byteArrayToInt(message, cursor, 2));
            cursor += 2;

            //header part
            if (message[cursor] == 0x01) {
                cursor++;
                iridiumMessage.setHeaderLength(TCPServerUtils.byteArrayToInt(message, cursor, 2));
                cursor += 2;
                iridiumMessage.setCdrReference(TCPServerUtils.byteArrayToInt(message, cursor, 4));
                cursor += 4;

                byte[] imei = new byte[15];
                System.arraycopy(message, cursor, imei, 0, 15);
                iridiumMessage.setImei(new String(imei, StandardCharsets.UTF_8));
                cursor += 15;

                iridiumMessage.setSessionStatus(TCPServerUtils.byteArrayToInt(message, cursor++, 1));
                if (iridiumMessage.getSessionStatus() != 0x00) {
                    error++;
                    System.out.println(String.format("Session status is %s;", iridiumMessage.getSessionStatus()));
                }

                iridiumMessage.setMomsn(TCPServerUtils.byteArrayToInt(message, cursor, 2));
                cursor += 2;
                iridiumMessage.setMtmsn(TCPServerUtils.byteArrayToInt(message, cursor, 2));
                cursor += 2;

                iridiumMessage.setEventDate(new Date(TCPServerUtils.byteArrayToLong(message, cursor, 4) * 1000L));
                cursor += 4;
            } else {
                error++;
                System.out.println("Header part is absent");
            }

            //location part
            if (message[cursor] == 0x03) {
                cursor++;
                iridiumMessage.setLocationLength(TCPServerUtils.byteArrayToInt(message, cursor, 2));
                cursor += 2;
                Location location = getLocation(message, cursor);
                iridiumMessage.setLatitudeDirection(location.getLatitudeDirection());
                iridiumMessage.setLatitude(location.getLatitude());
                iridiumMessage.setLongitudeDirection(location.getLongitudeDirection());
                iridiumMessage.setLongitude(location.getLongitude());
                cursor += 7;
                iridiumMessage.setCepRadius(TCPServerUtils.byteArrayToInt(message, cursor, 4));
                cursor += 4;
            } else {
                error++;
                System.out.println("Location part is absent");
            }

            //payload part
            if (message[cursor] == 0x02) {
                cursor++;
                iridiumMessage.setPayloadLength(TCPServerUtils.byteArrayToInt(message, cursor, 2));
                cursor += 2;

                StringBuilder payload = new StringBuilder();
                payload.append("[ ");
                for (int i = cursor; i < cursor + iridiumMessage.getPayloadLength(); i++) {
                    payload.append(message[i]);
                    payload.append(" ");
                }
                payload.append("]");

                iridiumMessage.setPayload(payload.toString());
            } else {
                error++;
                System.out.println("Payload part is absent");
            }
        } else {
            error++;
            System.out.println(String.format("Message starts with incorrect code [%d]", message[cursor - 1]));
        }
        iridiumMessage.setErrorCounter(error);
        return iridiumMessage;

    }

    private Location getLocation(byte[] bytes, int cursor) {
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
