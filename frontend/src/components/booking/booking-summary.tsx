'use client';

import { useEffect, useState } from 'react';
import { useBookingStore } from '@/store/booking-store';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { Badge } from '@/components/ui/badge';
import { Clock, Loader2, Trash2, Ticket, AlertTriangle } from 'lucide-react';

interface BookingSummaryProps {
  onLockSeats: () => void;
  onProceedToPayment: () => void;
  isLocking: boolean;
  isBooking: boolean;
  currency: string;
}

export function BookingSummary({
  onLockSeats,
  onProceedToPayment,
  isLocking,
  isBooking,
  currency,
}: BookingSummaryProps) {
  const { selectedSeats, removeSeat, lockId, lockExpiresAt, totalAmount, bookingId } =
    useBookingStore();
  const [timeLeft, setTimeLeft] = useState<number | null>(null);

  // Lock countdown timer
  useEffect(() => {
    if (!lockExpiresAt) {
      setTimeLeft(null);
      return;
    }

    const interval = setInterval(() => {
      const remaining = Math.max(0, new Date(lockExpiresAt).getTime() - Date.now());
      setTimeLeft(Math.floor(remaining / 1000));

      if (remaining <= 0) {
        clearInterval(interval);
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [lockExpiresAt]);

  const formatTime = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <Card className="sticky top-24">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Ticket className="h-5 w-5 text-primary" />
          Booking Summary
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Lock Timer */}
        {lockId && timeLeft !== null && (
          <div
            className={`flex items-center gap-2 p-3 rounded-lg text-sm font-medium ${
              timeLeft < 60
                ? 'bg-destructive/10 text-destructive'
                : 'bg-amber-500/10 text-amber-600 dark:text-amber-400'
            }`}
          >
            {timeLeft < 60 ? (
              <AlertTriangle className="h-4 w-4" />
            ) : (
              <Clock className="h-4 w-4" />
            )}
            <span>
              {timeLeft > 0
                ? `Seats locked for ${formatTime(timeLeft)}`
                : 'Lock expired — please re-select seats'}
            </span>
          </div>
        )}

        {/* Selected Seats */}
        {selectedSeats.length > 0 ? (
          <div className="space-y-2">
            <p className="text-sm font-medium text-muted-foreground">
              {selectedSeats.length} seat{selectedSeats.length > 1 ? 's' : ''} selected
            </p>
            <div className="max-h-48 overflow-y-auto space-y-1.5">
              {selectedSeats.map((seat) => (
                <div
                  key={seat.id}
                  className="flex items-center justify-between p-2 rounded-lg bg-muted/50 text-sm"
                >
                  <div>
                    <span className="font-medium">
                      Row {seat.row}, Seat {seat.number}
                    </span>
                    <Badge variant="outline" className="ml-2 text-xs">
                      {seat.sectionId}
                    </Badge>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="font-medium">
                      {seat.currency} {seat.price}
                    </span>
                    {!lockId && (
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-6 w-6"
                        onClick={() => removeSeat(seat.id)}
                      >
                        <Trash2 className="h-3 w-3" />
                      </Button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        ) : (
          <div className="text-center py-6 text-muted-foreground">
            <Ticket className="h-8 w-8 mx-auto mb-2 opacity-50" />
            <p className="text-sm">Click on available seats to select them</p>
          </div>
        )}

        {selectedSeats.length > 0 && (
          <>
            <Separator />

            {/* Total */}
            <div className="flex items-center justify-between font-semibold text-lg">
              <span>Total</span>
              <span className="text-primary">
                {currency} {totalAmount.toFixed(2)}
              </span>
            </div>

            {/* Action Buttons */}
            {!lockId ? (
              <Button
                className="w-full"
                size="lg"
                onClick={onLockSeats}
                disabled={isLocking || selectedSeats.length === 0}
              >
                {isLocking ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Locking seats...
                  </>
                ) : (
                  `Lock ${selectedSeats.length} Seat${selectedSeats.length > 1 ? 's' : ''}`
                )}
              </Button>
            ) : !bookingId ? (
              <Button
                className="w-full"
                size="lg"
                onClick={onProceedToPayment}
                disabled={isBooking || (timeLeft !== null && timeLeft <= 0)}
              >
                {isBooking ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Processing...
                  </>
                ) : (
                  'Proceed to Payment'
                )}
              </Button>
            ) : (
              <Button className="w-full" size="lg" variant="secondary" disabled>
                Booking in progress...
              </Button>
            )}

            <p className="text-xs text-center text-muted-foreground">
              Seats will be held for 10 minutes during checkout
            </p>
          </>
        )}
      </CardContent>
    </Card>
  );
}
