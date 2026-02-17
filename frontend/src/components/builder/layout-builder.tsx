'use client';

import { useState, useRef, useCallback } from 'react';
import { Stage, Layer, Rect, Circle, Text, Group, Transformer } from 'react-konva';
import type Konva from 'konva';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import {
  Plus, Trash2, RotateCw, Copy, Save, Download, Upload,
  ZoomIn, ZoomOut, Grid, MousePointer, Square
} from 'lucide-react';

// ─────────────────────────────────────────────
// Types
// ─────────────────────────────────────────────

interface BuilderSection {
  id: string;
  name: string;
  color: string;
  x: number;
  y: number;
  width: number;
  height: number;
  rotation: number;
  rows: number;
  seatsPerRow: number;
  price: number;
  seatSpacing: number;
  rowSpacing: number;
}

interface StageConfig {
  x: number;
  y: number;
  width: number;
  height: number;
  label: string;
}

type Tool = 'select' | 'section' | 'stage';

const SECTION_COLORS = [
  '#3b82f6', '#ef4444', '#22c55e', '#f59e0b', '#8b5cf6',
  '#ec4899', '#06b6d4', '#f97316', '#14b8a6', '#6366f1',
];

const TEMPLATES = {
  movie: { rows: 10, seatsPerRow: 20, sections: 1 },
  concert: { rows: 15, seatsPerRow: 30, sections: 3 },
  stadium: { rows: 25, seatsPerRow: 40, sections: 4 },
  theater: { rows: 12, seatsPerRow: 24, sections: 2 },
};

// ─────────────────────────────────────────────
// Component
// ─────────────────────────────────────────────

export function LayoutBuilder() {
  const stageRef = useRef<Konva.Stage>(null);
  const [tool, setTool] = useState<Tool>('select');
  const [sections, setSections] = useState<BuilderSection[]>([]);
  const [stageConfig, setStageConfig] = useState<StageConfig>({
    x: 350, y: 30, width: 300, height: 60, label: 'STAGE',
  });
  const [selectedSectionId, setSelectedSectionId] = useState<string | null>(null);
  const [canvasScale, setCanvasScale] = useState(1);

  const selectedSection = sections.find((s) => s.id === selectedSectionId);

  const addSection = useCallback(() => {
    const id = `section-${Date.now()}`;
    const colorIdx = sections.length % SECTION_COLORS.length;
    const newSection: BuilderSection = {
      id,
      name: `Section ${String.fromCharCode(65 + sections.length)}`,
      color: SECTION_COLORS[colorIdx],
      x: 100 + sections.length * 30,
      y: 150 + sections.length * 20,
      width: 300,
      height: 200,
      rotation: 0,
      rows: 5,
      seatsPerRow: 10,
      price: 50,
      seatSpacing: 28,
      rowSpacing: 30,
    };
    setSections((prev) => [...prev, newSection]);
    setSelectedSectionId(id);
  }, [sections.length]);

  const updateSection = useCallback((id: string, updates: Partial<BuilderSection>) => {
    setSections((prev) => prev.map((s) => (s.id === id ? { ...s, ...updates } : s)));
  }, []);

  const deleteSection = useCallback((id: string) => {
    setSections((prev) => prev.filter((s) => s.id !== id));
    if (selectedSectionId === id) setSelectedSectionId(null);
  }, [selectedSectionId]);

  const duplicateSection = useCallback((id: string) => {
    const source = sections.find((s) => s.id === id);
    if (!source) return;
    const newId = `section-${Date.now()}`;
    setSections((prev) => [
      ...prev,
      { ...source, id: newId, name: source.name + ' Copy', x: source.x + 40, y: source.y + 40 },
    ]);
    setSelectedSectionId(newId);
  }, [sections]);

  const applyTemplate = useCallback((templateName: keyof typeof TEMPLATES) => {
    const tmpl = TEMPLATES[templateName];
    const newSections: BuilderSection[] = [];
    for (let i = 0; i < tmpl.sections; i++) {
      newSections.push({
        id: `section-${Date.now()}-${i}`,
        name: `Section ${String.fromCharCode(65 + i)}`,
        color: SECTION_COLORS[i % SECTION_COLORS.length],
        x: 50 + i * 260,
        y: 150,
        width: 240,
        height: (tmpl.rows / tmpl.sections) * 30 + 40,
        rotation: 0,
        rows: Math.ceil(tmpl.rows / tmpl.sections),
        seatsPerRow: tmpl.seatsPerRow / tmpl.sections,
        price: 50 + i * 25,
        seatSpacing: 24,
        rowSpacing: 28,
      });
    }
    setSections(newSections);
    setSelectedSectionId(newSections[0]?.id || null);
  }, []);

  const exportLayout = useCallback(() => {
    const layout = {
      stageConfig,
      sections: sections.map((s) => ({
        ...s,
        rows: Array.from({ length: s.rows }, (_, rowIdx) => ({
          id: `${s.id}-row-${rowIdx}`,
          label: String.fromCharCode(65 + rowIdx),
          seats: Array.from({ length: s.seatsPerRow }, (_, seatIdx) => ({
            id: `${s.id}-${rowIdx}-${seatIdx}`,
            label: `${String.fromCharCode(65 + rowIdx)}${seatIdx + 1}`,
            row: String.fromCharCode(65 + rowIdx),
            number: seatIdx + 1,
            status: 'AVAILABLE',
            sectionId: s.id,
            x: 30 + seatIdx * s.seatSpacing,
            y: 40 + rowIdx * s.rowSpacing,
            price: s.price,
            currency: 'USD',
          })),
        })),
      })),
      canvasWidth: 1000,
      canvasHeight: 700,
      version: 1,
    };
    const blob = new Blob([JSON.stringify(layout, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'venue-layout.json';
    a.click();
    URL.revokeObjectURL(url);
  }, [sections, stageConfig]);

  const totalSeats = sections.reduce((sum, s) => sum + s.rows * s.seatsPerRow, 0);

  return (
    <div className="flex gap-6 h-[calc(100vh-12rem)]">
      {/* Canvas */}
      <div className="flex-1 flex flex-col">
        {/* Toolbar */}
        <div className="flex items-center gap-2 mb-3 flex-wrap">
          <Button
            variant={tool === 'select' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setTool('select')}
          >
            <MousePointer className="h-4 w-4 mr-1" /> Select
          </Button>
          <Button size="sm" variant="outline" onClick={addSection}>
            <Plus className="h-4 w-4 mr-1" /> Add Section
          </Button>
          <div className="h-6 w-px bg-border mx-1" />
          <Select onValueChange={(v) => applyTemplate(v as keyof typeof TEMPLATES)}>
            <SelectTrigger className="w-36 h-8 text-xs">
              <SelectValue placeholder="Templates..." />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="movie">Movie Theater</SelectItem>
              <SelectItem value="concert">Concert Hall</SelectItem>
              <SelectItem value="stadium">Stadium</SelectItem>
              <SelectItem value="theater">Theater</SelectItem>
            </SelectContent>
          </Select>
          <div className="h-6 w-px bg-border mx-1" />
          <Button size="sm" variant="outline" onClick={() => setCanvasScale((s) => Math.min(2, s + 0.1))}>
            <ZoomIn className="h-4 w-4" />
          </Button>
          <Button size="sm" variant="outline" onClick={() => setCanvasScale((s) => Math.max(0.3, s - 0.1))}>
            <ZoomOut className="h-4 w-4" />
          </Button>
          <span className="text-xs text-muted-foreground">{Math.round(canvasScale * 100)}%</span>
          <div className="flex-1" />
          <Badge variant="secondary">{totalSeats} seats</Badge>
          <Button size="sm" onClick={exportLayout}>
            <Download className="h-4 w-4 mr-1" /> Export JSON
          </Button>
          <Button size="sm" variant="default">
            <Save className="h-4 w-4 mr-1" /> Save Layout
          </Button>
        </div>

        {/* Canvas area */}
        <div className="flex-1 rounded-lg border bg-muted/20 overflow-hidden">
          <Stage
            ref={stageRef}
            width={1000}
            height={700}
            scaleX={canvasScale}
            scaleY={canvasScale}
            draggable
            onClick={(e) => {
              if (e.target === e.target.getStage()) setSelectedSectionId(null);
            }}
          >
            <Layer>
              {/* Grid */}
              {Array.from({ length: 40 }, (_, i) => (
                <Rect key={`gv-${i}`} x={i * 50} y={0} width={1} height={1400} fill="#e5e7eb" opacity={0.3} />
              ))}
              {Array.from({ length: 28 }, (_, i) => (
                <Rect key={`gh-${i}`} x={0} y={i * 50} width={2000} height={1} fill="#e5e7eb" opacity={0.3} />
              ))}

              {/* Stage/Screen */}
              <Group x={stageConfig.x} y={stageConfig.y} draggable
                onDragEnd={(e) => setStageConfig((c) => ({ ...c, x: e.target.x(), y: e.target.y() }))}>
                <Rect width={stageConfig.width} height={stageConfig.height}
                  fill="#1e293b" cornerRadius={8} shadowBlur={4} shadowColor="#000" shadowOpacity={0.2} />
                <Text text={stageConfig.label} fontSize={16} fontStyle="bold" fill="#94a3b8"
                  width={stageConfig.width} height={stageConfig.height} align="center" verticalAlign="middle" />
              </Group>

              {/* Sections */}
              {sections.map((section) => {
                const isSelected = selectedSectionId === section.id;
                return (
                  <Group key={section.id} x={section.x} y={section.y} rotation={section.rotation}
                    draggable
                    onClick={() => setSelectedSectionId(section.id)}
                    onDragEnd={(e) => updateSection(section.id, { x: e.target.x(), y: e.target.y() })}>
                    {/* Section background */}
                    <Rect width={section.width} height={section.height}
                      fill={section.color + '15'} stroke={isSelected ? section.color : section.color + '60'}
                      strokeWidth={isSelected ? 2 : 1} cornerRadius={6}
                      shadowBlur={isSelected ? 8 : 0} shadowColor={section.color} />
                    {/* Section label */}
                    <Text text={`${section.name} ($${section.price})`} fontSize={11} fontStyle="bold"
                      fill={section.color} x={8} y={8} />
                    {/* Seat dots */}
                    {Array.from({ length: section.rows }, (_, rowIdx) =>
                      Array.from({ length: section.seatsPerRow }, (_, seatIdx) => (
                        <Circle key={`${rowIdx}-${seatIdx}`}
                          x={30 + seatIdx * section.seatSpacing}
                          y={40 + rowIdx * section.rowSpacing}
                          radius={8} fill={section.color} opacity={0.7} />
                      ))
                    )}
                  </Group>
                );
              })}
            </Layer>
          </Stage>
        </div>
      </div>

      {/* Properties Panel */}
      <div className="w-72 shrink-0 space-y-4 overflow-y-auto">
        {selectedSection ? (
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-sm flex items-center justify-between">
                Section Properties
                <div className="flex gap-1">
                  <Button variant="ghost" size="icon" className="h-7 w-7"
                    onClick={() => duplicateSection(selectedSection.id)}>
                    <Copy className="h-3.5 w-3.5" />
                  </Button>
                  <Button variant="ghost" size="icon" className="h-7 w-7 text-destructive"
                    onClick={() => deleteSection(selectedSection.id)}>
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                </div>
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="space-y-1">
                <Label className="text-xs">Name</Label>
                <Input value={selectedSection.name} className="h-8 text-sm"
                  onChange={(e) => updateSection(selectedSection.id, { name: e.target.value })} />
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div className="space-y-1">
                  <Label className="text-xs">Rows</Label>
                  <Input type="number" value={selectedSection.rows} className="h-8 text-sm"
                    onChange={(e) => updateSection(selectedSection.id, { rows: parseInt(e.target.value) || 1 })} />
                </div>
                <div className="space-y-1">
                  <Label className="text-xs">Seats/Row</Label>
                  <Input type="number" value={selectedSection.seatsPerRow} className="h-8 text-sm"
                    onChange={(e) => updateSection(selectedSection.id, { seatsPerRow: parseInt(e.target.value) || 1 })} />
                </div>
              </div>
              <div className="space-y-1">
                <Label className="text-xs">Price ($)</Label>
                <Input type="number" value={selectedSection.price} className="h-8 text-sm"
                  onChange={(e) => updateSection(selectedSection.id, { price: parseFloat(e.target.value) || 0 })} />
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div className="space-y-1">
                  <Label className="text-xs">Seat Spacing</Label>
                  <Input type="number" value={selectedSection.seatSpacing} className="h-8 text-sm"
                    onChange={(e) => updateSection(selectedSection.id, { seatSpacing: parseInt(e.target.value) || 20 })} />
                </div>
                <div className="space-y-1">
                  <Label className="text-xs">Row Spacing</Label>
                  <Input type="number" value={selectedSection.rowSpacing} className="h-8 text-sm"
                    onChange={(e) => updateSection(selectedSection.id, { rowSpacing: parseInt(e.target.value) || 24 })} />
                </div>
              </div>
              <div className="space-y-1">
                <Label className="text-xs">Rotation (°)</Label>
                <Input type="number" value={selectedSection.rotation} className="h-8 text-sm"
                  onChange={(e) => updateSection(selectedSection.id, { rotation: parseFloat(e.target.value) || 0 })} />
              </div>
              <div className="space-y-1">
                <Label className="text-xs">Color</Label>
                <div className="flex gap-1 flex-wrap">
                  {SECTION_COLORS.map((color) => (
                    <button key={color} className="w-6 h-6 rounded-full border-2 transition-transform hover:scale-110"
                      style={{ backgroundColor: color, borderColor: selectedSection.color === color ? '#000' : 'transparent' }}
                      onClick={() => updateSection(selectedSection.id, { color })} />
                  ))}
                </div>
              </div>
              <div className="text-xs text-muted-foreground pt-2 border-t">
                Total: {selectedSection.rows * selectedSection.seatsPerRow} seats
              </div>
            </CardContent>
          </Card>
        ) : (
          <Card>
            <CardContent className="p-6 text-center text-sm text-muted-foreground">
              <Square className="h-8 w-8 mx-auto mb-2 opacity-50" />
              <p>Select a section to edit its properties, or add a new section.</p>
            </CardContent>
          </Card>
        )}

        {/* All Sections List */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm">Sections ({sections.length})</CardTitle>
          </CardHeader>
          <CardContent className="space-y-1.5">
            {sections.length === 0 ? (
              <p className="text-xs text-muted-foreground">No sections yet. Add one or use a template.</p>
            ) : (
              sections.map((s) => (
                <button key={s.id}
                  className={`w-full flex items-center gap-2 p-2 rounded text-left text-sm transition-colors ${
                    selectedSectionId === s.id ? 'bg-primary/10' : 'hover:bg-muted'
                  }`}
                  onClick={() => setSelectedSectionId(s.id)}>
                  <div className="w-3 h-3 rounded-full shrink-0" style={{ backgroundColor: s.color }} />
                  <span className="flex-1 truncate">{s.name}</span>
                  <span className="text-xs text-muted-foreground">{s.rows * s.seatsPerRow}</span>
                </button>
              ))
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
