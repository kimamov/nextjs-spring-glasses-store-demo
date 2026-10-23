import Link from "next/link";
import type { ComponentPropsWithRef } from "react";
import { twMerge } from "tailwind-merge";

type SharedProps = {
  variant?: "primary" | "secondary";
  className?: string;
};

type NativeButtonProps = ComponentPropsWithRef<"button"> & {
  href?: never;
};

type LinkButtonProps = ComponentPropsWithRef<typeof Link>;

export type ButtonProps = SharedProps & (NativeButtonProps | LinkButtonProps);

const baseStyles =
  "inline-flex min-h-12 items-center justify-center gap-5 rounded-full border px-7 py-3 text-sm font-semibold motion-safe:transition-colors focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand";

const variantStyles = {
  primary:
    "border-transparent bg-brand text-on-brand enabled:hover:bg-brand-hover [&:any-link:hover]:bg-brand-hover",
  secondary:
    "border-brand bg-transparent text-brand enabled:hover:bg-brand enabled:hover:text-on-brand [&:any-link:hover]:bg-brand [&:any-link:hover]:text-on-brand",
};

/** Pass href for a Next.js link; otherwise accepts all native button props. */
export function Button({ variant = "primary", className, ...props }: ButtonProps) {
  const styles = twMerge(
    baseStyles,
    variantStyles[variant],
    props.href === undefined && "disabled:cursor-not-allowed disabled:opacity-50",
    className,
  );

  if (props.href !== undefined) {
    return <Link {...props} className={styles} />;
  }

  const { type = "button", ...buttonProps } = props;
  return (
    <button
      {...buttonProps}
      type={type}
      className={styles}
    />
  );
}
