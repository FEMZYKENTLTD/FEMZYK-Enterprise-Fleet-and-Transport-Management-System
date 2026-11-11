import { getSession } from '@/lib/session';
import { redirect } from 'next/navigation';
import { roleHas } from '@/lib/domain';
import { listEmployees, departments } from '@/lib/services/employees';
import { EmployeesClient } from './EmployeesClient';

export const dynamic = 'force-dynamic';

export default async function EmployeesPage() {
  const session = await getSession();
  if (!session) redirect('/login');
  const [initial, depts] = await Promise.all([listEmployees({}), departments()]);
  return <EmployeesClient canWrite={roleHas(session.role, 'MANAGE_EMPLOYEES')} initial={initial} departments={depts} />;
}
