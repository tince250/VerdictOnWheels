import axios from "axios";

const API_BASE = "http://localhost:8000"; 

export const fetchLaws = async () => {
  const res = await axios.get(`${API_BASE}/laws`);
  return res.data;
};

export const fetchLawById = async (lawId) => {
  const res = await axios.get(`${API_BASE}/laws/${lawId}`);
  return res.data;
};

export const fetchArticle = async (lawId, articleId) => {
  const res = await axios.get(`${API_BASE}/laws/${lawId}/articles/${articleId}`);
  return res.data;
};

export const fetchJudgments = async () => {
  const res = await axios.get(`${API_BASE}/judgments`);
  return res.data;
};

export const fetchJudgmentById = async (judgmentId) => {
  const res = await axios.get(`${API_BASE}/judgments/${judgmentId}`);
  return res.data;
};
