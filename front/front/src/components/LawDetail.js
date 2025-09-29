import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { fetchLawById } from "../api/api";
import { Typography, Paper, Divider, List, ListItem } from "@mui/material";

const LawDetail = () => {
  const { lawId } = useParams();
  const [law, setLaw] = useState(null);

  useEffect(() => {
    fetchLawById(lawId).then(setLaw);
  }, [lawId]);

  if (!law) return <Typography>Loading...</Typography>;

  const formatTitle = (title) => {
    const parts = title.split("/");
    return parts[parts.length - 3].replace(/_/g, " ").replace(/\b\w/g, (c) => c.toUpperCase());
  };

  const formatYear = (date) => {
    return new Date(date).getFullYear();
  };

  return (
    <Paper style={{ padding: 16 }}>
      <Typography variant="h4" gutterBottom>{formatTitle(law.meta.title)}</Typography>
      <Typography variant="subtitle1" gutterBottom>Published: {formatYear(law.meta.date)}</Typography>
      {law.articles.map((article) => (
        <div key={article.id} style={{ marginBottom: 24 }}>
          <Typography variant="h6">{article.num} - {article.heading}</Typography>
          <Divider style={{ marginBottom: 16 }} />
          <List>
            {article.paragraphs.map((paragraph, index) => (
              <div key={paragraph.id} id={paragraph.id} style={{ marginBottom: 16 }}>
                <Typography variant="body1">
                  <strong>({index + 1})</strong> {paragraph.text}
                </Typography>
                {paragraph.references.length > 0 && (
                  <Typography variant="body2" style={{ marginTop: 8 }}>
                    References:{" "}
                    {paragraph.references.map((ref, refIndex) => (
                      <a
                        key={refIndex}
                        href={ref.href}
                        style={{ color: "blue", textDecoration: "underline", marginRight: 8 }}
                      >
                        {ref.showAs}
                      </a>
                    ))}
                  </Typography>
                )}
                {paragraph.points.length > 0 && (
                  <List style={{ paddingLeft: 16 }}>
                    {paragraph.points.map((point, pointIndex) => (
                      <ListItem key={point.id} id={point.id} style={{ display: "list-item" }}>
                        <Typography variant="body2">
                          <strong>{pointIndex + 1})</strong> {point.text}
                        </Typography>
                      </ListItem>
                    ))}
                  </List>
                )}
              </div>
            ))}
          </List>
        </div>
      ))}
    </Paper>
  );
};

export default LawDetail;
