import { NextResponse } from 'next/server';
import { guarded, query } from '@/lib/route';
import { buildReport, REPORT_KINDS, reportToCsv, reportToHtml, type ReportKind } from '@/lib/services/reports';

export const dynamic = 'force-dynamic';

function isKind(v: string | null): v is ReportKind {
  return Boolean(v) && REPORT_KINDS.some((r) => r.id === v);
}

export const GET = guarded('VIEW_REPORTS', async (_session, request) => {
  const params = query(request);
  const kind = params.get('kind');
  const format = params.get('format') ?? 'json';
  if (!isKind(kind)) return NextResponse.json({ error: 'Unknown report kind.' }, { status: 400 });

  const report = await buildReport(kind);
  if (format === 'csv') {
    return new NextResponse(reportToCsv(report), {
      headers: {
        'Content-Type': 'text/csv; charset=utf-8',
        'Content-Disposition': `attachment; filename="femzyk-${kind}-${new Date().toISOString().slice(0, 10)}.csv"`,
        'Cache-Control': 'no-store',
      },
    });
  }
  if (format === 'html') {
    return new NextResponse(reportToHtml(report), {
      headers: { 'Content-Type': 'text/html; charset=utf-8', 'Cache-Control': 'no-store' },
    });
  }
  return NextResponse.json({ report, kinds: REPORT_KINDS });
});

export const POST = guarded('VIEW_REPORTS', async (_session, request) => {
  const body = (await request.json().catch(() => ({}))) as { kind?: string };
  if (!isKind(body.kind ?? null)) return NextResponse.json({ error: 'Unknown report kind.' }, { status: 400 });
  return NextResponse.json({ report: await buildReport(body.kind as ReportKind), kinds: REPORT_KINDS });
});
