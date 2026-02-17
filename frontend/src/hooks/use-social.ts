'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';

// ─── Posts ───

export function useFeed(page = 0, size = 20) {
  return useQuery({
    queryKey: ['posts', 'feed', page, size],
    queryFn: () => apiClient.get(`/posts?page=${page}&size=${size}`),
  });
}

export function useTrendingPosts(page = 0, size = 20) {
  return useQuery({
    queryKey: ['posts', 'trending', page, size],
    queryFn: () => apiClient.get(`/posts/trending?page=${page}&size=${size}`),
  });
}

export function useUserPosts(authorId: string, page = 0, size = 20) {
  return useQuery({
    queryKey: ['posts', 'user', authorId, page, size],
    queryFn: () => apiClient.get(`/posts/user/${authorId}?page=${page}&size=${size}`),
    enabled: !!authorId,
  });
}

export function useCreatePost() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { content: string; imageUrl?: string; eventId?: string }) =>
      apiClient.post('/posts', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['posts'] });
    },
  });
}

export function useDeletePost() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (postId: string) => apiClient.delete(`/posts/${postId}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['posts'] });
    },
  });
}

export function useToggleLike() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (postId: string) => apiClient.post(`/posts/${postId}/like`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['posts'] });
    },
  });
}

// ─── Comments ───

export function useComments(postId: string, page = 0, size = 20) {
  return useQuery({
    queryKey: ['comments', postId, page, size],
    queryFn: () => apiClient.get(`/posts/${postId}/comments?page=${page}&size=${size}`),
    enabled: !!postId,
  });
}

export function useAddComment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ postId, content }: { postId: string; content: string }) =>
      apiClient.post(`/posts/${postId}/comments`, { content }),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['comments', variables.postId] });
      queryClient.invalidateQueries({ queryKey: ['posts'] });
    },
  });
}

// ─── Direct Messages ───

export function useConversations() {
  return useQuery({
    queryKey: ['conversations'],
    queryFn: () => apiClient.get<string[]>('/messages/conversations'),
  });
}

export function useConversation(partnerId: string, page = 0, size = 50) {
  return useQuery({
    queryKey: ['conversation', partnerId, page, size],
    queryFn: () => apiClient.get(`/messages/conversation/${partnerId}?page=${page}&size=${size}`),
    enabled: !!partnerId,
    refetchInterval: 5000,
  });
}

export function useSendMessage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { receiverId: string; content: string }) =>
      apiClient.post('/messages', data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['conversation', variables.receiverId] });
      queryClient.invalidateQueries({ queryKey: ['conversations'] });
    },
  });
}

export function useUnreadMessageCount() {
  return useQuery({
    queryKey: ['messages', 'unread-count'],
    queryFn: () => apiClient.get<{ count: number }>('/messages/unread/count'),
    refetchInterval: 15000,
  });
}

// ─── Event Discussions ───

export function useEventDiscussions(eventId: string, page = 0, size = 50) {
  return useQuery({
    queryKey: ['discussions', eventId, page, size],
    queryFn: () => apiClient.get(`/messages/discussions/${eventId}?page=${page}&size=${size}`),
    enabled: !!eventId,
  });
}

export function useAddDiscussionMessage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ eventId, content }: { eventId: string; content: string }) =>
      apiClient.post(`/messages/discussions/${eventId}`, { content }),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['discussions', variables.eventId] });
    },
  });
}
