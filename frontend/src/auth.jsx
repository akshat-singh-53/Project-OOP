import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api, hasToken, onUnauthorized, setToken } from './api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(hasToken());

  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
  }, []);

  useEffect(() => {
    onUnauthorized(logout);
    if (!hasToken()) return;
    api('/auth/me')
      .then(setUser)
      .catch(logout)
      .finally(() => setLoading(false));
  }, [logout]);

  const login = async (username, password) => {
    const res = await api('/auth/login', { method: 'POST', body: { username, password } });
    setToken(res.token);
    setUser(res.user);
    return res.user;
  };

  const register = async (form) => {
    const res = await api('/auth/register', { method: 'POST', body: form });
    setToken(res.token);
    setUser(res.user);
    return res.user;
  };

  const refresh = async () => setUser(await api('/auth/me'));

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, refresh }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
