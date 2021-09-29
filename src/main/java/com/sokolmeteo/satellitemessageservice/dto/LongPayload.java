package com.sokolmeteo.satellitemessageservice.dto;

import lombok.Data;

@Data
public class LongPayload {
    private String date;
    private String time;
    private int errors; //err
    private int count; //прошивка?
    private float voltage1; //U
    private float voltage2; //Uex
    private float temperature; //t
    private float pressure; //PR
    private int moisture; //HM
    private float windSpeed; //WV
    private int windDirection; //WD
    private float windFlaw; //VM
    private float precipitation; //RN
    private int solarRadiation; //GM
    private float ultraviolet; //UV
    private int illumination; //L
    private int windDirectionUZ; //AD
    private float windSpeedUZ; //AV
    private int snowCover; //L0

    private int reserve1;
    private int reserve2;
    private int reserve3;
    private int reserve4;
    private float pm25;
    private int pm10;
    private int co2;
    private int co;
    private int no;
    private int no2;
    private int so2;
    private int h2s;
    private int hcn;
    private int nh3;
    private int ch2o;

    // Дополнительные датчики БМВД
    private int ex01;
    private int ex02;
    private int ex03;
    private int ex04;
    private int ex05;
    private int ex06;
    private int ex07;

    private int ex11;
    private int ex12;
    private int ex13;
    private int ex14;
    private int ex15;
    private int ex16;
    private int ex17;

    private int ex21;
    private int ex22;
    private int ex23;
    private int ex24;
    private int ex25;
    private int ex26;
    private int ex27;

    private int ex31;
    private int ex32;
    private int ex33;
    private int ex34;
    private int ex35;
    private int ex36;
    private int ex37;

    private int ex41;
    private int ex42;
    private int ex43;
    private int ex44;
    private int ex45;
    private int ex46;
    private int ex47;

    private int ex51;
    private int ex52;
    private int ex53;
    private int ex54;
    private int ex55;
    private int ex56;
    private int ex57;

    private int ex61;
    private int ex62;
    private int ex63;
    private int ex64;
    private int ex65;
    private int ex66;
    private int ex67;

    private int ex71;
    private int ex72;
    private int ex73;
    private int ex74;
    private int ex75;
    private int ex76;
    private int ex77;
}
