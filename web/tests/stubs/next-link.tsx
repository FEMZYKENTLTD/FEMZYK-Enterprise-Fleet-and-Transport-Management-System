import type { ReactNode } from 'react';

/** Test double for next/link: renders a plain anchor with the same href. */
export default function Link({ href, children, ...rest }: { href: string; children: ReactNode; [key: string]: unknown }) {
  return (
    <a href={href} {...rest}>
      {children}
    </a>
  );
}
