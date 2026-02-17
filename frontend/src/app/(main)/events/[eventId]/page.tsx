'use client';

import { use, useEffect } from 'react';
import { format } from 'date-fns';
import { useEvent } from '@/hooks/use-events';
import { useSeatAvailability } from '@/hooks/use-booking';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { Skeleton } from '@/components/ui/skeleton';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import {
  Calendar,
  Clock,
  MapPin,
  Share2,
  Heart,
  Users,
  Ticket,
  Tag,
} from 'lucide-react';
import Link from 'next/link';
import { wsManager } from '@/lib/websocket';

export default function EventDetailPage({ params }: { params: Promise<{ eventId: string }> }) {
  const { eventId } = use(params);
  const { data: event, isLoading } = useEvent(eventId);
  const { data: availability } = useSeatAvailability(eventId);

  useEffect(() => {
    if (eventId) {
      wsManager.connect(eventId);
      return () => wsManager.disconnect();
    }
  }, [eventId]);

  if (isLoading) {
    return (
      <div className="container mx-auto max-w-7xl px-4 py-8">
        <Skeleton className="h-[400px] w-full rounded-xl mb-8" />
        <div className="grid lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 space-y-4">
            <Skeleton className="h-10 w-3/4" />
            <Skeleton className="h-6 w-1/2" />
            <Skeleton className="h-32 w-full" />
          </div>
          <Skeleton className="h-64 w-full" />
        </div>
      </div>
    );
  }

  if (!event) {
    return (
      <div className="container mx-auto max-w-7xl px-4 py-20 text-center">
        <h1 className="text-2xl font-bold mb-2">Event not found</h1>
        <p className="text-muted-foreground mb-4">The event you&apos;re looking for doesn&apos;t exist.</p>
        <Button asChild>
          <Link href="/events">Browse Events</Link>
        </Button>
      </div>
    );
  }

  return (
    <div className="container mx-auto max-w-7xl px-4 py-8">
      {/* Cover Image */}
      <div className="relative aspect-[21/9] rounded-xl overflow-hidden bg-muted mb-8">
        {event.coverImageUrl ? (
          <img
            src={event.coverImageUrl}
            alt={event.title}
            className="object-cover w-full h-full"
          />
        ) : (
          <div className="w-full h-full bg-gradient-to-br from-primary/20 to-primary/5 flex items-center justify-center">
            <Calendar className="h-20 w-20 text-primary/30" />
          </div>
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent" />
        <div className="absolute bottom-6 left-6 right-6 text-white">
          <Badge variant="secondary" className="mb-3">
            {event.category}
          </Badge>
          <h1 className="text-3xl md:text-4xl font-bold mb-2">{event.title}</h1>
          <div className="flex items-center gap-4 text-white/80 text-sm">
            <span className="flex items-center gap-1">
              <Calendar className="h-4 w-4" />
              {format(new Date(event.startDate), 'EEE, MMM d, yyyy')}
            </span>
            <span className="flex items-center gap-1">
              <MapPin className="h-4 w-4" />
              {event.venue.name}, {event.venue.city}
            </span>
          </div>
        </div>
      </div>

      {/* Content Grid */}
      <div className="grid lg:grid-cols-3 gap-8">
        {/* Main Content */}
        <div className="lg:col-span-2">
          <Tabs defaultValue="about" className="w-full">
            <TabsList className="mb-6">
              <TabsTrigger value="about">About</TabsTrigger>
              <TabsTrigger value="venue">Venue</TabsTrigger>
              <TabsTrigger value="discussion">Discussion</TabsTrigger>
            </TabsList>

            <TabsContent value="about" className="space-y-6">
              <div>
                <h2 className="text-xl font-semibold mb-3">About this event</h2>
                <div className="prose prose-sm dark:prose-invert max-w-none">
                  <p className="text-muted-foreground whitespace-pre-wrap">
                    {event.description}
                  </p>
                </div>
              </div>

              <Separator />

              <div>
                <h3 className="font-semibold mb-3">Event Details</h3>
                <div className="grid sm:grid-cols-2 gap-4">
                  <div className="flex items-start gap-3">
                    <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center shrink-0">
                      <Calendar className="h-5 w-5 text-primary" />
                    </div>
                    <div>
                      <p className="font-medium text-sm">Date & Time</p>
                      <p className="text-sm text-muted-foreground">
                        {format(new Date(event.startDate), 'EEEE, MMMM d, yyyy')}
                      </p>
                      <p className="text-sm text-muted-foreground">
                        {format(new Date(event.startDate), 'h:mm a')} –{' '}
                        {format(new Date(event.endDate), 'h:mm a')}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center shrink-0">
                      <MapPin className="h-5 w-5 text-primary" />
                    </div>
                    <div>
                      <p className="font-medium text-sm">Location</p>
                      <p className="text-sm text-muted-foreground">{event.venue.name}</p>
                      <p className="text-sm text-muted-foreground">{event.venue.address}</p>
                    </div>
                  </div>
                </div>
              </div>

              {event.tags.length > 0 && (
                <>
                  <Separator />
                  <div>
                    <h3 className="font-semibold mb-3">Tags</h3>
                    <div className="flex flex-wrap gap-2">
                      {event.tags.map((tag) => (
                        <Badge key={tag} variant="outline">
                          {tag}
                        </Badge>
                      ))}
                    </div>
                  </div>
                </>
              )}
            </TabsContent>

            <TabsContent value="venue">
              <Card>
                <CardContent className="p-6">
                  <h3 className="font-semibold text-lg mb-2">{event.venue.name}</h3>
                  <p className="text-muted-foreground mb-4">
                    {event.venue.address}, {event.venue.city}, {event.venue.country}
                  </p>
                  <div className="aspect-video rounded-lg bg-muted flex items-center justify-center">
                    <MapPin className="h-8 w-8 text-muted-foreground/50" />
                    <span className="ml-2 text-muted-foreground">Map view coming soon</span>
                  </div>
                </CardContent>
              </Card>
            </TabsContent>

            <TabsContent value="discussion">
              <Card>
                <CardContent className="p-6 text-center py-12">
                  <Users className="h-12 w-12 text-muted-foreground/50 mx-auto mb-3" />
                  <h3 className="font-semibold mb-1">Event Discussion</h3>
                  <p className="text-sm text-muted-foreground">
                    Join the conversation! Sign in to participate.
                  </p>
                </CardContent>
              </Card>
            </TabsContent>
          </Tabs>
        </div>

        {/* Sidebar — Booking Card */}
        <div className="lg:col-span-1">
          <Card className="sticky top-24">
            <CardHeader>
              <CardTitle className="flex items-center justify-between">
                <span className="flex items-center gap-2">
                  <Ticket className="h-5 w-5 text-primary" />
                  Tickets
                </span>
                <div className="flex gap-2">
                  <Button variant="ghost" size="icon" className="h-8 w-8">
                    <Heart className="h-4 w-4" />
                  </Button>
                  <Button variant="ghost" size="icon" className="h-8 w-8">
                    <Share2 className="h-4 w-4" />
                  </Button>
                </div>
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {/* Price Range */}
              <div className="flex items-center gap-2">
                <Tag className="h-4 w-4 text-primary" />
                <span className="font-semibold text-lg">
                  {event.minPrice === 0
                    ? 'Free'
                    : event.minPrice === event.maxPrice
                    ? `${event.currency} ${event.minPrice}`
                    : `${event.currency} ${event.minPrice} – ${event.maxPrice}`}
                </span>
              </div>

              {/* Availability */}
              <div className="flex items-center justify-between text-sm">
                <span className="text-muted-foreground">Available</span>
                <span className="font-medium">
                  {event.availableSeats} / {event.totalSeats} seats
                </span>
              </div>

              {availability && (
                <div className="space-y-2">
                  {availability.sections.map((section) => (
                    <div
                      key={section.sectionId}
                      className="flex items-center justify-between text-sm px-3 py-2 rounded-lg bg-muted/50"
                    >
                      <span>{section.sectionId}</span>
                      <span className="text-muted-foreground">
                        {section.available}/{section.total} · {event.currency} {section.price}
                      </span>
                    </div>
                  ))}
                </div>
              )}

              <Separator />

              {/* Time info */}
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <Clock className="h-4 w-4" />
                <span>
                  {format(new Date(event.startDate), 'h:mm a')} –{' '}
                  {format(new Date(event.endDate), 'h:mm a')}
                </span>
              </div>

              {/* Book Now Button */}
              <Button asChild size="lg" className="w-full">
                <Link href={`/events/${eventId}/book`}>
                  {event.availableSeats > 0 ? 'Select Seats & Book' : 'Join Waitlist'}
                </Link>
              </Button>

              <p className="text-xs text-center text-muted-foreground">
                Seats are locked for 10 minutes during checkout
              </p>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
