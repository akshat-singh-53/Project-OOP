import { useCallback, useEffect, useState } from 'react';
import { api } from './api';

/** Loads data from the API and exposes { data, error, loading, reload }. */
export function useApi(path, params, deps = []) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const key = JSON.stringify(params ?? {});

  const reload = useCallback(async () => {
    if (!path) return;
    setLoading(true);
    try {
      setData(await api(path, { params }));
      setError(null);
    } catch (e) {
      setError(e);
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [path, key, ...deps]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { data, error, loading, reload, setData };
}

/** Re-runs a callback every `ms` milliseconds while the component is mounted. */
export function useInterval(callback, ms) {
  useEffect(() => {
    const id = setInterval(callback, ms);
    return () => clearInterval(id);
  }, [callback, ms]);
}

/** Shared reference data (services, categories, enums). */
export function useReferenceData() {
  const services = useApi('/services');
  const categories = useApi('/categories');
  const enums = useApi('/meta/enums');
  return {
    services: services.data ?? [],
    categories: categories.data ?? [],
    enums: enums.data ?? {},
    reload: () => { services.reload(); categories.reload(); },
  };
}
