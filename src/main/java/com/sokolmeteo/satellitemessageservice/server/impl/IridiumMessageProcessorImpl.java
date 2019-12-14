package com.sokolmeteo.satellitemessageservice.server.impl;

import com.sokolmeteo.satellitemessageservice.dto.Client;
import com.sokolmeteo.satellitemessageservice.dto.IridiumMessage;
import com.sokolmeteo.satellitemessageservice.dto.Location;
import com.sokolmeteo.satellitemessageservice.repo.IridiumMessageRepository;
import com.sokolmeteo.satellitemessageservice.server.TCPServerMessageProcessor;
import com.sokolmeteo.satellitemessageservice.server.TCPServerUtils;
import com.sokolmeteo.satellitemessageservice.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class IridiumMessageProcessorImpl implements TCPServerMessageProcessor {
    private final IridiumMessageRepository iridiumMessageRepository;
    private final LocationService locationService;

    @Override
    public void clientConnected(Client client) {
        log.info(String.format("[%s] - connection opened", client.getId()));
    }

    @Override
    public void ClientDisconnected(Client client) {
        log.info(String.format("[%s] - connection closed", client.getId()));
    }

    @Override
    public void processMessage(Client client, byte[] bytes) {
        IridiumMessage iridiumMessage = messageReceived(bytes);
        if (iridiumMessage.getErrorCounter() == 0) {

        }
    }

    private IridiumMessage messageReceived(byte[] message) {
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
        iridiumMessageRepository.save(iridiumMessage);
        log.info(String.format("[%d] - message received", iridiumMessage.getId()));
        return iridiumMessage;
    }
}
