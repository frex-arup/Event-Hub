'use client';

import { useEffect, useRef, useCallback } from 'react';
import { Stage, Layer, Rect, Circle, Text, Group } from 'react-konva';
import type { KonvaEventObject } from 'konva/lib/Node';
import { useBookingStore } from '@/store/booking-store';
import { wsManager } from '@/lib/websocket';
import type { VenueLayout, Seat, SeatStatus, WsSeatEvent } from '@/types';

interface SeatMapProps {
  layout: VenueLayout;
  seats: Map<string, Seat>;
  userId?: string;
}

const SEAT_RADIUS = 12;
const SEAT_COLORS: Record<SeatStatus, string> = {
  AVAILABLE: '#22c55e',
  LOCKED: '#f59e0b',
  BOOKED: '#6b7280',
  BLOCKED: '#374151',
};
const SELECTED_COLOR = '#3b82f6';

export function SeatMap({ layout, seats, userId }: SeatMapProps) {
  const { selectedSeats, addSeat, removeSeat } = useBookingStore();
  const stageRef = useRef<any>(null);
  const selectedIds = new Set(selectedSeats.map((s) => s.id));

  // Subscribe to real-time seat updates
  useEffect(() => {
    const unsubscribe = wsManager.subscribe('*', (event: WsSeatEvent) => {
      // Seat status updates will be reflected via TanStack Query refetch
      // This handler can be used for optimistic UI updates
      console.log('[SeatMap] WS event:', event.type, event.seatIds);
    });

    return () => unsubscribe();
  }, []);

  const handleSeatClick = useCallback(
    (seat: Seat) => {
      if (seat.status !== 'AVAILABLE') return;
      if (selectedIds.has(seat.id)) {
        removeSeat(seat.id);
      } else {
        addSeat(seat);
      }
    },
    [selectedIds, addSeat, removeSeat]
  );

  const getSeatColor = (seat: Seat): string => {
    if (selectedIds.has(seat.id)) return SELECTED_COLOR;
    return SEAT_COLORS[seat.status] || SEAT_COLORS.AVAILABLE;
  };

  const getSeatOpacity = (seat: Seat): number => {
    if (seat.status === 'BOOKED' || seat.status === 'BLOCKED') return 0.4;
    if (seat.status === 'LOCKED') return 0.6;
    return 1;
  };

  const handleWheel = (e: KonvaEventObject<WheelEvent>) => {
    e.evt.preventDefault();
    const stage = stageRef.current;
    if (!stage) return;

    const scaleBy = 1.05;
    const oldScale = stage.scaleX();
    const pointer = stage.getPointerPosition();
    if (!pointer) return;

    const mousePointTo = {
      x: (pointer.x - stage.x()) / oldScale,
      y: (pointer.y - stage.y()) / oldScale,
    };

    const direction = e.evt.deltaY > 0 ? -1 : 1;
    const newScale = direction > 0 ? oldScale * scaleBy : oldScale / scaleBy;
    const clampedScale = Math.max(0.3, Math.min(3, newScale));

    stage.scale({ x: clampedScale, y: clampedScale });
    stage.position({
      x: pointer.x - mousePointTo.x * clampedScale,
      y: pointer.y - mousePointTo.y * clampedScale,
    });
  };

  return (
    <Stage
      ref={stageRef}
      width={layout.canvasWidth || 800}
      height={layout.canvasHeight || 600}
      draggable
      onWheel={handleWheel}
      className="bg-muted/20 rounded-lg border cursor-grab active:cursor-grabbing"
    >
      <Layer>
        {/* Stage/Screen */}
        {layout.stageConfig && (
          <Group x={layout.stageConfig.x} y={layout.stageConfig.y}>
            <Rect
              width={layout.stageConfig.width}
              height={layout.stageConfig.height}
              fill="#1e293b"
              cornerRadius={8}
            />
            <Text
              text={layout.stageConfig.label || 'STAGE'}
              fontSize={16}
              fontStyle="bold"
              fill="#94a3b8"
              width={layout.stageConfig.width}
              height={layout.stageConfig.height}
              align="center"
              verticalAlign="middle"
            />
          </Group>
        )}

        {/* Sections */}
        {layout.sections.map((section) => (
          <Group key={section.id} x={section.x} y={section.y} rotation={section.rotation || 0}>
            {/* Section background */}
            <Rect
              width={section.width}
              height={section.height}
              fill={section.color + '10'}
              stroke={section.color + '40'}
              strokeWidth={1}
              cornerRadius={4}
            />
            {/* Section label */}
            <Text
              text={section.name}
              fontSize={12}
              fontStyle="bold"
              fill={section.color}
              x={8}
              y={8}
            />

            {/* Rows & Seats */}
            {section.rows.map((row) => (
              <Group key={row.id}>
                {row.seats.map((seatData) => {
                  const seat = seats.get(seatData.id) || seatData;
                  const isSelected = selectedIds.has(seat.id);
                  const isClickable = seat.status === 'AVAILABLE';

                  return (
                    <Group key={seat.id} x={seat.x} y={seat.y}>
                      <Circle
                        radius={SEAT_RADIUS}
                        fill={getSeatColor(seat)}
                        opacity={getSeatOpacity(seat)}
                        stroke={isSelected ? '#1d4ed8' : undefined}
                        strokeWidth={isSelected ? 2 : 0}
                        shadowBlur={isSelected ? 6 : 0}
                        shadowColor={SELECTED_COLOR}
                        onClick={() => handleSeatClick(seat)}
                        onTap={() => handleSeatClick(seat)}
                        style={{ cursor: isClickable ? 'pointer' : 'not-allowed' }}
                      />
                      <Text
                        text={seat.label || String(seat.number)}
                        fontSize={8}
                        fill="#fff"
                        align="center"
                        verticalAlign="middle"
                        width={SEAT_RADIUS * 2}
                        height={SEAT_RADIUS * 2}
                        x={-SEAT_RADIUS}
                        y={-SEAT_RADIUS}
                        listening={false}
                      />
                    </Group>
                  );
                })}
              </Group>
            ))}
          </Group>
        ))}
      </Layer>
    </Stage>
  );
}
