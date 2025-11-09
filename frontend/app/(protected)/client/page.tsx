"use client";

import clsx from "clsx";
import React, { useEffect, useState } from "react";
import Chart from "./Chart";

type Category = { id: number; name: string; organizationId?: number };
export type InvDto = {
    id: number;
    organizationId: number;
    productId: number;
    productName: string;
    quantity: number;
    unitPrice: number;
    basePrice: number;
    updatedAt: string;
};

const money = (n: number) =>
    new Intl.NumberFormat("et-EE", { style: "currency", currency: "EUR" }).format(n);

export default function ClientProductsByCategory() {
    const [cats, setCats] = useState<Category[]>([]);
    const [groups, setGroups] = useState<Record<string, InvDto[]>>({});
    const [loading, setLoading] = useState(false);
    const [err, setErr] = useState<string | null>(null);

    useEffect(() => {
        let alive = true;

        const load = async () => {
            console.log('Loading products...');
            setLoading(true);
            try {
                // 1) Load categories
                const cRes = await fetch("/api/backend/categories", {
                    cache: "no-store",
                    credentials: "include",
                });
                if (!cRes.ok) throw new Error(`Categories HTTP ${cRes.status}`);
                const cJson = await cRes.json();
                const categoryList: Category[] = Array.isArray(cJson)
                    ? cJson
                    : cJson?.items ?? cJson?.content ?? [];

                if (!alive) return;
                setCats(categoryList);

                // 2) Fetch inventory per category
                const fetches = categoryList.map(async (c) => {
                    const res = await fetch(`/api/backend/inventory?categoryId=${c.id}`, {
                        cache: "no-store",
                        credentials: "include",
                    });
                    if (!res.ok) throw new Error(`Inventory HTTP ${res.status} (cat ${c.id})`);
                    const j = await res.json();
                    const arr: InvDto[] = Array.isArray(j) ? j : j?.items ?? j?.content ?? [];
                    return [c.name, arr] as const;
                });

                const results = await Promise.all(fetches);
                if (!alive) return;

                // Only keep categories that actually have products
                const grouped = Object.fromEntries(results.filter(([, arr]) => arr.length > 0));
                setGroups(grouped);
                setErr(null);
            } catch (e: any) {
                if (!alive) return;
                setErr(e?.message || "Failed to load products");
            } finally {
                setLoading(false);
            }
        };

        load();
        const refreshInterval = setInterval(load, 1000 * 15);
        return () => {
            clearInterval(refreshInterval);
            alive = false;
        };
    }, []);

    return (
        <div className="min-h-screen w-full bg-[#141224] text-white p-6">
            <div className="max-w-[1400px] mx-auto grid grid-cols-12 gap-6">
                <div className="col-span-12 text-center">
                    <h1 className="text-4xl md:text-5xl font-black tracking-widest select-none">
                        TUDENGIBAAR
                    </h1>
                    <p className="text-sm text-[#a7a3c7] mt-1">Product list by category</p>
                </div>

                <div className="col-span-12">
                    <div className="rounded-2xl bg-[#1b1830] border border-[#2a2640] p-4 md:p-5 shadow-[0_10px_30px_rgba(0,0,0,0.35)]">
                        {err && (
                            <div className="mb-4 text-sm text-red-200 bg-red-900/40 border border-red-800 rounded-lg px-3 py-2">
                                {err}
                            </div>
                        )}

                        {(loading && !cats?.length) ? (
                            <div className="h-64 grid place-items-center">
                                <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-[#7c6cff]" />
                            </div>
                        ) : (
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                                {cats
                                    .filter((c) => groups[c.name]?.length)
                                    .map((c) => {
                                        const items = groups[c.name];
                                        return (
                                            <div
                                                key={c.id}
                                                className="rounded-xl bg-[#201c31] border border-[#2a2640] p-4"
                                            >
                                                <div className="text-[#e9e6ff] font-extrabold tracking-wider text-lg mb-3">
                                                    {c.name.toUpperCase()}
                                                </div>
                                                <ul className={clsx("space-y-2")}>
                                                    {items
                                                        .slice()
                                                        .sort((a, b) => a.productName.localeCompare(b.productName))
                                                        .map((p) => (
                                                            <li
                                                                key={`${c.id}-${p.productId}`}
                                                                className="flex items-center justify-between rounded-lg bg-[#251f3a] border border-[#2c2944] px-4 py-2"
                                                            >
                                                                <div className="flex items-center gap-3 min-w-0">
                                                                    <span
                                                                        className="w-1 h-5 rounded-full bg-[#fe8f66]"
                                                                        aria-hidden
                                                                    />
                                                                    <span className="font-medium text-[#f1efff] truncate">
                                                                        {p.productName}
                                                                    </span>
                                                                </div>
                                                                <div className={clsx("flex items-center gap-4 shrink-0", {
                                                                    'transition-all duration-300': true,
                                                                    'pointer-events-none blur-[2px]': loading,
                                                                    'blur-none': !loading
                                                                })}>
                                                                    {/* Price */}
                                                                    <span className="text-sm font-semibold text-[#f1efff] tabular-nums">
                                                                        {money(Number(p.unitPrice))}
                                                                    </span>

                                                                    <span className={clsx("text-xs font-semibold tabular-nums w-[55px]", {
                                                                        'text-green-400 animate-bounce': p.unitPrice < p.basePrice,
                                                                        'text-red-400 before:content-["+"] animate-pulse': p.unitPrice > p.basePrice,
                                                                        'text-white/20 before:content-["+"]': p.unitPrice == p.basePrice,
                                                                    })}>
                                                                        {money(Number(p.unitPrice - p.basePrice))}
                                                                    </span>
                                                                </div>
                                                            </li>
                                                        ))}
                                                </ul>
                                            </div>
                                        );
                                    })}

                                <Chart groups={groups}/>

                                {Object.keys(groups).length === 0 && (
                                    <div className="text-center text-[#b7b4c7] py-10">
                                        No products to display
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}
