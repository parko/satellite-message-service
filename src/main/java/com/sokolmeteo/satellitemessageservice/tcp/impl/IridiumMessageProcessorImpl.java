package com.sokolmeteo.satellitemessageservice.tcp.impl;

import com.sokolmeteo.satellitemessageservice.dto.IridiumMessage;
import com.sokolmeteo.satellitemessageservice.dto.Location;
import com.sokolmeteo.satellitemessageservice.dto.enums.CardinalDirections;
import com.sokolmeteo.satellitemessageservice.repo.IridiumMessageRepository;
import com.sokolmeteo.satellitemessageservice.tcp.TCPServerMessageProcessor;
import com.sokolmeteo.satellitemessageservice.tcp.TCPServerUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
                System.out.println("IMEI:" + iridiumMessage.getImei());
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
                printLocation(message, cursor);
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

                boolean isLongFormat = iridiumMessage.getPayloadLength() > 100;
                System.out.println("isLongFormat: " + isLongFormat);

                String str;
                if (!isLongFormat) {
                    StringBuilder payload = new StringBuilder();
                    payload.append("[ ");
                    for (int i = cursor; i < cursor + iridiumMessage.getPayloadLength(); i++) {
                        payload.append(message[i]);
                        payload.append(" ");
                    }
                    payload.append("]");
                    str = payload.toString();
                } else {
                    byte[] array = new byte[iridiumMessage.getPayloadLength()];
                    int arrayIndex = 0;
                    for (int i = cursor; i < cursor + iridiumMessage.getPayloadLength(); i++) {
                        array[arrayIndex] = message[i];
                        arrayIndex++;
                    }
                    str = new String(array);
                    System.out.println("before: '" + str + "'");
                    if (str.endsWith(END_LINE)) {
                        str = str.substring(0, str.length() - END_LINE.length());
                        System.out.println("after: '" + str + "'");
                    }
                }
                iridiumMessage.setPayload(str);
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

    private static long getAsUnsignedNumber(final byte[] p_Bytes) {
        long result = 0L;
        final int remaining = p_Bytes.length - 1;
        int bitShifter = remaining * 8;
        for (int i = 0; i < p_Bytes.length - 1; i++) {
            final int convertedInt = p_Bytes[i] & 0xFF;
            result |= convertedInt << bitShifter;
            bitShifter -= 8;
        }
        result = result | p_Bytes[remaining] & 0xFF;
        return result & 0xFFFFFFFFL;
    }

    private void printLocation(byte[] bytes, int cursor) {
        byte[] allbytes = new byte[7];
        System.arraycopy(bytes, cursor, allbytes, 0, 7);
        System.out.println("Bytes: " + Arrays.toString(allbytes));

        final byte b1 = bytes[cursor];
        final String binaryStrByte1 = String
                .format("%8s", Integer.toBinaryString(b1 & 0xFF))
                .replace(' ', '0');

        final boolean nsFlag = binaryStrByte1.charAt(6) == '1';
        CardinalDirections latitudeDirection = nsFlag ? CardinalDirections.SOUTH : CardinalDirections.NORTH;

        final boolean ewFlag = binaryStrByte1.charAt(7) == '1';
        CardinalDirections longitudeDirection = ewFlag ? CardinalDirections.WEST : CardinalDirections.EAST;

        cursor = cursor + 1;

        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, cursor, 6);

        /**
         * Byte 2 lat degs
         */
        final byte latDegByte = byteBuffer.get();
        final short latDeg = latDegByte;
        /**
         * Bytes 3-4 thousandth of degree as unsigned int
         */
        byte[] dst = new byte[2];
        byteBuffer.get(dst, 0, 2);
        final int latDegDecimals = (int) getAsUnsignedNumber(dst);

        final String latDegreesStr = String.format("%s.%s", latDeg,
                latDegDecimals);
        final double latDegrees = Double.parseDouble(latDegreesStr);

        /**
         * Byte 5 lon degrees
         */
        final byte lonDegByte = byteBuffer.get();
        final short lonDeg = lonDegByte;

        /**
         * Bytes 6-7 thousands of lon degrees as an unsigned integer
         */
        dst = new byte[2];
        byteBuffer.get(dst, 0, 2);
        final int lonDegDecimals = (int) getAsUnsignedNumber(dst);
        final String lonDegreesStr = String.format("%s.%s", lonDeg,
                lonDegDecimals);
        final double lonDegrees = Double.parseDouble(lonDegreesStr);

        System.out.println(latDegreesStr + latitudeDirection.getLiteral() + ", " + lonDegreesStr + longitudeDirection.getLiteral());

        //        return new Location(latitudeDirection, latDegrees, longitudeDirection, lonDegrees);

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
