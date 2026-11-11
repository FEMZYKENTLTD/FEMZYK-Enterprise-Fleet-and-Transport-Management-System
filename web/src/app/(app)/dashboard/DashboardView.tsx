'use client';

import Link from 'next/link';
import {
  Car, Users, Route, Fuel, Wrench, AlertTriangle, BadgeCheck, TrendingUp, Gauge, ClipboardList, ArrowUpRight,
} from 'lucide-react';
import type { DashboardData } from '@/lib/types';
import type { Role } from '@/lib/domain';
import { label, statusTone, vehicleTone, CHART_COLORS } from '@/lib/domain';
import { money, num, compactMoney, monthLabel, date, dateTime } from '@/lib/format';
import { Badge, Card, CardHeader, StatCard, SectionTitle, EmptyState } from '@/components/ui';
import { Donut, Bars, LineArea } from '@/components/charts';

export function DashboardView({ data, role }: { data: DashboardData; role: Role }) {
  const k = data.kpis;
  const statusData = Object.entries(data.vehiclesByStatus)
    .sort((a, b) => b[1] - a[1])
    .map(([status, value], i) => ({ label: label(status), value, color: CHART_COLORS[i % CHART_COLORS.length] }));
  const typeData = Object.entries(data.vehiclesByType).map(([type, value], i) => ({ label: label(type), value, color: CHART_COLORS[i % CHART_COLORS.length] }));
  const monthly = data.expensesByMonth.map((m) => ({ label: monthLabel(m.month), value: m.total }));

  return (
    <div className="space-y-5">
      {/* KPI row 1 */}
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Vehicles" value={num(k.totalVehicles)} hint={`${k.availableVehicles} available · ${k.assignedVehicles} assigned`} icon={<Car className="h-4 w-4" />} tone="info" />
        <StatCard label="Fleet utilisation" value={`${k.fleetUtilisationPct}%`} hint={`${k.activeAssignments} active assignments`} icon={<Gauge className="h-4 w-4" />} tone="success" />
        <StatCard label="Trips this month" value={num(k.completedTripsThisMonth)} hint={`${k.activeTrips} active · ${k.plannedTrips} planned`} icon={<Route className="h-4 w-4" />} />
        <StatCard label="Fuel spend this month" value={money(k.monthFuelCost)} hint={`${num(k.monthDistanceKm)} km travelled`} icon={<Fuel className="h-4 w-4" />} tone="warning" />
      </div>

      {/* KPI row 2 */}
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Employees" value={num(k.totalEmployees)} hint={`${k.activeEmployees} active`} icon={<Users className="h-4 w-4" />} />
        <StatCard label="Drivers" value={num(k.totalDrivers)} hint={`${k.activeDrivers} active · ${k.expiringLicences} expiring soon`} icon={<BadgeCheck className="h-4 w-4" />} tone={k.expiredLicences > 0 ? 'danger' : 'default'} />
        <StatCard label="Maintenance this month" value={money(k.monthMaintenanceCost)} hint={`${k.overdueMaintenance} overdue · ${k.dueSoonMaintenance} due soon`} icon={<Wrench className="h-4 w-4" />} tone={k.overdueMaintenance > 0 ? 'danger' : 'default'} />
        <StatCard label="Total expenses this year" value={compactMoney(k.yearTotalExpenses)} hint={`${money(k.monthTotalExpenses)} in ${new Date().toLocaleDateString('en-GB', { month: 'long' })}`} icon={<TrendingUp className="h-4 w-4" />} tone="success" />
      </div>

      <div className="grid gap-4 xl:grid-cols-3">
        {/* Cost trend */}
        <Card className="xl:col-span-2">
          <CardHeader
            title="Cost trend — last six months"
            subtitle="Fuel + maintenance + other expenses, posted from real records"
            action={<Badge tone="bg-emerald-50 text-emerald-700 ring-emerald-600/20" dot>Live</Badge>}
          />
          <div className="px-5 pb-4 pt-5">
            <LineArea data={monthly} format={(v) => money(v)} />
            <div className="mt-5 grid grid-cols-3 divide-x divide-slate-100 border-t border-slate-100 pt-3 text-center">
              <div>
                <p className="text-[10px] font-semibold uppercase tracking-wider text-slate-400">Fuel</p>
                <p className="mt-0.5 text-sm font-bold tabular-nums text-slate-800">{compactMoney(data.expensesByMonth.reduce((s, m) => s + m.fuel, 0))}</p>
              </div>
              <div>
                <p className="text-[10px] font-semibold uppercase tracking-wider text-slate-400">Maintenance</p>
                <p className="mt-0.5 text-sm font-bold tabular-nums text-slate-800">{compactMoney(data.expensesByMonth.reduce((s, m) => s + m.maintenance, 0))}</p>
              </div>
              <div>
                <p className="text-[10px] font-semibold uppercase tracking-wider text-slate-400">Other</p>
                <p className="mt-0.5 text-sm font-bold tabular-nums text-slate-800">{compactMoney(data.expensesByMonth.reduce((s, m) => s + m.other, 0))}</p>
              </div>
            </div>
          </div>
        </Card>

        {/* Fleet by status */}
        <Card>
          <CardHeader title="Fleet by status" subtitle="State machine enforced on every change" />
          <div className="px-5 py-5">
            <Donut data={statusData} centerLabel="vehicles" centerValue={String(k.totalVehicles)} />
          </div>
          <div className="border-t border-slate-100 px-5 py-3">
            <p className="mb-2 text-[10px] font-semibold uppercase tracking-wider text-slate-400">By type</p>
            <div className="flex flex-wrap gap-1.5">
              {typeData.map((t) => (
                <span key={t.label} className="inline-flex items-center gap-1.5 rounded-md bg-slate-50 px-2 py-1 text-[11px] font-medium text-slate-600">
                  <span className="h-1.5 w-1.5 rounded-full" style={{ background: t.color }} />
                  {t.label} <b className="text-slate-800">{t.value}</b>
                </span>
              ))}
            </div>
          </div>
        </Card>
      </div>

      <div className="grid gap-4 xl:grid-cols-3">
        {/* Alerts */}
        <Card>
          <CardHeader title="Service alerts" subtitle="Next-service date and mileage" action={<Link href="/maintenance" className="text-[11px] font-semibold text-blue-700 hover:underline">Open</Link>} />
          <div className="divide-y divide-slate-100">
            {data.maintenanceAlerts.length === 0 && <EmptyState title="No service due" message="Every vehicle is within its service window." icon={<Wrench className="h-5 w-5" />} />}
            {data.maintenanceAlerts.map((a) => (
              <div key={a.vehicle_id} className="flex items-center justify-between gap-3 px-5 py-2.5">
                <div className="min-w-0">
                  <p className="truncate text-[13px] font-semibold text-slate-800">{a.registration_number}</p>
                  <p className="truncate text-[11px] text-slate-500">
                    {a.next_service_date ? `Due ${date(a.next_service_date)}` : 'Mileage-based'}
                    {a.next_service_mileage ? ` · ${num(a.next_service_mileage)} km` : ''}
                  </p>
                </div>
                <Badge tone={statusTone(a.due_status)}>{a.due_status === 'OVERDUE' ? 'Overdue' : 'Due soon'}</Badge>
              </div>
            ))}
          </div>
        </Card>

        <Card>
          <CardHeader title="Licence alerts" subtitle="Expiry within 30 days" action={<Link href="/drivers" className="text-[11px] font-semibold text-blue-700 hover:underline">Open</Link>} />
          <div className="divide-y divide-slate-100">
            {data.licenceAlerts.length === 0 && <EmptyState title="Licences are current" message="No driver licence expires in the next 30 days." icon={<BadgeCheck className="h-5 w-5" />} />}
            {data.licenceAlerts.map((l) => (
              <div key={l.driver_id} className="flex items-center justify-between gap-3 px-5 py-2.5">
                <div className="min-w-0">
                  <p className="truncate text-[13px] font-semibold text-slate-800">{l.driver_name}</p>
                  <p className="text-[11px] text-slate-500">Class {l.licence_category} · {date(l.licence_expiry_date)}</p>
                </div>
                <Badge tone={statusTone(l.licence_status)}>{l.days_remaining < 0 ? `${Math.abs(l.days_remaining)}d overdue` : `${l.days_remaining}d left`}</Badge>
              </div>
            ))}
          </div>
        </Card>

        <Card>
          <CardHeader title="Highest running cost" subtitle="Fuel + maintenance + other" action={<Link href="/reports" className="text-[11px] font-semibold text-blue-700 hover:underline">Reports</Link>} />
          <div className="px-5 py-4">
            <Bars data={data.topCostVehicles.map((v) => ({ label: `${v.registration_number}`, value: Math.round(v.total_cost) }))} format={(v) => compactMoney(v)} />
            <div className="mt-3 space-y-1">
              {data.topCostVehicles.filter((v) => v.cost_per_km !== null).slice(0, 3).map((v) => (
                <div key={v.registration_number} className="flex justify-between text-[11px] text-slate-500">
                  <span>{v.registration_number} · {num(v.distance_km)} km</span>
                  <span className="font-semibold text-slate-700">{money(v.cost_per_km ?? 0, true)}/km</span>
                </div>
              ))}
            </div>
          </div>
        </Card>
      </div>

      <div className="grid gap-4 xl:grid-cols-3">
        <Card className="xl:col-span-2">
          <CardHeader title="Recent trips" subtitle="Newest departures first" action={<Link href="/trips" className="text-[11px] font-semibold text-blue-700 hover:underline">All trips</Link>} />
          <div className="divide-y divide-slate-100">
            {data.recentTrips.length === 0 && <EmptyState title="No trips yet" message="Log the first trip from the Trips screen." />}
            {data.recentTrips.map((t) => (
              <div key={t.id} className="flex items-center gap-3 px-5 py-2.5">
                <span className={`grid h-8 w-8 shrink-0 place-items-center rounded-lg ${t.status === 'ACTIVE' ? 'bg-sky-50 text-sky-600' : 'bg-slate-100 text-slate-500'}`}>
                  <Route className="h-4 w-4" />
                </span>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-[13px] font-semibold text-slate-800">
                    {t.origin ?? 'HQ'} → {t.destination}
                  </p>
                  <p className="truncate text-[11px] text-slate-500">{t.trip_code} · {t.registration_number} · {t.driver_name}</p>
                </div>
                <div className="text-right">
                  <p className="text-[11px] text-slate-500">{date(t.departure_time)}</p>
                  <Badge tone={statusTone(t.status)}>{label(t.status)}</Badge>
                </div>
              </div>
            ))}
          </div>
        </Card>

        <Card>
          <CardHeader title="Recent activity" subtitle="Audit trail" action={<Link href="/audit" className="text-[11px] font-semibold text-blue-700 hover:underline">Open</Link>} />
          <div className="divide-y divide-slate-100">
            {data.recentAudit.length === 0 && <EmptyState title="No activity yet" />}
            {data.recentAudit.map((a) => (
              <div key={a.id} className="px-5 py-2.5">
                <div className="flex items-center justify-between gap-2">
                  <span className="truncate text-[12px] font-semibold text-slate-700">{a.description ?? a.action}</span>
                  <span className="shrink-0 text-[10px] text-slate-400">{dateTime(a.timestamp)}</span>
                </div>
                <p className="mt-0.5 text-[10px] uppercase tracking-wide text-slate-400">{a.username} · {a.module} · {a.action}</p>
              </div>
            ))}
          </div>
        </Card>
      </div>

      <Card>
        <CardHeader title="Expenses by category this year" subtitle="Manual and system-generated costs together" />
        <div className="px-5 py-4">
          <Bars data={data.expensesByCategory.map((c) => ({ label: label(c.category), value: Math.round(c.amount) }))} format={(v) => compactMoney(v)} />
        </div>
      </Card>

      {role !== 'VIEWER' && (
        <div className="rounded-xl border border-blue-200 bg-blue-50/60 px-4 py-3 text-[12px] leading-relaxed text-blue-900">
          <AlertTriangle className="mr-1.5 inline h-3.5 w-3.5" />
          Quick actions: <Link className="font-semibold underline" href="/assignments">assign a vehicle</Link>,{' '}
          <Link className="font-semibold underline" href="/fuel">log fuel</Link>,{' '}
          <Link className="font-semibold underline" href="/maintenance">book maintenance</Link> or{' '}
          <Link className="font-semibold underline" href="/reports">run a report</Link>.
        </div>
      )}

      <div className="flex flex-wrap gap-2 text-[11px] text-slate-400">
        <span className="inline-flex items-center gap-1"><ClipboardList className="h-3 w-3" /> {num(k.outOfServiceVehicles)} out of service</span>
        <span>·</span>
        <span>{num(k.retiredVehicles)} retired</span>
        <span>·</span>
        <span>{num(k.maintenanceVehicles)} in maintenance</span>
        <span>·</span>
        <Link href="/reports" className="inline-flex items-center gap-0.5 hover:text-slate-600">Open full report set <ArrowUpRight className="h-3 w-3" /></Link>
      </div>
    </div>
  );
}
