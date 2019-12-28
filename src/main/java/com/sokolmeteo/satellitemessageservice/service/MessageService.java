package com.sokolmeteo.satellitemessageservice.service;

import com.sokolmeteo.satellitemessageservice.config.properties.AppilcationProperties;
import com.sokolmeteo.satellitemessageservice.dto.IridiumMessage;
import com.sokolmeteo.satellitemessageservice.dto.Location;
import com.sokolmeteo.satellitemessageservice.dto.Payload;
import com.sokolmeteo.satellitemessageservice.server.TCPServerUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {
    private final AppilcationProperties appilcationProperties;
    private final LocationService locationService;

    public IridiumMessage byteArrayToIridiumMessage(byte[] message) {
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
                    log.warn("Session status is " + iridiumMessage.getSessionStatus() + ";");
                }

                iridiumMessage.setMomsn(TCPServerUtils.byteArrayToInt(message, cursor, 2));
                cursor += 2;
                iridiumMessage.setMtmsn(TCPServerUtils.byteArrayToInt(message, cursor, 2));
                cursor += 2;

                iridiumMessage.setEventDate(new Date(TCPServerUtils.byteArrayToLong(message, cursor, 4) * 1000L));
                cursor += 4;
            } else {
                error++;
                log.warn("Header part is absent");
            }

            //location part
            if (message[cursor] == 0x03) {
                cursor++;
                iridiumMessage.setLocationLength(TCPServerUtils.byteArrayToInt(message, cursor, 2));
                cursor += 2;
                Location location = locationService.decryptLocation(message, cursor);
                iridiumMessage.setLatitudeDirection(location.getLatitudeDirection());
                iridiumMessage.setLatitude(location.getLatitude());
                iridiumMessage.setLongitudeDirection(location.getLongitudeDirection());
                iridiumMessage.setLongitude(location.getLongitude());
                cursor += 7;
                iridiumMessage.setCepRadius(TCPServerUtils.byteArrayToInt(message, cursor, 4));
                cursor += 4;
            } else {
                error++;
                log.warn("Location part is absent");
            }

            //payload part
            if (message[cursor] == 0x02) {
                cursor++;
                iridiumMessage.setPayloadLength(TCPServerUtils.byteArrayToInt(message, cursor, 2));
                cursor += 2;
                StringBuilder payload = new StringBuilder("[ ");
                for (int i = cursor; i < cursor + iridiumMessage.getPayloadLength(); i++) {
                    payload.append(message[i]);
                    payload.append(" ");
                }
                payload.append("]");
                iridiumMessage.setPayload(payload.toString());
            } else {
                error++;
                log.warn("Payload part is absent");
            }
        } else {
            error++;
            log.warn(String.format("Message starts with incorrect code [%d]", message[cursor - 1]));
        }
        iridiumMessage.setErrorCounter(error);
        return iridiumMessage;
    }

    String generateLoginMessage(String imei) {
        return String.format("#L#%s;%s\r\n", imei, appilcationProperties.getSokolPassword());
    }

    String iridiumToBlackMessage(List<IridiumMessage> messages) {
        StringBuilder sokolMessage = new StringBuilder("#B#");
        for (IridiumMessage message : messages) {
            Payload payload = decryptPayload(message.getPayload());
            sokolMessage.append(payload.getDate());
            sokolMessage.append(";");
            sokolMessage.append(payload.getTime());
            sokolMessage.append(";");
            sokolMessage.append(message.getLatitude());
            sokolMessage.append(";");
            sokolMessage.append(message.getLatitudeDirection().getLiteral());
            sokolMessage.append(message.getLongitude());
            sokolMessage.append(";");
            sokolMessage.append(message.getLongitudeDirection().getLiteral());
            sokolMessage.append("NA;NA;");
            sokolMessage.append(message.getHeight() != null ? message.getHeight() : "NA");
            sokolMessage.append(";");
            sokolMessage.append("0;NA;0;0;NA;NA;");
            sokolMessage.append("ER:1:");
            sokolMessage.append(payload.getErrors());
            sokolMessage.append(",TR:1:");
            sokolMessage.append(payload.getCount());
            sokolMessage.append(",Upow:2:");
            sokolMessage.append(payload.getVoltage1());
            sokolMessage.append(",ExtUpow:2:");
            sokolMessage.append(payload.getVoltage2());
            sokolMessage.append(",Temp:2:");
            sokolMessage.append(payload.getTemperature());
            sokolMessage.append(",PR:2:");
            sokolMessage.append(payload.getPressure());
            sokolMessage.append(",HM:1:");
            sokolMessage.append(payload.getMoisture());
            sokolMessage.append(",WV:2:");
            sokolMessage.append(payload.getWindSpeed());
            sokolMessage.append(",WD:1:");
            sokolMessage.append(payload.getWindDirection());
            sokolMessage.append(",MWV:2:");
            sokolMessage.append(payload.getWindFlaw());
            sokolMessage.append(",RN:2:");
            sokolMessage.append(payload.getPrecipitation());
            sokolMessage.append(",SR:2:");
            sokolMessage.append(payload.getSolarRadiation());
            sokolMessage.append(",SH:1:");
            sokolMessage.append(payload.getSnowDepth());
            sokolMessage.append(",HG:1:");
            sokolMessage.append(payload.getSoilMoisture());
            sokolMessage.append(",TG:1:");
            sokolMessage.append(payload.getSoilTemperature());
            sokolMessage.append("|");
        }
        sokolMessage.append("\r\n");
        return sokolMessage.toString();
    }

    private Payload decryptPayload(String string) {
        Payload payload = new Payload();
        StringTokenizer tokenizer = new StringTokenizer(string, "[ ]");

        byte[] byteDate = {Byte.parseByte(tokenizer.nextToken()),
                Byte.parseByte(tokenizer.nextToken()),
                Byte.parseByte(tokenizer.nextToken()),
                Byte.parseByte(tokenizer.nextToken())};
        Calendar date = decryptDate(byteDate);
        SimpleDateFormat dateFormatter = new SimpleDateFormat("ddMMyy");
        SimpleDateFormat timeFormatter = new SimpleDateFormat("HHmmss");
        payload.setDate(dateFormatter.format(date.getTime()));
        payload.setTime(timeFormatter.format(date.getTime()));

        byte[] byteError = {Byte.parseByte(tokenizer.nextToken())};
        payload.setErrors((int) TCPServerUtils.byteArrayToLong(byteError, 0, 1));

        byte[] byteCount = {Byte.parseByte(tokenizer.nextToken())};
        payload.setCount((int) TCPServerUtils.byteArrayToLong(byteCount, 0, 1));

        byte[] byteVoltage1 = {Byte.parseByte(tokenizer.nextToken())};
        payload.setVoltage1(((float) TCPServerUtils.byteArrayToLong(byteVoltage1, 0, 1)) * 2 / 100);

        byte[] byteVoltage2 = {Byte.parseByte(tokenizer.nextToken())};
        payload.setVoltage2(((float) TCPServerUtils.byteArrayToLong(byteVoltage2, 0, 1)) / 16);

        byte[] byteTemperature = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        payload.setTemperature(((float) TCPServerUtils.byteArrayToInt(byteTemperature, 0, 2)) / 100);

        byte[] bytePressure = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        payload.setPressure(((float) TCPServerUtils.byteArrayToLong(bytePressure, 0, 2)) * 3 / 100);

        payload.setMoisture(Byte.parseByte(tokenizer.nextToken()));

        byte[] byteWindSpeed = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        payload.setWindSpeed(((float) TCPServerUtils.byteArrayToInt(byteWindSpeed, 0, 2)) / 100);

        byte[] byteWindDirection = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        payload.setWindDirection(TCPServerUtils.byteArrayToInt(byteWindDirection, 0, 2));

        byte[] byteWindFlaw = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        payload.setWindFlaw(((float) TCPServerUtils.byteArrayToInt(byteWindFlaw, 0, 2)) / 100);

        byte[] bytePrecipitation = {Byte.parseByte(tokenizer.nextToken())};
        payload.setPrecipitation(((float) TCPServerUtils.byteArrayToLong(bytePrecipitation, 0, 1)) / 10);

        byte[] byteSolarRadiation = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        payload.setSolarRadiation(((float) TCPServerUtils.byteArrayToInt(byteSolarRadiation, 0, 2)) / 10);

        byte[] byteSnowDepth = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        payload.setSnowDepth(TCPServerUtils.byteArrayToInt(byteSnowDepth, 0, 2));

        payload.setSoilMoisture(Byte.parseByte(tokenizer.nextToken()));

        byte[] byteSoilTemperature = {Byte.parseByte(tokenizer.nextToken())};
        payload.setSoilTemperature(((float)TCPServerUtils.byteArrayToInt(byteSoilTemperature, 0, 1)) / 2);
        return payload;
    }

    private Calendar decryptDate(byte[] bytes) {
        long epochTime = TCPServerUtils.byteArrayToLong(bytes, 0, 4) * 1000L;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(epochTime);
        calendar.add(Calendar.HOUR, -3);
        return calendar;
    }
}
