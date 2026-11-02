import { useEffect, useState } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Sidebar from './components/Sidebar';
import Header from './components/Header';
import Footer from './components/Footer';
import Dashboard from './pages/Dashboard';
import Products from './pages/Products';
import Recalls from './pages/Recalls';
import Analytics from './pages/Analytics';
import Monitoring from './pages/Monitoring';
import Reports from './pages/Reports';
import Settings from './pages/Settings';
import RoleSelection from './pages/RoleSelection';
import AdminLogin from './pages/AdminLogin';
import AnalystLogin from './pages/AnalystLogin';
import ViewerLogin from './pages/ViewerLogin';
import Register from './pages/Register';
import AnalystRegister from './pages/AnalystRegister';
import AdminUsers from './pages/AdminUsers';
import UserProfile from './pages/UserProfile';
import FloatingAiAssistant from './components/FloatingAiAssistant';

function WorkspaceShell({ dark, setDark }) {
  const [menu, setMenu] = useState(false);

  return (
    <div className="shell">
      <Sidebar open={menu} onClose={() => setMenu(false)} />
      <main>
        <Header dark={dark} toggle={() => setDark((v) => !v)} menu={() => setMenu(true)} />
        <div className="content">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/products" element={<Products />} />
            <Route path="/recalls" element={<Recalls />} />
            <Route path="/assistant" element={<Navigate to="/" replace />} />
            <Route path="/analytics" element={<Analytics />} />
            <Route path="/monitoring" element={<Monitoring />} />
            <Route path="/reports" element={<Reports />} />
            <Route path="/profile" element={<UserProfile />} />
            <Route
              path="/admin/users"
              element={
                <ProtectedRoute allowedRoles={['ADMIN']}>
                  <AdminUsers />
                </ProtectedRoute>
              }
            />
            <Route path="/settings" element={<Settings dark={dark} toggle={() => setDark((v) => !v)} />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </div>
        <FloatingAiAssistant />
        <Footer />
      </main>
    </div>
  );
}

export default function App() {
  const [dark, setDark] = useState(() => {
    try {
      return localStorage.getItem('recallradar_theme') === 'dark';
    } catch {
      return false;
    }
  });

  useEffect(() => {
    document.documentElement.dataset.theme = dark ? 'dark' : 'light';
    try {
      localStorage.setItem('recallradar_theme', dark ? 'dark' : 'light');
    } catch {}
  }, [dark]);

  return (
    <AuthProvider>
      <Routes>
        {/* Public Authentication Routes */}
        <Route path="/login" element={<RoleSelection />} />
        <Route path="/login/admin" element={<AdminLogin />} />
        <Route path="/login/analyst" element={<AnalystLogin />} />
        <Route path="/login/viewer" element={<ViewerLogin />} />
        <Route path="/register" element={<Register />} />
        <Route path="/register/analyst" element={<AnalystRegister />} />

        {/* Protected Enterprise Workspace */}
        <Route
          path="/*"
          element={
            <ProtectedRoute>
              <WorkspaceShell dark={dark} setDark={setDark} />
            </ProtectedRoute>
          }
        />
      </Routes>
    </AuthProvider>
  );
}
