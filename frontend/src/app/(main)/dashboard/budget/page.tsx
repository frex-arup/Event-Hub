'use client';

import { useState } from 'react';
import { DollarSign, Plus, TrendingUp, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { useOrganizerBudgets, useCreateBudget, useBudgetItems, useAddBudgetItem, useOrganizerAnalytics } from '@/hooks/use-finance';

export default function BudgetDashboardPage() {
  const { data: budgetsData, isLoading } = useOrganizerBudgets();
  const { data: analyticsData } = useOrganizerAnalytics();
  const createBudget = useCreateBudget();
  const [showCreate, setShowCreate] = useState(false);
  const [newBudget, setNewBudget] = useState({ eventId: '', name: '', totalBudget: 0 });

  const budgets = (budgetsData as any)?.content ?? [];
  const analytics = analyticsData as any;

  const handleCreate = () => {
    if (!newBudget.eventId || !newBudget.name) return;
    createBudget.mutate(newBudget, {
      onSuccess: () => {
        setShowCreate(false);
        setNewBudget({ eventId: '', name: '', totalBudget: 0 });
      },
    });
  };

  return (
    <div className="container mx-auto max-w-5xl px-4 py-8">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <DollarSign className="h-6 w-6" />
          <h1 className="text-2xl font-bold">Budget Dashboard</h1>
        </div>
        <Button onClick={() => setShowCreate(!showCreate)}>
          <Plus className="h-4 w-4 mr-1" /> New Budget
        </Button>
      </div>

      {/* Analytics Summary */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <Card>
          <CardContent className="pt-4">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <TrendingUp className="h-4 w-4" /> Total Revenue
            </div>
            <p className="text-2xl font-bold mt-1">
              ${analytics?.totalRevenue ?? '0.00'}
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-sm text-muted-foreground">Active Budgets</div>
            <p className="text-2xl font-bold mt-1">{budgets.length}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-sm text-muted-foreground">Total Spent</div>
            <p className="text-2xl font-bold mt-1">
              ${budgets.reduce((sum: number, b: any) => sum + (b.spent ?? 0), 0).toFixed(2)}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Create Budget Form */}
      {showCreate && (
        <Card className="mb-6">
          <CardHeader><CardTitle className="text-sm">Create Budget</CardTitle></CardHeader>
          <CardContent className="space-y-3">
            <Input
              placeholder="Event ID"
              value={newBudget.eventId}
              onChange={(e) => setNewBudget({ ...newBudget, eventId: e.target.value })}
            />
            <Input
              placeholder="Budget name"
              value={newBudget.name}
              onChange={(e) => setNewBudget({ ...newBudget, name: e.target.value })}
            />
            <Input
              type="number"
              placeholder="Total budget"
              value={newBudget.totalBudget || ''}
              onChange={(e) => setNewBudget({ ...newBudget, totalBudget: parseFloat(e.target.value) || 0 })}
            />
            <Button onClick={handleCreate} disabled={createBudget.isPending}>
              {createBudget.isPending ? <Loader2 className="h-4 w-4 animate-spin mr-1" /> : null}
              Create
            </Button>
          </CardContent>
        </Card>
      )}

      {/* Budget List */}
      {isLoading ? (
        <div className="flex justify-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      ) : budgets.length > 0 ? (
        <div className="space-y-4">
          {budgets.map((budget: any) => {
            const pct = budget.totalBudget > 0
              ? Math.min((budget.spent / budget.totalBudget) * 100, 100)
              : 0;
            return (
              <Card key={budget.id}>
                <CardContent className="pt-4">
                  <div className="flex items-center justify-between mb-2">
                    <h3 className="font-medium">{budget.name}</h3>
                    <span className="text-sm text-muted-foreground">
                      {budget.currency} {budget.spent?.toFixed(2)} / {budget.totalBudget?.toFixed(2)}
                    </span>
                  </div>
                  <div className="w-full h-2 bg-muted rounded-full overflow-hidden">
                    <div
                      className={`h-full rounded-full transition-all ${pct > 90 ? 'bg-red-500' : pct > 70 ? 'bg-yellow-500' : 'bg-green-500'}`}
                      style={{ width: `${pct}%` }}
                    />
                  </div>
                  <p className="text-xs text-muted-foreground mt-1">
                    {pct.toFixed(0)}% spent
                  </p>
                </CardContent>
              </Card>
            );
          })}
        </div>
      ) : (
        <div className="text-center py-12 text-muted-foreground">
          <DollarSign className="h-12 w-12 mx-auto mb-3 opacity-30" />
          <p>No budgets yet. Create one to start tracking expenses.</p>
        </div>
      )}
    </div>
  );
}
