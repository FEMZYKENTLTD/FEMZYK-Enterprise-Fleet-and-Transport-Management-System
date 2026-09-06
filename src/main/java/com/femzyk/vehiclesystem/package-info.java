/**
 * LEGACY SERIALIZATION COMPATIBILITY PACKAGE.
 *
 * <p>Earlier releases of the Femzyk Vehicle Management System (v4.x "Storage Edition") persisted data with
 * Java object serialization to {@code ~/FemzykVehicleSystem/users.dat} and
 * {@code ~/FemzykVehicleSystem/profiles/<user>/fleet-data.dat}. Java deserialization resolves classes by
 * fully-qualified name and {@code serialVersionUID}, so these minimal, read-only shadow classes keep the
 * original names/fields purely to let {@link com.femzyk.fleetmanagement.legacy.LegacyDataMigrator} read old
 * files and move the data into the SQLite database. They contain no behaviour and are not used anywhere else.</p>
 */
package com.femzyk.vehiclesystem;
