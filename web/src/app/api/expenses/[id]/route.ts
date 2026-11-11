import { NextResponse } from 'next/server';
import { guarded, readJson } from '@/lib/route';
import { deleteExpense, updateExpense } from '@/lib/services/operations';

type Ctx = { params: Promise<{ id: string }> };

export const PUT = guarded<Ctx>('MANAGE_EXPENSES', async (session, request, context) => {
  const { id } = await context.params;
  return NextResponse.json({ expense: await updateExpense(session, Number(id), await readJson(request)) });
});

export const DELETE = guarded<Ctx>('MANAGE_EXPENSES', async (session, _request, context) => {
  const { id } = await context.params;
  await deleteExpense(session, Number(id));
  return NextResponse.json({ ok: true });
});
