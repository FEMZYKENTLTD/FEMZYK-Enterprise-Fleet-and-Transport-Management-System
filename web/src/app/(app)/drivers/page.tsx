import { getSession } from '@/lib/session';
import { redirect } from 'next/navigation';
import { roleHas } from '@/lib/domain';
import { listDrivers } from '@/lib/services/drivers';
import { listEmployees } from '@/lib/services/employees';
import { DriversClient } from './DriversClient';

export const dynamic = 'force-dynamic';

export default async function DriversPage() {
  const session = await getSession();
  if (!session) redirect('/login');
  const [drivers, employees] = await Promise.all([listDrivers({}), listEmployees({ status: 'ACTIVE' })]);
  return (
    <DriversClient
      canWrite={roleHas(session.role, 'MANAGE_DRIVERS')}
      initial={drivers}
      employees={employees.map((e) => ({ value: String(e.id), label: `${e.first_name} ${e.last_name} (${e.employee_code})` }))}
    />
  );
}
