import Link from 'next/link';
import { CalendarDays, MapPin, Users, Zap, Shield, Globe } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';

const features = [
  {
    icon: CalendarDays,
    title: 'Event Discovery',
    description: 'Browse thousands of events across concerts, conferences, sports, and more.',
  },
  {
    icon: MapPin,
    title: 'Interactive Seating',
    description: 'Choose your perfect seat with our real-time interactive venue maps.',
  },
  {
    icon: Zap,
    title: 'Instant Booking',
    description: 'Lightning-fast booking with real-time seat locking — no overselling.',
  },
  {
    icon: Shield,
    title: 'Secure Payments',
    description: 'Multiple payment gateways with enterprise-grade security and refund support.',
  },
  {
    icon: Users,
    title: 'Social Network',
    description: 'Connect with fellow attendees, share experiences, and build your event community.',
  },
  {
    icon: Globe,
    title: 'Organizer Tools',
    description: 'Powerful dashboards, venue builders, budgeting, and analytics for organizers.',
  },
];

const categories = [
  { name: 'Concerts', emoji: '🎵', color: 'bg-purple-100 dark:bg-purple-900/30' },
  { name: 'Conferences', emoji: '🎤', color: 'bg-blue-100 dark:bg-blue-900/30' },
  { name: 'Sports', emoji: '⚽', color: 'bg-green-100 dark:bg-green-900/30' },
  { name: 'Theater', emoji: '🎭', color: 'bg-red-100 dark:bg-red-900/30' },
  { name: 'Workshops', emoji: '🔧', color: 'bg-orange-100 dark:bg-orange-900/30' },
  { name: 'Meetups', emoji: '🤝', color: 'bg-teal-100 dark:bg-teal-900/30' },
];

export default function HomePage() {
  return (
    <div>
      {/* Hero Section */}
      <section className="relative overflow-hidden bg-gradient-to-b from-primary/5 via-background to-background">
        <div className="container mx-auto max-w-7xl px-4 py-24 md:py-32">
          <div className="flex flex-col items-center text-center gap-8 max-w-3xl mx-auto">
            <div className="inline-flex items-center gap-2 rounded-full border px-4 py-1.5 text-sm font-medium text-muted-foreground">
              <Zap className="h-3.5 w-3.5 text-primary" />
              Built for 10,000+ concurrent users
            </div>
            <h1 className="text-4xl md:text-6xl font-bold tracking-tight">
              Discover Events.
              <br />
              <span className="text-primary">Book Instantly.</span>
              <br />
              Connect Together.
            </h1>
            <p className="text-lg md:text-xl text-muted-foreground max-w-2xl">
              The all-in-one platform for event discovery, real-time seat booking, social
              networking, and organizer management. Built for scale.
            </p>
            <div className="flex flex-col sm:flex-row gap-3">
              <Button size="lg" asChild className="text-base px-8">
                <Link href="/events">Explore Events</Link>
              </Button>
              <Button size="lg" variant="outline" asChild className="text-base px-8">
                <Link href="/auth/signup?role=ORGANIZER">Create an Event</Link>
              </Button>
            </div>
          </div>
        </div>
      </section>

      {/* Categories */}
      <section className="container mx-auto max-w-7xl px-4 py-16">
        <h2 className="text-2xl font-bold text-center mb-8">Browse by Category</h2>
        <div className="grid grid-cols-3 md:grid-cols-6 gap-4">
          {categories.map((cat) => (
            <Link
              key={cat.name}
              href={`/events?category=${cat.name.toUpperCase()}`}
              className="group"
            >
              <Card className="hover:shadow-md transition-shadow">
                <CardContent className="flex flex-col items-center gap-2 p-6">
                  <div
                    className={`w-12 h-12 rounded-full flex items-center justify-center text-2xl ${cat.color} group-hover:scale-110 transition-transform`}
                  >
                    {cat.emoji}
                  </div>
                  <span className="text-sm font-medium">{cat.name}</span>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      </section>

      {/* Features */}
      <section className="container mx-auto max-w-7xl px-4 py-16">
        <div className="text-center mb-12">
          <h2 className="text-3xl font-bold mb-3">Everything You Need</h2>
          <p className="text-muted-foreground text-lg">
            A complete platform for attendees, organizers, and communities.
          </p>
        </div>
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((feature) => {
            const Icon = feature.icon;
            return (
              <Card key={feature.title} className="hover:shadow-md transition-shadow">
                <CardContent className="p-6">
                  <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center mb-4">
                    <Icon className="h-5 w-5 text-primary" />
                  </div>
                  <h3 className="font-semibold text-lg mb-2">{feature.title}</h3>
                  <p className="text-sm text-muted-foreground">{feature.description}</p>
                </CardContent>
              </Card>
            );
          })}
        </div>
      </section>

      {/* CTA */}
      <section className="container mx-auto max-w-7xl px-4 py-16">
        <Card className="bg-primary text-primary-foreground">
          <CardContent className="flex flex-col md:flex-row items-center justify-between gap-6 p-8 md:p-12">
            <div>
              <h2 className="text-2xl md:text-3xl font-bold mb-2">Ready to host your event?</h2>
              <p className="text-primary-foreground/80">
                Create stunning events with interactive seat maps, real-time analytics, and more.
              </p>
            </div>
            <Button size="lg" variant="secondary" asChild className="shrink-0">
              <Link href="/auth/signup?role=ORGANIZER">Get Started Free</Link>
            </Button>
          </CardContent>
        </Card>
      </section>
    </div>
  );
}
