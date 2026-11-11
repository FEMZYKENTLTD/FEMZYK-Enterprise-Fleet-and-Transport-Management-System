import { getSession } from '@/lib/session';
import { redirect } from 'next/navigation';
import { roleHas } from '@/lib/domain';
import { listTrips } from '@/lib/services/trips';
import { listVehicles } from '@/lib/services/vehicles';
import { listDrivers } from '@/lib/services/drivers';
import { TripsClient } from './TripsClient';

export const dynamic = 'force-dynamic';

export default async function TripsPage() {
  const session = await getSession();
  if (!session) redirect('/login');
  const [trips, vehicles, drivers] = await Promise.all([listTrips({ limit: 200 }), listVehicles({}), listDrivers({})]);
  return (
    <TripsClient
      canWrite={roleHas(session.role, 'MANAGE_TRIPS')}
      initial={trips}
      vehicles={vehicles
        .filter((v) => !['RETIRED', 'OUT_OF_SERVICE', 'MAINTENANCE'].includes(v.status))
        .map((v) => ({ value: String(v.id), label: `${v.registration_number} — ${v.make} ${v.model} (${v.current_mileage.toLocaleString()} km)` }))}
      drivers={drivers.filter((d) => d.status === 'ACTIVE').map((d) => ({ value: String(d.id), label: `${d.employee_full_name} — class ${d.licence_category}` }))}
    />
  );
}
