import { useCallback, useEffect, useRef, useState, type DependencyList } from 'react';

interface AsyncState<T> {
  data: T | undefined;
  loading: boolean;
  error: Error | undefined;
  reload: () => void;
  setData: (updater: (prev: T | undefined) => T | undefined) => void;
}

/** Minimal data-fetching hook. Replace with TanStack Query when caching/invalidation is needed. */
export function useAsync<T>(fn: () => Promise<T>, deps: DependencyList = []): AsyncState<T> {
  const [data, setDataState] = useState<T>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error>();
  const [tick, setTick] = useState(0);
  const fnRef = useRef(fn);
  fnRef.current = fn;

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(undefined);
    fnRef
      .current()
      .then((res) => !cancelled && setDataState(res))
      .catch((err: Error) => !cancelled && setError(err))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick]);

  const reload = useCallback(() => setTick((t) => t + 1), []);
  const setData = useCallback((updater: (prev: T | undefined) => T | undefined) => setDataState(updater), []);

  return { data, loading, error, reload, setData };
}
