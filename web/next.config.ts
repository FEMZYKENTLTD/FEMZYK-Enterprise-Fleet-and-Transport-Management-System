import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  reactStrictMode: true,
  poweredByHeader: false,
  experimental: {
    // Server actions carry small JSON payloads; this keeps the default limit explicit.
    serverActions: { bodySizeLimit: '2mb' },
  },
};

export default nextConfig;
