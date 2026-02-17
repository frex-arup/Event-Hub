'use client';

import { useState } from 'react';
import { User, Edit, Save, X, Users, UserPlus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useAuthStore } from '@/store/auth-store';
import { useProfile, useUpdateProfile, useFollowCounts } from '@/hooks/use-profile';

export default function ProfilePage() {
  const { user } = useAuthStore();
  const userId = user?.id ?? '';
  const { data: profileData } = useProfile(userId);
  const { data: countsData } = useFollowCounts(userId);
  const updateProfile = useUpdateProfile();
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState('');
  const [bio, setBio] = useState('');

  const profile = profileData as any;
  const counts = countsData as any;

  const startEditing = () => {
    setName(profile?.name ?? user?.name ?? '');
    setBio(profile?.bio ?? '');
    setEditing(true);
  };

  const saveProfile = () => {
    updateProfile.mutate({ name, bio }, {
      onSuccess: () => setEditing(false),
    });
  };

  return (
    <div className="container mx-auto max-w-3xl px-4 py-8">
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2">
              <User className="h-5 w-5" />
              My Profile
            </CardTitle>
            {!editing ? (
              <Button variant="outline" size="sm" onClick={startEditing}>
                <Edit className="h-4 w-4 mr-1" /> Edit
              </Button>
            ) : (
              <div className="flex gap-2">
                <Button size="sm" onClick={saveProfile} disabled={updateProfile.isPending}>
                  <Save className="h-4 w-4 mr-1" /> Save
                </Button>
                <Button variant="ghost" size="sm" onClick={() => setEditing(false)}>
                  <X className="h-4 w-4" />
                </Button>
              </div>
            )}
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center gap-4">
            <div className="w-20 h-20 rounded-full bg-muted flex items-center justify-center text-3xl">
              {profile?.avatarUrl ? (
                <img src={profile.avatarUrl} alt="" className="w-full h-full rounded-full object-cover" />
              ) : (
                <User className="h-10 w-10 text-muted-foreground" />
              )}
            </div>
            <div className="flex-1">
              {editing ? (
                <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="Your name" />
              ) : (
                <h2 className="text-xl font-bold">{profile?.name ?? user?.name ?? 'User'}</h2>
              )}
              <p className="text-sm text-muted-foreground">{profile?.email ?? user?.email}</p>
              <p className="text-xs text-muted-foreground mt-1 capitalize">{profile?.role?.toLowerCase() ?? 'user'}</p>
            </div>
          </div>

          <div className="flex gap-6 text-center">
            <div>
              <p className="text-lg font-bold">{counts?.followers ?? 0}</p>
              <p className="text-xs text-muted-foreground">Followers</p>
            </div>
            <div>
              <p className="text-lg font-bold">{counts?.following ?? 0}</p>
              <p className="text-xs text-muted-foreground">Following</p>
            </div>
          </div>

          <div>
            <label className="text-sm font-medium">Bio</label>
            {editing ? (
              <textarea
                className="w-full mt-1 p-2 rounded-md border bg-background text-sm min-h-[80px]"
                value={bio}
                onChange={(e) => setBio(e.target.value)}
                placeholder="Tell people about yourself..."
              />
            ) : (
              <p className="text-sm text-muted-foreground mt-1">
                {profile?.bio || 'No bio yet.'}
              </p>
            )}
          </div>

          {profile?.interests && profile.interests.length > 0 && (
            <div>
              <label className="text-sm font-medium">Interests</label>
              <div className="flex flex-wrap gap-1.5 mt-1">
                {profile.interests.map((interest: string) => (
                  <span key={interest} className="px-2 py-0.5 bg-muted rounded-full text-xs">
                    {interest}
                  </span>
                ))}
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
