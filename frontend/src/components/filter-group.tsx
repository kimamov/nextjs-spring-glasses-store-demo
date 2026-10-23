"use client";

type FilterGroupProps = {
  name: string;
  label: string;
  options: readonly { value: string; label: string }[];
} & (
  | { type: "radio"; value: string; onChange: (value: string) => void }
  | { type: "checkbox"; value: string[]; onChange: (value: string[]) => void }
);

export function FilterGroup(props: FilterGroupProps) {
  return (
    <fieldset>
      <legend className="mb-2 font-semibold">{props.label}</legend>
      <div className="flex flex-col">
        {props.options.map((option) => (
          <label key={option.value} className="flex min-h-10 cursor-pointer items-center gap-3 text-sm">
            <input
              type={props.type}
              name={props.name}
              value={option.value}
              checked={props.type === "radio" ? props.value === option.value : props.value.includes(option.value)}
              className="size-4 accent-brand"
              onChange={(event) => {
                if (props.type === "radio") props.onChange(option.value);
                else props.onChange(event.target.checked
                  ? [...props.value, option.value]
                  : props.value.filter((value) => value !== option.value));
              }}
            />
            {option.label}
          </label>
        ))}
      </div>
    </fieldset>
  );
}
