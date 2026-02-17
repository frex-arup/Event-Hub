'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';

// ─── Budgets ───

export function useOrganizerBudgets(page = 0, size = 10) {
  return useQuery({
    queryKey: ['budgets', page, size],
    queryFn: () => apiClient.get(`/finance/budgets?page=${page}&size=${size}`),
  });
}

export function useEventBudgets(eventId: string) {
  return useQuery({
    queryKey: ['budgets', 'event', eventId],
    queryFn: () => apiClient.get(`/finance/budgets/event/${eventId}`),
    enabled: !!eventId,
  });
}

export function useBudget(budgetId: string) {
  return useQuery({
    queryKey: ['budget', budgetId],
    queryFn: () => apiClient.get(`/finance/budgets/${budgetId}`),
    enabled: !!budgetId,
  });
}

export function useCreateBudget() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { eventId: string; name: string; totalBudget: number; currency?: string }) =>
      apiClient.post('/finance/budgets', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['budgets'] });
    },
  });
}

export function useBudgetItems(budgetId: string) {
  return useQuery({
    queryKey: ['budget-items', budgetId],
    queryFn: () => apiClient.get(`/finance/budgets/${budgetId}/items`),
    enabled: !!budgetId,
  });
}

export function useAddBudgetItem() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ budgetId, ...data }: { budgetId: string; category: string; description?: string; estimatedAmount: number; actualAmount?: number }) =>
      apiClient.post(`/finance/budgets/${budgetId}/items`, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['budget-items', variables.budgetId] });
      queryClient.invalidateQueries({ queryKey: ['budgets'] });
    },
  });
}

// ─── Revenue ───

export function useEventRevenue(eventId: string, page = 0, size = 20) {
  return useQuery({
    queryKey: ['revenue', 'event', eventId, page, size],
    queryFn: () => apiClient.get(`/finance/revenue/event/${eventId}?page=${page}&size=${size}`),
    enabled: !!eventId,
  });
}

export function useOrganizerRevenue(page = 0, size = 20) {
  return useQuery({
    queryKey: ['revenue', 'organizer', page, size],
    queryFn: () => apiClient.get(`/finance/revenue/organizer?page=${page}&size=${size}`),
  });
}

export function useEventAnalytics(eventId: string) {
  return useQuery({
    queryKey: ['analytics', 'event', eventId],
    queryFn: () => apiClient.get(`/finance/analytics/event/${eventId}`),
    enabled: !!eventId,
  });
}

export function useOrganizerAnalytics() {
  return useQuery({
    queryKey: ['analytics', 'organizer'],
    queryFn: () => apiClient.get('/finance/analytics/organizer'),
  });
}

// ─── Settlements ───

export function useSettlements(page = 0, size = 10) {
  return useQuery({
    queryKey: ['settlements', page, size],
    queryFn: () => apiClient.get(`/finance/settlements?page=${page}&size=${size}`),
  });
}
