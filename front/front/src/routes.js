import React from 'react';
import { Routes, Route } from 'react-router-dom';
import LawList from './components/LawList';
import JudgmentList from './components/JudgmentList';
import LawDetail from './components/LawDetail';
import JudgmentDetail from './components/JudgmentDetail';
import AddCaseForm from './components/AddCaseForm';
import Navbar from './components/navbar';

const AppRoutes = () => {
  return (
    <>
      <Navbar />
      <Routes>
        <Route path="/laws" element={<LawList />} />
        <Route path="/judgments" element={<JudgmentList />} />
        <Route path="/laws/:lawId" element={<LawDetail />} />
        <Route path="/judgments/:judgmentId" element={<JudgmentDetail />} />
        <Route path="/add-case" element={<AddCaseForm />} />
      </Routes>
    </>
  );
};

export default AppRoutes;