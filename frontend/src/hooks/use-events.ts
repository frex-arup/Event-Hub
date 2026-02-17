'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import type { Event, EventSummary, CreateEventRequest, PaginatedResponse } from '@/types';

export function useEvents(params?: { page?: number; size?: number; category?: string; search?: string }) {
  return useQuery({
    queryKey: ['events', params],
    queryFn: () =>
      apiClient.get<PaginatedResponse<EventSummary>>('/events', {
        page: params?.page ?? 0,
        size: params?.size ?? 12,
        category: params?.category,
        search: params?.search,
      }),
  });
}

export function useEvent(eventId: string) {
  return useQuery({
    queryKey: ['event', eventId],
    queryFn: () => apiClient.get<Event>(`/events/${eventId}`),
    enabled: !!eventId,
  });
}

export function useTrendingEvents() {
  return useQuery({
    queryKey: ['events', 'trending'],
    queryFn: () => apiClient.get<EventSummary[]>('/events/trending'),
  });
}

export function useCreateEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateEventRequest) => apiClient.post<Event>('/events', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
    },
  });
}

export function useOrganizerEvents(organizerId: string) {
  return useQuery({
    queryKey: ['events', 'organizer', organizerId],
    queryFn: () => apiClient.get<PaginatedResponse<EventSummary>>(`/events/organizer/${organizerId}`),
    enabled: !!organizerId,
  });
}
