import React from 'react';
import LoginRegister from './components/LoginRegister';
import UserDashboard from "./pages/UserDashboard";
import AdminDashboard from "./pages/AdminDashboard";
import {BrowserRouter, Route, Routes} from "react-router-dom";
function App() {
  return (
      <BrowserRouter>
          <Routes>
              <Route path="/" element={<LoginRegister />} />
              <Route path="/user" element={<UserDashboard />} />
              <Route path="/admin" element={<AdminDashboard />} />
          </Routes>
      </BrowserRouter>
  );
}

export default App;
