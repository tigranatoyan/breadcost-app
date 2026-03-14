/** @type {import('next').NextConfig} */
const nextConfig = {
  output: "standalone",
  eslint: {
    ignoreDuringBuilds: true, // lint runs standalone via `npm run lint`
  },
};
export default nextConfig;
