'use client';

import Link from 'next/link';
import { format } from 'date-fns';
import { Calendar, MapPin, Tag } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import type { EventSummary } from '@/types';

interface EventCardProps {
  event: EventSummary;
}

export function EventCard({ event }: EventCardProps) {
  return (
    <Link href={`/events/${event.id}`}>
      <Card className="group overflow-hidden hover:shadow-lg transition-all duration-300 h-full">
        <div className="relative aspect-[16/9] bg-muted overflow-hidden">
          {event.coverImageUrl ? (
            <img
              src={event.coverImageUrl}
              alt={event.title}
              className="object-cover w-full h-full group-hover:scale-105 transition-transform duration-300"
            />
          ) : (
            <div className="w-full h-full bg-gradient-to-br from-primary/20 to-primary/5 flex items-center justify-center">
              <Calendar className="h-12 w-12 text-primary/40" />
            </div>
          )}
          <Badge className="absolute top-3 left-3" variant="secondary">
            {event.category}
          </Badge>
          {event.availableSeats <= 10 && event.availableSeats > 0 && (
            <Badge className="absolute top-3 right-3 bg-destructive text-destructive-foreground">
              {event.availableSeats} left
            </Badge>
          )}
        </div>
        <CardContent className="p-4 space-y-3">
          <h3 className="font-semibold text-lg line-clamp-2 group-hover:text-primary transition-colors">
            {event.title}
          </h3>
          <div className="space-y-1.5 text-sm text-muted-foreground">
            <div className="flex items-center gap-2">
              <Calendar className="h-3.5 w-3.5 shrink-0" />
              <span>{format(new Date(event.startDate), 'EEE, MMM d · h:mm a')}</span>
            </div>
            <div className="flex items-center gap-2">
              <MapPin className="h-3.5 w-3.5 shrink-0" />
              <span className="truncate">
                {event.venue.name}, {event.venue.city}
              </span>
            </div>
          </div>
          <div className="flex items-center justify-between pt-2 border-t">
            <div className="flex items-center gap-1">
              <Tag className="h-3.5 w-3.5 text-primary" />
              <span className="font-semibold text-primary">
                {event.minPrice === 0 ? 'Free' : `From ${event.currency} ${event.minPrice}`}
              </span>
            </div>
            <div className="flex items-center gap-2">
              {event.organizer.avatarUrl && (
                <img
                  src={event.organizer.avatarUrl}
                  alt={event.organizer.name}
                  className="h-5 w-5 rounded-full"
                />
              )}
              <span className="text-xs text-muted-foreground">{event.organizer.name}</span>
            </div>
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}

export function EventCardSkeleton() {
  return (
    <Card className="overflow-hidden h-full">
      <Skeleton className="aspect-[16/9] w-full" />
      <CardContent className="p-4 space-y-3">
        <Skeleton className="h-6 w-3/4" />
        <div className="space-y-2">
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-2/3" />
        </div>
        <div className="flex justify-between pt-2">
          <Skeleton className="h-4 w-20" />
          <Skeleton className="h-4 w-16" />
        </div>
      </CardContent>
    </Card>
  );
}
