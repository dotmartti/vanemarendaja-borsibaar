"use client";

import { ReactNode } from "react";
import { usePathname } from "next/navigation";
import {
  SidebarProvider,
  SidebarTrigger,
} from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/sidebar";

// Protected layout wraps pages that require auth.
// Authentication and onboarding flow are handled by middleware.
export default function ProtectedLayout({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  // Hide sidebar completely on POS ID pages (e.g., /pos/123)
  const isPOSIDPage = pathname?.match(/^\/pos\/[^/]+$/);

  return (
    <SidebarProvider>
      {!isPOSIDPage && <AppSidebar />}
        <main className="w-full">
            {!isPOSIDPage && <SidebarTrigger className="ml-4 mt-4" />}
            {children}
        </main>
    </SidebarProvider>
  );
}
