import React from 'react';
import { 
  Box, 
  Paper, 
  Typography, 
  Chip,
  Divider
} from '@mui/material';
import GavelIcon from '@mui/icons-material/Gavel';

const SimilarCaseCard = ({ caseNumber, court, similarity, provisions = [] }) => {
    console.log('Card provisions:', provisions);
  
  return (
    <Paper 
      elevation={2}
      sx={{ 
        p: 2, 
        width: 280,
        minWidth: 280,
        height: '160px',
        display: 'flex',
        flexDirection: 'column',
        borderLeft: '4px solid #EABA90',
        flexShrink: 0, // Prevent shrinking in flex container
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
        <GavelIcon sx={{ mr: 1, color: '#9e9e9e' }} fontSize="small" />
        <Typography variant="subtitle2" fontWeight="bold" noWrap>
          {caseNumber || 'Nepoznat broj'}
        </Typography>
      </Box>

      <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }} noWrap>
        {court || 'Nepoznat sud'}
      </Typography>

      <Chip 
        label={`${Math.round(similarity * 100)}% sličnosti`} 
        size="small" 
        sx={{ 
          alignSelf: 'flex-start', 
          mb: 2,
          bgcolor: similarity > 0.7 ? '#e8f5e9' : '#fff3e0',
          color: similarity > 0.7 ? '#2e7d32' : '#e65100',
          fontWeight: 'bold'
        }}
      />

      <Divider sx={{ mb: 1 }} />
      
      <Typography variant="body2" fontWeight="bold" sx={{ mb: 0.5 }}>
        Primenjene odredbe:
      </Typography>
      
      <Box sx={{ overflow: 'auto', flex: 1 }}>
        {provisions && provisions.length > 0 ? (
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
            {provisions.map((provision, index) => (
              <Chip
                key={index}
                label={provision}
                size="small"
                variant="outlined"
                sx={{ 
                  fontSize: '0.75rem', 
                  height: '24px',
                  backgroundColor: '#f5f5f5'
                }}
              />
            ))}

          </Box>
        ) : (
          <Typography variant="body2" color="text.secondary">
            Nema podataka
          </Typography>
        )}
      </Box>
    </Paper>
  );
};

export default SimilarCaseCard;