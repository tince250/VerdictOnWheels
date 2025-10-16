import React, { useEffect, useState } from "react";
import { fetchJudgments } from "../api/api";
import { Link } from "react-router-dom";
import { List, ListItem, ListItemButton, ListItemText, Typography, Paper } from "@mui/material";

const JudgmentList = () => {
  const [judgments, setJudgments] = useState([]);

  useEffect(() => {
    fetchJudgments().then(setJudgments);
    console.log("Fetched judgments:", judgments);
  }, []);

  return (
    <Paper style={{ padding: 16, marginBottom: 16 }}>
      <Typography variant="h5" gutterBottom>Presude</Typography>
      
    </Paper>
  );
};

export default JudgmentList;
