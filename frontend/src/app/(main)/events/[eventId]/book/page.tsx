'use client';

import { use, useEffect, useMemo, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { useEvent } from '@/hooks/use-events';
import { useVenueLayout, useLockSeats, useCreateBooking, useInitiatePayment } from '@/hooks/use-booking';
import { useBookingStore } from '@/store/booking-store';
import { useAuthStore } from '@/store/auth-store';
import { SeatMap } from '@/components/booking/seat-map';
import { BookingSummary } from '@/components/booking/booking-summary';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { ArrowLeft } from 'lucide-react';
import { toast } from 'sonner';
import { wsManager } from '@/lib/websocket';
import type { Seat } from '@/types';
import Link from 'next/link';
import { v4 as uuidv4 } from 'uuid';

export default function BookingPage({ params }: { params: Promise<{ eventId: string }> }) {
  const { eventId } = use(params);
  const router = useRouter();
  const { user } = useAuthStore();
  const { reset, setIdempotencyKey, idempotencyKey } = useBookingStore();

  const { data: event, isLoading: eventLoading } = useEvent(eventId);
  const { data: layout, isLoading: layoutLoading } = useVenueLayout(eventId);

  const lockSeats = useLockSeats();
  const createBooking = useCreateBooking();
  const initiatePayment = useInitiatePayment();

  // Generate idempotency key on mount
  useEffect(() => {
    if (!idempotencyKey) {
      setIdempotencyKey(uuidv4());
    }
  }, [idempotencyKey, setIdempotencyKey]);

  // Connect WebSocket for real-time seat updates
  useEffect(() => {
    if (eventId) {
      wsManager.connect(eventId);
      return () => {
        wsManager.disconnect();
      };
    }
  }, [eventId]);

  // Reset booking state on unmount
  useEffect(() => {
    return () => {
      reset();
    };
  }, [reset]);

  // Build seat lookup map
  const seatMap = useMemo(() => {
    const map = new Map<string, Seat>();
    if (layout) {
      layout.sections.forEach((section) => {
        section.rows.forEach((row) => {
          row.seats.forEach((seat) => {
            map.set(seat.id, seat);
          });
        });
      });
    }
    return map;
  }, [layout]);

  const handleLockSeats = useCallback(() => {
    const { selectedSeats } = useBookingStore.getState();
    if (!user || selectedSeats.length === 0) return;

    lockSeats.mutate(
      {
        eventId,
        seatIds: selectedSeats.map((s) => s.id),
        userId: user.id,
      },
      {
        onSuccess: () => {
          toast.success('Seats locked! Complete your booking within 10 minutes.');
        },
        onError: () => {
          toast.error('Failed to lock seats. Some may already be taken.');
        },
      }
    );
  }, [eventId, user, lockSeats]);

  const handleProceedToPayment = useCallback(() => {
    const { selectedSeats, idempotencyKey: key } = useBookingStore.getState();
    if (!key || selectedSeats.length === 0) return;

    createBooking.mutate(
      {
        eventId,
        seatIds: selectedSeats.map((s) => s.id),
        idempotencyKey: key,
      },
      {
        onSuccess: (booking) => {
          // Initiate payment
          initiatePayment.mutate(
            {
              bookingId: booking.id,
              gateway: 'STRIPE',
              returnUrl: `${window.location.origin}/bookings/${booking.id}/confirmation`,
            },
            {
              onSuccess: (session) => {
                if (session.redirectUrl) {
                  window.location.href = session.redirectUrl;
                } else {
                  router.push(`/bookings/${booking.id}/confirmation`);
                }
              },
              onError: () => {
                toast.error('Payment initiation failed. Please try again.');
              },
            }
          );
        },
        onError: () => {
          toast.error('Booking failed. Please try again.');
        },
      }
    );
  }, [eventId, createBooking, initiatePayment, router]);

  if (eventLoading || layoutLoading) {
    return (
      <div className="container mx-auto max-w-7xl px-4 py-8">
        <Skeleton className="h-8 w-48 mb-6" />
        <div className="grid lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2">
            <Skeleton className="h-[500px] w-full rounded-lg" />
          </div>
          <Skeleton className="h-96 w-full" />
        </div>
      </div>
    );
  }

  if (!event || !layout) {
    return (
      <div className="container mx-auto max-w-7xl px-4 py-20 text-center">
        <h1 className="text-2xl font-bold mb-2">Seat map not available</h1>
        <p className="text-muted-foreground mb-4">
          The seating layout for this event hasn&apos;t been configured yet.
        </p>
        <Button asChild>
          <Link href={`/events/${eventId}`}>Back to Event</Link>
        </Button>
      </div>
    );
  }

  return (
    <div className="container mx-auto max-w-7xl px-4 py-8">
      {/* Header */}
      <div className="flex items-center gap-4 mb-6">
        <Button variant="ghost" size="icon" asChild>
          <Link href={`/events/${eventId}`}>
            <ArrowLeft className="h-5 w-5" />
          </Link>
        </Button>
        <div>
          <h1 className="text-2xl font-bold">{event.title}</h1>
          <p className="text-sm text-muted-foreground">Select your seats</p>
        </div>
      </div>

      {/* Seat Map Legend */}
      <div className="flex flex-wrap gap-4 mb-4 text-sm">
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 rounded-full bg-[#22c55e]" />
          <span>Available</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 rounded-full bg-[#3b82f6]" />
          <span>Selected</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 rounded-full bg-[#f59e0b]" />
          <span>Locked</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 rounded-full bg-[#6b7280] opacity-50" />
          <span>Booked</span>
        </div>
      </div>

      {/* Main Content */}
      <div className="grid lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 overflow-hidden rounded-lg border">
          <SeatMap layout={layout} seats={seatMap} userId={user?.id} />
        </div>
        <div>
          <BookingSummary
            onLockSeats={handleLockSeats}
            onProceedToPayment={handleProceedToPayment}
            isLocking={lockSeats.isPending}
            isBooking={createBooking.isPending || initiatePayment.isPending}
            currency={event.currency}
          />
        </div>
      </div>
    </div>
  );
}
