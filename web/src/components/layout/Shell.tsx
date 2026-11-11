'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useEffect, useState, type ReactNode } from 'react';
import {
  LayoutDashboard, Users, Contact, Car, ClipboardList, Route, Wrench, Fuel, ReceiptText,
  BarChart3, ScrollText, Server, ShieldCheck, LogOut, Menu, X, KeyRound, Building2, ChevronRight,
} from 'lucide-react';
import type { Permission, Role } from '@/lib/domain';
import { ROLE_PERMISSIONS, label } from '@/lib/domain';
import { Button, Modal, Field, TextInput, ToastHost, toast } from '@/components/ui';
import { api } from '@/lib/api';

type NavItem = { href: string; label: string; icon: ReactNode; permission: Permission };

const GROUPS: { title: string; items: NavItem[] }[] = [
  {
    title: 'Overview',
    items: [{ href: '/dashboard', label: 'Dashboard', icon: <LayoutDashboard className="h-4 w-4" />, permission: 'VIEW_DASHBOARD' }],
  },
  {
    title: 'People',
    items: [
      { href: '/employees', label: 'Employees', icon: <Users className="h-4 w-4" />, permission: 'VIEW_RECORDS' },
      { href: '/drivers', label: 'Drivers', icon: <Contact className="h-4 w-4" />, permission: 'VIEW_RECORDS' },
    ],
  },
  {
    title: 'Fleet',
    items: [
      { href: '/vehicles', label: 'Vehicles', icon: <Car className="h-4 w-4" />, permission: 'VIEW_RECORDS' },
      { href: '/assignments', label: 'Assignments', icon: <ClipboardList className="h-4 w-4" />, permission: 'VIEW_RECORDS' },
      { href: '/trips', label: 'Trips', icon: <Route className="h-4 w-4" />, permission: 'VIEW_RECORDS' },
    ],
  },
  {
    title: 'Operations',
    items: [
      { href: '/maintenance', label: 'Maintenance', icon: <Wrench className="h-4 w-4" />, permission: 'VIEW_RECORDS' },
      { href: '/fuel', label: 'Fuel', icon: <Fuel className="h-4 w-4" />, permission: 'VIEW_RECORDS' },
      { href: '/expenses', label: 'Expenses', icon: <ReceiptText className="h-4 w-4" />, permission: 'VIEW_RECORDS' },
    ],
  },
  {
    title: 'Insight',
    items: [
      { href: '/reports', label: 'Reports', icon: <BarChart3 className="h-4 w-4" />, permission: 'VIEW_REPORTS' },
      { href: '/audit', label: 'Audit log', icon: <ScrollText className="h-4 w-4" />, permission: 'VIEW_AUDIT_LOG' },
      { href: '/system', label: 'Data & system', icon: <Server className="h-4 w-4" />, permission: 'EXPORT_DATA' },
    ],
  },
  {
    title: 'Administration',
    items: [{ href: '/users', label: 'Users & roles', icon: <ShieldCheck className="h-4 w-4" />, permission: 'MANAGE_USERS' }],
  },
];

export type SessionInfo = { id: number; username: string; fullName: string; email: string; role: Role; mustChangePassword: boolean };

export function Shell({ user, children }: { user: SessionInfo; children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [passwordOpen, setPasswordOpen] = useState(user.mustChangePassword);
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [confirm, setConfirm] = useState('');
  const [busy, setBusy] = useState(false);

  const permissions = ROLE_PERMISSIONS[user.role] ?? [];
  const can = (p: Permission) => permissions.includes(p);

  useEffect(() => { setOpen(false); }, [pathname]);

  async function signOut() {
    await api.post('/api/auth/logout');
    router.push('/login');
    router.refresh();
  }

  async function changePassword() {
    if (next !== confirm) { toast('The new passwords do not match.', 'error'); return; }
    setBusy(true);
    const result = await api.post('/api/auth/password', { currentPassword: current, newPassword: next });
    setBusy(false);
    if (!result.ok) { toast(result.error, 'error'); return; }
    toast('Password changed.');
    setPasswordOpen(false);
    setCurrent(''); setNext(''); setConfirm('');
    router.refresh();
  }

  const active = (href: string) => pathname === href || pathname.startsWith(`${href}/`);

  return (
    <div className="flex min-h-screen bg-[#f4f6f9]">
      {/* Sidebar */}
      <aside className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col bg-[#0f172a] transition-transform lg:translate-x-0 ${open ? 'translate-x-0' : '-translate-x-full'}`}>
        <div className="flex items-center justify-between px-5 pb-3 pt-5">
          <Link href="/dashboard" className="flex items-center gap-2.5">
            <span className="grid h-9 w-9 place-items-center rounded-lg bg-blue-700 text-sm font-black text-white">FE</span>
            <span>
              <span className="block text-[15px] font-bold tracking-wide text-white">FEMZYK</span>
              <span className="block text-[10px] font-medium uppercase tracking-[0.14em] text-slate-400">Fleet &amp; Transport</span>
            </span>
          </Link>
          <button className="rounded p-1 text-slate-400 hover:text-white lg:hidden" onClick={() => setOpen(false)} aria-label="Close menu"><X className="h-4 w-4" /></button>
        </div>

        <nav className="thin-scroll flex-1 overflow-y-auto px-2.5 pb-4">
          {GROUPS.map((group) => {
            const items = group.items.filter((i) => can(i.permission));
            if (!items.length) return null;
            return (
              <div key={group.title} className="mb-3">
                <p className="px-2.5 pb-1 pt-2 text-[10px] font-bold uppercase tracking-[0.12em] text-slate-500">{group.title}</p>
                {items.map((item) => (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={`mb-0.5 flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-[13px] font-medium transition ${
                      active(item.href) ? 'bg-blue-700 text-white shadow-sm' : 'text-slate-300 hover:bg-white/5 hover:text-white'}`}
                  >
                    {item.icon}
                    <span className="flex-1">{item.label}</span>
                    {active(item.href) && <ChevronRight className="h-3.5 w-3.5 opacity-70" />}
                  </Link>
                ))}
              </div>
            );
          })}
        </nav>

        <div className="border-t border-white/10 px-3 py-3">
          <div className="mb-2 flex items-center gap-2.5 rounded-lg px-2 py-1.5">
            <span className="grid h-8 w-8 place-items-center rounded-full bg-white/10 text-xs font-semibold text-white">
              {user.fullName.split(/\s+/).slice(0, 2).map((p) => p[0]).join('')}
            </span>
            <div className="min-w-0">
              <p className="truncate text-xs font-semibold text-white">{user.fullName}</p>
              <p className="truncate text-[11px] text-slate-400">{label(user.role)}</p>
            </div>
          </div>
          <div className="flex gap-1.5">
            <button onClick={() => setPasswordOpen(true)} className="flex flex-1 items-center justify-center gap-1.5 rounded-md bg-white/5 px-2 py-1.5 text-[11px] font-medium text-slate-300 hover:bg-white/10 hover:text-white">
              <KeyRound className="h-3.5 w-3.5" /> Password
            </button>
            <button onClick={signOut} className="flex flex-1 items-center justify-center gap-1.5 rounded-md bg-white/5 px-2 py-1.5 text-[11px] font-medium text-slate-300 hover:bg-white/10 hover:text-white">
              <LogOut className="h-3.5 w-3.5" /> Sign out
            </button>
          </div>
        </div>
      </aside>

      {open && <div className="fixed inset-0 z-30 bg-slate-900/40 lg:hidden" onClick={() => setOpen(false)} />}

      {/* Main */}
      <div className="flex min-w-0 flex-1 flex-col lg:pl-64">
        <header className="sticky top-0 z-20 flex h-14 items-center gap-3 border-b border-slate-200 bg-white/90 px-4 backdrop-blur lg:px-6">
          <button className="rounded-md p-1.5 text-slate-500 hover:bg-slate-100 lg:hidden" onClick={() => setOpen(true)} aria-label="Open menu">
            <Menu className="h-5 w-5" />
          </button>
          <div className="flex min-w-0 items-center gap-2 text-sm">
            <Building2 className="hidden h-4 w-4 text-slate-400 sm:block" />
            <span className="truncate font-semibold text-slate-800">FEMZYK ENTERPRISES LTD</span>
            <span className="hidden rounded-full bg-blue-50 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-blue-700 sm:inline">Live</span>
          </div>
          <div className="ml-auto flex items-center gap-2">
            <span className="hidden text-xs text-slate-500 md:inline">Signed in as <b className="text-slate-700">{user.username}</b></span>
            <span className="grid h-8 w-8 place-items-center rounded-full bg-slate-900 text-[11px] font-semibold text-white">
              {user.fullName.split(/\s+/).slice(0, 2).map((p) => p[0]).join('')}
            </span>
          </div>
        </header>

        <main className="mx-auto w-full max-w-[1500px] flex-1 px-4 py-5 lg:px-6 lg:py-6">{children}</main>

        <footer className="border-t border-slate-200 px-4 py-3 text-[11px] text-slate-400 lg:px-6">
          FEMZYK Enterprise Fleet &amp; Transport Management System v3.0 · web edition · Java 17 desktop + PostgreSQL cloud, one design language ·
          CSC 392 SIWES, Lagos State University
        </footer>
      </div>

      <Modal
        open={passwordOpen}
        onClose={() => { if (!user.mustChangePassword) setPasswordOpen(false); }}
        title={user.mustChangePassword ? 'Set a new password' : 'Change password'}
        subtitle={user.mustChangePassword ? 'Your account was created with a temporary password. Choose your own before continuing.' : 'Passwords need 8+ characters with at least one letter and one digit.'}
        width="max-w-md"
        footer={<>
          {!user.mustChangePassword && <Button variant="secondary" onClick={() => setPasswordOpen(false)}>Cancel</Button>}
          <Button onClick={changePassword} loading={busy} disabled={!current || !next}>Update password</Button>
        </>}
      >
        <div className="space-y-3">
          <Field label="Current password" required><TextInput type="password" value={current} onChange={(e) => setCurrent(e.target.value)} autoComplete="current-password" /></Field>
          <Field label="New password" required hint="At least 8 characters, one letter and one digit."><TextInput type="password" value={next} onChange={(e) => setNext(e.target.value)} autoComplete="new-password" /></Field>
          <Field label="Confirm new password" required><TextInput type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} autoComplete="new-password" /></Field>
        </div>
      </Modal>

      <ToastHost />
    </div>
  );
}
