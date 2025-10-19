package com.example.server.model;

import es.ucm.fdi.gaia.jcolibri.cbrcore.Attribute;
import es.ucm.fdi.gaia.jcolibri.cbrcore.CaseComponent;

import java.util.ArrayList;
import java.util.List;

public class Judgment implements CaseComponent {
    // Judgment identity
    private String id;
    private String filePath;
    private String court;
    private String caseNumber;
    private String judge;
    private String prosecutor;
    private String defendant;

    // Legal core
    private String offense;
    private String verdictType;
    private List<String> appliedProvisions = new ArrayList<>();
    private Boolean isGuilty;

    // CBR attributes
    private List<String> violationTypes = new ArrayList<>(); // e.g., failedToYield, distanceTooShort
    private Double speedKmh;            // vehicle speed (km/h)
    private Double speedLimitKmh;       // speed limit (km/h), if known
    private Double alcoholLevelPromil;  // ‰
    private String roadCondition;       // dry, wet, night, etc.
    private String injurySeverity;      // none / light / serious / fatal (normalized)
    private Double damageEur;           // estimated damage if present
    private String mentalState;         // nehat / umislaj (negligence / intent)
    private Boolean priorRecord;        // previously prosecuted/convicted
    private String punishmentType;
    private Integer sentenceMonths;
    private Integer fine;
    private Boolean drivingBan;
    private Boolean accidentOccured;
    private String roadType;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCourt() {
        return court;
    }

    public Integer getFine() {
        return fine;
    }

    public void setFine(Integer fine) {
        this.fine = fine;
    }

    public void setCourt(String court) {
        this.court = court;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public String getJudge() {
        return judge;
    }

    public void setJudge(String judge) {
        this.judge = judge;
    }

    public String getProsecutor() {
        return prosecutor;
    }

    public void setProsecutor(String prosecutor) {
        this.prosecutor = prosecutor;
    }

    public String getDefendant() {
        return defendant;
    }

    public void setDefendant(String defendant) {
        this.defendant = defendant;
    }

    public String getOffense() {
        return offense;
    }

    public void setOffense(String offense) {
        this.offense = offense;
    }

    public String getVerdictType() {
        return verdictType;
    }

    public void setVerdictType(String verdictType) {
        this.verdictType = verdictType;
    }

    public List<String> getAppliedProvisions() {
        return appliedProvisions;
    }

    public void setAppliedProvisions(List<String> appliedProvisions) {
        this.appliedProvisions = appliedProvisions;
    }

    public List<String> getViolationTypes() {
        return violationTypes;
    }

    public void setViolationTypes(List<String> violationTypes) {
        this.violationTypes = violationTypes;
    }

    public Double getSpeedKmh() {
        return speedKmh;
    }

    public void setSpeedKmh(Double speedKmh) {
        this.speedKmh = speedKmh;
    }

    public Double getSpeedLimitKmh() {
        return speedLimitKmh;
    }

    public void setSpeedLimitKmh(Double speedLimitKmh) {
        this.speedLimitKmh = speedLimitKmh;
    }

    public Boolean getDrivingBan() {
        return drivingBan;
    }

    public void setDrivingBan(Boolean drivingBan) {
        this.drivingBan = drivingBan;
    }

    public Double getAlcoholLevelPromil() {
        return alcoholLevelPromil;
    }

    public void setAlcoholLevelPromil(Double alcoholLevelPromil) {
        this.alcoholLevelPromil = alcoholLevelPromil;
    }

    public String getRoadCondition() {
        return roadCondition;
    }

    public void setRoadCondition(String roadCondition) {
        this.roadCondition = roadCondition;
    }

    public String getInjurySeverity() {
        return injurySeverity;
    }

    public void setInjurySeverity(String injurySeverity) {
        this.injurySeverity = injurySeverity;
    }

    public Double getDamageEur() {
        return damageEur;
    }

    public Boolean getGuilty() {
        return isGuilty;
    }

    public void setGuilty(Boolean guilty) {
        isGuilty = guilty;
    }

    public void setDamageEur(Double damageEur) {
        this.damageEur = damageEur;
    }

    public String getMentalState() {
        return mentalState;
    }

    public void setMentalState(String mentalState) {
        this.mentalState = mentalState;
    }

    public Boolean getPriorRecord() {
        return priorRecord;
    }

    public void setPriorRecord(Boolean priorRecord) {
        this.priorRecord = priorRecord;
    }

    public String getPunishmentType() {
        return punishmentType;
    }

    public void setPunishmentType(String punishmentType) {
        this.punishmentType = punishmentType;
    }

    public Integer getSentenceMonths() {
        return sentenceMonths;
    }

    public void setSentenceMonths(Integer sentenceMonths) {
        this.sentenceMonths = sentenceMonths;
    }

    public Boolean getAccidentOccured() {
        return accidentOccured;
    }

    public void setAccidentOccured(Boolean accidentOccured) {
        this.accidentOccured = accidentOccured;
    }

    public String getRoadType() {
        return roadType;
    }

    public void setRoadType(String roadType) {
        this.roadType = roadType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public Attribute getIdAttribute() {
        return new Attribute("id", this.getClass());
    }

    @Override
    public String toString() {
        return "Judgment{" +
                "id='" + id + '\'' +
                ", filePath='" + filePath + '\'' +
                ", court='" + court + '\'' +
                ", caseNumber='" + caseNumber + '\'' +
                ", judge='" + judge + '\'' +
                ", prosecutor='" + prosecutor + '\'' +
                ", defendant='" + defendant + '\'' +
                ", offense='" + offense + '\'' +
                ", verdictType='" + verdictType + '\'' +
                ", appliedProvisions=" + appliedProvisions +
                ", isGuilty=" + isGuilty +
                ", violationTypes=" + violationTypes +
                ", speedKmh=" + speedKmh +
                ", speedLimitKmh=" + speedLimitKmh +
                ", alcoholLevelPromil=" + alcoholLevelPromil +
                ", roadCondition='" + roadCondition + '\'' +
                ", injurySeverity='" + injurySeverity + '\'' +
                ", damageEur=" + damageEur +
                ", mentalState='" + mentalState + '\'' +
                ", priorRecord=" + priorRecord +
                ", punishmentType='" + punishmentType + '\'' +
                ", sentenceMonths=" + sentenceMonths +
                ", fine=" + fine +
                ", drivingBan=" + drivingBan +
                ", accidentOccured=" + accidentOccured +
                ", roadType='" + roadType + '\'' +
                '}';
    }
}
