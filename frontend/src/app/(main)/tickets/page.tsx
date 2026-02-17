'use client';

import { useMyBookings } from '@/hooks/use-booking';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Calendar, MapPin, QrCode, Ticket, Download } from 'lucide-react';
import { format } from 'date-fns';
import type { BookingStatus } from '@/types';

const statusColors: Record<BookingStatus, string> = {
  CONFIRMED: 'bg-green-500/10 text-green-600 border-green-200',
  PENDING: 'bg-amber-500/10 text-amber-600 border-amber-200',
  CANCELLED: 'bg-red-500/10 text-red-600 border-red-200',
  EXPIRED: 'bg-gray-500/10 text-gray-600 border-gray-200',
  REFUNDED: 'bg-blue-500/10 text-blue-600 border-blue-200',
};

export default function TicketsPage() {
  const { data, isLoading } = useMyBookings();

  return (
    <div className="container mx-auto max-w-4xl px-4 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold mb-2">My Tickets</h1>
        <p className="text-muted-foreground">Your event bookings and tickets</p>
      </div>

      <Tabs defaultValue="upcoming">
        <TabsList className="mb-6">
          <TabsTrigger value="upcoming">Upcoming</TabsTrigger>
          <TabsTrigger value="past">Past</TabsTrigger>
          <TabsTrigger value="cancelled">Cancelled</TabsTrigger>
        </TabsList>

        <TabsContent value="upcoming" className="space-y-4">
          {isLoading ? (
            Array.from({ length: 3 }).map((_, i) => (
              <Skeleton key={i} className="h-40 w-full rounded-lg" />
            ))
          ) : data?.content && data.content.length > 0 ? (
            data.content
              .filter((b) => b.status === 'CONFIRMED' || b.status === 'PENDING')
              .map((booking) => (
                <Card key={booking.id} className="overflow-hidden">
                  <CardContent className="p-0">
                    <div className="flex flex-col sm:flex-row">
                      <div className="flex-1 p-6 space-y-3">
                        <div className="flex items-start justify-between">
                          <div>
                            <h3 className="font-semibold text-lg">Event #{booking.eventId.slice(0, 8)}</h3>
                            <Badge className={statusColors[booking.status]} variant="outline">
                              {booking.status}
                            </Badge>
                          </div>
                          <span className="font-bold text-primary text-lg">
                            {booking.currency} {booking.totalAmount.toFixed(2)}
                          </span>
                        </div>

                        <div className="flex flex-wrap gap-4 text-sm text-muted-foreground">
                          <span className="flex items-center gap-1">
                            <Calendar className="h-3.5 w-3.5" />
                            {format(new Date(booking.createdAt), 'MMM d, yyyy')}
                          </span>
                          <span className="flex items-center gap-1">
                            <Ticket className="h-3.5 w-3.5" />
                            {booking.seats.length} seat{booking.seats.length > 1 ? 's' : ''}
                          </span>
                        </div>

                        <div className="flex flex-wrap gap-2">
                          {booking.seats.map((seat) => (
                            <Badge key={seat.seatId} variant="secondary" className="text-xs">
                              {seat.sectionName} · Row {seat.row} · Seat {seat.number}
                            </Badge>
                          ))}
                        </div>

                        <div className="flex gap-2 pt-2">
                          <Button size="sm" variant="outline">
                            <QrCode className="h-4 w-4 mr-1" />
                            Show QR
                          </Button>
                          <Button size="sm" variant="outline">
                            <Download className="h-4 w-4 mr-1" />
                            Download
                          </Button>
                        </div>
                      </div>

                      {booking.qrCode && (
                        <div className="w-32 sm:w-40 bg-muted flex items-center justify-center p-4 border-l">
                          <QrCode className="h-20 w-20 text-muted-foreground/50" />
                        </div>
                      )}
                    </div>
                  </CardContent>
                </Card>
              ))
          ) : (
            <div className="text-center py-16">
              <Ticket className="h-12 w-12 text-muted-foreground/50 mx-auto mb-3" />
              <h3 className="font-semibold mb-1">No upcoming tickets</h3>
              <p className="text-sm text-muted-foreground">
                Browse events and book your first ticket!
              </p>
            </div>
          )}
        </TabsContent>

        <TabsContent value="past">
          <div className="text-center py-16 text-muted-foreground">
            <Calendar className="h-12 w-12 mx-auto mb-3 opacity-50" />
            <p>No past events yet</p>
          </div>
        </TabsContent>

        <TabsContent value="cancelled">
          <div className="text-center py-16 text-muted-foreground">
            <Ticket className="h-12 w-12 mx-auto mb-3 opacity-50" />
            <p>No cancelled bookings</p>
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
}
