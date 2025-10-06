import React, { useState, useEffect } from 'react';
import { Box, Typography, CircularProgress, Card, CardContent, Grid, Divider } from '@mui/material';

const JudgmentDetail = ({ judgmentId }) => {
  const [judgment, setJudgment] = useState(null);

  useEffect(() => {
    const fetchJudgment = async () => {
      try {
        const response = await fetch(`http://localhost:8000/judgments/K_37_2024`);
        const data = await response.json();
        setJudgment(data);
      } catch (error) {
        console.error('Error fetching judgment:', error);
      }
    };
    fetchJudgment();
  }, [judgmentId]);

  const processTextWithRefs = (section) => {
    if (!section) return '';
    const { text, refs } = section;
    if (!refs || refs.length === 0) return text;

    const elements = [];
    let lastIndex = 0;

    refs.forEach((ref, i) => {
      if (ref.start > lastIndex) {
        elements.push(text.slice(lastIndex, ref.start));
      }

      const lawUrl = `http://localhost:3000/laws/${ref.href}`;
      elements.push(
        <a key={i} href={lawUrl} style={{ color: 'black', textDecoration: 'underline' }}>
          {ref.showAs || ref.href}
        </a>
      );

      lastIndex = ref.end;
    });

    if (lastIndex < text.length) {
      elements.push(text.slice(lastIndex));
    }

    return elements;
  };

  if (!judgment) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height="100vh">
        <CircularProgress />
      </Box>
    );
  }

  const { metadata, sections } = judgment;

  return (
    <Box sx={{ padding: 3 }}>
      <Card variant="outlined">
        <CardContent>
          <Typography variant="h4" gutterBottom>
            Broj: {metadata.FRBRthis.split('/').pop().replace(/_/g, '').replace(/(\d+)(\d{4})/, '$1/$2')}
          </Typography>
          <Typography variant="h6" color="textSecondary" gutterBottom>
            Datum: {metadata.FRBRdate}
          </Typography>
          <Typography variant="h6" color="textSecondary" gutterBottom>
            Sud: {metadata.FRBRauthor.replace(/#|_/g, ' ').toUpperCase().trim()}
          </Typography>

          <Divider sx={{ margin: '16px 0' }} />

          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
                Uvod:
              </Typography>
              <Typography variant="body1" paragraph sx={{ textAlign: 'justify' }}>
                {processTextWithRefs(sections.introduction)}
              </Typography>
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
                Okrivljeni:
              </Typography>
              <Typography variant="body1" paragraph sx={{ textAlign: 'justify' }}>
                {processTextWithRefs(sections.defendant)}
              </Typography>
            </Grid>
          </Grid>

          <Divider sx={{ margin: '16px 0' }} />

          <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
            Presuda:
          </Typography>
          <Typography variant="body1" paragraph sx={{ textAlign: 'justify' }}>
            {processTextWithRefs(sections.verdict)}
          </Typography>

          <Divider sx={{ margin: '16px 0' }} />

          <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
            Razlog:
          </Typography>
          <Typography variant="body1" paragraph sx={{ textAlign: 'justify' }}>
            {processTextWithRefs(sections.reasoning)}
          </Typography>

          <Divider sx={{ margin: '16px 0' }} />

          <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
            Izriče:
          </Typography>
          <Typography variant="body1" paragraph sx={{ textAlign: 'justify' }}>
            {processTextWithRefs(sections.punishment)}
          </Typography>

          <Divider sx={{ margin: '16px 0' }} />

          <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
            Obrazloženje:
          </Typography>
          <Typography variant="body1" paragraph sx={{ textAlign: 'justify' }}>
            {processTextWithRefs(sections.arguments)}
          </Typography>
        </CardContent>
      </Card>
    </Box>
  );
};

export default JudgmentDetail;
