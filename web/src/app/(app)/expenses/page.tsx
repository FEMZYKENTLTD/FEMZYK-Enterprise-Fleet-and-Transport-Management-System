import { getSession } from '@/lib/session';
import { redirect } from 'next/navigation';
import { roleHas } from '@/lib/domain';
import { listExpenses } from '@/lib/services/operations';
import { listVehicles } from '@/lib/services/vehicles';
import { ExpensesClient } from './ExpensesClient';

export const dynamic = 'force-dynamic';

export default async function ExpensesPage() {
  const session = await getSession();
  if (!session) redirect('/login');
  const [expenses, vehicles] = await Promise.all([listExpenses({}), listVehicles({})]);
  return (
    <ExpensesClient
      canWrite={roleHas(session.role, 'MANAGE_EXPENSES')}
      initial={expenses}
      vehicles={vehicles.map((v) => ({ value: String(v.id), label: `${v.registration_number} — ${v.make} ${v.model}` }))}
    />
  );
}
