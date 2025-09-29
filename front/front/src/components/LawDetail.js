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

  const renderTextWithReferences = (text, references) => {
    let parts = [text];
    references.forEach((ref) => {
      const refIndex = parts.findIndex((part) => typeof part === "string" && part.includes(ref.showAs));
      if (refIndex !== -1) {
        const before = parts[refIndex].split(ref.showAs)[0];
        const after = parts[refIndex].split(ref.showAs)[1];
        parts.splice(refIndex, 1, before, ref, after);
      }
    });

    return parts.map((part, index) => {
      if (typeof part === "string") return <span key={index}>{part}</span>;
      return (
        <a
          key={index}
          href={part.href}
          style={{ color: "blue", textDecoration: "underline" }}
        >
          {part.showAs}
        </a>
      );
    });
  };

  return (
    <Paper style={{ padding: 16 }}>
      <Typography variant="h4" gutterBottom>{formatTitle(law.meta.title)}</Typography>
      <Typography variant="subtitle1" gutterBottom>Published: {formatYear(law.meta.date)}</Typography>
      {law.articles.map((article) => (
        <div key={article.id} id={`clan${article.num.split(" ")[1]}`} style={{ marginBottom: 24 }}>
          <Typography variant="h6">{article.num} - {article.heading}</Typography>
          <Divider style={{ marginBottom: 16 }} />
          <List>
            {article.paragraphs.map((paragraph, index) => (
              <div key={paragraph.id} id={paragraph.id} style={{ marginBottom: 16 }}>
                <Typography variant="body1">
                  <strong>({index + 1})</strong>{" "}
                  {renderTextWithReferences(paragraph.text, paragraph.references)}
                </Typography>
                {paragraph.points.length > 0 && (
                  <List style={{ paddingLeft: 16 }}>
                    {paragraph.points.map((point, pointIndex) => (
                      <ListItem key={point.id} id={point.id} style={{ display: "list-item" }}>
                        <Typography variant="body2">
                          <strong></strong> {point.text}
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
