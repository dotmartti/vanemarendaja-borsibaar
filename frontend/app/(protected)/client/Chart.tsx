"use client";

import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { InvDto } from "./page";

type HistoryDto = {
  id: number;
  inventoryId: number;
  priceBefore: number;
  priceAfter: number;
  createdAt: string;
};
type CurrentHistory = {
  productInv: InvDto;
  priceHistory: HistoryDto[];
};

export default function PriceHistoryData({ groups }: { groups: Record<string, InvDto[]> }) {
  const [currentHistory, setCurrentHistory] = useState<CurrentHistory | null>(null);

  // Keep product rotation stable across reloads
  const groupsRef = useRef<Record<string, InvDto[]>>({});
  useEffect(() => { groupsRef.current = groups; }, [groups]);
  const activeIdxRef = useRef<number>(0);
  const activeProductRef = useRef<InvDto | null>(null);

  const flatten = (g: Record<string, InvDto[]>) => {
    const catNames = Object.keys(g).sort((a, b) => a.localeCompare(b));
    return catNames.flatMap((name) =>
      [...(g[name] ?? [])].sort((a, b) => a.productId - b.productId)
    );
  };

  const loadPriceHistory = useCallback(async (productInv: InvDto) => {
    if (!productInv) return;
    try {
      const historyRes = await fetch(
        `/api/backend/inventory/product/${productInv.productId}/history`,
        { cache: "no-store", credentials: "include" }
      );
      if (!historyRes.ok) throw new Error(`Price history HTTP ${historyRes.status}`);
      const historyJson: HistoryDto[] = await historyRes.json();
      setCurrentHistory({ productInv, priceHistory: historyJson });
    } catch (e) {
      console.error(e);
    }
  }, []);

  const rotateOnce = useCallback(() => {
    const flat = flatten(groupsRef.current);
    if (flat.length === 0) return;
    const current = activeProductRef.current;
    let nextIdx: number;
    if (current) {
      const idxNow = flat.findIndex((p) => p.productId === current.productId);
      nextIdx = idxNow >= 0 ? (idxNow + 1) % flat.length : activeIdxRef.current % flat.length;
    } else {
      nextIdx = activeIdxRef.current % flat.length;
    }
    const next = flat[nextIdx];
    activeIdxRef.current = nextIdx;
    activeProductRef.current = next;
    loadPriceHistory(next);
  }, [loadPriceHistory]);

  useEffect(() => {
    if (!activeProductRef.current) {
      const flat = flatten(groupsRef.current);
      if (flat.length > 0) {
        activeIdxRef.current = activeIdxRef.current % flat.length;
        const initial = flat[activeIdxRef.current];
        activeProductRef.current = initial;
        loadPriceHistory(initial);
      }
    }
    const id = setInterval(rotateOnce, 5000);
    return () => clearInterval(id);
  }, [rotateOnce, loadPriceHistory]);

  // -------- helpers / formatting --------
  const money = useMemo(
    () => new Intl.NumberFormat("et-EE", { style: "currency", currency: "EUR" }),
    []
  );
  const fmtTallinn = useMemo(() => {
    const f = new Intl.DateTimeFormat("et-EE", {
      timeZone: "Europe/Tallinn",
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
    return (s: string) => f.format(new Date(s));
  }, []);

  // Sort newest first; compute current price and deltas
  const view = useMemo(() => {
    if (!currentHistory) return null;

    const base = Number(
      (currentHistory.productInv as any)?.basePrice ??
      currentHistory.productInv?.unitPrice ??
      0
    );

    const rows = [...(currentHistory.priceHistory ?? [])]
      .map(h => ({
        ...h,
        priceBefore: Number(h.priceBefore),
        priceAfter: Number(h.priceAfter),
        createdAt: h.createdAt,
      }))
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

    const currentPrice = rows.length ? rows[0].priceAfter : base;

    return {
      basePrice: base,
      unitPrice: currentHistory.productInv?.unitPrice ?? null,
      currentPrice,
      rows
    };
  }, [currentHistory]);

  // -------- UI --------
  if (!view || !currentHistory) {
    return (
      <div className="w-full rounded-2xl border border-neutral-800 p-4 text-sm text-neutral-300">
        Laadin...
      </div>
    );
  }

  const { productInv } = currentHistory;

  return (
    <div className="w-full rounded-2xl border border-neutral-800 p-4">
      <div className="mb-3 flex items-center justify-between">
        <div className="text-lg font-semibold text-neutral-100">
          {productInv.productName} — Price history (data view)
        </div>
        <div className="text-xs text-neutral-400">
          Product ID: {productInv.productId}
        </div>
      </div>

      <div className="grid grid-cols-1 gap-2 md:grid-cols-3 mb-4">
        <div className="rounded-lg bg-neutral-900/40 p-3">
          <div className="text-xs text-neutral-400">Base price</div>
          <div className="text-base text-neutral-100">{money.format(view.basePrice)}</div>
        </div>
        <div className="rounded-lg bg-neutral-900/40 p-3">
          <div className="text-xs text-neutral-400">Unit price</div>
          <div className="text-base text-neutral-100">
            {view.unitPrice != null ? money.format(Number(view.unitPrice)) : "—"}
          </div>
        </div>
        <div className="rounded-lg bg-neutral-900/40 p-3">
          <div className="text-xs text-neutral-400">Current price</div>
          <div className="text-base text-neutral-100">{money.format(view.currentPrice)}</div>
        </div>
      </div>

      <div className="overflow-x-auto rounded-lg border border-neutral-800">
        <table className="min-w-full text-sm">
          <thead className="bg-neutral-900/60 text-neutral-300">
            <tr>
              <th className="px-3 py-2 text-left font-medium">Tallinn time</th>
              <th className="px-3 py-2 text-right font-medium">Before</th>
              <th className="px-3 py-2 text-right font-medium">After</th>
              <th className="px-3 py-2 text-right font-medium">Δ</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-neutral-800">
            {view.rows.map((r) => {
              const delta = r.priceAfter - r.priceBefore;
              const sign = delta > 0 ? "+" : delta < 0 ? "–" : "";
              const abs = Math.abs(delta);
              return (
                <tr key={r.id} className="hover:bg-neutral-900/40">
                  <td className="px-3 py-2 text-neutral-200 whitespace-nowrap">
                    {fmtTallinn(r.createdAt)}
                  </td>
                  <td className="px-3 py-2 text-right text-neutral-300">
                    {money.format(r.priceBefore)}
                  </td>
                  <td className="px-3 py-2 text-right text-neutral-100 font-medium">
                    {money.format(r.priceAfter)}
                  </td>
                  <td
                    className={`px-3 py-2 text-right ${
                      delta > 0 ? "text-emerald-400" : delta < 0 ? "text-rose-400" : "text-neutral-400"
                    }`}
                  >
                    {sign}{money.format(abs)}
                  </td>
                </tr>
              );
            })}
            {view.rows.length === 0 && (
              <tr>
                <td className="px-3 py-3 text-neutral-400" colSpan={4}>
                  No price changes recorded.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="mt-3 text-xs text-neutral-500">
        Times are shown in <span className="font-medium">Europe/Tallinn</span>. Newest first.
      </div>
    </div>
  );
}
