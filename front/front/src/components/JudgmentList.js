import React, { useEffect, useState } from "react";
import { fetchJudgments } from "../api/api";
import { Link } from "react-router-dom";
import {
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Typography,
  Paper,
} from "@mui/material";

const JudgmentList = () => {
  const [judgments, setJudgments] = useState([]);

  useEffect(() => {
    fetchJudgments().then((data) => {
      setJudgments(data);
      console.log("Fetched judgments:", data);
    });
  }, []);

  return (
    <Paper style={{ padding: 16, marginBottom: 16 }}>
      <Typography variant="h5" gutterBottom>
        Presude
      </Typography>
      <List>
        {judgments.map((name) => (
          <ListItem key={name} disablePadding>
            <ListItemButton component={Link} to={`/judgments/${name}`}>
              <ListItemText primary={name} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
    </Paper>
  );
};

export default JudgmentList;
