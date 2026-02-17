'use client';

import { useState } from 'react';
import { Bell, Check, CheckCheck, Settings, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useNotifications, useUnreadNotifications, useUnreadCount, useMarkAsRead, useMarkAllAsRead } from '@/hooks/use-notifications';

export default function NotificationsPage() {
  const [tab, setTab] = useState('all');
  const { data: allNotifications, isLoading: loadingAll } = useNotifications();
  const { data: unreadNotifications, isLoading: loadingUnread } = useUnreadNotifications();
  const { data: unreadCountData } = useUnreadCount();
  const markAsRead = useMarkAsRead();
  const markAllAsRead = useMarkAllAsRead();

  const notifications = (tab === 'unread' ? unreadNotifications : allNotifications) as any;
  const isLoading = tab === 'unread' ? loadingUnread : loadingAll;
  const unreadCount = (unreadCountData as any)?.count ?? 0;

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'BOOKING_CONFIRMED': return '🎫';
      case 'BOOKING_FAILED': return '❌';
      case 'PAYMENT_REFUNDED': return '💰';
      case 'EVENT_REMINDER': return '📅';
      case 'NEW_FOLLOWER': return '👤';
      case 'NEW_MESSAGE': return '💬';
      case 'WELCOME': return '🎉';
      default: return '🔔';
    }
  };

  return (
    <div className="container mx-auto max-w-3xl px-4 py-8">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <Bell className="h-6 w-6" />
          <h1 className="text-2xl font-bold">Notifications</h1>
          {unreadCount > 0 && (
            <span className="bg-red-500 text-white text-xs font-bold px-2 py-0.5 rounded-full">
              {unreadCount}
            </span>
          )}
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => markAllAsRead.mutate()}
            disabled={unreadCount === 0}
          >
            <CheckCheck className="h-4 w-4 mr-1" />
            Mark all read
          </Button>
          <Button variant="ghost" size="icon">
            <Settings className="h-4 w-4" />
          </Button>
        </div>
      </div>

      <Tabs value={tab} onValueChange={setTab}>
        <TabsList className="mb-4">
          <TabsTrigger value="all">All</TabsTrigger>
          <TabsTrigger value="unread">
            Unread {unreadCount > 0 && `(${unreadCount})`}
          </TabsTrigger>
        </TabsList>

        <TabsContent value={tab}>
          {isLoading ? (
            <div className="flex justify-center py-12">
              <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
            </div>
          ) : (
            <div className="space-y-2">
              {notifications?.content?.length > 0 ? (
                notifications.content.map((n: any) => (
                  <Card
                    key={n.id}
                    className={`transition-colors ${!n.read ? 'border-primary/30 bg-primary/5' : ''}`}
                  >
                    <CardContent className="flex items-start gap-3 py-3 px-4">
                      <span className="text-xl mt-0.5">{getTypeIcon(n.type)}</span>
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-sm">{n.title}</p>
                        <p className="text-sm text-muted-foreground mt-0.5">{n.message}</p>
                        <p className="text-xs text-muted-foreground mt-1">
                          {new Date(n.createdAt).toLocaleString()}
                        </p>
                      </div>
                      {!n.read && (
                        <Button
                          variant="ghost"
                          size="icon"
                          className="shrink-0"
                          onClick={() => markAsRead.mutate(n.id)}
                        >
                          <Check className="h-4 w-4" />
                        </Button>
                      )}
                    </CardContent>
                  </Card>
                ))
              ) : (
                <div className="text-center py-12 text-muted-foreground">
                  <Bell className="h-12 w-12 mx-auto mb-3 opacity-30" />
                  <p>No notifications yet</p>
                </div>
              )}
            </div>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
}
