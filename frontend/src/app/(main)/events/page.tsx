'use client';

import { Suspense, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import { useEvents } from '@/hooks/use-events';
import { EventCard, EventCardSkeleton } from '@/components/events/event-card';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Search, SlidersHorizontal } from 'lucide-react';

const categories = [
  { value: 'ALL', label: 'All Categories' },
  { value: 'CONCERT', label: 'Concerts' },
  { value: 'CONFERENCE', label: 'Conferences' },
  { value: 'SPORTS', label: 'Sports' },
  { value: 'THEATER', label: 'Theater' },
  { value: 'MOVIE', label: 'Movies' },
  { value: 'MEETUP', label: 'Meetups' },
  { value: 'WORKSHOP', label: 'Workshops' },
];

export default function EventsPage() {
  return (
    <Suspense fallback={
      <div className="container mx-auto max-w-7xl px-4 py-8">
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {Array.from({ length: 8 }).map((_, i) => (
            <EventCardSkeleton key={i} />
          ))}
        </div>
      </div>
    }>
      <EventsContent />
    </Suspense>
  );
}

function EventsContent() {
  const searchParams = useSearchParams();
  const initialCategory = searchParams.get('category') || 'ALL';

  const [search, setSearch] = useState('');
  const [category, setCategory] = useState(initialCategory);
  const [page, setPage] = useState(0);

  const { data, isLoading } = useEvents({
    page,
    size: 12,
    category: category === 'ALL' ? undefined : category,
    search: search || undefined,
  });

  return (
    <div className="container mx-auto max-w-7xl px-4 py-8">
      {/* Page Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold mb-2">Discover Events</h1>
        <p className="text-muted-foreground">Find and book amazing events happening near you</p>
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3 mb-8">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search events, venues, organizers..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
            className="pl-10"
          />
        </div>
        <Select
          value={category}
          onValueChange={(value) => {
            setCategory(value);
            setPage(0);
          }}
        >
          <SelectTrigger className="w-full sm:w-48">
            <SlidersHorizontal className="mr-2 h-4 w-4" />
            <SelectValue placeholder="Category" />
          </SelectTrigger>
          <SelectContent>
            {categories.map((cat) => (
              <SelectItem key={cat.value} value={cat.value}>
                {cat.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* Event Grid */}
      {isLoading ? (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {Array.from({ length: 8 }).map((_, i) => (
            <EventCardSkeleton key={i} />
          ))}
        </div>
      ) : data?.content && data.content.length > 0 ? (
        <>
          <div className="grid sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {data.content.map((event) => (
              <EventCard key={event.id} event={event} />
            ))}
          </div>

          {/* Pagination */}
          {data.totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 mt-10">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
              >
                Previous
              </Button>
              <span className="text-sm text-muted-foreground px-4">
                Page {page + 1} of {data.totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => p + 1)}
                disabled={data.last}
              >
                Next
              </Button>
            </div>
          )}
        </>
      ) : (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <Search className="h-12 w-12 text-muted-foreground/50 mb-4" />
          <h3 className="text-lg font-semibold mb-1">No events found</h3>
          <p className="text-muted-foreground text-sm">
            Try adjusting your filters or search terms.
          </p>
        </div>
      )}
    </div>
  );
}
