import React from "react";
import { createRoot } from "react-dom/client";
import AppRoutes from "./routes";
import { BrowserRouter } from 'react-router-dom';

const container = document.getElementById("root");
const root = createRoot(container);
root.render(
  <React.StrictMode>
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  </React.StrictMode>
);
