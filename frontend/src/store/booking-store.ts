import { create } from 'zustand';
import type { Seat, BookingStatus } from '@/types';

interface BookingState {
  selectedSeats: Seat[];
  lockId: string | null;
  lockExpiresAt: string | null;
  bookingId: string | null;
  bookingStatus: BookingStatus | null;
  idempotencyKey: string | null;
  totalAmount: number;
  currency: string;

  addSeat: (seat: Seat) => void;
  removeSeat: (seatId: string) => void;
  clearSeats: () => void;
  setLock: (lockId: string, expiresAt: string) => void;
  clearLock: () => void;
  setBooking: (bookingId: string, status: BookingStatus) => void;
  setIdempotencyKey: (key: string) => void;
  reset: () => void;
}

export const useBookingStore = create<BookingState>((set, get) => ({
  selectedSeats: [],
  lockId: null,
  lockExpiresAt: null,
  bookingId: null,
  bookingStatus: null,
  idempotencyKey: null,
  totalAmount: 0,
  currency: 'USD',

  addSeat: (seat) => {
    const current = get().selectedSeats;
    if (current.find((s) => s.id === seat.id)) return;
    const updated = [...current, seat];
    set({
      selectedSeats: updated,
      totalAmount: updated.reduce((sum, s) => sum + s.price, 0),
      currency: seat.currency,
    });
  },

  removeSeat: (seatId) => {
    const updated = get().selectedSeats.filter((s) => s.id !== seatId);
    set({
      selectedSeats: updated,
      totalAmount: updated.reduce((sum, s) => sum + s.price, 0),
    });
  },

  clearSeats: () => set({ selectedSeats: [], totalAmount: 0 }),

  setLock: (lockId, expiresAt) => set({ lockId, lockExpiresAt: expiresAt }),

  clearLock: () => set({ lockId: null, lockExpiresAt: null }),

  setBooking: (bookingId, status) => set({ bookingId, bookingStatus: status }),

  setIdempotencyKey: (key) => set({ idempotencyKey: key }),

  reset: () =>
    set({
      selectedSeats: [],
      lockId: null,
      lockExpiresAt: null,
      bookingId: null,
      bookingStatus: null,
      idempotencyKey: null,
      totalAmount: 0,
      currency: 'USD',
    }),
}));
