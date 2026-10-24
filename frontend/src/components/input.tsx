"use client";

import { useId, type ComponentPropsWithRef } from "react";
import { twMerge } from "tailwind-merge";

type InputProps = ComponentPropsWithRef<"input"> & {
  label: string;
  error?: string;
};

export function Input({ label, error, className, ...props }: InputProps) {
  const errorId = useId();

  return (
    <label className={twMerge("flex flex-col gap-2 text-sm", className)}>
      {label}
      <input
        {...props}
        aria-invalid={error ? true : props["aria-invalid"]}
        aria-describedby={error ? errorId : props["aria-describedby"]}
        className="min-h-12 w-full rounded-lg border border-divider px-3 focus-visible:outline-2 focus-visible:outline-brand"
      />
      {error && <span id={errorId} className="text-red-700">{error}</span>}
    </label>
  );
}
