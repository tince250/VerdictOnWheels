import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import {
  Box,
  Typography,
  CircularProgress,
  Card,
  CardContent,
  Divider,
  List,
  ListItem,
  ListItemText,
  Paper
} from '@mui/material';

const JudgmentDetail = () => {
  const { judgmentId } = useParams();
  const [judgment, setJudgment] = useState(null);
  const [metadata, setMetadata] = useState(null);

  useEffect(() => {
    const fetchJudgment = async () => {
      try {
        console.log('Fetching judgment for ID:', judgmentId);
        const response = await fetch(`http://localhost:8000/judgments/${judgmentId}`);
        const data = await response.json();
        setJudgment(data);
      } catch (error) {
        console.error('Error fetching judgment:', error);
      }
    };

    const fetchMetadata = async () => {
      try {
        const response = await fetch(`http://localhost:8000/judgments/${judgmentId}/metadata`);
        const data = await response.json();
        setMetadata(data);
      } catch (error) {
        console.error('Error fetching judgment metadata:', error);
      }
    };

    fetchJudgment();
    fetchMetadata();
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

  if (!judgment || !metadata) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height="100vh">
        <CircularProgress />
      </Box>
    );
  }

  const { sections } = judgment;

  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'row',
        gap: 3,
        padding: 3,
        width: '100%',
        height: '100vh',
        boxSizing: 'border-box',
      }}
    >

      <Box
        sx={{
          flex: 2,
          overflowY: 'auto',
          paddingRight: 2,
        }}
      >
        <Card variant="outlined" sx={{ mb: 2 }}>
          <CardContent>
            <Typography variant="h4" gutterBottom>
              Broj:{' '}
              {judgment.metadata.FRBRthis
                .split('/')
                .pop()
                .replace(/_/g, '')
                .replace(/(\d+)(\d{4})/, '$1/$2')}
            </Typography>
            <Typography variant="h6" color="textSecondary" gutterBottom>
              Datum: {judgment.metadata.FRBRdate}
            </Typography>
            <Typography variant="h6" color="textSecondary" gutterBottom>
              Sud: {judgment.metadata.FRBRauthor.replace(/#|_/g, ' ').toUpperCase().trim()}
            </Typography>

            <Divider sx={{ margin: '16px 0' }} />

            <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
              Uvod:
            </Typography>
            <Typography variant="body1" paragraph sx={{ textAlign: 'justify' }}>
              {processTextWithRefs(sections.introduction)}
            </Typography>

            <Divider sx={{ margin: '16px 0' }} />

            <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
              Okrivljeni:
            </Typography>
            <Typography variant="body1" paragraph sx={{ textAlign: 'justify' }}>
              {processTextWithRefs(sections.defendant)}
            </Typography>

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


      <Box
        sx={{
          flex: 1,
          position: 'sticky',
          top: 24,
          height: 'calc(100vh - 48px)',
          overflowY: 'auto',
        }}
      >
        <Paper elevation={3} sx={{ padding: 2, borderRadius: 2 }}>
          <Typography variant="h5" gutterBottom sx={{ fontWeight: 'bold' }}>
            Metadata
          </Typography>
          <Divider sx={{ marginBottom: 2 }} />

          <List dense>
            <ListItem>
              <ListItemText primary="Broj slučaja" secondary={metadata.caseNumber || '-'} />
            </ListItem>
            <ListItem>
              <ListItemText primary="Sud" secondary={metadata.court || '-'} />
            </ListItem>
            <ListItem>
              <ListItemText primary="Sudija" secondary={metadata.judge || '-'} />
            </ListItem>
            <ListItem>
              <ListItemText primary="Tužilac" secondary={metadata.prosecutor || '-'} />
            </ListItem>
            <ListItem>
              <ListItemText primary="Okrivljeni" secondary={metadata.defendant || '-'} />
            </ListItem>
            <Divider sx={{ marginY: 1 }} />
            <ListItem>
              <ListItemText primary="Prekršaj ili krivično delo" secondary={metadata.offense || '-'} />
            </ListItem>
            <ListItem>
              <ListItemText primary="Tip presude" secondary={metadata.verdictType || '-'} />
            </ListItem>
            <ListItem>
              <ListItemText
                primary="Primijenjene odredbe"
                secondary={metadata.appliedProvisions?.join(', ') || '-'}
              />
            </ListItem>
            <Divider sx={{ marginY: 1 }} />
            <ListItem>
              <ListItemText primary="Brzina (km/h)" secondary={metadata.speedKmh ?? '-'} />
            </ListItem>
            <ListItem>
              <ListItemText primary="Ograničenje brzine (km/h)" secondary={metadata.speedLimitKmh ?? '-'} />
            </ListItem>
            <ListItem>
              <ListItemText primary="Alkohol (‰)" secondary={metadata.alcoholLevelPromil ?? '-'} />
            </ListItem>
            <ListItem>
              <ListItemText primary="Stanje na putu" secondary={metadata.roadCondition || '-'} />
            </ListItem>
            <ListItem>
              <ListItemText primary="Stepen povrede" secondary={metadata.injurySeverity || '-'} />
            </ListItem>
            <ListItem>
              <ListItemText primary="Oštećenje (€)" secondary={metadata.damageEur ?? '-'} />
            </ListItem>
            <ListItem>
              <ListItemText primary="Mentalno stanje" secondary={metadata.mentalState || '-'} />
            </ListItem>
            <ListItem>
              <ListItemText primary="Dosije" secondary={metadata.priorRecord ? 'Da' : 'Ne'} />
            </ListItem>
            <ListItem>
              <ListItemText primary="Vrsta kazne" secondary={metadata.punishmentType || '-'} />
            </ListItem>
            <ListItem>
              <ListItemText primary="Kazna (meseci)" secondary={metadata.sentenceMonths ?? '-'} />
            </ListItem>
            <ListItem>
              <ListItemText primary="Saobraćajna nesreća" secondary={metadata.accidentOccured ? 'Da' : 'Ne'} />
            </ListItem>
            <ListItem>
              <ListItemText primary="Vrsta puta" secondary={metadata.roadType || '-'} />
            </ListItem>
          </List>
        </Paper>
      </Box>
    </Box>
  );
};

export default JudgmentDetail;
