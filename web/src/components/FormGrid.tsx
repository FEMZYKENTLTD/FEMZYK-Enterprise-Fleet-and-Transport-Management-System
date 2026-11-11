'use client';

import { useEffect, useState, type ReactNode } from 'react';
import { Button, Field, Select, TextArea, TextInput, toast } from './ui';
import { api } from '@/lib/api';

export function FormGrid({ children }: { children: ReactNode }) {
  return <div className="grid gap-3 sm:grid-cols-2">{children}</div>;
}

export function FormActions({
  onCancel, onSave, busy, label = 'Save record', danger,
}: { onCancel: () => void; onSave: () => void; busy: boolean; label?: string; danger?: ReactNode }) {
  return (
    <div className="mt-5 flex flex-wrap items-center justify-between gap-2 border-t border-slate-100 pt-4">
      <div>{danger}</div>
      <div className="flex gap-2">
        <Button variant="secondary" onClick={onCancel} disabled={busy}>Cancel</Button>
        <Button onClick={onSave} loading={busy}>{label}</Button>
      </div>
    </div>
  );
}

/** Small hook that keeps form state and submits to an endpoint. */
export function useFormSubmit(onDone: () => void) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(method: 'POST' | 'PUT', url: string, payload: unknown, successMessage = 'Saved.') {
    setBusy(true);
    setError(null);
    const result = method === 'POST' ? await api.post(url, payload) : await api.put(url, payload);
    setBusy(false);
    if (!result.ok) {
      setError(result.error);
      toast(result.error, 'error');
      return false;
    }
    toast(successMessage);
    onDone();
    return true;
  }

  return { busy, error, submit };
}

/** Loads options once (vehicles, drivers, …) for select inputs. */
export function useOptions<T>(url: string, key: string, map: (row: T) => { value: string; label: string }) {
  const [options, setOptions] = useState<{ value: string; label: string }[]>([]);
  useEffect(() => {
    let alive = true;
    api.get<Record<string, T[]>>(url).then((result) => {
      if (!alive || !result.ok) return;
      setOptions((result.data[key] ?? []).map(map));
    });
    return () => { alive = false; };
  }, [url, key]); // eslint-disable-line react-hooks/exhaustive-deps
  return options;
}

export { Field, Select, TextArea, TextInput };
