'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Calendar, MapPin, Tag, Image, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { apiClient } from '@/lib/api-client';

const CATEGORIES = ['MUSIC', 'SPORTS', 'TECH', 'ARTS', 'FOOD', 'BUSINESS', 'EDUCATION', 'HEALTH', 'TRAVEL', 'OTHER'];

export default function CreateEventPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({
    title: '',
    description: '',
    category: 'MUSIC',
    coverImageUrl: '',
    startDate: '',
    endDate: '',
    totalSeats: 100,
    minPrice: 0,
    maxPrice: 0,
    currency: 'USD',
    tags: '',
  });

  const update = (field: string, value: any) =>
    setForm((prev) => ({ ...prev, [field]: value }));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const tags = form.tags ? form.tags.split(',').map((t) => t.trim()).filter(Boolean) : [];
      const payload = {
        title: form.title,
        description: form.description,
        category: form.category,
        coverImageUrl: form.coverImageUrl || null,
        startDate: new Date(form.startDate).toISOString(),
        endDate: new Date(form.endDate).toISOString(),
        totalSeats: form.totalSeats,
        availableSeats: form.totalSeats,
        minPrice: form.minPrice,
        maxPrice: form.maxPrice,
        currency: form.currency,
        tags,
      };

      const event = await apiClient.post('/events', payload);
      router.push(`/events/${(event as any).id}`);
    } catch (err: any) {
      setError(err?.message || 'Failed to create event');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container mx-auto max-w-2xl px-4 py-8">
      <Card>
        <CardHeader>
          <CardTitle>Create New Event</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            {error && (
              <div className="p-3 bg-destructive/10 text-destructive text-sm rounded-md">{error}</div>
            )}

            <div>
              <label className="text-sm font-medium">Title *</label>
              <Input
                value={form.title}
                onChange={(e) => update('title', e.target.value)}
                placeholder="Event title"
                required
              />
            </div>

            <div>
              <label className="text-sm font-medium">Description</label>
              <textarea
                className="w-full mt-1 p-2 rounded-md border bg-background text-sm min-h-[100px]"
                value={form.description}
                onChange={(e) => update('description', e.target.value)}
                placeholder="Describe your event..."
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-sm font-medium">Category *</label>
                <select
                  className="w-full mt-1 p-2 rounded-md border bg-background text-sm"
                  value={form.category}
                  onChange={(e) => update('category', e.target.value)}
                >
                  {CATEGORIES.map((c) => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="text-sm font-medium">Currency</label>
                <Input
                  value={form.currency}
                  onChange={(e) => update('currency', e.target.value)}
                  placeholder="USD"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-sm font-medium flex items-center gap-1">
                  <Calendar className="h-3.5 w-3.5" /> Start Date *
                </label>
                <Input
                  type="datetime-local"
                  value={form.startDate}
                  onChange={(e) => update('startDate', e.target.value)}
                  required
                />
              </div>
              <div>
                <label className="text-sm font-medium flex items-center gap-1">
                  <Calendar className="h-3.5 w-3.5" /> End Date *
                </label>
                <Input
                  type="datetime-local"
                  value={form.endDate}
                  onChange={(e) => update('endDate', e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="grid grid-cols-3 gap-4">
              <div>
                <label className="text-sm font-medium">Total Seats</label>
                <Input
                  type="number"
                  value={form.totalSeats}
                  onChange={(e) => update('totalSeats', parseInt(e.target.value) || 0)}
                  min={1}
                />
              </div>
              <div>
                <label className="text-sm font-medium">Min Price</label>
                <Input
                  type="number"
                  step="0.01"
                  value={form.minPrice}
                  onChange={(e) => update('minPrice', parseFloat(e.target.value) || 0)}
                  min={0}
                />
              </div>
              <div>
                <label className="text-sm font-medium">Max Price</label>
                <Input
                  type="number"
                  step="0.01"
                  value={form.maxPrice}
                  onChange={(e) => update('maxPrice', parseFloat(e.target.value) || 0)}
                  min={0}
                />
              </div>
            </div>

            <div>
              <label className="text-sm font-medium flex items-center gap-1">
                <Image className="h-3.5 w-3.5" /> Cover Image URL
              </label>
              <Input
                value={form.coverImageUrl}
                onChange={(e) => update('coverImageUrl', e.target.value)}
                placeholder="https://..."
              />
            </div>

            <div>
              <label className="text-sm font-medium flex items-center gap-1">
                <Tag className="h-3.5 w-3.5" /> Tags
              </label>
              <Input
                value={form.tags}
                onChange={(e) => update('tags', e.target.value)}
                placeholder="music, live, concert (comma-separated)"
              />
            </div>

            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
              Create Event
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
