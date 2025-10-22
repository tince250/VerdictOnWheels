import React from 'react';
import { Box, Typography } from '@mui/material';
import SimilarCaseCard from './SimilarCaseCard';

const SimilarCasesScroll = ({ similarCases = [] }) => {
      console.log('Card provisions:', similarCases);

  if (!similarCases || similarCases.length === 0) {
    return (
      <Typography variant="body2">
        Nema sličnih slučajeva.
      </Typography>
    );
  }

  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'row',
        width: '100%',
        height: '100%',
        overflowX: 'auto',
        overflowY: 'hidden',
        gap: 2,
        // Custom scrollbar styling
        '&::-webkit-scrollbar': {
          height: 8,
        },
        '&::-webkit-scrollbar-track': {
          backgroundColor: '#f1f1f1',
          borderRadius: 4,
        },
        '&::-webkit-scrollbar-thumb': {
          backgroundColor: '#EABA90',
          borderRadius: 4,
        },
      }}
    >
      {similarCases.map((caseItem, index) => (
        <SimilarCaseCard
          key={index}
          caseNumber={caseItem.caseDesc.caseNumber}
          court={caseItem.caseDesc.court}
          similarity={caseItem.similarity}
          provisions={caseItem.caseDesc.appliedProvisions || []}
        />
      ))}
    </Box>
  );
};

export default SimilarCasesScroll;