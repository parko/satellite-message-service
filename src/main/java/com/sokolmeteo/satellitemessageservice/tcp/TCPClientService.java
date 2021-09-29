package com.sokolmeteo.satellitemessageservice.tcp;

import com.sokolmeteo.satellitemessageservice.dto.IridiumMessage;
import com.sokolmeteo.satellitemessageservice.dto.LongPayload;
import com.sokolmeteo.satellitemessageservice.dto.Payload;
import com.sokolmeteo.satellitemessageservice.repo.IridiumMessageRepository;
import lombok.NonNull;

import org.springframework.scheduling.annotation.Scheduled;

import java.text.SimpleDateFormat;
import java.util.*;

public class TCPClientService {
    private final TCPClient client;
    private final IridiumMessageRepository repository;

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("ddMMyy");
    private static final SimpleDateFormat timeFormatter = new SimpleDateFormat("HHmmss");

    static {
        dateFormatter.setTimeZone(UTC);
        timeFormatter.setTimeZone(UTC);
    }


    private final static int MESSAGE_PACKET_SIZE = 4;

    public TCPClientService(TCPClient client, IridiumMessageRepository repository) {
        this.client = client;
        this.repository = repository;
    }

    @Scheduled(fixedDelay = 60000)
    public void export() {
        System.out.println("Export messages");
        List<IridiumMessage> iridiumMessages = repository.findByErrorCounterAndSent(0, false);
        if (iridiumMessages.size() > 0) {
            Map<String, List<List<IridiumMessage>>> sorted = sortByImei(iridiumMessages);
            for (String imei : sorted.keySet()) {
                for (List<IridiumMessage> messages : sorted.get(imei)) {
                    boolean response = client.sendMessage(imei, getBlackMessage(messages));
                    if (response) {
                        for (IridiumMessage message : messages) {
                            message.setSent(true);
                        }
                        repository.saveAll(messages);
                    }
                }
            }
            System.out.println("Export is successful");
        } else System.out.println("Nothing to export");
    }

    private Map<String, List<List<IridiumMessage>>> sortByImei(@NonNull List<IridiumMessage> rawMessages) {
        Map<String, List<IridiumMessage>> sorted = new HashMap<>();
        for (IridiumMessage message : rawMessages) {
            if (sorted.containsKey(message.getImei())) {
                sorted.get(message.getImei()).add(message);
            } else {
                List<IridiumMessage> messages = new ArrayList<>();
                messages.add(message);
                sorted.put(message.getImei(), messages);
            }
        }

        Map<String, List<List<IridiumMessage>>> groupedMap = new HashMap<>();
        for (String imei : sorted.keySet()) {
            List<List<IridiumMessage>> grouped = new ArrayList<>();
            List<IridiumMessage> messages = new ArrayList<>();
            Iterator<IridiumMessage> iterator = sorted.get(imei).iterator();
            while (iterator.hasNext()) {
                messages.add(iterator.next());
                if (messages.size() >= MESSAGE_PACKET_SIZE || !iterator.hasNext()) {
                    grouped.add(messages);
                    messages = new ArrayList<>();
                }
            }
            groupedMap.put(imei, grouped);
        }
        return groupedMap;
    }

    private String getBlackMessage(List<IridiumMessage> messages) {
        StringBuilder sokolMessage = new StringBuilder("#B#");
        System.out.println("Send message");

        for (IridiumMessage message : messages) {
            boolean isLongFormat = message.getPayload().length() > 100;

            System.out.println("isLongFormat: " + isLongFormat);

            sokolMessage.append(dateFormatter.format(message.getEventDate()));
            sokolMessage.append(";");
            sokolMessage.append(timeFormatter.format(message.getEventDate()));
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

            if (!isLongFormat) {
                Payload payload = getPayload(message.getPayload());

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
            else {
                LongPayload longPayload = getLongPayload(message.getPayload());
                sokolMessage.append("ER:2:");
                sokolMessage.append(longPayload.getErrors());
                sokolMessage.append(",TR:2:");
                sokolMessage.append(longPayload.getCount());
                sokolMessage.append(",Upow:2:");
                sokolMessage.append(longPayload.getVoltage1());
                sokolMessage.append(",ExtUpow:2:");
                sokolMessage.append(longPayload.getVoltage2());
                sokolMessage.append(",t:2:");
                sokolMessage.append(longPayload.getTemperature());
                sokolMessage.append(",PR:2:");
                sokolMessage.append(longPayload.getPressure());
                sokolMessage.append(",HM:2:");
                sokolMessage.append(longPayload.getMoisture());
                sokolMessage.append(",WV:2:");
                sokolMessage.append(longPayload.getWindSpeed());
                sokolMessage.append(",WD:2:");
                sokolMessage.append(longPayload.getWindDirection());
                sokolMessage.append(",RN:2:");
                sokolMessage.append(longPayload.getPrecipitation());
                sokolMessage.append(",UV:2:");
                sokolMessage.append(longPayload.getUltraviolet());
                sokolMessage.append(",L:2:");
                sokolMessage.append(longPayload.getIllumination());
                sokolMessage.append(",AV:2:");
                sokolMessage.append(longPayload.getWindSpeedUZ());
                sokolMessage.append(",AD:2:");
                sokolMessage.append(longPayload.getWindDirectionUZ());
                sokolMessage.append(",L0:2:");
                sokolMessage.append(longPayload.getSnowCover());
                sokolMessage.append(",GM:2:");
                sokolMessage.append(longPayload.getSolarRadiation());
                sokolMessage.append(",EX01:2:");
                sokolMessage.append(longPayload.getEx01());
                sokolMessage.append(",EX02:2:");
                sokolMessage.append(longPayload.getEx02());
                sokolMessage.append(",EX03:2:");
                sokolMessage.append(longPayload.getEx03());
                sokolMessage.append(",EX04:2:");
                sokolMessage.append(longPayload.getEx04());
                sokolMessage.append(",EX05:2:");
                sokolMessage.append(longPayload.getEx05());
                sokolMessage.append(",EX06:2:");
                sokolMessage.append(longPayload.getEx06());
                sokolMessage.append(",EX07:2:");
                sokolMessage.append(longPayload.getEx07());
                sokolMessage.append(",EX11:2:");
                sokolMessage.append(longPayload.getEx11());
                sokolMessage.append(",EX12:2:");
                sokolMessage.append(longPayload.getEx12());
                sokolMessage.append(",EX13:2:");
                sokolMessage.append(longPayload.getEx13());
                sokolMessage.append(",EX14:2:");
                sokolMessage.append(longPayload.getEx14());
                sokolMessage.append(",EX15:2:");
                sokolMessage.append(longPayload.getEx15());
                sokolMessage.append(",EX16:2:");
                sokolMessage.append(longPayload.getEx16());
                sokolMessage.append(",EX17:2:");
                sokolMessage.append(longPayload.getEx17());
                sokolMessage.append(",EX21:2:");
                sokolMessage.append(longPayload.getEx21());
                sokolMessage.append(",EX22:2:");
                sokolMessage.append(longPayload.getEx22());
                sokolMessage.append(",EX23:2:");
                sokolMessage.append(longPayload.getEx23());
                sokolMessage.append(",EX24:2:");
                sokolMessage.append(longPayload.getEx24());
                sokolMessage.append(",EX25:2:");
                sokolMessage.append(longPayload.getEx25());
                sokolMessage.append(",EX26:2:");
                sokolMessage.append(longPayload.getEx26());
                sokolMessage.append(",EX27:2:");
                sokolMessage.append(longPayload.getEx27());
                sokolMessage.append(",EX31:2:");
                sokolMessage.append(longPayload.getEx31());
                sokolMessage.append(",EX32:2:");
                sokolMessage.append(longPayload.getEx32());
                sokolMessage.append(",EX33:2:");
                sokolMessage.append(longPayload.getEx33());
                sokolMessage.append(",EX34:2:");
                sokolMessage.append(longPayload.getEx34());
                sokolMessage.append(",EX35:2:");
                sokolMessage.append(longPayload.getEx35());
                sokolMessage.append(",EX36:2:");
                sokolMessage.append(longPayload.getEx36());
                sokolMessage.append(",EX37:2:");
                sokolMessage.append(longPayload.getEx37());
                sokolMessage.append(",EX41:2:");
                sokolMessage.append(longPayload.getEx41());
                sokolMessage.append(",EX42:2:");
                sokolMessage.append(longPayload.getEx42());
                sokolMessage.append(",EX43:2:");
                sokolMessage.append(longPayload.getEx43());
                sokolMessage.append(",EX44:2:");
                sokolMessage.append(longPayload.getEx44());
                sokolMessage.append(",EX45:2:");
                sokolMessage.append(longPayload.getEx45());
                sokolMessage.append(",EX46:2:");
                sokolMessage.append(longPayload.getEx46());
                sokolMessage.append(",EX47:2:");
                sokolMessage.append(longPayload.getEx47());
                sokolMessage.append(",EX51:2:");
                sokolMessage.append(longPayload.getEx51());
                sokolMessage.append(",EX52:2:");
                sokolMessage.append(longPayload.getEx52());
                sokolMessage.append(",EX53:2:");
                sokolMessage.append(longPayload.getEx53());
                sokolMessage.append(",EX54:2:");
                sokolMessage.append(longPayload.getEx54());
                sokolMessage.append(",EX55:2:");
                sokolMessage.append(longPayload.getEx55());
                sokolMessage.append(",EX56:2:");
                sokolMessage.append(longPayload.getEx56());
                sokolMessage.append(",EX57:2:");
                sokolMessage.append(longPayload.getEx57());
                sokolMessage.append(",EX61:2:");
                sokolMessage.append(longPayload.getEx61());
                sokolMessage.append(",EX62:2:");
                sokolMessage.append(longPayload.getEx62());
                sokolMessage.append(",EX63:2:");
                sokolMessage.append(longPayload.getEx63());
                sokolMessage.append(",EX64:2:");
                sokolMessage.append(longPayload.getEx64());
                sokolMessage.append(",EX65:2:");
                sokolMessage.append(longPayload.getEx65());
                sokolMessage.append(",EX66:2:");
                sokolMessage.append(longPayload.getEx66());
                sokolMessage.append(",EX67:2:");
                sokolMessage.append(longPayload.getEx67());
                sokolMessage.append(",EX71:2:");
                sokolMessage.append(longPayload.getEx71());
                sokolMessage.append(",EX72:2:");
                sokolMessage.append(longPayload.getEx72());
                sokolMessage.append(",EX73:2:");
                sokolMessage.append(longPayload.getEx73());
                sokolMessage.append(",EX74:2:");
                sokolMessage.append(longPayload.getEx74());
                sokolMessage.append(",EX75:2:");
                sokolMessage.append(longPayload.getEx75());
                sokolMessage.append(",EX76:2:");
                sokolMessage.append(longPayload.getEx76());
                sokolMessage.append(",EX77:2:");
                sokolMessage.append(longPayload.getEx76());
                sokolMessage.append(",PM25:2:");
                sokolMessage.append(longPayload.getPm25());
                sokolMessage.append(",PM10:2:");
                sokolMessage.append(longPayload.getPm10());
                sokolMessage.append(",CO2:2:");
                sokolMessage.append(longPayload.getCo2());
                sokolMessage.append(",CO:2:");
                sokolMessage.append(longPayload.getCo());
                sokolMessage.append(",NO:2:");
                sokolMessage.append(longPayload.getNo());
                sokolMessage.append(",NO2:2:");
                sokolMessage.append(longPayload.getNo2());
                sokolMessage.append(",SO2:2:");
                sokolMessage.append(longPayload.getSo2());
                sokolMessage.append(",H2S:2:");
                sokolMessage.append(longPayload.getH2s());
                sokolMessage.append(",HCN:2:");
                sokolMessage.append(longPayload.getHcn());
                sokolMessage.append(",NH3:2:");
                sokolMessage.append(longPayload.getNh3());
                sokolMessage.append(",CH20:2:");
                sokolMessage.append(longPayload.getCh2o());
                sokolMessage.append(",R1:2:");
                sokolMessage.append(longPayload.getReserve1());
                sokolMessage.append(",R2:2:");
                sokolMessage.append(longPayload.getReserve2());
                sokolMessage.append(",R3:2:");
                sokolMessage.append(longPayload.getReserve3());
                sokolMessage.append(",R4:2:");
                sokolMessage.append(longPayload.getReserve4());
                sokolMessage.append(",VM:2:");
                sokolMessage.append(longPayload.getWindFlaw());

                sokolMessage.append("|");
            }
        }
        sokolMessage.append("\r\n");
        System.out.println(sokolMessage.toString());
        return sokolMessage.toString();
    }

    private Payload getPayload(String string) {
        Payload payload = new Payload();
        StringTokenizer tokenizer = new StringTokenizer(string, "[ ]");

        byte[] byteDate = {Byte.parseByte(tokenizer.nextToken()),
                Byte.parseByte(tokenizer.nextToken()),
                Byte.parseByte(tokenizer.nextToken()),
                Byte.parseByte(tokenizer.nextToken())};
        Calendar date = getDate(byteDate);
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

    private LongPayload getLongPayload(String string) {
        LongPayload longPayload = new LongPayload();
        StringTokenizer tokenizer = new StringTokenizer(string, "[ ]");

        byte[] byteDate = {Byte.parseByte(tokenizer.nextToken()),
                Byte.parseByte(tokenizer.nextToken()),
                Byte.parseByte(tokenizer.nextToken()),
                Byte.parseByte(tokenizer.nextToken())};
        Calendar date = getDate(byteDate);
        SimpleDateFormat dateFormatter = new SimpleDateFormat("ddMMyy");
        SimpleDateFormat timeFormatter = new SimpleDateFormat("HHmmss");
        longPayload.setDate(dateFormatter.format(date.getTime()));
        longPayload.setTime(timeFormatter.format(date.getTime()));

        byte[] byteError = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setErrors((int) TCPServerUtils.byteArrayToLong(byteError, 0, 2));

        byte[] byteCount = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setCount((int) TCPServerUtils.byteArrayToLong(byteCount, 0, 2));

        byte[] byteTemperature = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setTemperature(((float) TCPServerUtils.byteArrayToInt(byteTemperature, 0, 2)) / 100);

        byte[] bytePressure = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setPressure(((float) TCPServerUtils.byteArrayToLong(bytePressure, 0, 2)) / 100); // Умножаем на три?

        longPayload.setMoisture(Byte.parseByte(tokenizer.nextToken()));

        byte[] byteWindSpeed = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setWindSpeed(((float) TCPServerUtils.byteArrayToInt(byteWindSpeed, 0, 2)) / 100);

        byte[] byteWindDirection = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setWindDirection(TCPServerUtils.byteArrayToInt(byteWindDirection, 0, 2));

        byte[] bytePrecipitation = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setPrecipitation(((float) TCPServerUtils.byteArrayToLong(bytePrecipitation, 0, 2)) / 10);

        byte[] byteUltraViolet = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setUltraviolet(((float) TCPServerUtils.byteArrayToLong(byteUltraViolet, 0, 2)) / 100);

        byte[] byteIllumination = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setIllumination((int) TCPServerUtils.byteArrayToLong(byteIllumination, 0, 2));

        byte[] byteWindSpeedUZ = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setUltraviolet(((float) TCPServerUtils.byteArrayToLong(byteWindSpeedUZ, 0, 2)) / 100);

        byte[] byteWindDirectionUZ = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setWindDirectionUZ(((int) TCPServerUtils.byteArrayToLong(byteWindDirectionUZ, 0, 2)));

        byte[] byteSnowCover = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setSnowCover(((int) TCPServerUtils.byteArrayToLong(byteSnowCover, 0, 2)));

        byte[] byteSolarRadiation = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setSolarRadiation(((int) TCPServerUtils.byteArrayToLong(byteSolarRadiation, 0, 2)));



        byte[] byteEx01 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx01(((int) TCPServerUtils.byteArrayToLong(byteEx01, 0, 2)));

        byte[] byteEx02 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx02(((int) TCPServerUtils.byteArrayToLong(byteEx02, 0, 2)));

        byte[] byteEx03 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx03(((int) TCPServerUtils.byteArrayToLong(byteEx03, 0, 2)));

        byte[] byteEx04 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx04(((int) TCPServerUtils.byteArrayToLong(byteEx04, 0, 2)));

        byte[] byteEx05 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx05(((int) TCPServerUtils.byteArrayToLong(byteEx05, 0, 2)));

        byte[] byteEx06 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx06(((int) TCPServerUtils.byteArrayToLong(byteEx06, 0, 2)));

        byte[] byteEx07 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx07(((int) TCPServerUtils.byteArrayToLong(byteEx07, 0, 2)));

        byte[] byteEx11 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx11(((int) TCPServerUtils.byteArrayToLong(byteEx11, 0, 2)));

        byte[] byteEx12 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx12(((int) TCPServerUtils.byteArrayToLong(byteEx12, 0, 2)));

        byte[] byteEx13 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx13(((int) TCPServerUtils.byteArrayToLong(byteEx13, 0, 2)));

        byte[] byteEx14 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx14(((int) TCPServerUtils.byteArrayToLong(byteEx14, 0, 2)));

        byte[] byteEx15 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx15(((int) TCPServerUtils.byteArrayToLong(byteEx15, 0, 2)));

        byte[] byteEx16 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx16(((int) TCPServerUtils.byteArrayToLong(byteEx16, 0, 2)));

        byte[] byteEx17 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx17(((int) TCPServerUtils.byteArrayToLong(byteEx17, 0, 2)));

        byte[] byteEx21 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx21(((int) TCPServerUtils.byteArrayToLong(byteEx21, 0, 2)));

        byte[] byteEx22 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx22(((int) TCPServerUtils.byteArrayToLong(byteEx22, 0, 2)));

        byte[] byteEx23 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx23(((int) TCPServerUtils.byteArrayToLong(byteEx23, 0, 2)));

        byte[] byteEx24 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx24(((int) TCPServerUtils.byteArrayToLong(byteEx24, 0, 2)));

        byte[] byteEx25 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx25(((int) TCPServerUtils.byteArrayToLong(byteEx25, 0, 2)));

        byte[] byteEx26 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx26(((int) TCPServerUtils.byteArrayToLong(byteEx26, 0, 2)));

        byte[] byteEx27 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx27(((int) TCPServerUtils.byteArrayToLong(byteEx27, 0, 2)));

        byte[] byteEx31 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx31(((int) TCPServerUtils.byteArrayToLong(byteEx31, 0, 2)));

        byte[] byteEx32 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx32(((int) TCPServerUtils.byteArrayToLong(byteEx32, 0, 2)));

        byte[] byteEx33 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx33(((int) TCPServerUtils.byteArrayToLong(byteEx33, 0, 2)));

        byte[] byteEx34 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx34(((int) TCPServerUtils.byteArrayToLong(byteEx34, 0, 2)));

        byte[] byteEx35 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx35(((int) TCPServerUtils.byteArrayToLong(byteEx35, 0, 2)));

        byte[] byteEx36 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx36(((int) TCPServerUtils.byteArrayToLong(byteEx36, 0, 2)));

        byte[] byteEx37 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx37(((int) TCPServerUtils.byteArrayToLong(byteEx37, 0, 2)));

        byte[] byteEx41 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx41(((int) TCPServerUtils.byteArrayToLong(byteEx41, 0, 2)));

        byte[] byteEx42 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx42(((int) TCPServerUtils.byteArrayToLong(byteEx42, 0, 2)));

        byte[] byteEx43 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx43(((int) TCPServerUtils.byteArrayToLong(byteEx43, 0, 2)));

        byte[] byteEx44 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx44(((int) TCPServerUtils.byteArrayToLong(byteEx44, 0, 2)));

        byte[] byteEx45 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx45(((int) TCPServerUtils.byteArrayToLong(byteEx45, 0, 2)));

        byte[] byteEx46 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx46(((int) TCPServerUtils.byteArrayToLong(byteEx46, 0, 2)));

        byte[] byteEx47 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx47(((int) TCPServerUtils.byteArrayToLong(byteEx47, 0, 2)));

        byte[] byteEx51 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx51(((int) TCPServerUtils.byteArrayToLong(byteEx51, 0, 2)));

        byte[] byteEx52 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx52(((int) TCPServerUtils.byteArrayToLong(byteEx52, 0, 2)));

        byte[] byteEx53 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx53(((int) TCPServerUtils.byteArrayToLong(byteEx53, 0, 2)));

        byte[] byteEx54 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx54(((int) TCPServerUtils.byteArrayToLong(byteEx54, 0, 2)));

        byte[] byteEx55 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx55(((int) TCPServerUtils.byteArrayToLong(byteEx55, 0, 2)));

        byte[] byteEx56 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx56(((int) TCPServerUtils.byteArrayToLong(byteEx56, 0, 2)));

        byte[] byteEx57 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx57(((int) TCPServerUtils.byteArrayToLong(byteEx57, 0, 2)));

        byte[] byteEx61 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx61(((int) TCPServerUtils.byteArrayToLong(byteEx61, 0, 2)));

        byte[] byteEx62 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx62(((int) TCPServerUtils.byteArrayToLong(byteEx62, 0, 2)));

        byte[] byteEx63 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx63(((int) TCPServerUtils.byteArrayToLong(byteEx63, 0, 2)));

        byte[] byteEx64 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx64(((int) TCPServerUtils.byteArrayToLong(byteEx64, 0, 2)));

        byte[] byteEx65 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx65(((int) TCPServerUtils.byteArrayToLong(byteEx65, 0, 2)));

        byte[] byteEx66 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx66(((int) TCPServerUtils.byteArrayToLong(byteEx66, 0, 2)));

        byte[] byteEx67 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx67(((int) TCPServerUtils.byteArrayToLong(byteEx67, 0, 2)));

        byte[] byteEx71 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx71(((int) TCPServerUtils.byteArrayToLong(byteEx71, 0, 2)));

        byte[] byteEx72 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx72(((int) TCPServerUtils.byteArrayToLong(byteEx72, 0, 2)));

        byte[] byteEx73 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx73(((int) TCPServerUtils.byteArrayToLong(byteEx73, 0, 2)));

        byte[] byteEx74 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx74(((int) TCPServerUtils.byteArrayToLong(byteEx74, 0, 2)));

        byte[] byteEx75 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx75(((int) TCPServerUtils.byteArrayToLong(byteEx75, 0, 2)));

        byte[] byteEx76 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx76(((int) TCPServerUtils.byteArrayToLong(byteEx76, 0, 2)));

        byte[] byteEx77 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setEx77(((int) TCPServerUtils.byteArrayToLong(byteEx77, 0, 2)));


        byte[] bytePm25 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setPm25(((float) TCPServerUtils.byteArrayToLong(bytePm25, 0, 2)) / 10);

        byte[] bytePm10 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setPm10(((int) TCPServerUtils.byteArrayToLong(bytePm10, 0, 2)));

        byte[] byteCo2 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setCo2(((int) TCPServerUtils.byteArrayToLong(byteCo2, 0, 2)));

        byte[] byteCo = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setCo(((int) TCPServerUtils.byteArrayToLong(byteCo, 0, 2)));

        byte[] byteNo = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setNo(((int) TCPServerUtils.byteArrayToLong(byteNo, 0, 2)));

        byte[] byteNo2 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setNo2(((int) TCPServerUtils.byteArrayToLong(byteNo2, 0, 2)));

        byte[] byteSo2 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setSo2(((int) TCPServerUtils.byteArrayToLong(byteSo2, 0, 2)));

        byte[] byteH2s = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setH2s(((int) TCPServerUtils.byteArrayToLong(byteH2s, 0, 2)));

        byte[] byteHcn = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setHcn(((int) TCPServerUtils.byteArrayToLong(byteHcn, 0, 2)));

        byte[] byteNh3 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setNh3(((int) TCPServerUtils.byteArrayToLong(byteNh3, 0, 2)));

        byte[] byteCh2o = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setCh2o(((int) TCPServerUtils.byteArrayToLong(byteCh2o, 0, 2)));

        byte[] byteReserve1 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setReserve1(((int) TCPServerUtils.byteArrayToLong(byteReserve1, 0, 2)));

        byte[] byteReserve2 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setReserve2(((int) TCPServerUtils.byteArrayToLong(byteReserve2, 0, 2)));

        byte[] byteReserve3 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setReserve3(((int) TCPServerUtils.byteArrayToLong(byteReserve3, 0, 2)));

        byte[] byteReserve4 = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setReserve4(((int) TCPServerUtils.byteArrayToLong(byteReserve4, 0, 2)));

        byte[] byteVoltage1 = {Byte.parseByte(tokenizer.nextToken())};
        longPayload.setVoltage1(((float) TCPServerUtils.byteArrayToLong(byteVoltage1, 0, 2)) / 100);

        byte[] byteVoltage2 = {Byte.parseByte(tokenizer.nextToken())};
        longPayload.setVoltage2(((float) TCPServerUtils.byteArrayToLong(byteVoltage2, 0, 2)) / 100);

        byte[] byteWindFlaw = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        longPayload.setWindFlaw(((float) TCPServerUtils.byteArrayToInt(byteWindFlaw, 0, 2)) / 100);


        return longPayload;
    }

    private Calendar getDate(byte[] bytes) {
        long epochTime = TCPServerUtils.byteArrayToLong(bytes, 0, 4) * 1000L;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(epochTime);
        calendar.add(Calendar.HOUR, -3);
        return calendar;
    }
}
