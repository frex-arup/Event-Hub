'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';

export function useProfile(userId: string) {
  return useQuery({
    queryKey: ['profile', userId],
    queryFn: () => apiClient.get(`/profiles/${userId}`),
    enabled: !!userId,
  });
}

export function useUpdateProfile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { name?: string; bio?: string; avatarUrl?: string }) =>
      apiClient.put('/profiles', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['profile'] });
    },
  });
}

export function useUpdateInterests() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (interests: string[]) =>
      apiClient.put('/profiles/interests', { interests }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['profile'] });
    },
  });
}

export function useFollow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (targetUserId: string) =>
      apiClient.post(`/profiles/follow/${targetUserId}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['profile'] });
      queryClient.invalidateQueries({ queryKey: ['follow'] });
    },
  });
}

export function useUnfollow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (targetUserId: string) =>
      apiClient.delete(`/profiles/follow/${targetUserId}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['profile'] });
      queryClient.invalidateQueries({ queryKey: ['follow'] });
    },
  });
}

export function useFollowers(userId: string, page = 0, size = 20) {
  return useQuery({
    queryKey: ['follow', 'followers', userId, page, size],
    queryFn: () => apiClient.get(`/profiles/${userId}/followers?page=${page}&size=${size}`),
    enabled: !!userId,
  });
}

export function useFollowing(userId: string, page = 0, size = 20) {
  return useQuery({
    queryKey: ['follow', 'following', userId, page, size],
    queryFn: () => apiClient.get(`/profiles/${userId}/following?page=${page}&size=${size}`),
    enabled: !!userId,
  });
}

export function useFollowCounts(userId: string) {
  return useQuery({
    queryKey: ['follow', 'counts', userId],
    queryFn: () => apiClient.get<{ followers: number; following: number }>(`/profiles/${userId}/follow-counts`),
    enabled: !!userId,
  });
}

export function useIsFollowing(userId: string, targetUserId: string) {
  return useQuery({
    queryKey: ['follow', 'is-following', userId, targetUserId],
    queryFn: () => apiClient.get<{ following: boolean }>(`/profiles/${userId}/is-following/${targetUserId}`),
    enabled: !!userId && !!targetUserId,
  });
}
