from typing import List, Optional
from pydantic import BaseModel

class GenerateJudgmentDTO(BaseModel):
    id: Optional[str] = ''
    court: Optional[str] = ''
    caseNumber: Optional[str] = ''
    judge: Optional[str] = ''
    prosecutor: Optional[str] = ''
    defendant: Optional[str] = ''

    offense: Optional[str] = ''
    verdictType: Optional[str] = ''
    appliedProvisions: List[str] = []
    isGuilty: Optional[bool] = False

    violationTypes: List[str] = []
    speedKmh: Optional[int] = 0
    speedLimitKmh: Optional[int] = 0
    alcoholLevelPromil: Optional[float] = 0.0
    roadCondition: Optional[str] = ''
    injurySeverity: Optional[str] = ''
    damageEur: Optional[int] = 0
    mentalState: Optional[str] = ''
    priorRecord: Optional[bool] = False
    punishmentType: Optional[str] = ''
    sentenceMonths: Optional[int] = 0
    fine: Optional[int] = 0
    drivingBan: Optional[int] = 0
    accidentOccured: Optional[bool] = False
    roadType: Optional[str] = ''

    date: Optional[str] = ''

    textDescription: Optional[str] = ''
