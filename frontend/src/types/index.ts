// ─────────────────────────────────────────────
// Auth & User Types
// ─────────────────────────────────────────────
export type UserRole = 'USER' | 'ORGANIZER' | 'ADMIN';

export interface User {
  id: string;
  email: string;
  name: string;
  avatarUrl?: string;
  role: UserRole;
  interests: string[];
  bio?: string;
  createdAt: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  name: string;
  email: string;
  password: string;
  role?: UserRole;
}

export interface AuthResponse {
  user: User;
  tokens: AuthTokens;
}

// ─────────────────────────────────────────────
// Event Types
// ─────────────────────────────────────────────
export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'COMPLETED';
export type EventCategory = 'CONCERT' | 'CONFERENCE' | 'SPORTS' | 'THEATER' | 'MOVIE' | 'MEETUP' | 'WORKSHOP' | 'OTHER';

export interface Event {
  id: string;
  title: string;
  description: string;
  category: EventCategory;
  status: EventStatus;
  coverImageUrl?: string;
  startDate: string;
  endDate: string;
  venue: Venue;
  organizer: OrganizerSummary;
  tags: string[];
  minPrice: number;
  maxPrice: number;
  currency: string;
  totalSeats: number;
  availableSeats: number;
  createdAt: string;
  updatedAt: string;
}

export interface EventSummary {
  id: string;
  title: string;
  category: EventCategory;
  status: EventStatus;
  coverImageUrl?: string;
  startDate: string;
  venue: VenueSummary;
  organizer: OrganizerSummary;
  minPrice: number;
  currency: string;
  availableSeats: number;
}

export interface CreateEventRequest {
  title: string;
  description: string;
  category: EventCategory;
  startDate: string;
  endDate: string;
  venueId: string;
  tags: string[];
  coverImageUrl?: string;
}

export interface OrganizerSummary {
  id: string;
  name: string;
  avatarUrl?: string;
}

// ─────────────────────────────────────────────
// Venue & Seating Types
// ─────────────────────────────────────────────
export interface Venue {
  id: string;
  name: string;
  address: string;
  city: string;
  country: string;
  latitude?: number;
  longitude?: number;
  capacity: number;
}

export interface VenueSummary {
  id: string;
  name: string;
  city: string;
}

export type SeatStatus = 'AVAILABLE' | 'LOCKED' | 'BOOKED' | 'BLOCKED';

export interface SeatSection {
  id: string;
  name: string;
  color: string;
  price: number;
  currency: string;
  rows: SeatRow[];
  x: number;
  y: number;
  width: number;
  height: number;
  rotation: number;
}

export interface SeatRow {
  id: string;
  label: string;
  seats: Seat[];
}

export interface Seat {
  id: string;
  label: string;
  row: string;
  number: number;
  status: SeatStatus;
  sectionId: string;
  x: number;
  y: number;
  price: number;
  currency: string;
}

export interface VenueLayout {
  id: string;
  venueId: string;
  eventId: string;
  name: string;
  sections: SeatSection[];
  stageConfig?: StageConfig;
  canvasWidth: number;
  canvasHeight: number;
  version: number;
}

export interface StageConfig {
  x: number;
  y: number;
  width: number;
  height: number;
  label: string;
  shape: 'rectangle' | 'semicircle' | 'circle';
}

export interface SeatAvailability {
  eventId: string;
  sections: {
    sectionId: string;
    available: number;
    total: number;
    price: number;
  }[];
  lastUpdated: string;
}

// ─────────────────────────────────────────────
// Booking Types
// ─────────────────────────────────────────────
export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'EXPIRED' | 'REFUNDED';

export interface Booking {
  id: string;
  eventId: string;
  userId: string;
  seats: BookedSeat[];
  status: BookingStatus;
  totalAmount: number;
  currency: string;
  idempotencyKey: string;
  paymentId?: string;
  qrCode?: string;
  createdAt: string;
  updatedAt: string;
}

export interface BookedSeat {
  seatId: string;
  sectionName: string;
  row: string;
  number: number;
  price: number;
}

export interface CreateBookingRequest {
  eventId: string;
  seatIds: string[];
  idempotencyKey: string;
}

export interface SeatLockRequest {
  eventId: string;
  seatIds: string[];
  userId: string;
}

export interface SeatLockResponse {
  lockId: string;
  seatIds: string[];
  expiresAt: string;
}

// ─────────────────────────────────────────────
// Payment Types
// ─────────────────────────────────────────────
export type PaymentStatus = 'INITIATED' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | 'REFUNDED';
export type PaymentGateway = 'STRIPE' | 'RAZORPAY' | 'PAYPAL';

export interface Payment {
  id: string;
  bookingId: string;
  amount: number;
  currency: string;
  gateway: PaymentGateway;
  status: PaymentStatus;
  gatewayTransactionId?: string;
  createdAt: string;
}

export interface InitiatePaymentRequest {
  bookingId: string;
  gateway: PaymentGateway;
  returnUrl: string;
}

export interface PaymentSession {
  sessionId: string;
  redirectUrl: string;
  gateway: PaymentGateway;
}

// ─────────────────────────────────────────────
// Social Types
// ─────────────────────────────────────────────
export interface Post {
  id: string;
  authorId: string;
  author: User;
  content: string;
  imageUrl?: string;
  eventId?: string;
  likes: number;
  comments: number;
  isLiked: boolean;
  createdAt: string;
}

export interface Comment {
  id: string;
  postId: string;
  authorId: string;
  author: User;
  content: string;
  createdAt: string;
}

export interface DirectMessage {
  id: string;
  senderId: string;
  receiverId: string;
  content: string;
  read: boolean;
  createdAt: string;
}

// ─────────────────────────────────────────────
// Notification Types
// ─────────────────────────────────────────────
export type NotificationType = 'BOOKING_CONFIRMED' | 'PAYMENT_SUCCESS' | 'EVENT_REMINDER' | 'NEW_FOLLOWER' | 'NEW_MESSAGE' | 'EVENT_UPDATE';

export interface Notification {
  id: string;
  userId: string;
  type: NotificationType;
  title: string;
  message: string;
  read: boolean;
  metadata?: Record<string, string>;
  createdAt: string;
}

// ─────────────────────────────────────────────
// Pagination
// ─────────────────────────────────────────────
export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

// ─────────────────────────────────────────────
// Finance Types
// ─────────────────────────────────────────────
export interface BudgetItem {
  id: string;
  eventId: string;
  category: string;
  description: string;
  estimatedAmount: number;
  actualAmount?: number;
  currency: string;
}

export interface RevenueAnalytics {
  eventId: string;
  totalRevenue: number;
  totalBookings: number;
  totalRefunds: number;
  netRevenue: number;
  currency: string;
  dailyRevenue: { date: string; amount: number }[];
}

// ─────────────────────────────────────────────
// WebSocket Event Types
// ─────────────────────────────────────────────
export type WsEventType = 'SEAT_LOCKED' | 'SEAT_RELEASED' | 'SEAT_BOOKED' | 'AVAILABILITY_UPDATE';

export interface WsSeatEvent {
  type: WsEventType;
  eventId: string;
  seatIds: string[];
  userId?: string;
  timestamp: string;
}
