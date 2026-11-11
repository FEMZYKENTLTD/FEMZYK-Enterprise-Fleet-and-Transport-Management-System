import { getSession } from '@/lib/session';
import { redirect } from 'next/navigation';
import { roleHas } from '@/lib/domain';
import { listAssignments } from '@/lib/services/assignments';
import { listVehicles } from '@/lib/services/vehicles';
import { listDrivers } from '@/lib/services/drivers';
import { AssignmentsClient } from './AssignmentsClient';

export const dynamic = 'force-dynamic';

export default async function AssignmentsPage() {
  const session = await getSession();
  if (!session) redirect('/login');
  const [assignments, vehicles, drivers] = await Promise.all([listAssignments({}), listVehicles({}), listDrivers({})]);
  return (
    <AssignmentsClient
      canWrite={roleHas(session.role, 'MANAGE_ASSIGNMENTS')}
      initial={assignments}
      vehicles={vehicles.filter((v) => v.status === 'AVAILABLE').map((v) => ({
        value: String(v.id),
        label: `${v.registration_number} — ${v.year} ${v.make} ${v.model} (${v.vehicle_type.toLowerCase()})`,
      }))}
      drivers={drivers.filter((d) => d.status === 'ACTIVE').map((d) => ({
        value: String(d.id),
        label: `${d.employee_full_name} — class ${d.licence_category}`,
      }))}
    />
  );
}
