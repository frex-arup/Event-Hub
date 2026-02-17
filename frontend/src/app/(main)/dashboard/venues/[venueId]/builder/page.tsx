'use client';

import { LayoutBuilder } from '@/components/builder/layout-builder';
import { Button } from '@/components/ui/button';
import { ArrowLeft } from 'lucide-react';
import Link from 'next/link';

export default function LayoutBuilderPage() {
  return (
    <div className="space-y-4">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" asChild>
          <Link href="/dashboard/venues">
            <ArrowLeft className="h-5 w-5" />
          </Link>
        </Button>
        <div>
          <h1 className="text-2xl font-bold">Seating Layout Builder</h1>
          <p className="text-sm text-muted-foreground">
            Design your venue layout with drag-and-drop sections
          </p>
        </div>
      </div>
      <LayoutBuilder />
    </div>
  );
}
