import { getSession } from '@/lib/session';
import { redirect } from 'next/navigation';
import { roleHas } from '@/lib/domain';
import { listFuel } from '@/lib/services/operations';
import { listVehicles } from '@/lib/services/vehicles';
import { listDrivers } from '@/lib/services/drivers';
import { FuelClient } from './FuelClient';

export const dynamic = 'force-dynamic';

export default async function FuelPage() {
  const session = await getSession();
  if (!session) redirect('/login');
  const [fuel, vehicles, drivers] = await Promise.all([listFuel({}), listVehicles({}), listDrivers({})]);
  return (
    <FuelClient
      canWrite={roleHas(session.role, 'MANAGE_FUEL')}
      initial={fuel}
      vehicles={vehicles.map((v) => ({ value: String(v.id), label: `${v.registration_number} — ${v.make} ${v.model} (${v.current_mileage.toLocaleString()} km)` }))}
      drivers={drivers.filter((d) => d.status === 'ACTIVE').map((d) => ({ value: String(d.id), label: d.employee_full_name }))}
    />
  );
}
