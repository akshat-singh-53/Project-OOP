import { useCallback, useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth';
import { useInterval } from '../hooks';
import { relative } from '../format';

const MENUS = {
  EMPLOYEE: [
    { section: 'Self service' },
    { to: '/', label: 'Dashboard', icon: '🏠', end: true },
    { to: '/report', label: 'Report an incident', icon: '➕' },
    { to: '/incidents', label: 'My incidents', icon: '📋' },
    { to: '/notifications', label: 'Notifications', icon: '🔔' },
  ],
  TECHNICIAN: [
    { section: 'My work' },
    { to: '/', label: 'Dashboard', icon: '🏠', end: true },
    { to: '/incidents?mine=1', label: 'My assigned queue', icon: '🧰' },
    { to: '/incidents?unassigned=1', label: 'Unassigned queue', icon: '📥' },
    { to: '/incidents', label: 'All incidents', icon: '📋' },
    { section: 'Insight' },
    { to: '/intelligence', label: 'SLA risk & correlation', icon: '🧠' },
    { to: '/sla', label: 'SLA policies & violations', icon: '⏱️' },
    { to: '/report', label: 'Report an incident', icon: '➕' },
    { to: '/notifications', label: 'Notifications', icon: '🔔' },
  ],
  ADMIN: [
    { section: 'Operations' },
    { to: '/', label: 'Dashboard', icon: '📊', end: true },
    { to: '/incidents', label: 'Incidents', icon: '📋' },
    { to: '/incidents?unassigned=1', label: 'Unassigned queue', icon: '📥' },
    { to: '/intelligence', label: 'SLA risk & correlation', icon: '🧠' },
    { to: '/sla', label: 'SLA policies & violations', icon: '⏱️' },
    { section: 'Administration' },
    { to: '/admin/users', label: 'Users & technicians', icon: '👥' },
    { to: '/admin/catalog', label: 'Services & categories', icon: '🗂️' },
    { to: '/admin/settings', label: 'Strategies & settings', icon: '⚙️' },
    { to: '/admin/audit', label: 'Audit log', icon: '🧾' },
    { to: '/admin/outbox', label: 'Notification outbox', icon: '📤' },
  ],
};

const ROLE_NAMES = { EMPLOYEE: 'Employee', TECHNICIAN: 'Support Engineer', ADMIN: 'Administrator' };

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    document.body.className = 'role-' + user.role;
    return () => { document.body.className = ''; };
  }, [user.role]);

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="brand">🛠️ I-ITSM</div>
        <div className="brand-sub">Intelligent IT Service Management</div>
        {MENUS[user.role].map((item, i) =>
          item.section ? (
            <div key={i} className="nav-section">{item.section}</div>
          ) : (
            <NavLink key={item.to} to={item.to} end={item.end}
                     className={({ isActive }) => (isActive && item.to.indexOf('?') < 0 ? 'active' : '')}>
              <span>{item.icon}</span> {item.label}
            </NavLink>
          ),
        )}
        <div className="spacer" />
        <NavLink to="/profile"><span>👤</span> My profile</NavLink>
        <a href="#/" onClick={(e) => { e.preventDefault(); logout(); navigate('/login'); }}><span>🚪</span> Log out</a>
      </aside>
      <div className="main">
        <header className="topbar">
          <div className="muted small">{ROLE_NAMES[user.role]} portal</div>
          <div className="row">
            <NotificationBell />
            <div className="user-chip">
              <div className="avatar">{user.fullName.split(' ').map((p) => p[0]).join('').slice(0, 2)}</div>
              <div>
                <div style={{ fontWeight: 600, fontSize: '0.9rem' }}>{user.fullName}</div>
                <div className="row" style={{ gap: 6 }}>
                  <span className="role-tag">{user.userCode}</span>
                  {user.levelTitle && <span className="muted small">{user.levelTitle}</span>}
                </div>
              </div>
            </div>
          </div>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

function NotificationBell() {
  const [count, setCount] = useState(0);
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState([]);
  const navigate = useNavigate();

  const poll = useCallback(() => {
    api('/notifications/unread-count').then((r) => setCount(r.count)).catch(() => {});
  }, []);
  useEffect(poll, [poll]);
  useInterval(poll, 15000);

  const toggle = async () => {
    if (!open) setItems(await api('/notifications', { params: { limit: 15 } }));
    setOpen(!open);
  };

  const openItem = async (n) => {
    if (!n.read) await api(`/notifications/${n.id}/read`, { method: 'POST' });
    setOpen(false);
    poll();
    if (n.incidentCode) navigate('/incidents/code/' + n.incidentCode);
  };

  return (
    <>
      <button className="bell" onClick={toggle} title="Notifications">
        🔔{count > 0 && <span className="count">{count}</span>}
      </button>
      {open && (
        <div className="dropdown">
          <div className="row between" style={{ padding: '10px 14px' }}>
            <strong>Notifications</strong>
            <button className="ghost small" onClick={async () => {
              await api('/notifications/read-all', { method: 'POST' });
              setItems(items.map((i) => ({ ...i, read: true }))); poll();
            }}>Mark all read</button>
          </div>
          {items.length === 0 && <div className="empty">Nothing yet</div>}
          {items.map((n) => (
            <div key={n.id} className={'item' + (n.read ? '' : ' unread')} onClick={() => openItem(n)}>
              <div style={{ fontWeight: 600 }}>{n.subject}</div>
              <div className="muted small">{n.message}</div>
              <div className="muted small">{relative(n.createdAt)}</div>
            </div>
          ))}
        </div>
      )}
    </>
  );
}
