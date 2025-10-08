import React, { useEffect, useState } from "react";
import { fetchLaws } from "../api/api";
import { Link } from "react-router-dom";
import { List, ListItem, ListItemButton, ListItemText, Typography, Paper } from "@mui/material";

const LawList = () => {
  const [laws, setLaws] = useState([]);

  useEffect(() => {
    fetchLaws().then(setLaws);
  }, []);

  const formatTitle = (title) => {
    
    const parts = title.split("/");
    return parts[parts.length - 3].replace(/_/g, " ").replace(/\b\w/g, (c) => c.toUpperCase());
  };

  const formatYear = (date) => {
    return new Date(date).getFullYear();
  };

  return (
    <Paper style={{ padding: 16, marginBottom: 16 }}>
      <Typography variant="h5" gutterBottom>Zakonici</Typography>
      <List>
        {laws.map((law, i) => (
          <ListItem key={i} disablePadding>
            <ListItemButton component={Link} to={`/laws/${law.title.split("/")[law.title.split("/").length - 3]}`}>
              <ListItemText
                primary={formatTitle(law.title)}
              />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
    </Paper>
  );
};

export default LawList;