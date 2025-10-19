from typing import List, Optional
from pydantic import BaseModel

class GenerateJudgmentDTO(BaseModel):
    id: Optional[str] = None
    court: Optional[str] = None
    caseNumber: Optional[str] = None
    judge: Optional[str] = None
    prosecutor: Optional[str] = None
    defendant: Optional[str] = None

    offense: Optional[str] = None
    verdictType: Optional[str] = None
    appliedProvisions: List[str] = []
    isGuilty: Optional[bool] = None

    violationTypes: List[str] = []
    speedKmh: Optional[float] = None
    speedLimitKmh: Optional[float] = None
    alcoholLevelPromil: Optional[float] = None
    roadCondition: Optional[str] = None
    injurySeverity: Optional[str] = None
    damageEur: Optional[float] = None
    mentalState: Optional[str] = None
    priorRecord: Optional[bool] = None
    punishmentType: Optional[str] = None
    sentenceMonths: Optional[int] = None
    fine: Optional[int] = None
    drivingBan: Optional[bool] = None
    accidentOccured: Optional[bool] = None
    roadType: Optional[str] = None

    textDescription: Optional[str] = None
