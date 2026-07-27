import type { ComponentType } from 'react';
import About from './pages/About';

/**
 * A single navigable page of the app. The `id` is what appears in the URL as `?feature=<id>` and is
 * also what `hivemodule.xml` points its admin extenders at. Keep the ids stable and aligned with the
 * existing extender ids.
 *
 * This extension contributes exactly one page to the Polarion administration tree; the registry is
 * kept anyway, so adding a second page is a one-entry change and the dev Landing stub keeps working.
 */
export interface Feature {
  id: string;
  label: string;
  description: string;
  component: ComponentType;
}

export const FEATURES: Feature[] = [
  {
    id: 'about',
    label: 'About',
    description: 'Extension version and general information.',
    component: About,
  },
];

export function findFeature(id: string | null): Feature | undefined {
  return FEATURES.find((f) => f.id === id);
}
