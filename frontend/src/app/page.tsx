import Image from "next/image";
import { Button } from "@/components/button";

const frameTips = [
  {
    number: "01",
    title: "Find your shape",
    description:
      "Round, square, or somewhere in between. Explore silhouettes that feel like you.",
  },
  {
    number: "02",
    title: "Make it your color",
    description:
      "Keep it classic with black and tortoiseshell, or bring a little color to your everyday.",
  },
  {
    number: "03",
    title: "Check the details",
    description:
      "Compare frame styles, colors, and prices to find a pair that fits your wish list.",
  },
];

export default function Home() {
  return (
    <>
      <section
        aria-labelledby="hero-heading"
        className="grid overflow-hidden rounded-panel bg-surface-warm md:grid-cols-2"
      >
        <div className="flex flex-col items-start justify-center px-7 py-12 sm:px-10 sm:py-16 lg:px-12">
          <p className="text-sm font-semibold uppercase tracking-eyebrow text-brand">
            A fresh perspective
          </p>
          <h1
            id="hero-heading"
            className="mt-6 text-display font-semibold sm:text-display-lg"
          >
            See the world.
            <br />
            Be yourself.
          </h1>
          <p className="mt-6 max-w-sm text-base leading-7 text-muted">
            A new outlook starts with the right pair. Discover everyday glasses
            with thoughtful shapes and plenty of personality.
          </p>
          <Button
            href="/glasses"
            className="mt-8"
          >
            Find your glasses <span aria-hidden="true">↗</span>
          </Button>
        </div>
        <div className="relative min-h-72 md:min-h-[520px]">
          <Image
            src="/images/products/brille-02-havanna-bild-01.png"
            alt="AI-generated round tortoiseshell glasses, front view"
            fill
            priority
            sizes="(min-width: 1152px) 528px, (min-width: 768px) 50vw, 100vw"
            className="object-contain"
          />
          <p className="absolute right-5 bottom-5 left-5 rounded-lg bg-surface px-4 py-3 text-sm font-medium text-brand">
            A little detail. A whole new look.
          </p>
        </div>
      </section>

      <section aria-labelledby="frame-guide-heading" className="py-20 sm:py-24">
        <div className="max-w-xl">
          <p className="text-sm font-semibold uppercase tracking-eyebrow text-brand">
            Your frame, your way
          </p>
          <h2
            id="frame-guide-heading"
            className="mt-4 text-section font-semibold sm:text-section-lg"
          >
            Good style starts with you.
          </h2>
          <p className="mt-4 leading-7 text-muted">
            Your glasses are part of your everyday. Here are three simple things
            to consider when choosing your next pair.
          </p>
        </div>
        <div className="mt-10 grid gap-8 sm:grid-cols-3 sm:gap-10">
          {frameTips.map((tip) => (
            <div key={tip.number} className="border-t border-divider pt-5">
              <span className="text-xs font-semibold tracking-wider text-accent">
                {tip.number}
              </span>
              <h3 className="mt-5 text-xl font-semibold tracking-tight">
                {tip.title}
              </h3>
              <p className="mt-3 text-sm leading-6 text-muted">
                {tip.description}
              </p>
            </div>
          ))}
        </div>
      </section>

      <section
        aria-labelledby="collection-heading"
        className="grid gap-8 rounded-panel bg-surface-sage px-7 py-10 sm:px-10 sm:py-12 md:grid-cols-2 md:items-center md:gap-16"
      >
        <div>
          <p className="text-sm font-semibold uppercase tracking-eyebrow text-brand">
            Meet the collection
          </p>
          <h2
            id="collection-heading"
            className="mt-4 text-section font-semibold sm:text-section-lg"
          >
            Everyday frames.
            <br />
            Anything but ordinary.
          </h2>
        </div>
        <div>
          <p className="leading-7 text-muted">
            From understated favorites to statement shapes, explore a small
            collection with a fresh point of view. Each frame has its own
            character. Find the one that brings out yours.
          </p>
          <Button
            href="/glasses"
            variant="secondary"
            className="mt-6"
          >
            Explore the collection <span aria-hidden="true">→</span>
          </Button>
        </div>
      </section>
    </>
  );
}
