'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Plus, MapPin, Layout } from 'lucide-react';
import Link from 'next/link';

const mockVenues = [
  { id: '1', name: 'Grand Arena', city: 'New York', capacity: 5000, layouts: 2 },
  { id: '2', name: 'Tech Center', city: 'San Francisco', capacity: 800, layouts: 1 },
  { id: '3', name: 'City Theater', city: 'Chicago', capacity: 1200, layouts: 3 },
];

export default function VenuesPage() {
  const [showCreate, setShowCreate] = useState(false);

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold mb-2">Venues & Layouts</h1>
          <p className="text-muted-foreground">Manage venues and design seating layouts</p>
        </div>
        <Dialog open={showCreate} onOpenChange={setShowCreate}>
          <DialogTrigger asChild>
            <Button>
              <Plus className="h-4 w-4 mr-2" />
              New Venue
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Create Venue</DialogTitle>
            </DialogHeader>
            <div className="space-y-4 pt-4">
              <div className="space-y-2">
                <Label>Venue Name</Label>
                <Input placeholder="e.g. Grand Arena" />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>City</Label>
                  <Input placeholder="City" />
                </div>
                <div className="space-y-2">
                  <Label>Country</Label>
                  <Input placeholder="Country" />
                </div>
              </div>
              <div className="space-y-2">
                <Label>Address</Label>
                <Input placeholder="Full address" />
              </div>
              <div className="space-y-2">
                <Label>Capacity</Label>
                <Input type="number" placeholder="0" />
              </div>
              <Button className="w-full" onClick={() => setShowCreate(false)}>Create Venue</Button>
            </div>
          </DialogContent>
        </Dialog>
      </div>

      <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
        {mockVenues.map((venue) => (
          <Card key={venue.id} className="hover:shadow-md transition-shadow">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <MapPin className="h-5 w-5 text-primary" />
                {venue.name}
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex justify-between text-sm text-muted-foreground">
                <span>{venue.city}</span>
                <span>Capacity: {venue.capacity.toLocaleString()}</span>
              </div>
              <div className="flex items-center gap-2 text-sm">
                <Layout className="h-4 w-4" />
                <span>{venue.layouts} layout(s)</span>
              </div>
              <div className="flex gap-2">
                <Button variant="outline" size="sm" className="flex-1" asChild>
                  <Link href={`/dashboard/venues/${venue.id}/layouts`}>Manage Layouts</Link>
                </Button>
                <Button size="sm" className="flex-1" asChild>
                  <Link href={`/dashboard/venues/${venue.id}/builder`}>Layout Builder</Link>
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
