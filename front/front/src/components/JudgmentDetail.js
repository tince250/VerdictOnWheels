import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { fetchJudgmentById } from "../api/api";
import { Typography, Paper, Divider, List, ListItem, Chip, Box } from "@mui/material";
import ReferenceLink from "./ReferenceLink";

const JudgmentDetail = () => {
  const { judgmentId } = useParams();
  const [judgment, setJudgment] = useState(null);

  useEffect(() => {
    fetchJudgmentById(judgmentId).then(setJudgment);
  }, [judgmentId]);

  if (!judgment) return <Typography>Loading...</Typography>;

  return (
    <Paper style={{ padding: 16 }}>
      <Typography variant="h4">{judgment.meta.case_number}</Typography>
      <Typography variant="subtitle1">Court: {judgment.meta.court}</Typography>
      <Typography variant="subtitle1">Date: {judgment.meta.date}</Typography>
      <Box mt={1} mb={2}>
        <Chip label={`Judges: ${judgment.meta.judges.join(", ")}`} style={{ marginRight: 8 }} />
        <Chip label={`Defendant: ${judgment.meta.defendant.join(", ")}`} style={{ marginRight: 8 }} />
        <Chip label={`Prosecutor: ${judgment.meta.prosecutor.join(", ")}`} style={{ marginRight: 8 }} />
      </Box>

      <Typography variant="h5">Facts</Typography>
      <List>
        {judgment.facts.map((f, i) => (
          <ListItem key={i}>
            <Typography>{f}</Typography>
          </ListItem>
        ))}
      </List>

      <Typography variant="h5">Body</Typography>
      {judgment.body.map((section, i) => (
        <div key={i} style={{ marginBottom: 16 }}>
          <Typography variant="h6">{section.section}</Typography>
          <Divider />
          {section.paragraphs.map((p, j) => (
            <Typography key={j} paragraph>
              {p.text}{" "}
              {p.references.map((ref, k) => (
                <ReferenceLink key={k} href={ref.href} showAs={ref.showAs} />
              ))}
            </Typography>
          ))}
        </div>
      ))}
    </Paper>
  );
};

export default JudgmentDetail;
