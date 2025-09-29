import React from "react";
import { Link } from "react-router-dom";
import { Button } from "@mui/material";

const ReferenceLink = ({ href, showAs }) => {
  if (href.startsWith("/laws")) {
    return (
      <Button component={Link} to={href} size="small" variant="outlined" style={{ marginLeft: 4, marginRight: 4 }}>
        {showAs}
      </Button>
    );
  }
  return <span>{showAs}</span>;
};

export default ReferenceLink;
