package com.example.server.model;

public class CaseDetails {
    private String caseNumber;
    private String defendant;
    private int speedKmh;
    private int speedLimitKmh;
    private String accidentOccured;
    private String damageEur;
    private String injurySeverity;
    private String roadType;
    private double alcoholLevelPromil;
    private int speedOver;

    // Default constructor
    public CaseDetails() {}

    // Parameterized constructor
    public CaseDetails(String caseNumber, String defendant, int speedKmh, int speedLimitKmh,
                     String accidentOccured, String damageEur, String injurySeverity,
                     String roadType, double alcoholLevelPromil) {
        this.caseNumber = caseNumber;
        this.defendant = defendant;
        this.speedKmh = speedKmh;
        this.speedLimitKmh = speedLimitKmh;
        this.accidentOccured = accidentOccured;
        this.damageEur = damageEur;
        this.injurySeverity = injurySeverity;
        this.roadType = roadType;
        this.alcoholLevelPromil = alcoholLevelPromil;
    }

    // Getters and setters
    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public String getDefendant() {
        return defendant;
    }

    public void setDefendant(String defendant) {
        this.defendant = defendant;
    }

    public int getSpeedKmh() {
        return speedKmh;
    }

    public void setSpeedKmh(int speedKmh) {
        this.speedKmh = speedKmh;
    }

    public int getSpeedLimitKmh() {
        return speedLimitKmh;
    }

    public void setSpeedLimitKmh(int speedLimitKmh) {
        this.speedLimitKmh = speedLimitKmh;
    }

    public String getAccidentOccured() {
        return accidentOccured;
    }

    public void setAccidentOccured(Boolean accidentOccuredString) {
        this.accidentOccured = accidentOccuredString ? "da" : "ne";
    }

    public void setAccidentOccured(String accidentOccuredString) {
        this.accidentOccured = accidentOccuredString;
    }

    public String getDamageEur() {
        return damageEur;
    }

    public void setDamageEur(String damageEur) {
        this.damageEur = damageEur;
    }

    public String getInjurySeverity() {
        return injurySeverity;
    }

    public void setInjurySeverity(String injurySeverity) {
        this.injurySeverity = injurySeverity;
    }

    public String getRoadType() {
        return roadType;
    }

    public void setRoadType(String roadType) {
        this.roadType = roadType;
    }

    public double getAlcoholLevelPromil() {
        return alcoholLevelPromil;
    }

    public void setAlcoholLevelPromil(double alcoholLevelPromil) {
        this.alcoholLevelPromil = alcoholLevelPromil;
    }

    public int getSpeedOver() {
        return speedOver;
    }

    public void setSpeedOver(int speed, int allowedSpeed){
        this.speedOver = speed - allowedSpeed;
    }
}

