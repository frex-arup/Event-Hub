'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { useBookingStore } from '@/store/booking-store';
import type {
  Booking,
  CreateBookingRequest,
  SeatLockRequest,
  SeatLockResponse,
  SeatAvailability,
  VenueLayout,
  InitiatePaymentRequest,
  PaymentSession,
  PaginatedResponse,
} from '@/types';

export function useSeatAvailability(eventId: string) {
  return useQuery({
    queryKey: ['seatAvailability', eventId],
    queryFn: () => apiClient.get<SeatAvailability>(`/seats/availability/${eventId}`),
    enabled: !!eventId,
    refetchInterval: 5000,
  });
}

export function useVenueLayout(eventId: string) {
  return useQuery({
    queryKey: ['venueLayout', eventId],
    queryFn: () => apiClient.get<VenueLayout>(`/venues/layout/${eventId}`),
    enabled: !!eventId,
  });
}

export function useLockSeats() {
  const { setLock } = useBookingStore();

  return useMutation({
    mutationFn: (data: SeatLockRequest) => apiClient.post<SeatLockResponse>('/seats/lock', data),
    onSuccess: (response) => {
      setLock(response.lockId, response.expiresAt);
    },
  });
}

export function useReleaseSeats() {
  const { clearLock, clearSeats } = useBookingStore();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (lockId: string) => apiClient.post(`/seats/release/${lockId}`),
    onSuccess: () => {
      clearLock();
      clearSeats();
      queryClient.invalidateQueries({ queryKey: ['seatAvailability'] });
    },
  });
}

export function useCreateBooking() {
  const { setBooking } = useBookingStore();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateBookingRequest) => apiClient.post<Booking>('/bookings', data),
    onSuccess: (booking) => {
      setBooking(booking.id, booking.status);
      queryClient.invalidateQueries({ queryKey: ['seatAvailability'] });
    },
  });
}

export function useInitiatePayment() {
  return useMutation({
    mutationFn: (data: InitiatePaymentRequest) =>
      apiClient.post<PaymentSession>('/payments/initiate', data),
  });
}

export function useBooking(bookingId: string) {
  return useQuery({
    queryKey: ['booking', bookingId],
    queryFn: () => apiClient.get<Booking>(`/bookings/${bookingId}`),
    enabled: !!bookingId,
  });
}

export function useMyBookings(params?: { page?: number; size?: number }) {
  return useQuery({
    queryKey: ['myBookings', params],
    queryFn: () =>
      apiClient.get<PaginatedResponse<Booking>>('/bookings/me', {
        page: params?.page ?? 0,
        size: params?.size ?? 10,
      }),
  });
}

export function useCancelBooking() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (bookingId: string) => apiClient.post(`/bookings/${bookingId}/cancel`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myBookings'] });
    },
  });
}
