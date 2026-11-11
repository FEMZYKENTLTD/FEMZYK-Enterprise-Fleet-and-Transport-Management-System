import type { Metadata, Viewport } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: {
    default: 'FEMZYK Fleet — Enterprise Fleet & Transport Management',
    template: '%s · FEMZYK Fleet',
  },
  description:
    'Enterprise fleet and transport management: vehicles, drivers, assignments, trips, maintenance, fuel, expenses and reporting. FEMZYK ENTERPRISES LTD.',
  applicationName: 'FEMZYK Fleet',
  authors: [{ name: 'Olufemi Keripe — FEMZYK ENTERPRISES LTD' }],
  robots: { index: false, follow: false },
};

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  themeColor: '#0f172a',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
