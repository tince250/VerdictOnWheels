import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import LawList from "./components/LawList";
import LawDetail from "./components/LawDetail";
import JudgmentList from "./components/JudgmentList";
import JudgmentDetail from "./components/JudgmentDetail";

const AppRoutes = () => (
  <Router>
    <Routes>
      <Route path="/" element={<div>
        <h1>Law & Judgment Review</h1>
        <LawList />
        <JudgmentList />
      </div>} />
      <Route path="/laws/:lawId" element={<LawDetail />} />
      <Route path="/laws/:lawId/articles/:articleId" element={<LawDetail />} />
      <Route path="/judgments/:judgmentId" element={<JudgmentDetail />} />
    </Routes>
  </Router>
);

export default AppRoutes;