'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { useAuthStore } from '@/store/auth-store';
import type { AuthResponse, LoginRequest, SignupRequest, User } from '@/types';

export function useCurrentUser() {
  const { setUser } = useAuthStore();

  return useQuery({
    queryKey: ['currentUser'],
    queryFn: async () => {
      const user = await apiClient.get<User>('/auth/me');
      setUser(user);
      return user;
    },
    retry: false,
    enabled: typeof window !== 'undefined' && !!localStorage.getItem('access_token'),
  });
}

export function useLogin() {
  const { setUser } = useAuthStore();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: LoginRequest) => apiClient.post<AuthResponse>('/auth/login', data),
    onSuccess: (response) => {
      localStorage.setItem('access_token', response.tokens.accessToken);
      localStorage.setItem('refresh_token', response.tokens.refreshToken);
      setUser(response.user);
      queryClient.invalidateQueries({ queryKey: ['currentUser'] });
    },
  });
}

export function useSignup() {
  const { setUser } = useAuthStore();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: SignupRequest) => apiClient.post<AuthResponse>('/auth/signup', data),
    onSuccess: (response) => {
      localStorage.setItem('access_token', response.tokens.accessToken);
      localStorage.setItem('refresh_token', response.tokens.refreshToken);
      setUser(response.user);
      queryClient.invalidateQueries({ queryKey: ['currentUser'] });
    },
  });
}

export function useLogout() {
  const { logout } = useAuthStore();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => apiClient.post('/auth/logout'),
    onSettled: () => {
      logout();
      queryClient.clear();
    },
  });
}
