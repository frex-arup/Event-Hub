'use client';

import { useState } from 'react';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Textarea } from '@/components/ui/textarea';
import { Separator } from '@/components/ui/separator';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Heart, MessageCircle, Share2, Image, Send, TrendingUp } from 'lucide-react';

interface MockPost {
  id: string;
  author: { name: string; avatar?: string; initials: string };
  content: string;
  likes: number;
  comments: number;
  timeAgo: string;
  isLiked: boolean;
}

const mockPosts: MockPost[] = [
  {
    id: '1',
    author: { name: 'Sarah Chen', initials: 'SC' },
    content: 'Just got my tickets for the Summer Music Festival! Who else is going? 🎶🎉',
    likes: 24,
    comments: 8,
    timeAgo: '2h ago',
    isLiked: false,
  },
  {
    id: '2',
    author: { name: 'Marcus Johnson', initials: 'MJ' },
    content:
      'The Tech Conference 2026 lineup looks incredible. Really excited about the keynote on distributed systems architecture.',
    likes: 45,
    comments: 12,
    timeAgo: '4h ago',
    isLiked: true,
  },
  {
    id: '3',
    author: { name: 'Emily Rodriguez', initials: 'ER' },
    content:
      'Amazing experience at the Comedy Night last weekend! The venue was perfect and the comedians were hilarious 😂',
    likes: 67,
    comments: 15,
    timeAgo: '1d ago',
    isLiked: false,
  },
];

export default function SocialPage() {
  const [newPost, setNewPost] = useState('');
  const [posts, setPosts] = useState(mockPosts);

  const toggleLike = (postId: string) => {
    setPosts((prev) =>
      prev.map((p) =>
        p.id === postId
          ? { ...p, isLiked: !p.isLiked, likes: p.isLiked ? p.likes - 1 : p.likes + 1 }
          : p
      )
    );
  };

  return (
    <div className="container mx-auto max-w-3xl px-4 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold mb-2">Social Feed</h1>
        <p className="text-muted-foreground">Connect with the event community</p>
      </div>

      <Tabs defaultValue="feed">
        <TabsList className="mb-6">
          <TabsTrigger value="feed">Feed</TabsTrigger>
          <TabsTrigger value="trending">Trending</TabsTrigger>
          <TabsTrigger value="following">Following</TabsTrigger>
        </TabsList>

        <TabsContent value="feed" className="space-y-6">
          {/* New Post */}
          <Card>
            <CardContent className="p-4">
              <div className="flex gap-3">
                <Avatar className="h-10 w-10">
                  <AvatarFallback>U</AvatarFallback>
                </Avatar>
                <div className="flex-1 space-y-3">
                  <Textarea
                    placeholder="What's on your mind? Share about an event..."
                    value={newPost}
                    onChange={(e) => setNewPost(e.target.value)}
                    className="min-h-[80px] resize-none"
                  />
                  <div className="flex items-center justify-between">
                    <Button variant="ghost" size="sm">
                      <Image className="h-4 w-4 mr-1" />
                      Photo
                    </Button>
                    <Button size="sm" disabled={!newPost.trim()}>
                      <Send className="h-4 w-4 mr-1" />
                      Post
                    </Button>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Posts */}
          {posts.map((post) => (
            <Card key={post.id}>
              <CardHeader className="pb-3">
                <div className="flex items-center gap-3">
                  <Avatar className="h-10 w-10">
                    <AvatarImage src={post.author.avatar} />
                    <AvatarFallback>{post.author.initials}</AvatarFallback>
                  </Avatar>
                  <div>
                    <p className="font-semibold text-sm">{post.author.name}</p>
                    <p className="text-xs text-muted-foreground">{post.timeAgo}</p>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="pt-0 space-y-4">
                <p className="text-sm">{post.content}</p>
                <Separator />
                <div className="flex items-center gap-4">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => toggleLike(post.id)}
                    className={post.isLiked ? 'text-red-500' : ''}
                  >
                    <Heart className={`h-4 w-4 mr-1 ${post.isLiked ? 'fill-current' : ''}`} />
                    {post.likes}
                  </Button>
                  <Button variant="ghost" size="sm">
                    <MessageCircle className="h-4 w-4 mr-1" />
                    {post.comments}
                  </Button>
                  <Button variant="ghost" size="sm">
                    <Share2 className="h-4 w-4 mr-1" />
                    Share
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </TabsContent>

        <TabsContent value="trending">
          <Card>
            <CardContent className="p-8 text-center">
              <TrendingUp className="h-12 w-12 text-muted-foreground/50 mx-auto mb-3" />
              <h3 className="font-semibold mb-1">Trending Topics</h3>
              <p className="text-sm text-muted-foreground">
                Trending discussions and popular event topics will appear here.
              </p>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="following">
          <Card>
            <CardContent className="p-8 text-center">
              <MessageCircle className="h-12 w-12 text-muted-foreground/50 mx-auto mb-3" />
              <h3 className="font-semibold mb-1">Following Feed</h3>
              <p className="text-sm text-muted-foreground">
                Follow people and organizers to see their posts here.
              </p>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
