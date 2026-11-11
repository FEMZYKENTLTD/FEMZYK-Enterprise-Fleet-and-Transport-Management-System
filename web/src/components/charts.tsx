'use client';

import { useId } from 'react';

/**
 * Dependency-free SVG charts (the desktop edition draws these with Java2D).
 * Deliberately lightweight: no chart library ships to the browser.
 */

export function Donut({
  data, size = 168, thickness = 22, centerLabel, centerValue,
}: {
  data: { label: string; value: number; color: string }[];
  size?: number; thickness?: number; centerLabel?: string; centerValue?: string;
}) {
  const total = data.reduce((s, d) => s + d.value, 0);
  const radius = (size - thickness) / 2;
  const circumference = 2 * Math.PI * radius;
  let offset = 0;

  return (
    <div className="flex flex-wrap items-center gap-5">
      <div className="relative shrink-0" style={{ width: size, height: size }}>
        <svg width={size} height={size} className="-rotate-90">
          <circle cx={size / 2} cy={size / 2} r={radius} fill="none" stroke="#eef2f7" strokeWidth={thickness} />
          {total > 0 && data.map((d) => {
            const length = (d.value / total) * circumference;
            const dash = `${length} ${circumference - length}`;
            const el = (
              <circle
                key={d.label}
                cx={size / 2} cy={size / 2} r={radius} fill="none"
                stroke={d.color} strokeWidth={thickness} strokeDasharray={dash} strokeDashoffset={-offset}
                strokeLinecap="butt"
              />
            );
            offset += length;
            return el;
          })}
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-2xl font-bold tabular-nums text-slate-900">{centerValue ?? total}</span>
          {centerLabel && <span className="text-[10px] font-medium uppercase tracking-wider text-slate-500">{centerLabel}</span>}
        </div>
      </div>
      <ul className="min-w-[120px] flex-1 space-y-1.5">
        {data.map((d) => (
          <li key={d.label} className="flex items-center justify-between gap-3 text-xs">
            <span className="flex items-center gap-2 truncate text-slate-600">
              <span className="h-2 w-2 shrink-0 rounded-sm" style={{ background: d.color }} />
              <span className="truncate">{d.label}</span>
            </span>
            <span className="tabular-nums font-semibold text-slate-800">{d.value}</span>
          </li>
        ))}
        {data.length === 0 && <li className="text-xs text-slate-400">No data yet</li>}
      </ul>
    </div>
  );
}

export function Bars({
  data, height = 200, format,
}: {
  data: { label: string; value: number; color?: string }[];
  height?: number; format?: (v: number) => string;
}) {
  const max = Math.max(...data.map((d) => d.value), 1);
  return (
    <div className="space-y-2.5">
      {data.length === 0 && <p className="py-6 text-center text-xs text-slate-400">No data yet</p>}
      {data.map((d) => (
        <div key={d.label} className="group">
          <div className="mb-1 flex items-center justify-between gap-3 text-[12px]">
            <span className="truncate font-medium text-slate-600">{d.label}</span>
            <span className="shrink-0 tabular-nums font-semibold text-slate-800">{format ? format(d.value) : d.value}</span>
          </div>
          <div className="h-2 overflow-hidden rounded-full bg-slate-100">
            <div
              className="h-full rounded-full transition-all duration-500"
              style={{ width: `${Math.max((d.value / max) * 100, d.value > 0 ? 2 : 0)}%`, background: d.color ?? '#1d4ed8' }}
              title={format ? format(d.value) : String(d.value)}
            />
          </div>
        </div>
      ))}
      <div className="sr-only" style={{ height }}>chart</div>
    </div>
  );
}

export function LineArea({
  data, height = 190, format,
}: {
  data: { label: string; value: number }[];
  height?: number; format?: (v: number) => string;
}) {
  const gid = useId().replace(/:/g, '');
  const width = 640;
  const padX = 8;
  const padY = 16;
  const max = Math.max(...data.map((d) => d.value), 1);
  const min = 0;
  const stepX = data.length > 1 ? (width - padX * 2) / (data.length - 1) : 0;
  const y = (v: number) => height - padY - ((v - min) / (max - min || 1)) * (height - padY * 2);
  const points = data.map((d, i) => [padX + i * stepX, y(d.value)] as const);
  const line = points.map((p, i) => `${i === 0 ? 'M' : 'L'}${p[0].toFixed(1)},${p[1].toFixed(1)}`).join(' ');
  const area = `${line} L${padX + (data.length - 1) * stepX},${height - padY} L${padX},${height - padY} Z`;

  return (
    <div>
      <svg viewBox={`0 0 ${width} ${height}`} className="w-full" style={{ height }} role="img" aria-label="Monthly cost trend">
        <defs>
          <linearGradient id={`g-${gid}`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#1d4ed8" stopOpacity="0.22" />
            <stop offset="100%" stopColor="#1d4ed8" stopOpacity="0.01" />
          </linearGradient>
        </defs>
        {[0.25, 0.5, 0.75, 1].map((f) => (
          <line key={f} x1={padX} x2={width - padX} y1={height - padY - f * (height - padY * 2)} y2={height - padY - f * (height - padY * 2)} stroke="#eef2f7" strokeWidth="1" />
        ))}
        <path d={area} fill={`url(#g-${gid})`} />
        <path d={line} fill="none" stroke="#1d4ed8" strokeWidth="2.2" strokeLinejoin="round" strokeLinecap="round" />
        {points.map((p, i) => (
          <g key={data[i].label}>
            <circle cx={p[0]} cy={p[1]} r="3.2" fill="#fff" stroke="#1d4ed8" strokeWidth="2" />
            <title>{`${data[i].label}: ${format ? format(data[i].value) : data[i].value}`}</title>
          </g>
        ))}
      </svg>
      <div className="mt-1 flex justify-between px-1 text-[10px] font-medium text-slate-400">
        {data.map((d) => <span key={d.label}>{d.label}</span>)}
      </div>
    </div>
  );
}
