'use client';

import { useState } from 'react';
import { MessageSquare, Send, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { useConversations, useConversation, useSendMessage, useUnreadMessageCount } from '@/hooks/use-social';

export default function MessagesPage() {
  const [selectedPartner, setSelectedPartner] = useState<string | null>(null);
  const [message, setMessage] = useState('');
  const { data: partners, isLoading: loadingPartners } = useConversations();
  const { data: conversation, isLoading: loadingConvo } = useConversation(selectedPartner ?? '', 0, 50);
  const { data: unreadData } = useUnreadMessageCount();
  const sendMessage = useSendMessage();

  const partnerList = (partners as any) ?? [];
  const messages = (conversation as any)?.content ?? [];
  const unreadCount = (unreadData as any)?.count ?? 0;

  const handleSend = () => {
    if (!message.trim() || !selectedPartner) return;
    sendMessage.mutate(
      { receiverId: selectedPartner, content: message.trim() },
      { onSuccess: () => setMessage('') }
    );
  };

  return (
    <div className="container mx-auto max-w-5xl px-4 py-8">
      <div className="flex items-center gap-3 mb-6">
        <MessageSquare className="h-6 w-6" />
        <h1 className="text-2xl font-bold">Messages</h1>
        {unreadCount > 0 && (
          <span className="bg-red-500 text-white text-xs font-bold px-2 py-0.5 rounded-full">
            {unreadCount}
          </span>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 h-[600px]">
        {/* Conversation list */}
        <Card className="md:col-span-1 overflow-auto">
          <CardHeader className="py-3">
            <CardTitle className="text-sm">Conversations</CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            {loadingPartners ? (
              <div className="flex justify-center py-8">
                <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
              </div>
            ) : partnerList.length > 0 ? (
              partnerList.map((partnerId: string) => (
                <button
                  key={partnerId}
                  className={`w-full text-left px-4 py-3 border-b hover:bg-muted/50 transition-colors text-sm ${
                    selectedPartner === partnerId ? 'bg-muted' : ''
                  }`}
                  onClick={() => setSelectedPartner(partnerId)}
                >
                  <p className="font-medium truncate">{partnerId.substring(0, 8)}...</p>
                </button>
              ))
            ) : (
              <div className="text-center py-8 text-muted-foreground text-sm">
                No conversations yet
              </div>
            )}
          </CardContent>
        </Card>

        {/* Chat area */}
        <Card className="md:col-span-2 flex flex-col">
          {selectedPartner ? (
            <>
              <CardHeader className="py-3 border-b">
                <CardTitle className="text-sm">
                  Chat with {selectedPartner.substring(0, 8)}...
                </CardTitle>
              </CardHeader>
              <CardContent className="flex-1 overflow-auto p-4 space-y-3">
                {loadingConvo ? (
                  <div className="flex justify-center py-8">
                    <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
                  </div>
                ) : messages.length > 0 ? (
                  messages.map((msg: any) => (
                    <div
                      key={msg.id}
                      className={`flex ${msg.senderId === selectedPartner ? 'justify-start' : 'justify-end'}`}
                    >
                      <div
                        className={`max-w-[70%] px-3 py-2 rounded-lg text-sm ${
                          msg.senderId === selectedPartner
                            ? 'bg-muted'
                            : 'bg-primary text-primary-foreground'
                        }`}
                      >
                        <p>{msg.content}</p>
                        <p className="text-[10px] mt-1 opacity-60">
                          {new Date(msg.createdAt).toLocaleTimeString()}
                        </p>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="text-center text-muted-foreground text-sm py-8">
                    No messages yet. Start the conversation!
                  </div>
                )}
              </CardContent>
              <div className="p-3 border-t flex gap-2">
                <Input
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  placeholder="Type a message..."
                  onKeyDown={(e) => e.key === 'Enter' && handleSend()}
                />
                <Button
                  onClick={handleSend}
                  disabled={!message.trim() || sendMessage.isPending}
                  size="icon"
                >
                  <Send className="h-4 w-4" />
                </Button>
              </div>
            </>
          ) : (
            <div className="flex-1 flex items-center justify-center text-muted-foreground">
              <div className="text-center">
                <MessageSquare className="h-12 w-12 mx-auto mb-3 opacity-30" />
                <p>Select a conversation to start chatting</p>
              </div>
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
