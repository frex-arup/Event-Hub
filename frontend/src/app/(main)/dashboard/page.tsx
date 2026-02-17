'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { CalendarDays, Ticket, DollarSign, Users, TrendingUp, ArrowUpRight } from 'lucide-react';

const stats = [
  { label: 'Total Events', value: '12', change: '+2 this month', icon: CalendarDays, color: 'text-blue-600' },
  { label: 'Tickets Sold', value: '4,328', change: '+12.5%', icon: Ticket, color: 'text-green-600' },
  { label: 'Revenue', value: '$48,250', change: '+8.2%', icon: DollarSign, color: 'text-purple-600' },
  { label: 'Attendees', value: '3,891', change: '+15.3%', icon: Users, color: 'text-orange-600' },
];

export default function DashboardPage() {
  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold mb-2">Dashboard</h1>
        <p className="text-muted-foreground">Welcome back! Here&apos;s your event overview.</p>
      </div>

      {/* Stats Grid */}
      <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {stats.map((stat) => {
          const Icon = stat.icon;
          return (
            <Card key={stat.label}>
              <CardContent className="p-6">
                <div className="flex items-center justify-between mb-4">
                  <div className={`w-10 h-10 rounded-lg bg-muted flex items-center justify-center`}>
                    <Icon className={`h-5 w-5 ${stat.color}`} />
                  </div>
                  <div className="flex items-center gap-1 text-xs text-green-600">
                    <TrendingUp className="h-3 w-3" />
                    {stat.change}
                  </div>
                </div>
                <div className="text-2xl font-bold">{stat.value}</div>
                <div className="text-sm text-muted-foreground">{stat.label}</div>
              </CardContent>
            </Card>
          );
        })}
      </div>

      {/* Recent Activity & Quick Actions */}
      <div className="grid lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Recent Bookings</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {[1, 2, 3, 4, 5].map((i) => (
                <div key={i} className="flex items-center justify-between py-2 border-b last:border-0">
                  <div>
                    <p className="font-medium text-sm">Booking #{String(i).padStart(4, '0')}</p>
                    <p className="text-xs text-muted-foreground">2 seats · Summer Concert</p>
                  </div>
                  <div className="text-right">
                    <p className="font-medium text-sm text-green-600">$120.00</p>
                    <p className="text-xs text-muted-foreground">2 min ago</p>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Upcoming Events</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {[
                { name: 'Summer Music Festival', date: 'Mar 15, 2026', sold: '850/1000' },
                { name: 'Tech Conference 2026', date: 'Apr 2, 2026', sold: '320/500' },
                { name: 'Comedy Night', date: 'Apr 10, 2026', sold: '95/150' },
              ].map((event) => (
                <div key={event.name} className="flex items-center justify-between py-2 border-b last:border-0">
                  <div>
                    <p className="font-medium text-sm">{event.name}</p>
                    <p className="text-xs text-muted-foreground">{event.date}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm text-muted-foreground">{event.sold}</span>
                    <ArrowUpRight className="h-4 w-4 text-muted-foreground" />
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
