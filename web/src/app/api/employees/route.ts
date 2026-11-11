import { NextResponse } from 'next/server';
import { handler, guarded, query, readJson } from '@/lib/route';
import { createEmployee, departments, employeeStatistics, listEmployees } from '@/lib/services/employees';

export const GET = handler(async (_session, request) => {
  const params = query(request);
  if (params.get('stats') === '1') {
    return NextResponse.json({ statistics: await employeeStatistics(), departments: await departments() });
  }
  const employees = await listEmployees({
    q: params.get('q') ?? undefined,
    department: params.get('department') ?? undefined,
    status: params.get('status') ?? undefined,
    includeDeleted: params.get('deleted') === '1',
  });
  return NextResponse.json({ employees });
});

export const POST = guarded('MANAGE_EMPLOYEES', async (session, request) => {
  const employee = await createEmployee(session, await readJson(request));
  return NextResponse.json({ employee }, { status: 201 });
});
