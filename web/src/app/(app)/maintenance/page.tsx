import { getSession } from '@/lib/session';
import { redirect } from 'next/navigation';
import { roleHas } from '@/lib/domain';
import { listMaintenance, maintenanceDue } from '@/lib/services/operations';
import { listVehicles } from '@/lib/services/vehicles';
import { MaintenanceClient } from './MaintenanceClient';

export const dynamic = 'force-dynamic';

export default async function MaintenancePage() {
  const session = await getSession();
  if (!session) redirect('/login');
  const [records, due, vehicles] = await Promise.all([listMaintenance({}), maintenanceDue(), listVehicles({})]);
  return (
    <MaintenanceClient
      canWrite={roleHas(session.role, 'MANAGE_MAINTENANCE')}
      initial={records}
      due={due}
      vehicles={vehicles.map((v) => ({ value: String(v.id), label: `${v.registration_number} — ${v.make} ${v.model} (${v.current_mileage.toLocaleString()} km)` }))}
    />
  );
}
