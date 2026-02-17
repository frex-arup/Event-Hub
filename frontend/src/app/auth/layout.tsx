import { CalendarDays } from 'lucide-react';
import Link from 'next/link';

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen flex">
      {/* Left panel — branding */}
      <div className="hidden lg:flex lg:w-1/2 bg-primary items-center justify-center p-12">
        <div className="max-w-md text-primary-foreground">
          <Link href="/" className="flex items-center gap-2 font-bold text-2xl mb-8">
            <CalendarDays className="h-8 w-8" />
            EventHub
          </Link>
          <h2 className="text-3xl font-bold mb-4">
            Your gateway to unforgettable experiences
          </h2>
          <p className="text-primary-foreground/80 text-lg">
            Discover events, book seats in real-time, connect with communities, and manage
            everything from one powerful platform.
          </p>
          <div className="mt-12 grid grid-cols-2 gap-4 text-sm">
            <div className="rounded-lg bg-primary-foreground/10 p-4">
              <div className="font-bold text-2xl mb-1">10K+</div>
              <div className="text-primary-foreground/70">Concurrent Users</div>
            </div>
            <div className="rounded-lg bg-primary-foreground/10 p-4">
              <div className="font-bold text-2xl mb-1">0</div>
              <div className="text-primary-foreground/70">Oversold Tickets</div>
            </div>
            <div className="rounded-lg bg-primary-foreground/10 p-4">
              <div className="font-bold text-2xl mb-1">Real-time</div>
              <div className="text-primary-foreground/70">Seat Updates</div>
            </div>
            <div className="rounded-lg bg-primary-foreground/10 p-4">
              <div className="font-bold text-2xl mb-1">3+</div>
              <div className="text-primary-foreground/70">Payment Gateways</div>
            </div>
          </div>
        </div>
      </div>

      {/* Right panel — form */}
      <div className="flex-1 flex items-center justify-center p-6 md:p-12">
        <div className="w-full max-w-md">{children}</div>
      </div>
    </div>
  );
}
