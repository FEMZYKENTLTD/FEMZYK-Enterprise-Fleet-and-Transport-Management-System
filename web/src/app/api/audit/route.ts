import { NextResponse } from 'next/server';
import { guarded, query } from '@/lib/route';
import { db } from '@/lib/db';

export const dynamic = 'force-dynamic';

export const GET = guarded('VIEW_AUDIT_LOG', async (_session, request) => {
  const params = query(request);
  const q = params.get('q');
  const limit = Math.min(Number(params.get('limit') ?? 300), 1000);
  let req = db().from('audit_logs').select('*').order('timestamp', { ascending: false }).limit(limit);
  if (q?.trim()) {
    const term = q.trim().replace(/[%,]/g, ' ');
    req = req.or(`username.ilike.%${term}%,action.ilike.%${term}%,module.ilike.%${term}%,description.ilike.%${term}%`);
  }
  const { data, error } = await req;
  if (error) return NextResponse.json({ error: error.message }, { status: 500 });
  return NextResponse.json({ audit: data ?? [] });
});
