import { NextResponse } from 'next/server';
import { handler, guarded, query, readJson } from '@/lib/route';
import { createExpense, expenseTotals, listExpenses } from '@/lib/services/operations';

export const GET = handler(async (_session, request) => {
  const params = query(request);
  if (params.get('totals') === '1') return NextResponse.json({ totals: await expenseTotals() });
  const expenses = await listExpenses({
    q: params.get('q') ?? undefined,
    category: params.get('category') ?? undefined,
    vehicleId: params.get('vehicleId') ? Number(params.get('vehicleId')) : undefined,
    from: params.get('from') ?? undefined,
    to: params.get('to') ?? undefined,
    excludeSystem: params.get('manual') === '1',
  });
  return NextResponse.json({ expenses });
});

export const POST = guarded('MANAGE_EXPENSES', async (session, request) => {
  const expense = await createExpense(session, await readJson(request));
  return NextResponse.json({ expense }, { status: 201 });
});
