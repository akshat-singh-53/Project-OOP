import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth';
import Layout from './components/Layout';
import { Loading } from './components/ui';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import ReportIncident from './pages/ReportIncident';
import IncidentList from './pages/IncidentList';
import IncidentDetail, { IncidentByCode } from './pages/IncidentDetail';
import Notifications from './pages/Notifications';
import Intelligence from './pages/Intelligence';
import SlaPage from './pages/SlaPage';
import Profile from './pages/Profile';
import AdminUsers from './pages/admin/AdminUsers';
import AdminCatalog from './pages/admin/AdminCatalog';
import AdminSettings from './pages/admin/AdminSettings';
import AdminAudit from './pages/admin/AdminAudit';
import AdminOutbox from './pages/admin/AdminOutbox';

function RequireRole({ roles, children }) {
  const { user } = useAuth();
  if (!roles.includes(user.role)) {
    return <div className="alert error">You do not have access to this page.</div>;
  }
  return children;
}

const STAFF = ['TECHNICIAN', 'ADMIN'];

export default function App() {
  const { user, loading } = useAuth();
  if (loading) return <Loading />;

  if (!user) {
    return (
      <Routes>
        <Route path="/register" element={<Register />} />
        <Route path="*" element={<Login />} />
      </Routes>
    );
  }

  return (
    <Routes>
      <Route path="/login" element={<Navigate to="/" replace />} />
      <Route path="/register" element={<Navigate to="/" replace />} />
      <Route element={<Layout />}>
        <Route index element={<Dashboard />} />
        <Route path="report" element={<ReportIncident />} />
        <Route path="incidents" element={<IncidentList />} />
        <Route path="incidents/code/:code" element={<IncidentByCode />} />
        <Route path="incidents/:id" element={<IncidentDetail />} />
        <Route path="notifications" element={<Notifications />} />
        <Route path="profile" element={<Profile />} />
        <Route path="intelligence" element={<RequireRole roles={STAFF}><Intelligence /></RequireRole>} />
        <Route path="sla" element={<RequireRole roles={STAFF}><SlaPage /></RequireRole>} />
        <Route path="admin/users" element={<RequireRole roles={['ADMIN']}><AdminUsers /></RequireRole>} />
        <Route path="admin/catalog" element={<RequireRole roles={['ADMIN']}><AdminCatalog /></RequireRole>} />
        <Route path="admin/settings" element={<RequireRole roles={['ADMIN']}><AdminSettings /></RequireRole>} />
        <Route path="admin/audit" element={<RequireRole roles={['ADMIN']}><AdminAudit /></RequireRole>} />
        <Route path="admin/outbox" element={<RequireRole roles={['ADMIN']}><AdminOutbox /></RequireRole>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
