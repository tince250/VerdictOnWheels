import React, { useState } from 'react';
import { 
  Box, 
  Button, 
  Typography, 
  Paper, 
  TextField, 
  Checkbox,
  FormControlLabel,
  Container,
  Divider,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  Radio,
  RadioGroup
} from '@mui/material';
// Import the separate component
import SimilarCasesScroll from './SimilarCasesScroll';

function AddCaseForm() {
  const [activeStep, setActiveStep] = useState(0);
  const steps = ['Unos informacija', 'Pregled i potvrda'];
  
  // Form state
  const [caseData, setCaseData] = useState({
    id: '',
    court: '',
    caseNumber: '',
    place: '',
    judge: '',
    prosecutor: '',
    defendant: '',
    roadType: '',
    speedKmh: 0,
    damageEur: 0,
    alcoholLevelPromil: 0.0,
    speedLimitKmh: 0,
    mentalState: '',
    injurySeverity: '',
    textDescription: '',
    priorRecord: false,
    accidentOccured: false,
    appliedProvisions: [],
    isGuilty: false
  });

  // Verdict state
  const [verdict, setVerdict] = useState({
    ruleVerdict: '',
    caseVerdict: '',
    finalVerdict: 'KRIV JE',
    ruleFine: '0',
    ruleJailMonths: '0',
    ruleDrivingBan: '0',
    caseFine: '0',
    caseJailMonths: '0',
    caseDrivingBan: '0',
    finalFine: '',
    finalJailMonths: '',
    finalDrivingBan: '',
    appliedRules: '',
    similarCases: '',
    similarCasesData: [] // For storing the raw case data
  });

  // Change handler
  const handleChange = (e) => {
    const { name, value, checked, type } = e.target;
    setCaseData({
      ...caseData,
      [name]: type === 'checkbox' ? checked : value
    });
  };

  const handleVerdictChange = (e) => {
    const { name, value } = e.target;
    setVerdict({
      ...verdict,
      [name]: value
    });
  };

  const handleNext = () => {
    // Format the data for API
    const formattedData = {
      ...caseData,
      speedKmh: caseData.speedKmh ? parseInt(caseData.speedKmh) : 0,
      speedLimitKmh: caseData.speedLimitKmh ? parseInt(caseData.speedLimitKmh) : 0,
      alcoholLevelPromil: caseData.alcoholLevelPromil ? parseFloat(caseData.alcoholLevelPromil) : 0.0,
      damageEur: caseData.damageEur ? parseInt(caseData.damageEur) : 0,
    };
    
    console.log('Calling rule-based endpoint with:', formattedData);
    
    // First, call the rule-based endpoint
    fetch('http://localhost:8080/api/judgment/rule-based', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(formattedData)
    })
    .then(response => response.json())
    .then(ruleData => {
      console.log('Rule-based response:', ruleData);

      // Process the new response format
      let ruleFine = "0";
      let ruleVerdict = "NIJE KRIV";
      let appliedRulesDescription = "";
      let provisions = [];

      // Check if we have the new response format
      if (ruleData.penalties) {
        // New format
        ruleFine = (ruleData.penalties.to_pay_min && ruleData.penalties.to_pay_max) 
          ? `${ruleData.penalties.to_pay_min} - ${ruleData.penalties.to_pay_max}` 
          : "0";
        
        ruleVerdict = (Object.keys(ruleData.penalties).length > 0) ? "KRIV JE" : "NIJE KRIV";
        appliedRulesDescription = ruleData.description || "";
        provisions = ruleData.appliedProvisions || [];
      } else {
        // Old format
        ruleFine = (ruleData.to_pay_min && ruleData.to_pay_max) 
          ? `${ruleData.to_pay_min} - ${ruleData.to_pay_max}` 
          : "0";
        
        ruleVerdict = ruleData.triggered_rule ? "KRIV JE" : "NIJE KRIV";
        appliedRulesDescription = ruleData.triggered_rule || "";
      }
      
      // Update case data with applied provisions
      setCaseData(prevData => ({
        ...prevData,
        appliedProvisions: provisions
      }));
      
      // Update verdict state with rule-based data
      setVerdict(prevVerdict => ({
        ...prevVerdict,
        ruleVerdict: ruleVerdict,
        ruleFine: ruleFine,
        appliedRules: appliedRulesDescription,
      }));
      
      // Then call the CBR endpoint
      return fetch(`http://localhost:8080/api/judgment/cbr-judgment?k=5`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formattedData)
      });
    })
    .then(response => response.json())
    .then(cbrData => {
      console.log('CBR response:', cbrData);
      
      // Process the CBR response
      if (cbrData && cbrData.judgment) {
        // Extract the verdict and penalties from the suggested judgment
        const caseVerdict = determineVerdict(cbrData.judgment);
        const caseFine = extractFine(cbrData.judgment) || "0";
        const caseJailMonths = extractJailMonths(cbrData.judgment) || "0";
        const caseDrivingBan = extractDrivingBan(cbrData.judgment) || "0";
        
        // Store the raw similar cases data for the cards
        const similarCasesData = cbrData.similarCases || [];
        
        // Format similar cases for text display (backward compatibility)
        let similarCasesText = '';
        if (similarCasesData.length > 0) {
          similarCasesText = similarCasesData.map(sc => 
            `Presuda ${sc.caseDesc.caseNumber || 'Nepoznat'} - ${sc.caseDesc.court || 'Nepoznat sud'} (${Math.round(sc.similarity * 100)}% sličnosti)`
          ).join('\n');
        }
        
        // Update the verdict state with CBR data
        setVerdict(prevVerdict => ({
          ...prevVerdict,
          caseVerdict: caseVerdict,
          caseFine: caseFine,
          caseJailMonths: caseJailMonths,
          caseDrivingBan: caseDrivingBan,
          similarCases: similarCasesText,
          similarCasesData: similarCasesData, // Store raw data for cards
          // Set the final verdict based on rule and case verdicts
          finalVerdict: prevVerdict.ruleVerdict || caseVerdict || "KRIV JE"
        }));
      }
      
      // Move to next step
      setActiveStep(1);
    })
    .catch(error => {
      console.error('Error in API calls:', error);
      // Still move to next step even if there's an error
      setActiveStep(1);
    });
  };

  // Helper functions for extracting data from the CBR response
  const determineVerdict = (judgment) => {
    if (!judgment) return "";
    return judgment.guilty === true ? "KRIV JE" : "NIJE KRIV";
  };
  
  const extractFine = (judgment) => {
    return judgment.fine ? judgment.fine.toString() : "0";
  };
  
  const extractJailMonths = (judgment) => {
    return judgment.jailMonths ? judgment.jailMonths.toString() : "0";
  };
  
  const extractDrivingBan = (judgment) => {
    return judgment.drivingBan ? judgment.drivingBan.toString() : "0";
  };

  const handleBack = () => {
    setActiveStep(0);
  };

  const handleSubmit = () => {
    // Final submission logic
    const formattedData = {
        ...caseData,
        isGuilty: verdict.finalVerdict === 'KRIV JE',
        fine: verdict.finalFine ? parseInt(verdict.finalFine) : 0,
        sentenceMonths: verdict.finalJailMonths ? parseInt(verdict.finalJailMonths) : 0,
        drivingBan: verdict.finalDrivingBan ? parseInt(verdict.finalDrivingBan) : 0
    };

    console.log('Submitting final judgment:', formattedData);
  
    // First, call the generate endpoint
    fetch('http://localhost:8000/judgments/generate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formattedData)
    })
    .then(response => response.json())
    .then(data => {
        // Check if generation was successful
        if (data.status === "ok") {
        console.log('Judgment generated successfully, now submitting');
        
        // Then call the original endpoint to insert the judgment
        return fetch('http://localhost:8080/api/judgment/', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formattedData)
        });
        } else {
        throw new Error('Failed to generate judgment');
        }
    })
    .then(response => {
        if (response.ok) {
        console.log('Judgment submitted successfully');
        alert('Presuda je uspešno kreirana!');
        // window.location.href = '/judgments'; // Uncomment to redirect
        } else {
        console.error('Error submitting judgment:', response.statusText);
        alert('Došlo je do greške prilikom kreiranja presude. Pokušajte ponovo.');
        }
    })
    .catch(error => {
        console.error('Error in judgment process:', error);
        alert('Došlo je do greške prilikom kreiranja presude. Pokušajte ponovo.');
    });
  };

  // Options
  const roadTypeOptions = [
    { value: 'town_road', label: 'Naseljeno mesto' },
    { value: 'rural_road', label: 'Nenaseljeno mesto' }
  ];
  
  const mentalStateOptions = [
    { value: 'nehat', label: 'Nehat' },
    { value: 'umisljaj', label: 'Umišljaj' }
  ];
  
  const injurySeverityOptions = [
    { value: 'none', label: 'Nema' },
    { value: 'light', label: 'Laka' },
    { value: 'serious', label: 'Teška' },
    { value: 'fatal', label: 'Fatalna' }
  ];

  // Form content
  const renderForm = () => {
    if (activeStep === 0) {
      return (
        <Box sx={{ p: 3 }}>
          <Typography variant="h5" gutterBottom sx={{ mb: 3 }}>
            Kreirajte novu presudu
          </Typography>
          
          <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 2 }}>
            Osnovne informacije o slučaju
          </Typography>
          
          <Box sx={{ mb: 4 }}>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, mb: 2 }}>
              <TextField 
                label="Broj slučaja"
                name="caseNumber"
                value={caseData.caseNumber}
                onChange={handleChange}
                size="small"
                sx={{ width: '230px' }}
              />
              <TextField 
                label="Sud"
                name="court"
                value={caseData.court}
                onChange={handleChange}
                size="small"
                sx={{ width: '230px' }}
              />
              <TextField 
                label="Mesto"
                name="place"
                value={caseData.place}
                onChange={handleChange}
                size="small"
                sx={{ width: '230px' }}
              />
            </Box>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2 }}>
              <TextField 
                label="Sudija"
                name="judge"
                value={caseData.judge}
                onChange={handleChange}
                size="small"
                sx={{ width: '230px' }}
              />
              <TextField 
                label="Tužilac"
                name="prosecutor"
                value={caseData.prosecutor}
                onChange={handleChange}
                size="small"
                sx={{ width: '230px' }}
              />
            </Box>
          </Box>
          
          <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 2 }}>
            Informacije o osudenom
          </Typography>
          
          <Box sx={{ mb: 3, display: 'flex', alignItems: 'center', gap: 3 }}>
            <TextField 
              label="Okrivljeni"
              name="defendant"
              value={caseData.defendant}
              onChange={handleChange}
              size="small"
              sx={{ width: '230px' }}
            />
            <FormControlLabel 
              control={
                <Checkbox 
                  checked={caseData.priorRecord}
                  onChange={handleChange}
                  name="priorRecord"
                  sx={{ 
                    color: '#9e9e9e', 
                    '&.Mui-checked': { 
                    color: '#D19A6A' 
                    } 
                }}
                />
              }
              label="Ranija osuđivanost"
            />
          </Box>
          
          <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 2, mt: 3 }}>
            Činjenično stanje
          </Typography>
          
          <Box sx={{ mb: 3, display: 'flex', flexWrap: 'wrap', gap: 2 }}>
            <FormControl size="small" sx={{ width: '230px' }}>
              <InputLabel>Vrsta puta</InputLabel>
              <Select
                name="roadType"
                value={caseData.roadType}
                label="Vrsta puta"
                onChange={handleChange}
              >
                {roadTypeOptions.map((option) => (
                  <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>
                ))}
              </Select>
            </FormControl>
            
            <FormControl size="small" sx={{ width: '230px' }}>
              <InputLabel>Mentalno stanje</InputLabel>
              <Select
                name="mentalState"
                value={caseData.mentalState}
                label="Mentalno stanje"
                onChange={handleChange}
              >
                {mentalStateOptions.map((option) => (
                  <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>
          
          <Box sx={{ mb: 3, display: 'flex', flexWrap: 'wrap', gap: 2 }}>
            <TextField 
              label="Brzina (km/h)"
              name="speedKmh"
              value={caseData.speedKmh}
              onChange={handleChange}
              size="small"
              sx={{ width: '230px' }}
            />
            
            <TextField 
              label="Ograničenje brzine (km/h)"
              name="speedLimitKmh"
              value={caseData.speedLimitKmh}
              onChange={handleChange}
              size="small"
              sx={{ width: '230px' }}
            />
            
            <TextField 
              label="Nivo alkohola (‰)"
              name="alcoholLevelPromil"
              value={caseData.alcoholLevelPromil}
              onChange={handleChange}
              size="small"
              sx={{ width: '230px' }}
            />
          </Box>
          
          <Box sx={{ mb: 2 }}>
            <FormControlLabel 
              control={
                <Checkbox 
                  checked={caseData.accidentOccured}
                  onChange={handleChange}
                  name="accidentOccured"
                  sx={{ 
                    color: '#9e9e9e', 
                    '&.Mui-checked': { 
                    color: '#D19A6A' 
                    } 
                }}
                />
              }
              label="Došlo do nezgode"
            />
          </Box>
          
          <Box sx={{ mb: 4, display: 'flex', flexWrap: 'wrap', gap: 2 }}>
            <FormControl 
              size="small" 
              disabled={!caseData.accidentOccured}
              sx={{ width: '230px' }}
            >
              <InputLabel>Težina povrede</InputLabel>
              <Select
                name="injurySeverity"
                value={caseData.accidentOccured ? caseData.injurySeverity : ''}
                label="Težina povrede"
                onChange={handleChange}
              >
                {injurySeverityOptions.map((option) => (
                  <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>
                ))}
              </Select>
            </FormControl>
            
            <TextField 
              label="Materijalna šteta (EUR)"
              name="damageEur"
              value={caseData.accidentOccured ? caseData.damageEur : ''}
              onChange={handleChange}
              size="small"
              disabled={!caseData.accidentOccured}
              sx={{ width: '230px' }}
            />
          </Box>
          
          <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 2 }}>
            Opis slučaja
          </Typography>
          
          <TextField 
            fullWidth
            multiline
            rows={4}
            label="Tekstualni opis - detalji"
            name="textDescription"
            value={caseData.textDescription}
            onChange={handleChange}
          />
        </Box>
      );
    } else {
      return (
        <Box sx={{ p: 3 }}>
          <Typography variant="h5" gutterBottom sx={{ mb: 3 }}>
            Kreirajte novu presudu
          </Typography>
          
          <Box sx={{ display: 'flex', mb: 4, gap: 3 }}>
            {/* Left section - Rule-based reasoning */}
            <Box sx={{ width: '50%' }}>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                <Typography variant="subtitle1" fontWeight="bold" sx={{ mr: 2 }}>
                  Rasuđivanje po pravilima:
                </Typography>
                <Typography 
                  variant="subtitle1" 
                  fontWeight="bold" 
                  sx={{ 
                    color: verdict.ruleVerdict === 'KRIV JE' ? '#f44336' : '#4caf50',
                  }}
                >
                  {verdict.ruleVerdict}
                </Typography>
              </Box>
              
              <Typography variant="subtitle1" sx={{ mb: 1 }}>
                Detalji presude:
              </Typography>
              
              <Box sx={{ mb: 2 }}>
                <TextField
                  fullWidth
                  size="small"
                  label="Novčana kazna"
                  value={verdict.ruleFine}
                  onChange={handleVerdictChange}
                  name="ruleFine"
                  sx={{ mb: 2 }}
                  disabled
                />
                <TextField
                  fullWidth
                  size="small"
                  label="Meseci zatvora"
                  value={verdict.ruleJailMonths}
                  onChange={handleVerdictChange}
                  name="ruleJailMonths"
                  sx={{ mb: 2 }}
                  disabled
                />
                <TextField
                  fullWidth
                  size="small"
                  label="Zabrana upravljanja"
                  value={verdict.ruleDrivingBan}
                  onChange={handleVerdictChange}
                  name="ruleDrivingBan"
                  disabled
                />
              </Box>
              
              <Typography variant="subtitle1" sx={{ mb: 1, mt: 3 }}>
                Primenjena pravila:
              </Typography>
              
              <Paper 
                elevation={1} 
                sx={{ 
                  p: 2, 
                  bgcolor: '#f5f5f5', 
                  height: '200px',
                  overflow: 'auto'
                }}
              >
                <Box sx={{ display: 'flex' }}>
                  {/* Description column */}
                  <Box sx={{ flex: 1, pr: 2 }}>
                    <Typography variant="body2" fontWeight="bold" sx={{ mb: 1 }}>
                      Opis:
                    </Typography>
                    <Typography variant="body2">
                      {verdict.appliedRules || "Nema opisa"}
                    </Typography>
                  </Box>
                  
                  {/* Applied provisions column */}
                  <Box sx={{ flex: 1 }}>
                    <Typography variant="body2" fontWeight="bold" sx={{ mb: 1 }}>
                      Primenjene odredbe:
                    </Typography>
                    {caseData.appliedProvisions && caseData.appliedProvisions.length > 0 ? (
                      <ul style={{ paddingLeft: '20px', margin: 0 }}>
                        {caseData.appliedProvisions.map((provision, index) => (
                          <li key={index}>
                            <Typography variant="body2">{provision}</Typography>
                          </li>
                        ))}
                      </ul>
                    ) : (
                      <Typography variant="body2">Nema primenjenih odredbi</Typography>
                    )}
                  </Box>
                </Box>
              </Paper>
            </Box>
            
            {/* Right section - Case-based reasoning */}
            <Box sx={{ width: '50%' }}>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                <Typography variant="subtitle1" fontWeight="bold" sx={{ mr: 2 }}>
                  Rasuđivanje po slučajevima:
                </Typography>
                <Typography 
                  variant="subtitle1" 
                  fontWeight="bold" 
                  sx={{ 
                    color: verdict.caseVerdict === 'KRIV JE' ? '#f44336' : '#4caf50',
                  }}
                >
                  {verdict.caseVerdict}
                </Typography>
              </Box>
              
              <Typography variant="subtitle1" sx={{ mb: 1 }}>
                Detalji presude:
              </Typography>
              
              <Box sx={{ mb: 2 }}>
                <TextField
                  fullWidth
                  size="small"
                  label="Novčana kazna"
                  value={verdict.caseFine}
                  onChange={handleVerdictChange}
                  name="caseFine"
                  sx={{ mb: 2 }}
                  disabled
                />
                <TextField
                  fullWidth
                  size="small"
                  label="Meseci zatvora"
                  value={verdict.caseJailMonths}
                  onChange={handleVerdictChange}
                  name="caseJailMonths"
                  sx={{ mb: 2 }}
                  disabled
                />
                <TextField
                  fullWidth
                  size="small"
                  label="Zabrana upravljanja"
                  value={verdict.caseDrivingBan}
                  onChange={handleVerdictChange}
                  name="caseDrivingBan"
                  disabled
                />
              </Box>
              
              <Typography variant="subtitle1" sx={{ mb: 1, mt: 3 }}>
                Slični slučajevi:
              </Typography>
              
              <Paper 
                elevation={1} 
                sx={{ 
                  p: 2, 
                  bgcolor: '#f5f5f5', 
                  height: '200px',
                  overflow: 'hidden'
                }}
              >
                {/* Use the imported SimilarCasesScroll component */}
                <SimilarCasesScroll similarCases={verdict.similarCasesData || []} />
              </Paper>
            </Box>
          </Box>
          
          <Divider sx={{ mt: 3, mb: 3 }} />
          
          {/* Final verdict section */}
          <Box>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
              <Typography variant="h6" fontWeight="bold" sx={{ mr: 2 }}>
                Konačna presuda:
              </Typography>
              <RadioGroup
                row
                name="finalVerdict"
                value={verdict.finalVerdict}
                onChange={handleVerdictChange}
              >
                <FormControlLabel 
                
                  value="KRIV JE" 
                  control={<Radio sx={{ 
                        color: '#D19A6A', 
                        '&.Mui-checked': { 
                        color: '#D19A6A' 
                        } 
                    }}/>} 
                  label={<Typography sx={{ color: '#f44336', fontWeight: 'bold' }}>KRIV JE</Typography>}
                />
                <FormControlLabel 
                  value="NIJE KRIV" 
                  control={<Radio sx={{ 
                    color: "#D19A6A", 
                    '&.Mui-checked': { 
                      color: "#D19A6A" 
                    } 
                  }}/>} 
                  label={<Typography sx={{ color: '#4caf50', fontWeight: 'bold' }}>NIJE KRIV</Typography>}
                />
              </RadioGroup>
            </Box>
            
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2 }}>
              <TextField
                size="small"
                label="Novčana kazna"
                value={verdict.finalFine}
                onChange={handleVerdictChange}
                name="finalFine"
                sx={{ width: '230px' }}
              />
              <TextField
                size="small"
                label="Meseci zatvora"
                value={verdict.finalJailMonths}
                onChange={handleVerdictChange}
                name="finalJailMonths"
                sx={{ width: '230px' }}
              />
              <TextField
                size="small"
                label="Zabrana upravljanja"
                value={verdict.finalDrivingBan}
                onChange={handleVerdictChange}
                name="finalDrivingBan"
                sx={{ width: '230px' }}
              />
            </Box>
          </Box>
        </Box>
      );
    }
  };

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <Paper elevation={3} sx={{ p: 0 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', p: 2 }}>
          <Box sx={{ 
            width: 24, 
            height: 24, 
            borderRadius: '50%', 
            bgcolor: activeStep === 0 ? '#D19A6A' : '#e0e0e0', 
            color: activeStep === 0 ? 'white' : '#757575', 
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'center',
            mr: 1,
            fontSize: '14px'
          }}>
            1
          </Box>
          <Typography variant="body2" sx={{ mr: 4 }}>Unos informacija</Typography>
          <Box sx={{ 
            width: 24, 
            height: 24, 
            borderRadius: '50%', 
            bgcolor: activeStep === 1 ? '#D19A6A' : '#e0e0e0', 
            color: activeStep === 1 ? 'white' : '#757575', 
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'center',
            mr: 1,
            fontSize: '14px'
          }}>
            2
          </Box>
          <Typography variant="body2">Pregled i potvrda</Typography>
        </Box>
        <Divider />
        
        {renderForm()}
        
        <Box sx={{ display: 'flex', justifyContent: 'space-between', p: 3 }}>
          <Button
            color="inherit"
            disabled={activeStep === 0}
            onClick={handleBack}
            sx={{ mr: 1, textTransform: 'uppercase' }}
            variant="outlined"
          >
            Nazad
          </Button>
          <Button
            variant="contained"
            onClick={activeStep === steps.length - 1 ? handleSubmit : handleNext}
            sx={{ 
              bgcolor: '#EABA90',
              color: 'black',
              '&:hover': {
                bgcolor: '#D19A6A',
              },
              textTransform: 'uppercase'
            }}
          >
            {activeStep === steps.length - 1 ? 'Potvrdi' : 'Dalje'}
          </Button>
        </Box>
      </Paper>
    </Container>
  );
}

export default AddCaseForm;